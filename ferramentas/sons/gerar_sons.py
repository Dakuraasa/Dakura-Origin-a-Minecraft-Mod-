"""
Sons originais do Datura Origins, sintetizados do zero (nenhum som do Minecraft é reutilizado).
Gera .ogg (mono, 44.1 kHz) em assets/dakura_origins/sounds/ e o sounds.json.

Uso: python gerar_sons.py   (precisa de numpy e do ffmpeg no PATH)
"""
import json
import os
import sys
import subprocess
import tempfile
import wave

import numpy as np

SR = 44100
AQUI = os.path.dirname(os.path.abspath(__file__))
ASSETS = os.path.normpath(os.path.join(AQUI, '..', '..', 'projeto', 'src', 'main', 'resources', 'assets', 'dakura_origins'))
OUT = os.path.join(ASSETS, 'sounds')
rng = np.random.default_rng(7)


def t(sec):
    return np.linspace(0, sec, int(SR * sec), endpoint=False)


def env(n, a=0.01, d=0.3, curve=3.0):
    x = np.linspace(0, 1, n)
    att = np.clip(x / max(a, 1e-4), 0, 1)
    return att * (1 - x) ** curve if d else att


def noise(n):
    return rng.standard_normal(n)


def lowpass(x, k):
    """Média móvel simples (k amostras) como filtro passa-baixa."""
    if k <= 1:
        return x
    c = np.cumsum(np.insert(x, 0, 0))
    y = (c[k:] - c[:-k]) / k
    return np.concatenate([y, np.zeros(len(x) - len(y))])


def sweep(f0, f1, sec, shape='sin'):
    tt = t(sec)
    f = np.geomspace(f0, f1, len(tt))
    ph = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(ph) if shape == 'sin' else np.sign(np.sin(ph)) * 0.5


def norm(x, peak=0.85):
    m = np.max(np.abs(x)) or 1
    return x / m * peak


def save(name, x):
    os.makedirs(OUT, exist_ok=True)
    x = np.clip(norm(x), -1, 1)
    pcm = (x * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix='.wav', delete=False) as tmp:
        wav = tmp.name
    with wave.open(wav, 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '4',
                    os.path.join(OUT, name + '.ogg')], check=True)
    os.remove(wav)


def mix(*parts):
    n = max(len(p) for p in parts)
    out = np.zeros(n)
    for p in parts:
        out[:len(p)] += p
    return out


def whoosh(sec, lo, hi):
    n = int(SR * sec)
    x = noise(n)
    k = np.linspace(lo, hi, n).astype(int)
    y = np.array([0.0] * n)
    acc = 0.0
    for i in range(n):  # passa-baixa variável (um polo)
        a = 1.0 / k[i]
        acc += a * (x[i] - acc)
        y[i] = acc
    return y * np.sin(np.linspace(0, np.pi, n)) ** 1.5


def bubbles(sec, count, f0=300, f1=900):
    n = int(SR * sec)
    out = np.zeros(n)
    for _ in range(count):
        s = rng.integers(0, max(1, n - 4000))
        d = rng.uniform(0.04, 0.12)
        f = rng.uniform(f0, f1)
        b = sweep(f, f * 2.2, d)
        b = b * env(len(b), 0.005, 1, 2)
        b = b[:n - s]
        out[s:s + len(b)] += b * rng.uniform(0.4, 1)
    return out


def boom(sec, f0=90, f1=30, grit=0.4):
    body = sweep(f0, f1, sec)
    n = len(body)
    body = body * env(n, 0.002, 1, 2.5)
    crack = lowpass(noise(n), 6) * env(n, 0.001, 1, 6) * grit
    return body + crack


def growl(sec, f=70):
    tt = t(sec)
    wob = 1 + 0.15 * np.sin(2 * np.pi * 9 * tt)
    ph = 2 * np.pi * np.cumsum(f * wob) / SR
    x = np.sin(ph) + 0.5 * np.sin(2 * ph + 0.3) + 0.3 * np.sign(np.sin(3 * ph))
    return lowpass(x + 0.3 * noise(len(tt)), 8) * np.sin(np.linspace(0, np.pi, len(tt))) ** 0.6


SOUNDS = {
    'cauldron_throw': lambda: whoosh(0.45, 60, 6) + 0.3 * sweep(220, 160, 0.45) * env(int(SR * 0.45), 0.05, 1, 1),
    'cauldron_shatter': lambda: mix(boom(0.6, 160, 60, 0.6), 0.6 * sweep(1800, 900, 0.25) * env(int(SR * 0.25), 0.001, 1, 4),
                                    0.5 * bubbles(0.8, 18, 200, 600)),
    'poison_bubble': lambda: bubbles(1.2, 26, 180, 520) * 0.8,
    'poison_burst': lambda: mix(boom(0.9, 120, 40, 0.8), 0.7 * bubbles(1.0, 30, 150, 400)),
    'mushroom_rise': lambda: mix(lowpass(noise(int(SR * 1.4)), 40) * np.linspace(0.2, 1, int(SR * 1.4)) * 3,
                                 0.6 * sweep(60, 240, 1.4) * env(int(SR * 1.4), 0.2, 1, 0.8),
                                 0.4 * bubbles(1.4, 10, 300, 800)),
    'magma_launch': lambda: mix(whoosh(0.6, 30, 4), 0.5 * lowpass(noise(int(SR * 0.6)), 3) * env(int(SR * 0.6), 0.01, 1, 2)),
    'magma_explode': lambda: mix(boom(1.1, 110, 28, 0.9), 0.4 * lowpass(noise(int(SR * 1.1)), 2) * env(int(SR * 1.1), 0.05, 1, 1.5)),
    'dragon_grab': lambda: mix(growl(1.0, 62), 0.5 * boom(0.3, 200, 90, 0.3)),
    'dragon_slam': lambda: mix(boom(1.6, 80, 22, 1.0), 0.7 * growl(1.0, 48), 0.5 * lowpass(noise(int(SR * 1.6)), 3) * env(int(SR * 1.6), 0.02, 1, 2)),
    'wing_beat': lambda: whoosh(0.35, 120, 20) * 1.2,
    'punch_hit': lambda: mix(boom(0.22, 220, 90, 0.7), 0.5 * lowpass(noise(int(SR * 0.22)), 2) * env(int(SR * 0.22), 0.001, 1, 6)),
    'punch_final': lambda: mix(boom(0.9, 140, 35, 1.0), 0.6 * whoosh(0.5, 200, 10), 0.4 * sweep(900, 300, 0.4) * env(int(SR * 0.4), 0.001, 1, 3)),
    'mana_cast': lambda: mix(0.6 * sweep(520, 1040, 0.5) * env(int(SR * 0.5), 0.02, 1, 2), 0.5 * sweep(780, 1560, 0.5) * env(int(SR * 0.5), 0.05, 1, 2.5),
                             0.4 * whoosh(0.5, 80, 12)),
    'mana_burst': lambda: mix(boom(0.7, 180, 60, 0.4), 0.5 * sweep(1400, 500, 0.6) * env(int(SR * 0.6), 0.001, 1, 3),
                              0.4 * sweep(2100, 900, 0.5) * env(int(SR * 0.5), 0.001, 1, 4)),
    'flower_bloom': lambda: mix(*[0.35 * sweep(f, f * 1.01, 1.4) * env(int(SR * 1.4), 0.05 + i * 0.12, 1, 2.5)
                                  for i, f in enumerate((523, 659, 784, 1046))], 0.2 * whoosh(1.4, 30, 6)),
    'fire_roar': lambda: mix(0.8 * lowpass(noise(int(SR * 1.2)), 6) * env(int(SR * 1.2), 0.05, 1, 1.5), 0.5 * boom(1.2, 90, 40, 0.8)),
    'dice_roll': lambda: dice(),
    'jackpot': lambda: mix(*[0.4 * sweep(f, f, 0.9) * env(int(SR * 0.9), 0.005, 1, 3) * (np.arange(int(SR * 0.9)) >= int(SR * 0.08 * i))
                             for i, f in enumerate((784, 988, 1175, 1568, 1976))], 0.3 * whoosh(0.9, 40, 8)),
    'dark_comet': lambda: mix(whoosh(0.7, 50, 3) * 1.3, 0.5 * sweep(140, 55, 0.7) * env(int(SR * 0.7), 0.02, 1, 1.5)),
    'chain_bind': lambda: mix(*[0.6 * clank(0.25) * 1.0 for _ in range(1)], *[np.concatenate([np.zeros(int(SR * 0.09 * i)), 0.5 * clank(0.2)]) for i in range(1, 5)]),
    'devour': lambda: mix(growl(0.9, 55), 0.6 * lowpass(noise(int(SR * 0.9)), 30) * env(int(SR * 0.9), 0.1, 1, 1.2), 0.4 * sweep(300, 70, 0.9) * env(int(SR * 0.9), 0.02, 1, 1)),
}


def clank(sec):
    n = int(SR * sec)
    x = sum(np.sin(2 * np.pi * f * t(sec) + rng.random()) for f in (1380, 2210, 3170, 4100))
    return (x / 4 + 0.4 * noise(n)) * env(n, 0.001, 1, 6)


def dice():
    out = np.zeros(int(SR * 1.2))
    pos = 0.0
    gap = 0.05
    for i in range(14):
        k = clank(0.04) * (1 - i / 16) * (0.6 + 0.4 * rng.random())
        a = int(SR * pos)
        out[a:a + len(k)] += k[:len(out) - a]
        pos += gap * (1 + i * 0.12) + rng.random() * 0.02
        if pos > 1.15:
            break
    return lowpass(out, 3)


def main():
    entries = {}
    only = set(sys.argv[1:])
    for name, fn in SOUNDS.items():
        if only and name not in only:
            continue
        save(name, fn())
        entries[name] = {'sounds': [f'dakura_origins:{name}'], 'subtitle': f'subtitles.dakura_origins.{name}'}
        print('ok', name)
    path = os.path.join(ASSETS, 'sounds.json')
    old = {}
    if os.path.exists(path):
        with open(path, encoding='utf-8') as f:
            old = json.load(f)
    old.update(entries)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(old, f, indent='\t')


if __name__ == '__main__':
    main()
