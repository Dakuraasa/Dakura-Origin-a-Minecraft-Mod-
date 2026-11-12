"""
Gerador dos modelos 3D do Datura Origins (Etapas 1 a 3 + textura).

Para cada peça cria, a partir das referências:
  - assets/dakura_origins/geo/<peca>.geo.json          (Bedrock Geometry 1.12.0, abre no Blockbench)
  - assets/dakura_origins/animations/<peca>.animation.json  (idle / walk / attack para o Blockbench)
  - assets/dakura_origins/textures/entity/geo/<peca>[_variante].png  (textura com o mesmo layout UV)
  - ferramentas/modelos/PLANEJAMENTO.md                 (hierarquia de ossos, pivôs, cubos, escala)

Uso:  python gerar_modelos.py      (precisa de Pillow: pip install pillow)

Convenção: as peças são escritas em "espaço Java" (y para baixo, relativo ao pivô do osso pai,
igual ao ModelPartBuilder) e convertidas para coordenadas Bedrock absolutas no arquivo
(y para cima, chão em 0, pescoço em 24). O carregador do mod (GeoModel.java) faz a volta.
Ossos em inglês, snake_case. Só cubos e rotações simples.
"""
import json
import math
import os
import random

from PIL import Image

AQUI = os.path.dirname(os.path.abspath(__file__))
RAIZ = os.path.normpath(os.path.join(AQUI, '..', '..', 'projeto', 'src', 'main', 'resources', 'assets', 'dakura_origins'))
GEO_DIR = os.path.join(RAIZ, 'geo')
ANIM_DIR = os.path.join(RAIZ, 'animations')
TEX_DIR = os.path.join(RAIZ, 'textures', 'entity', 'geo')


# ====================================================================== modelo

class Bone:
    def __init__(self, name, parent=None, pivot=(0, 0, 0), rot=(0, 0, 0)):
        self.name, self.parent, self.pivot, self.rot = name, parent, pivot, rot
        self.cubes = []      # (from, size, mat, uvkey, mirror, inflate)
        self.children = []
        if parent:
            parent.children.append(self)

    def cube(self, f, s, mat, uvkey=None, mirror=False, inflate=0):
        self.cubes.append((f, s, mat, uvkey, mirror, inflate))
        return self

    def child(self, name, pivot=(0, 0, 0), rot=(0, 0, 0)):
        return Bone(name, self, pivot, rot)

    def abs_pivot(self):
        if self.parent is None:
            return self.pivot
        p = self.parent.abs_pivot()
        return tuple(p[i] + self.pivot[i] for i in range(3))

    def walk(self):
        yield self
        for c in self.children:
            yield from c.walk()


class Model:
    def __init__(self, name, attach, tex=128, desc=''):
        self.name, self.attach, self.tex, self.desc = name, attach, tex, desc
        self.roots = []
        self.uv = {}

    def root(self, name, pivot=(0, 0, 0), rot=(0, 0, 0)):
        b = Bone(name, None, pivot, rot)
        self.roots.append(b)
        return b

    def bones(self):
        for r in self.roots:
            yield from r.walk()


def uvsize(s):
    w, h, d = (int(math.ceil(v)) for v in s)
    return 2 * (d + w), d + h


def pack(model):
    """Empacota as caixas de UV (prateleiras). Cubos com a mesma uvkey dividem a região."""
    x = y = shelf = 0
    W = H = model.tex
    for b in model.bones():
        for (f, s, mat, key, mir, inf) in b.cubes:
            k = key or (b.name, f, s)
            if k in model.uv:
                continue
            w, h = uvsize(s)
            if x + w > W:
                x, y, shelf = 0, y + shelf, 0
            if y + h > H:
                raise SystemExit(f'{model.name}: textura {W} pequena demais')
            model.uv[k] = (x, y, s, mat)
            x += w
            shelf = max(shelf, h)


def to_geo(model):
    bones = []
    for b in model.bones():
        jp = b.abs_pivot()
        e = {'name': b.name, 'pivot': [r(jp[0]), r(24 - jp[1]), r(jp[2])]}
        if b.parent:
            e['parent'] = b.parent.name
        if any(b.rot):
            e['rotation'] = [r(math.degrees(v)) for v in b.rot]
        cubes = []
        for (f, s, mat, key, mir, inf) in b.cubes:
            k = key or (b.name, f, s)
            u, v = model.uv[k][0], model.uv[k][1]
            ax, ay, az = jp[0] + f[0], jp[1] + f[1], jp[2] + f[2]
            c = {'origin': [r(ax), r(24 - (ay + s[1])), r(az)], 'size': [r(s[0]), r(s[1]), r(s[2])], 'uv': [u, v]}
            if inf:
                c['inflate'] = inf
            if mir:
                c['mirror'] = True
            cubes.append(c)
        if cubes:
            e['cubes'] = cubes
        bones.append(e)
    return {'format_version': '1.12.0', 'minecraft:geometry': [{
        'description': {'identifier': 'geometry.dakura_origins.' + model.name, 'texture_width': model.tex,
                        'texture_height': model.tex, 'visible_bounds_width': 6, 'visible_bounds_height': 5,
                        'visible_bounds_offset': [0, 1.5, 0]},
        'bones': bones}]}


def r(v):
    return round(v, 3) + 0.0


# ====================================================================== textura

MATS = {}


def mat(name, cols, pattern='noise', **kw):
    MATS[name] = (cols, pattern, kw)


def paint(model, variant=None, overrides=None):
    rnd = random.Random(model.name)
    img = Image.new('RGBA', (model.tex, model.tex), (0, 0, 0, 0))
    px = img.load()
    for (x0, y0, s, matname) in model.uv.values():
        if overrides and matname in overrides:
            matname = overrides[matname]
        cols, pattern, kw = MATS[matname]
        w, h, d = (int(math.ceil(v)) for v in s)
        faces = [  # (x, y, largura, altura, face)
            (x0 + d, y0, w, d, 'top'), (x0 + d + w, y0, w, d, 'bottom'),
            (x0, y0 + d, d, h, 'side'), (x0 + d, y0 + d, w, h, 'front'),
            (x0 + d + w, y0 + d, d, h, 'side'), (x0 + 2 * d + w, y0 + d, w, h, 'back')]
        for (fx, fy, fw, fh, face) in faces:
            for j in range(fh):
                for i in range(fw):
                    c = shade(cols, pattern, kw, i, j, fw, fh, face, rnd)
                    if c is not None:
                        px[fx + i, fy + j] = c
    return img


def lerp(a, b, t):
    return tuple(int(a[k] + (b[k] - a[k]) * t) for k in range(len(a)))


def shade(cols, pattern, kw, i, j, fw, fh, face, rnd):
    base = cols[rnd.randrange(min(2, len(cols)))]
    if pattern == 'noise':
        c = cols[rnd.randrange(len(cols))]
    elif pattern == 'grad':        # do primeiro ao último tom ao longo da altura
        t = j / max(1, fh - 1)
        c = lerp(cols[0], cols[-1], t)
        c = tuple(max(0, min(255, v + rnd.randint(-6, 6))) for v in c)
    elif pattern == 'feather':     # pena branca com chevrons bege e ponta escura
        chev = kw['chev']
        t = j / max(1, fh - 1)
        zig = (j + abs(i - fw / 2)) % 4 == 0
        if t > 0.86:
            c = kw['tip']
        elif zig and face in ('front', 'back'):
            c = chev[rnd.randrange(len(chev))]
        else:
            c = base
    elif pattern == 'scale':       # escamas em meia-lua
        row = j // 2
        off = 1 if row % 2 else 0
        edge = ((i + off) % 3 == 0) or (j % 2 == 1 and (i + off) % 3 == 1)
        c = cols[2] if edge else base
        if len(cols) > 3 and face == 'bottom':
            c = cols[3]
    elif pattern == 'spots':       # chapéu vermelho com pintas creme
        spot = kw['spot']
        h = (i * 7 + j * 13 + (i // 3) * 5) % 17
        c = spot[rnd.randrange(len(spot))] if h < 2 and face in ('top', 'front', 'side', 'back') else base
    elif pattern == 'membrane':    # membrana com nervuras e bordas mais escuras
        vein = kw['vein']
        if i % 5 == 0 or j == fh - 1:
            c = vein
        else:
            c = cols[rnd.randrange(len(cols))]
    elif pattern == 'glow':        # núcleo de magma: crostas escuras e rachaduras brilhantes
        crack = kw['crack']
        c = crack if (i * 3 + j * 5) % 7 == 0 or (i == j) else cols[rnd.randrange(len(cols))]
    else:
        c = base
    if len(c) == 3:
        c = c + (255,)
    return c


# ====================================================================== peças

def feather_wing(parent, side, tag, palette, scale=1.0, arm_len=10, fore_len=9, primaries=(10, 11, 12, 13, 12),
                 secondaries=(8, 8, 9, 9), mirror_uv=True):
    """Asa de penas: braço + antebraço + penas (cada pena é um osso, para poder abrir em leque)."""
    s = side
    m = mirror_uv and s < 0
    al, fl = int(arm_len * scale), int(fore_len * scale)
    arm = parent.child(f'{tag}_arm')
    arm.cube((0 if s > 0 else -al, -1, -1), (al, 2, 2), palette + '_bone', f'{palette}_arm{al}', m)
    arm.cube((0 if s > 0 else -al, 1, -0.5), (al, 3, 1), palette + '_covert', f'{palette}_cov{al}', m)
    for k, L in enumerate(secondaries):
        L = int(L * scale)
        f = arm.child(f'{tag}_sec_{k}', (s * (1.5 + k * (al - 2) / max(1, len(secondaries) - 1)), 1, 0), (0, 0, s * 0.04 * k))
        f.cube((-1, 0, -0.5), (2, L, 1), palette + '_feather', f'{palette}_f{L}', m)
    fore = arm.child(f'{tag}_fore', (s * al, 0, 0), (0, 0, 0))
    fore.cube((0 if s > 0 else -fl, -1, -0.5), (fl, 2, 1), palette + '_bone', f'{palette}_fore{fl}', m)
    for k, L in enumerate(primaries):
        L = int(L * scale)
        f = fore.child(f'{tag}_pri_{k}', (s * (1 + k * (fl - 1) / max(1, len(primaries) - 1)), 0.5, 0), (0, 0, -s * (0.1 + 0.13 * k)))
        f.cube((-1, 0, -0.5), (2, L, 1), palette + '_feather', f'{palette}_f{L}', m)
        f.cube((-0.5, L, -0.5), (1, 2, 1), palette + '_tip', f'{palette}_tip', m)
    return arm


def seraph_wings():
    m = Model('seraph_wings', 'body', 128, 'Seis asas de penas brancas com chevrons bege (referência 1), três pares nas costas.')
    root = m.root('wings', (0, 0, 2.6))
    for pair in range(3):
        sc = (1.0, 0.85, 0.72)[pair]
        for s, sn in ((1, 'l'), (-1, 'r')):
            w = root.child(f'wing_{pair}_{sn}', (s * 1.5, 1.0 + pair * 3.2, 0))
            feather_wing(w, s, f'wing_{pair}_{sn}', 'angel', sc, 13, 13, (16, 18, 20, 22, 21), (12, 13, 14, 15))
    return m


def demon_horns():
    m = Model('demon_horns', 'head', 64, 'Chifres vermelhos curvos com chamas na ponta (referência 2), Demônio e Súcubo.')
    root = m.root('horns', (0, 0, 0))
    for s, sn in ((1, 'l'), (-1, 'r')):
        cur = root.child(f'horn_{sn}', (s * 3.2, -7, -0.5), (0.1, -s * 0.1, -s * 0.15))
        sizes = [(3, 3), (3, 3), (2, 3), (2, 2), (1, 2)]
        for k, (t, L) in enumerate(sizes):
            if k:
                cur = cur.child(f'horn_{sn}_{k}', (s * (sizes[k - 1][1] - 0.4), 0, 0), (0, -s * 0.12, -s * 0.32))
            cur.cube((0 if s > 0 else -L, -t / 2, -t / 2), (L, t, t), 'horn_dark' if k == 0 else ('horn' if k < 3 else 'horn_hi'),
                     f'h{k}', s < 0)
        fl = cur.child(f'flame_{sn}', (s * 2, 0, 0), (0, 0, -s * 0.4))
        fl.cube((0 if s > 0 else -1, -2, -0.5), (1, 2, 1), 'flame', 'fl0', s < 0)
        fl2 = fl.child(f'flame_{sn}_tip', (s * 0.5, -2, 0), (0, 0, s * 0.5))
        fl2.cube((0 if s > 0 else -1, -2, -0.5), (1, 2, 1), 'flame_hi', 'fl1', s < 0)
        wisp = root.child(f'wisp_{sn}', (s * 4.2, -5, -1), (0, 0, -s * 0.9))
        wisp.cube((0 if s > 0 else -2, -0.5, -0.5), (2, 1, 1), 'horn', 'w0', s < 0)
        wisp.child(f'wisp_{sn}_2', (s * 1.6, 0, 0), (0, 0, -s * 0.8)).cube((0 if s > 0 else -2, -0.5, -0.5), (2, 1, 1), 'flame', 'w1', s < 0)
    return m


def mushroom_cap():
    m = Model('mushroom_cap', 'head', 256, 'Chapéu de cogumelo gigante vermelho com pintas creme e cabelo laranja (referência 3).')
    cap = m.root('cap', (0, -8, 0))
    cap.cube((-13, -1, -13), (26, 1, 26), 'gills')
    cap.cube((-14, -5, -14), (28, 4, 28), 'cap')
    cap.cube((-11, -7, -11), (22, 2, 22), 'cap')
    cap.cube((-6, -8, -6), (12, 1, 12), 'cap_top')
    hair = m.root('hair', (0, 0, 0))
    hair.child('hair_bangs').cube((-4.5, -8.5, -4.6), (9, 3, 1), 'hair')
    for s, sn in ((1, 'l'), (-1, 'r')):
        hl = hair.child(f'hair_{sn}', (s * 4.5, -7, 0), (0, 0, -s * 0.05))
        hl.cube((-1, -1, -4), (2, 11, 8), 'hair', f'side', s < 0)
    hair.child('hair_back', (0, -7, 4.5)).cube((-4.5, -1, -1), (9, 11, 2), 'hair')
    return m


def mermaid_tail():
    m = Model('mermaid_tail', 'body', 256, 'Cauda grande de sereia em gomos que curvam para trás, barbatanas laterais e '
                                         'barbatana final com raios longos (referência 4). Duas texturas: viva e marinha.')
    cur = m.root('tail', (0, 10.5, 0))
    sizes = [(8, 5), (8, 5), (7, 5), (7, 4), (6, 4), (5, 3), (4, 3), (3, 2)]
    rest = [0, 0.06, 0.12, 0.2, 0.3, 0.38, 0.45, 0.5]
    for k, (w, d) in enumerate(sizes):
        cur = cur.child(f'tail_{k}', (0, 0 if k == 0 else 4, 0), (rest[k], 0, 0))
        cur.cube((-w / 2, 0, -d / 2), (w, 4, d), 'scale')
        cur.cube((-w / 2 - 0.5, 1.5, -d / 2 - 0.5), (w + 1, 1, d + 1), 'scale_belt')
        if k == 0:
            for s, sn in ((1, 'l'), (-1, 'r')):
                fin = cur.child(f'hip_fin_{sn}', (s * 4, 1, 0), (0, 0, -s * 0.5))
                for j in range(3):
                    fin.child(f'hip_fin_{sn}_{j}', (0, 0, 0), (0.3 * j, 0, -s * 0.25 * j)).cube(
                        (0 if s > 0 else -1, 0, -0.5), (1, 7 - j, 1), 'fin_ray', f'hr{j}', s < 0)
    fluke = cur.child('fluke', (0, 3.5, 0), (0.35, 0, 0))
    fluke.cube((-7, 0, 0), (14, 9, 0), 'fin')
    for j in range(7):
        a = (j - 3) * 0.33
        L = 16 - abs(j - 3) * 2
        ray = fluke.child(f'fluke_ray_{j}', (0, 0, 0), (0, 0, a))
        ray.cube((-1, 0, -0.5), (2, L, 1), 'fin_ray', f'ray{L}')
        ray.cube((-0.5, L, -0.5), (1, 3, 1), 'fin', f'raytip')
    return m


def bird_wing():
    m = Model('bird_wing', 'body', 128, 'Uma asa de ave de rapina (referência: penas marrons e oliva, coberteiras azul-acinzentadas, '
                                      'pontas azul-ardósia), presa só de um lado das costas. Abre como um élitro para planar.')
    root = m.root('wing', (1.2, 1.0, 2.4))
    root.cube((-1, 0, -1), (2, 9, 2), 'eagle_bone')
    root.cube((-4, 0.5, -0.6), (4, 8, 1), 'eagle_covert')
    for i in range(6):
        L = 12 - i
        root.child(f'c{i}', (-1.0 - i * 0.7, 1 + i * 1.4, 0), (0, 0, 0.05 * i)).cube((-1, 0, -0.5), (2, L, 1),
                                                                                   'eagle' if i % 2 == 0 else 'eagle_olive', f'c{L}')
    hand = root.child('hand', (0, 8.6, 0))
    hand.cube((-0.5, 0, -0.5), (1, 9, 1), 'eagle_bone')
    hand.cube((-3, 0.5, -0.6), (3, 6, 1), 'eagle_covert', 'hcov')
    for i in range(8):
        L = 17 - i
        f = hand.child(f'p{i}', (-0.5 - i * 0.45, 0.4 + i * 1.15, 0), (0, 0, 0.11 * i))
        f.cube((-1, 0, -0.5), (2, L, 1), 'eagle' if i % 3 else 'eagle_olive', f'p{L}')
        f.cube((-1, L, -0.5), (2, 3, 1), 'eagle_tip', 'tip')
    return m


def dragon_wings():
    m = Model('dragon_wings', 'body', 256, 'Asas de dragão grandes, erguidas, com dedos de osso e membranas (referência 7). '
                                         'Variantes vermelha e marrom.')
    root = m.root('wings', (0, 1.5, 2.6))
    for s, sn in ((1, 'l'), (-1, 'r')):
        mir = s < 0
        w = root.child(f'wing_{sn}', (s * 2, 0, 0), (0, -s * 0.25, -s * 0.95))
        w.cube((0 if s > 0 else -11, -1, -1), (11, 2, 2), 'dbone', 'arm', mir)
        w.child(f'wing_{sn}_web', (0, 0.5, 0)).cube((0 if s > 0 else -11, 0, 0), (11, 12, 0), 'membrane', 'web0', mir)
        fore = w.child(f'wing_{sn}_fore', (s * 10.5, 0, 0), (0, 0, -s * 0.35))
        fore.cube((0 if s > 0 else -12, -1, -1), (12, 2, 2), 'dbone', 'fore', mir)
        fore.child(f'wing_{sn}_claw', (s * 12, -1, 0), (0, 0, -s * 0.6)).cube((0 if s > 0 else -3, -1, -0.5), (3, 1, 1), 'dclaw', 'claw', mir)
        for k, (L, a, H) in enumerate(((26, -0.25, 11), (28, 0.35, 13), (24, 0.95, 12), (18, 1.55, 9))):
            f = fore.child(f'wing_{sn}_finger_{k}', (s * 11.5, 0, 0), (0, 0, s * a))
            f.cube((0 if s > 0 else -L, -0.5, -0.5), (L, 1, 1), 'dbone', f'fing{L}', mir)
            web = f.child(f'wing_{sn}_web_{k}', (0, 0, 0))
            for j, (a0, a1, hh) in enumerate(((0, 0.5, 1.0), (0.5, 0.8, 0.62), (0.8, 1.0, 0.3))):
                x0, ln = int(L * a0), int(L * (a1 - a0))
                web.cube((x0 if s > 0 else -x0 - ln, 0, 0), (ln, max(1, int(H * hh)), 0), 'membrane', f'web{L}_{j}', mir)
    return m


def rounded(bone, f, s, mat, key, axis='z', inflate=0):
    """Gomo arredondado: duas caixas cruzadas (perfil octogonal) em vez de um bloco quadrado."""
    (x, y, z), (w, h, d) = f, s
    if axis == 'z':
        bone.cube((x + 0.5, y, z), (max(0.5, w - 1), h, d), mat, key + 'a', inflate=inflate)
        bone.cube((x, y + 0.5, z), (w, max(0.5, h - 1), d), mat, key + 'b', inflate=inflate)
    else:  # eixo y
        bone.cube((x + 0.5, y, z), (max(0.5, w - 1), h, d), mat, key + 'a', inflate=inflate)
        bone.cube((x, y, z + 0.5), (w, h, max(0.5, d - 1)), mat, key + 'b', inflate=inflate)


def dragon_tail():
    m = Model('dragon_tail', 'body', 128, 'Cauda longa de dragão, 12 gomos arredondados com escamas esculpidas, anéis escuros, '
                                         'placas de barriga e crista de espinhos; ponta em lança (referência 7). Variantes vermelha e marrom.')
    cur = m.root('tail', (0, 10.5, 1.6), (-0.5, 0, 0))
    sizes = [(7, 6), (7, 6), (6, 5.5), (6, 5), (5, 4.5), (5, 4), (4, 3.5), (4, 3), (3, 2.5), (3, 2), (2, 2), (2, 1.5)]
    pitch = [0, 0.1, 0.1, 0.08, 0.07, 0.06, 0.05, 0.04, 0.02, 0, 0, 0]
    for k, (w, h) in enumerate(sizes):
        cur = cur.child(f'tail_{k}', (0, 0, 0 if k == 0 else 4), (pitch[k], 0, 0))
        rounded(cur, (-w / 2, -h / 2, 0), (w, h, 4.4), 'dscale', f'seg{k}')
        cur.cube((-w / 2 + 0.3, -h / 2 - 0.3, 0), (w - 0.6, h + 0.6, 1), 'dring', f'ring{k}')
        cur.cube((-w / 2 + 1, h / 2 - 0.4, 0.6), (w - 2, 1, 3.2), 'dbelly', f'bel{k}')
        sp = cur.child(f'tail_{k}_spike', (0, -h / 2, 1.6), (-0.45, 0, 0))
        sp.cube((-0.5, -max(1.2, 2.6 - k * 0.12), -0.6), (1, max(1.2, 2.6 - k * 0.12), 1.6), 'dspike', f'sp{k}')
        if k < 9:
            for s in (-1, 1):
                cur.child(f'tail_{k}_side_{"l" if s > 0 else "r"}', (s * w / 2, -h / 4, 2), (0, 0, s * 0.5)).cube(
                    (-0.4, -0.4, -1), (0.8, 0.8, 2), 'dspike', 'sside')
    tip = cur.child('tail_tip', (0, 0, 4.2))
    tip.cube((-3, -0.5, 0), (6, 1, 2.5), 'dspike', 'tip0')
    tip.cube((-2, -0.5, 2.5), (4, 1, 1.5), 'dspike', 'tip1')
    tip.cube((-1, -0.5, 4), (2, 1, 1.2), 'dspike', 'tip2')
    return m


def dragon_horns():
    m = Model('dragon_horns', 'head', 64, 'Chifres dracônicos grandes cor de carvão, que sobem e curvam para trás, '
                                         'em gomos arredondados com anéis esculpidos e ponta afilada.')
    root = m.root('horns', (0, 0, 0))
    thick = [3, 2.8, 2.5, 2.2, 1.8, 1.4, 1.0]
    length = [3, 3, 3, 3, 2.6, 2.2, 2.0]
    for s, sn in ((1, 'l'), (-1, 'r')):
        cur = root.child(f'horn_{sn}', (s * 2.6, -7.4, -0.2), (1.15, s * 0.32, -s * 0.18))
        for k, (t, L) in enumerate(zip(thick, length)):
            if k:
                cur = cur.child(f'horn_{sn}_{k}', (0, 0, length[k - 1] - 0.3), (-0.33, s * 0.04, 0))
            mt = 'char' if k < 4 else 'char_tip'
            rounded(cur, (-t / 2, -t / 2, 0), (t, t, L), mt, f'h{k}')
            if k < 5:
                cur.cube((-t / 2 - 0.25, -t / 2 - 0.25, 0.2), (t + 0.5, t + 0.5, 0.7), 'char_ridge', f'r{k}')
    return m


def fox_tail():
    m = Model('fox_tail', 'body', 128, 'Cauda de raposa enorme e felpuda (referências da Raposa de Fogo e de Neve): gomos '
                                      'arredondados que engordam no meio, tufos de pelo soltos e ponta clara, em curva de S. '
                                      'Texturas: fogo (ruiva com ponta branca), neve (cinza e branca) e kitsune (toda branca).')
    cur = m.root('tail', (0, 10.5, 2.0), (-0.95, 0, 0))
    widths = [3, 4.5, 6, 7.5, 8.5, 8.5, 8, 6.5, 4.5, 2.5]
    pitch = [0, 0.3, 0.28, 0.22, 0.16, 0.12, 0.1, 0.1, 0.12, 0.12]
    for k, w in enumerate(widths):
        cur = cur.child(f'fox_{k}', (0, 0, 0 if k == 0 else 2.8), (pitch[k], 0, 0))
        mt = 'fox_fur' if k < 6 else ('fox_mid' if k < 8 else 'fox_tip')
        rounded(cur, (-w / 2, -w / 2, 0), (w, w, 3.4), mt, f'f{k}')
        if w >= 6:
            cur.cube((-w / 2 + 1.5, -w / 2 - 0.5, 0.4), (w - 3, w + 1, 2.6), mt, f'fz{k}')
            cur.cube((-w / 2 - 0.5, -w / 2 + 1.5, 0.4), (w + 1, w - 3, 2.6), mt, f'fx{k}')
        if 2 <= k <= 8:
            for j, (px, py, a) in enumerate(((0.5, -0.5, 0.6), (-0.5, 0.4, -0.7))):
                tuft = cur.child(f'fox_{k}_tuft_{j}', (px * w * 0.7, py * w * 0.7, 2.2), (0.5 * a, 0, a))
                tuft.cube((-1, -1, 0), (2, 2, 2.6), 'fox_tip' if k >= 7 else 'fox_mid', f't{k}')
    return m


def bunny_ears():
    m = Model('bunny_ears', 'head', 64, 'Orelhas de coelho selvagem da floresta: longas, largas, pesadas e muito felpudas, '
                                       'tombadas para trás da cabeça; parte de dentro clara, pontas escuras e tufos de pelo na base.')
    root = m.root('ears', (0, 0, 0))
    widths = [2.6, 3.2, 3.5, 3.2, 2.4]
    thick = [1.5, 1.5, 1.4, 1.3, 1.1]
    lens = [2.4, 3.0, 3.2, 3.0, 2.6]
    for s, sn in ((1, 'l'), (-1, 'r')):
        cur = root.child(f'ear_{sn}', (s * 2.3, -7.6, 0.6), (-0.55, s * 0.28, s * 0.32))
        base = cur
        for k in range(5):
            if k:
                cur = cur.child(f'ear_{sn}_{k}', (0, -lens[k - 1] + 0.2, 0), (-0.24, 0, s * 0.05))
            w, t, L = widths[k], thick[k], lens[k]
            mt = 'ear_tip' if k == 4 else 'ear_fur'
            cur.cube((-w / 2, -L, -t / 2), (w, L, t), mt, f'e{k}')
            cur.cube((-w / 2 - 0.35, -L + 0.4, -t / 2 + 0.3), (w + 0.7, L - 0.8, t - 0.6), 'ear_fluff', f'fl{k}')
            if k < 4:
                cur.cube((-w / 2 + 0.6, -L, -t / 2 - 0.2), (w - 1.2, L, 0.4), 'ear_in', f'in{k}')
        base.cube((-1.9, -1.0, -1.1), (3.8, 1.6, 2.2), 'ear_fluff', 'tuft')
    return m


def phoenix_wing():
    m = Model('phoenix_wing', 'body', 128, 'Asa de fogo da Fênix (uma; a outra é espelhada): penas longas em degradê de creme a '
                                          'dourado, laranja e vermelho, com pontas em brasa. Desenhada com brilho emissivo.')
    root = m.root('wing', (1.4, 1.2, 2.4))
    feather_wing(root, 1, 'phx', 'phx', 1.0, 11, 11, (16, 18, 20, 21, 19), (11, 12, 13, 14), mirror_uv=False)
    return m


def toxic_cauldron():
    m = Model('toxic_cauldron', 'fx', 64, 'Caldeirão de ferro escuro com veneno roxo, arremessado pela Dama Cogumelo.')
    c = m.root('cauldron', (0, 16, 0))
    c.cube((-6, -1, -6), (12, 1, 12), 'iron')
    for s in (-1, 1):
        c.cube((s * 5.5 - 0.5, -10, -6), (1, 9, 12), 'iron', 'wallx')
        c.cube((-6, -10, s * 5.5 - 0.5), (12, 9, 1), 'iron', 'wallz')
        c.cube((s * 4 - 1, 0, -1), (2, 2, 2), 'iron_dark', 'foot')
    c.cube((-5, -8, -5), (10, 1, 10), 'poison')
    c.child('rim').cube((-6.5, -10.5, -6.5), (13, 1, 13), 'iron_dark')
    return m


def magma_core():
    m = Model('magma_core', 'fx', 64, 'Bloco de magma verde girando (projétil do Dragão Marrom).')
    c = m.root('core', (0, 0, 0))
    c.cube((-4, -4, -4), (8, 8, 8), 'magma')
    for i, (x, y, z) in enumerate(((-5, -2, -2), (3, -5, -1), (-2, 3, 2), (1, -1, 3))):
        c.child(f'chunk_{i}', (x, y, z), (i * 0.4, i * 0.7, 0.2)).cube((-1, -1, -1), (2, 2, 2), 'magma_hi', 'ch')
    return m


# ====================================================================== materiais

def palettes():
    mat('angel_bone', [(226, 218, 200), (214, 204, 182)])
    mat('angel_covert', [(238, 234, 226), (226, 220, 208)], 'feather', chev=[(196, 176, 146), (170, 148, 116)], tip=(206, 190, 160))
    mat('angel_feather', [(240, 236, 228), (228, 222, 210)], 'feather', chev=[(196, 176, 146), (160, 134, 100)], tip=(178, 158, 126))
    mat('angel_tip', [(170, 150, 118), (150, 130, 98)])
    mat('gold', [(240, 188, 58), (226, 170, 44), (252, 210, 90)])
    mat('gold_hi', [(255, 226, 120), (250, 206, 84)], 'grad')
    mat('horn_dark', [(120, 14, 18), (100, 10, 14), (140, 20, 24)])
    mat('horn', [(170, 24, 28), (150, 18, 22), (196, 40, 38)])
    mat('horn_hi', [(210, 54, 46), (190, 40, 36)])
    mat('flame', [(255, 120, 40), (240, 80, 30), (255, 160, 60)], 'grad')
    mat('flame_hi', [(255, 200, 90), (255, 140, 50)], 'grad')
    mat('gills', [(232, 218, 190), (214, 198, 166), (198, 180, 148)])
    mat('cap', [(196, 40, 40), (176, 30, 34), (214, 60, 50)], 'spots', spot=[(240, 226, 186), (250, 240, 212)])
    mat('cap_top', [(206, 50, 46), (186, 36, 36)], 'spots', spot=[(250, 240, 212)])
    mat('hair', [(238, 128, 70), (226, 96, 58), (196, 50, 46)], 'grad')
    # sereia: textura viva (fora d'água) — a variante "dark" troca estes nomes
    mat('scale', [(72, 204, 192), (54, 176, 172), (120, 236, 220), (190, 250, 236)], 'scale')
    mat('scale_belt', [(96, 222, 206), (70, 196, 186)])
    mat('fin', [(176, 246, 236), (130, 226, 220), (210, 255, 248)])
    mat('fin_ray', [(232, 255, 250), (196, 244, 238)], 'grad')
    mat('scale_dark', [(40, 70, 78), (30, 56, 64), (64, 100, 106), (90, 120, 120)], 'scale')
    mat('scale_belt_dark', [(52, 84, 90), (38, 66, 72)])
    mat('fin_dark', [(72, 112, 116), (54, 88, 94), (96, 136, 136)])
    mat('fin_ray_dark', [(150, 182, 180), (96, 130, 132)], 'grad')
    # kitsune: branca com pontas de chama azul
    mat('fur', [(246, 248, 252), (230, 236, 246)])
    mat('fur_mid', [(214, 232, 250), (196, 220, 246)])
    mat('fur_tip', [(150, 226, 255), (96, 200, 255), (200, 244, 255)], 'grad')
    # vastaya ave
    mat('eagle_bone', [(110, 76, 42), (96, 66, 36)])
    mat('eagle_covert', [(150, 162, 182), (128, 140, 162), (176, 186, 202)], 'feather', chev=[(110, 120, 140), (196, 204, 216)], tip=(96, 106, 128))
    mat('eagle', [(140, 100, 58), (122, 86, 48)], 'feather', chev=[(100, 108, 60), (160, 128, 80)], tip=(110, 124, 146))
    mat('eagle_olive', [(104, 110, 62), (90, 96, 54)], 'feather', chev=[(140, 100, 58)], tip=(110, 124, 146))
    mat('eagle_tip', [(108, 120, 144), (86, 96, 118), (132, 144, 164)], 'grad')
    # dragões: a textura base é a vermelha; a marrom troca estes nomes
    mat('dbone', [(112, 30, 30), (92, 22, 24), (130, 40, 38)])
    mat('dclaw', [(40, 12, 12), (60, 18, 18)])
    mat('membrane', [(140, 22, 28), (118, 16, 22), (160, 34, 38)], 'membrane', vein=(86, 10, 16))
    mat('dscale', [(130, 26, 30), (110, 20, 24), (156, 40, 40)], 'scale')
    mat('dbelly', [(170, 70, 60), (150, 56, 50)])
    mat('dspike', [(50, 10, 12), (70, 16, 18)])
    mat('dring', [(84, 14, 18), (70, 10, 14)])
    mat('dbone_b', [(176, 146, 104), (152, 124, 88), (196, 166, 122)])
    mat('dclaw_b', [(60, 40, 24), (80, 56, 34)])
    mat('membrane_b', [(120, 82, 46), (98, 64, 36), (140, 98, 58)], 'membrane', vein=(70, 46, 24))
    mat('dscale_b', [(132, 92, 52), (112, 76, 42), (156, 112, 66)], 'scale')
    mat('dbelly_b', [(196, 164, 116), (176, 146, 100)])
    mat('dspike_b', [(66, 42, 22), (86, 58, 32)])
    mat('dring_b', [(96, 64, 34), (80, 52, 28)])
    # chifres dracônicos (carvão)
    mat('char', [(48, 45, 50), (40, 38, 43), (58, 55, 61)])
    mat('char_ridge', [(78, 74, 82), (66, 62, 70)])
    mat('char_tip', [(70, 66, 74), (24, 22, 27)], 'grad')
    # cauda de raposa: base = Raposa de Fogo
    mat('fox_fur', [(232, 112, 46), (214, 92, 38), (244, 136, 64)])
    mat('fox_mid', [(240, 150, 90), (226, 128, 70)])
    mat('fox_tip', [(255, 250, 244), (238, 234, 228)])
    mat('fox_fur_s', [(178, 182, 190), (160, 164, 174), (196, 200, 206)])
    mat('fox_mid_s', [(214, 218, 224), (200, 204, 212)])
    mat('fox_tip_s', [(252, 252, 255), (240, 242, 248)])
    mat('fox_fur_w', [(250, 251, 255), (236, 240, 248)])
    mat('fox_mid_w', [(244, 247, 255), (228, 236, 250)])
    mat('fox_tip_w', [(255, 255, 255), (238, 246, 255)])
    # orelhas de coelho selvagem
    mat('ear_fur', [(168, 124, 86), (150, 108, 74), (184, 140, 100)])
    mat('ear_fluff', [(196, 158, 118), (180, 142, 104), (210, 176, 136)])
    mat('ear_in', [(236, 200, 186), (222, 184, 172)])
    mat('ear_tip', [(120, 86, 60), (64, 46, 34)], 'grad')
    # fênix
    mat('phx_bone', [(255, 214, 120), (250, 190, 90)])
    mat('phx_covert', [(255, 246, 220), (255, 200, 100)], 'grad')
    mat('phx_feather', [(255, 236, 170), (255, 170, 70), (214, 44, 34)], 'grad')
    mat('phx_tip', [(200, 30, 30), (130, 10, 20)], 'grad')
    # efeitos
    mat('iron', [(62, 54, 72), (50, 44, 60), (74, 66, 86)])
    mat('iron_dark', [(36, 30, 44), (44, 38, 54)])
    mat('poison', [(150, 60, 210), (170, 80, 230), (120, 40, 180)])
    mat('magma', [(30, 90, 34), (22, 66, 26), (40, 110, 44)], 'glow', crack=(150, 255, 120))
    mat('magma_hi', [(60, 160, 60), (90, 220, 90)], 'glow', crack=(200, 255, 170))


# ====================================================================== animações (Blockbench)

def keyframes(fn, length, steps=8):
    out = {}
    for i in range(steps + 1):
        t = length * i / steps
        out[f'{t:.2f}'] = [round(v, 2) for v in fn(t)]
    return out


def animations(model):
    """idle / walk / attack. No jogo os mesmos movimentos são calculados por código (física e estados)."""
    names = [b.name for b in model.bones()]
    A = {}

    def anim(key, length, loop, per_bone):
        bones = {}
        for i, n in enumerate(names):
            f = per_bone(n, i)
            if f:
                bones[n] = {'rotation': keyframes(f, length)}
        A[f'animation.{model.name}.{key}'] = {'loop': loop, 'animation_length': length, 'bones': bones}

    def wave(amp, speed, phase_mul):
        return lambda n, i: (lambda t: (math.sin(t * speed * 2 * math.pi - i * phase_mul) * amp, 0, 0))

    def sway(amp, speed, phase_mul):
        return lambda n, i: (lambda t: (0, 0, math.sin(t * speed * 2 * math.pi - i * phase_mul) * amp))

    if 'wing' in model.name:
        anim('idle', 2.0, True, lambda n, i: (lambda t: (0, 0, math.sin(t * math.pi - i * 0.3) * 3)) if n.endswith(('arm', '_l', '_r', 'wing')) else None)
        anim('walk', 1.0, True, lambda n, i: (lambda t: (math.sin(t * 2 * math.pi) * 4, 0, math.sin(t * 2 * math.pi - i * 0.4) * 6)) if n.endswith(('arm', '_l', '_r', 'wing')) else None)
        anim('attack', 0.6, False, lambda n, i: (lambda t: (0, 0, -math.sin(t / 0.6 * math.pi) * 45)) if n.endswith(('arm', '_l', '_r', 'wing')) else None)
    elif 'tail' in model.name or 'ears' in model.name:
        anim('idle', 3.0, True, sway(6, 0.34, 0.5))
        anim('walk', 1.0, True, sway(12, 1, 0.6))
        anim('attack', 0.6, False, wave(-25, 0.83, 0.3))
    else:
        anim('idle', 2.0, True, lambda n, i: (lambda t: (math.sin(t * math.pi + i) * 2, 0, 0)))
        anim('walk', 1.0, True, lambda n, i: (lambda t: (math.sin(t * 2 * math.pi + i) * 4, 0, 0)))
        anim('attack', 0.6, False, lambda n, i: (lambda t: (-math.sin(t / 0.6 * math.pi) * 15, 0, 0)))
    return {'format_version': '1.8.0', 'animations': A}


# ====================================================================== saída

def plan(models):
    out = ['# Planejamento dos modelos 3D (Etapa 1)', '',
           'Gerado por `gerar_modelos.py`. Unidades em 1/16 de bloco; coordenadas Bedrock (y para cima, pescoço em 24).', '']
    for m in models:
        out += [f'## {m.name}', '', m.desc, '', f'- Presa a: **{m.attach}** · textura {m.tex}×{m.tex}', '',
                '| osso | pai | pivô | rotação (°) | cubos (origem → tamanho) |', '|---|---|---|---|---|']
        for b in m.bones():
            jp = b.abs_pivot()
            piv = f'{r(jp[0])}, {r(24 - jp[1])}, {r(jp[2])}'
            rot = ', '.join(str(r(math.degrees(v))) for v in b.rot) if any(b.rot) else '—'
            cubes = '<br>'.join(f'[{r(jp[0] + f[0])}, {r(24 - (jp[1] + f[1] + s[1]))}, {r(jp[2] + f[2])}] → {list(s)}'
                                for (f, s, *_r) in b.cubes) or '—'
            out.append(f'| {b.name} | {b.parent.name if b.parent else "—"} | {piv} | {rot} | {cubes} |')
        out.append('')
    return '\n'.join(out)


VARIANTS = {
    'mermaid_tail': {'dark': {'scale': 'scale_dark', 'scale_belt': 'scale_belt_dark', 'fin': 'fin_dark', 'fin_ray': 'fin_ray_dark'}},
    'dragon_wings': {'brown': {'dbone': 'dbone_b', 'dclaw': 'dclaw_b', 'membrane': 'membrane_b'}},
    'dragon_tail': {'brown': {'dscale': 'dscale_b', 'dbelly': 'dbelly_b', 'dspike': 'dspike_b', 'dring': 'dring_b'}},
    'fox_tail': {'snow': {'fox_fur': 'fox_fur_s', 'fox_mid': 'fox_mid_s', 'fox_tip': 'fox_tip_s'},
                 'white': {'fox_fur': 'fox_fur_w', 'fox_mid': 'fox_mid_w', 'fox_tip': 'fox_tip_w'}},
}


def main():
    palettes()
    for d in (GEO_DIR, ANIM_DIR, TEX_DIR):
        os.makedirs(d, exist_ok=True)
    models = [seraph_wings(), demon_horns(), mushroom_cap(), mermaid_tail(), fox_tail(), bird_wing(), dragon_wings(),
              dragon_tail(), dragon_horns(), bunny_ears(), phoenix_wing(), toxic_cauldron(), magma_core()]
    for m in models:
        pack(m)
        with open(os.path.join(GEO_DIR, m.name + '.geo.json'), 'w', encoding='utf-8') as f:
            json.dump(to_geo(m), f, indent='\t')
        with open(os.path.join(ANIM_DIR, m.name + '.animation.json'), 'w', encoding='utf-8') as f:
            json.dump(animations(m), f, indent='\t')
        paint(m).save(os.path.join(TEX_DIR, m.name + '.png'))
        for vname, ov in VARIANTS.get(m.name, {}).items():
            paint(m, vname, ov).save(os.path.join(TEX_DIR, f'{m.name}_{vname}.png'))
        print(f'{m.name}: {sum(1 for _ in m.bones())} ossos, {sum(len(b.cubes) for b in m.bones())} cubos')
    with open(os.path.join(AQUI, 'PLANEJAMENTO.md'), 'w', encoding='utf-8') as f:
        f.write(plan(models))


if __name__ == '__main__':
    main()
