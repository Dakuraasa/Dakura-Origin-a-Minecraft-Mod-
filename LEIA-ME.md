# Dakura Origins

Mod de raças para Minecraft **1.21.1 / Fabric**, feito para rodar junto com o Cobbleverse.
Ao entrar num mundo, a tela trava num livro de origens e o jogador escolhe a raça.

## Pastas

```
DakuraOrigins/
├─ COMPILAR.bat             → gera o .jar em builds\
├─ TESTAR-NO-MINECRAFT.bat  → abre um Minecraft de teste com o mod
├─ ATUALIZAR-GITHUB.bat     → envia as mudanças para o GitHub (um clique)
├─ SUBIR-NO-GITHUB.bat      → primeiro envio + release da versão
├─ builds/                  → o .jar pronto aparece aqui
├─ logs/                    → logs de compilação/teste
├─ arte/                    → artes originais + scripts que geraram os pixels
└─ projeto/                 → código-fonte (projeto Gradle/Fabric)
   └─ src/
      ├─ main/java/...      → lógica comum (raças, comando, rede, poderes)
      ├─ main/resources/
      │  ├─ dakura_races/   → UM ARQUIVO .json POR RAÇA
      │  └─ assets/dakura_origins/textures/race/ → ícone de cada raça
      └─ client/java/...    → a tela do livro (fumaça, página, layout)
```

## Pré-requisito

Java **JDK 21** instalado (ex.: Eclipse Temurin 21). Depois é só dar dois cliques no `COMPILAR.bat`.

## Comandos no jogo

| Comando | Quem | O que faz |
|---|---|---|
| `/raca` | todos | mostra sua raça |
| `/raca trocar` | todos | tira sua raça e reabre o livro |
| `/raca trocar <jogador>` | OP | faz outro jogador escolher de novo |
| `/raca definir <jogador> <raça>` | OP | define a raça direto |
| `/raca lista` | todos | lista as raças instaladas |

Os mesmos comandos existem em inglês: `/race`, `/race change`, `/race set`, `/race list`.

No livro: setas (ou ← →, A/D) folheiam, a roda do mouse rola a página e o botão pede **dois cliques** para confirmar.

Teclas (trocáveis em *Opções → Controles → Dakura Origins*): habilidades **Z** e **B**, página da raça **O**.

## HUD

No canto inferior esquerdo aparecem as duas teclas das habilidades da raça (com a tecla configurada
no momento e a recarga em forma de relógio, estilo LoL) e um livrinho que abre a **página da raça**:
descrição, vantagens, fraquezas, habilidades com recarga, controles e a maturidade (Dracônica).
Com o chat aberto o livrinho também pode ser clicado.

## Idiomas

Todo texto do mod usa o sistema de tradução do Minecraft e segue o idioma escolhido no jogo.
Arquivos em `assets/dakura_origins/lang/` (`pt_br.json` e `en_us.json`). Para outro idioma,
copie o `en_us.json` com o nome do código do idioma (ex.: `es_es.json`) e traduza os valores.

Textos das raças têm chaves automáticas: `dakura_origins.race.<id>.name`, `.epithet`,
`.description`, `.trait.<n>`, `.ability.<n>.name`, `.ability.<n>.description` (n começa em 0).
O texto escrito no JSON da raça é só o reserva, usado quando o idioma não tem a chave.
Nos campos `message` dos poderes dá para usar uma chave de tradução ou um texto pronto.

## Como criar uma raça nova (sem Java)

1. Copie `dakura_races/vampirica.json` para `dakura_races/<id>.json` e edite:
   - `order`: número da página (vira o numeral romano).
   - `tag`: selo do perfil no topo da página (`combat`, `exploration`, `casual`, `support` ou `mixed`).
   - `variant_of` (opcional): vira uma variante na mesma página da raça base (fita para alternar).
   - `icon` + `icon_size`: o PNG em `assets/dakura_origins/textures/race/` e o tamanho dele em pixels.
     **Padrão dos ícones:** a arte original sem alterações (só recorte), fundo transparente e a
     figura centralizada no recorte. Na página, todo ícone é encaixado numa caixa de 88×96,
     sem distorcer: arte pequena cresce em escala inteira, arte grande é reduzida só na tela.
   - `palette.ink`: 6 tons do escuro ao claro (a nebulosa). `sky` é o fundo, `glow` as estrelas, `text`/`accent`/`muted` as tintas da página.
   - `traits`: só o texto da lista ✦ (`vantagem`, `desvantagem` ou `neutro`).
   - `powers`: as mecânicas passivas, montadas com os tipos abaixo.
   - `abilities`: até 2 habilidades (teclas 1 e 2), com `type`, `name`, `description` e `cooldown` (20 ticks = 1 s).
2. Pronto. Nenhum outro arquivo precisa mudar.

### Tipos de poder (`powers`)

| type | O que faz | Parâmetros |
|---|---|---|
| `dakura_origins:attribute` | muda um atributo vanilla | `attribute`, `operation` (`add_value` / `add_multiplied_base` / `add_multiplied_total`), `amount`; ou `stage_values` (um valor por estágio de maturidade); ou `maturity_range` [início, fim] com `smooth`, `smooth_step`, `check_space` (para tamanho) |
| `dakura_origins:status_effect` | efeito permanente, sem ícone | `effect`, `amplifier` |
| `dakura_origins:regeneration` | cura extra periódica | `interval`, `amount`, `exhaustion`, `when` (`"in_heat"` = só no fogo/lava) |
| `dakura_origins:sunlight_damage` | o sol queima | `interval`, `damage`, `fire_seconds`, `helmet_protects` |
| `dakura_origins:restricted_diet` | não come comida comum | `allowed` (itens ou `#tags`), `message` |
| `dakura_origins:maturity` | substitui a XP por uma barra de maturidade com estágios | `thresholds`, `points_per_health`, `only_monsters`, `colors` |
| `dakura_origins:damage_immunity` | ignora um tipo de dano | `damage_tag` (ex.: `minecraft:is_fire`) |
| `dakura_origins:no_armor` | não pode vestir armadura | `message` |
| `dakura_origins:claustrophobia` | lento, faminto e trêmulo sem espaço acima | `clearance`, `slowness`, `exhaustion_per_second` |

Tipos novos desta versão: `marker` (`kind`), `food_multiplier`, `sleep_altitude`, `berserk`, `flight`,
`statue`, `damage_cap`, `desperate_regeneration`, `enchanted_weakness`, `forest_stealth`, além de
`conditional_attribute` com `"when": "desert" | "nether"`, `no_armor` com `slots` e `damage_multiplier` com `damage_types`.

Segunda leva: `flag` (aura `"aura#RRGGBB"`, `fireproof`...; `when`: `night`/`day`/`low_health`), `xp_multiplier`, `glide`,
`ally_aura`, `noble_tools`, `greed`, `cursed_rhythm` (`hits`, `bonus`), `pacifist`, `aquatic` (`dry_seconds`), `rebirth` (`cooldown`, `radius`, `damage`), `rabbit_frenzy` (`max`, `finisher`), `toxic_affinity`, `golden_ward` (`interval`, `push`, `min`, `max`), `fungal_emergency` (`threshold`, `cooldown`, `radius`, `damage`, `stay`); `maturity` aceita `per_kill`; `no_armor` com `heavy_only`; e `conditional_attribute` com `day`, `water`, `high`, `no_gold`.

Todos aceitam `"requires_food": true`, que desliga o poder quando a fome chega a zero.

### Tipos de habilidade (`abilities`)

| type | O que faz |
|---|---|
| `dakura_origins:vampire_bite` | agarra e drena a presa (`range`, `duration`, `hits`, `damage_per_hit`, `heal_ratio`, `food_ratio`, `saturation_ratio`, `monster_effects`) |
| `dakura_origins:bat_form` | vira o morcego vanilla e voa (`fly_speed`) |
| `dakura_origins:fireball` | bola de fogo que explode e incendeia (`speed`, `life`, `explosion_power`, `block_power`, `fire_count`, `fire_radius`) |
| `dakura_origins:breath` | rajada contínua de fogo em cone (`duration`, `windup`, `range`, `cone`, `damage`, `damage_interval`, `fire_seconds`, `break_interval`, `break_per_interval`, `slow`) |

Também: `passive`, `charged_jump` (segurar e soltar), `headbutt`, `boulder`, `meteor`, `summon_undead`,
`charm`, `morph` (toque/segurar), `feather_barrage`, `buff_mode`, `stomp`, `phase`, `vine_snare`, `solar_beam`,
`dash`, `keen_sight`, `fox_form`, `fox_clones`, `tether`, `mirror_shield`, `shadow_orb`, `shadow_step`, `drain_kiss`,
`mimic`, `war_cry`, `brutal_charge`, `fairy_perch`, `fairy_dust`, `golden_fist`, `casino`, `cursed_threads`, `waterfowl_dance`, `foxfire`, `nine_tails`,
`seraph_blessing`, `celestial_altar`, `tidal_song`, `tsunami`,
`solar_dive`, `dawn_wings`, `rabbit_rush`, `moon_leap` (segurar e soltar), `toxic_cauldron`, `magma_volley` e `skyfall_grab`. `tether` agora é estilo Physgun (`duration`, `throw`, `impact`); `dash` aceita `orb`; `glide` usa o voo de élitro de verdade.

`fireball` e `breath` aceitam `maturity_damage`: o multiplicador do dano em cada estágio de
maturidade (ex.: `[1.0, 1.4, 1.8]`).

Quem não pode ser mordido fica na tag `data/dakura_origins/tags/entity_type/bite_immune.json`.
Os blocos que o Sopro queima ficam na tag `data/dakura_origins/tags/block/breath_breakable.json`.

### Ícones das raças

Os ícones são os olhos desenhados pelo Artur (`arte/Olhos`). Arquivos de raças ainda não implementadas ficam guardados
para quando a raça entrar: `olhos kitsune negra_` (Kitsune Sombria, ainda não criada). A Huli Jing usa `Olhoraposafogo` e a Huli Jing das Neves usa `olho_estilo_skin`.

### Modelos 3D e efeitos (forma definitiva)

As partes do corpo são modelos do Blockbench lidos pelo jogo (`assets/dakura_origins/geo`), gerados por
`ferramentas/modelos/gerar_modelos.py`. Veja `ferramentas/modelos/COMO-USAR-NO-BLOCKBENCH.md` e as regras em
`ferramentas/INSTRUCOES-MODELOS-3D.md`. Efeitos das habilidades novas são malhas desenhadas por código
(`client/fx/FxWorld`), sem partículas vanilla, e os sons são `.ogg` originais (`ferramentas/sons/gerar_sons.py`).

Brilho emissivo: peças mágicas (asas do Serafim e da Fênix, caudas de raposa, asas da Fada) usam
`client/render/Emissive` — luz máxima mais uma passada aditiva na camada "eyes", que os shaders (Iris/OptiFine)
tratam como emissiva (glow/bloom). Intensidade, cor e pulsação são escolhidas em cada peça; `Emissive.GLOBAL`
escala tudo. Os efeitos de habilidade usam a camada aditiva do raio (`RenderLayer.getLightning`).

### Diretriz visual do projeto

Embelezar tudo ao máximo: efeitos especiais, golpes e animações fortes e chamativos, mesmo fugindo do
estilo do Minecraft, com formas arredondadas e caricatas (caudas fofas com física, asas, pentagramas,
cortes em arco, chamas-fantasma). Toda habilidade nova segue esse padrão, com som próprio.

### Mecânica nova (Java)

Crie uma classe que implemente `RacePower` ou `Ability` (pasta `power/`) e registre o tipo em
`PowerRegistry.bootstrap()`. Depois é só usar o `type` novo nos JSONs.

### Addons (outros mods)

Outro mod pode trazer raças próprias só colocando JSONs em `dakura_races/`. Elas aparecem
automaticamente. Para tipos novos, o addon implementa `DakuraOriginsPlugin` e declara no
`fabric.mod.json` dele: `"entrypoints": { "dakura_origins": ["com.exemplo.MeuAddon"] }`.
