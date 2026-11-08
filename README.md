<p align="center">
  <img src="projeto/src/main/resources/assets/dakura_origins/textures/gui/book.png" width="128" alt="Dakura Origins">
</p>

<h1 align="center">Dakura Origins</h1>

<p align="center">
  Mod de raças para <b>Minecraft 1.21.1</b> com <b>Fabric</b>.<br>
  Escolha sua origem e jogue com vantagens, fraquezas e habilidades próprias.
</p>

<p align="center">
  <a href="https://www.curseforge.com/minecraft/mc-mods/origins">Inspirado no Origins</a> ·
  Compatível com Cobbleverse · Funciona em mundos compartilhados pelo Essential
</p>

---

## Requisitos

| | Versão |
|---|---|
| Minecraft | **1.21.1** |
| Fabric Loader | **0.16 ou mais nova** (testado com 0.20.1) |
| Fabric API | versão para **1.21.1** |

## Instalação

1. Instale o **Fabric Loader** para 1.21.1.
2. Baixe o `.jar` na aba **Releases** deste repositório.
3. Coloque o `.jar` na pasta `mods`, junto com a **Fabric API**.
4. Em servidor ou mundo compartilhado, **todos os jogadores** precisam ter o mod.

## Como usar

- Ao entrar num mundo novo, o livro de origens abre e você escolhe a sua raça.
- `/raca trocar` (ou `/race change`) volta para a escolha de raça.
- Teclas padrão (trocáveis em *Opções → Controles → Dakura Origins*):

| Tecla | Ação |
|---|---|
| **Z** | Habilidade 1 |
| **B** | Habilidade 2 |
| **O** | Página da sua raça |

- No canto inferior esquerdo, a HUD mostra as teclas e a recarga de cada habilidade.
- Idiomas: português (pt_br) e inglês (en_us), seguindo o idioma do jogo.

## Raças

### Humano
A experiência clássica do Minecraft, sem vantagens nem fraquezas.

### Vampírica
- **Vantagens:** força e velocidade, visão noturna, regeneração.
- **Fraquezas:** o sol queima, só se alimenta de sangue, perde os dons com a fome zerada.
- **Mordida Vampírica:** agarra a presa e drena sua vida.
- **Forma de Morcego:** vira um morcego com voo livre.

### Dracônica
- **Vantagens:** corpo de 2,5 a 3,5 blocos, couraça de ferro natural, muita vida, imune a fogo e se regenera nele, cresce matando monstros (barra de maturidade).
- **Fraquezas:** só come carne, não usa armadura, fica lenta e trêmula em lugares apertados.
- **Bola de Fogo:** explosão com força de TNT e incêndio.
- **Pisão Dracônico:** onda de choque que arremessa os inimigos para cima.

### Espectro
- **Vantagens:** semitransparente, não sente fome, visão noturna, monstros só atacam se provocados, **Segunda Chance** ao morrer (volta com 1 coração).
- **Fraquezas:** 8 corações, água e chuva o dissolvem, o Nether o desfaz, poções positivas o ferem.
- **Forma Espectral:** fica invisível e atravessa paredes.

### Dríade
- **Vantagens:** 2,5 blocos, tiara de flores, fotossíntese (regenera e se alimenta ao sol), faz as plantas crescerem, mais forte em florestas, imune a veneno e decomposição.
- **Fraquezas:** cega, não come carne, mais fraca à noite, morre se quebrar uma árvore, dano dobrado de fogo, pega fogo no Nether.
- **Vinhas Rasteiras:** vinhas pelo chão que prendem o inimigo.
- **Raio Solar:** só de dia, um raio de luz que queima e quebra blocos.

### Elfo
- **Vantagens:** mais rápido, quedas mais seguras, +50% de dano com projéteis, fome lenta.
- **Fraquezas:** 9 corações, explosões causam o dobro de dano, recusa comida estragada.
- **Recuo Ágil:** salto rápido para trás.
- **Olhar de Caçador:** vê o contorno de criaturas através de paredes.

## Criar raças novas

As raças são arquivos JSON em `projeto/src/main/resources/dakura_races/`. Outros mods podem adicionar raças pelo mesmo caminho. Os detalhes estão no [LEIA-ME.md](LEIA-ME.md).

## Compilar

Requer o **JDK 21**. No Windows, rode `COMPILAR.bat`. O `.jar` sai na pasta `builds/`.

## Créditos

- Ícone da Vampírica: arte de **EV GANIN**.
- Fontes: EB Garamond e Cormorant Garamond (licença OFL).

---

## Uma homenagem ao Origins

O Dakura Origins nasceu da admiração pelo **[Origins](https://www.curseforge.com/minecraft/mc-mods/origins)**, de **apace100**. A ideia de escolher uma origem no começo do mundo, com dons e fraquezas que mudam o jeito de jogar, é dele. Este projeto foi escrito do zero, mas não existiria sem essa inspiração.

Obrigado ao apace100 e a todos que contribuíram com o Origins por mostrar o quanto o Minecraft fica mais rico quando cada jogador começa a história sendo alguém diferente. Se você gostou deste mod, conheça o original.
