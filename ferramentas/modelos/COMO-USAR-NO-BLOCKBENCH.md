# Modelos 3D do Datura Origins no Blockbench

Os modelos das partes do corpo e dos projéteis ficam em
`projeto/src/main/resources/assets/dakura_origins/`:

| O quê | Pasta |
|---|---|
| Modelo (Bedrock Geometry 1.12.0) | `geo/<peça>.geo.json` |
| Animações (idle / walk / attack) | `animations/<peça>.animation.json` |
| Texturas | `textures/entity/geo/<peça>.png` (e variantes `_dark`, `_brown`) |

Peças: `seraph_wings`, `seraph_halo`, `demon_horns`, `mushroom_cap`, `mermaid_tail` (+ `_dark`),
`kitsune_tail`, `bird_wing`, `dragon_wings` (+ `_brown`), `dragon_tail` (+ `_brown`), `toxic_cauldron`, `magma_core`.
As asas da Fada são uma sprite em pixel art (`textures/entity/geo/fairy_wings.png`), não um modelo.

## Abrir e editar

1. No Blockbench: **File → Open Model** e escolha o `.geo.json` (ele abre como *Bedrock Entity*).
2. Na aba **Textures**, arraste o `.png` com o mesmo nome (ou clique em *Import Texture*). O layout de UV já bate.
3. Para ver as animações: aba **Animate** → **Animation → Import Animations** → escolha o `.animation.json`.
   No jogo os movimentos são calculados por código (física de capa, voo, nado), as animações servem de prévia.
4. Edite e salve com **File → Save Model** (mesmo arquivo `.geo.json`). Mantenha os nomes dos ossos:
   o código usa esses nomes para animar (ex.: `wing_0_l`, `tail_3`, `flame_l`).
5. No jogo, F3+T recarrega texturas; o modelo é relido ao reiniciar o jogo.

## Gerar de novo do zero

`python ferramentas/modelos/gerar_modelos.py` recria todos os `.geo.json`, `.animation.json`, as texturas e o
`PLANEJAMENTO.md` (hierarquia de ossos, pivôs, cubos e escala). Cuidado: isso sobrescreve edições feitas no Blockbench.

## Sons

`python ferramentas/sons/gerar_sons.py` sintetiza os `.ogg` originais (nada de sons do Minecraft) em
`assets/dakura_origins/sounds/` e atualiza o `sounds.json`. Precisa de numpy e ffmpeg.
