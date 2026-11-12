<p align="center">
<p align="center">
  <img src="banner.png" width="70%" alt="Datura Origins Banner">
</p>

<h1 align="center">Datura Origins</h1>

<p align="center">
  A race mod for <b>Minecraft 1.21.1</b> on <b>Fabric</b>.<br>
  Pick an origin when you first join a world and play with its strengths, weaknesses and active abilities.
</p>

---

## About

Datura Origins is a standalone race mod inspired by [Origins](https://www.curseforge.com/minecraft/mc-mods/origins)
by apace100. It does not depend on Origins and shares no code with it: races, powers, abilities, the selection book
and every effect are written from scratch. Origins is credited here because its idea of choosing a race with built-in
trade-offs is the foundation this mod is built on.

When a player joins a world for the first time a book opens with every race. Each page shows the race's portrait,
lore, advantages, vanilla tweaks, weaknesses and abilities. The choice stays until it is changed with a command.

Datura Origins is also built to be extended: any mod can add races with a JSON file, and add-ons can register their
own power and ability types through a small entrypoint (see [Creating races in your own mod](#creating-races-in-your-mod)).

## Features

- 27 races and variants, each with passive powers, drawbacks and up to two active abilities.
- A selection book with one page per race; variants share their base race's page.
- Maturity stages for some races that unlock stronger versions of their powers over time.
- Ability visuals built from vanilla particles, Blockbench models and pixel-art textures.
- Race cosmetics (tails, horns, ears, wings) that can be hidden in the config.
- Fully data-driven races, discovered from every installed mod.
- English and Brazilian Portuguese translations.

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.21.1 |
| Fabric Loader | 0.16 or newer |
| Fabric API | any build for 1.21.1 |
| Java | 21 |

## Installation

1. Install Fabric Loader for Minecraft 1.21.1.
2. Download the latest `datura-origins-<version>.jar` from the [Releases](../../releases) page.
3. Put it in your `mods` folder together with Fabric API.
4. Launch the game. The race book opens the first time you enter a world.

Multiplayer servers need the mod on the server and on every client.

## Usage

| Action | Default key |
|---|---|
| Ability 1 | `Z` |
| Ability 2 | `B` |
| Open your race page | `O` |

Keys can be rebound under *Options → Controls → Datura Origins*.

| Command | Permission | Description |
|---|---|---|
| `/do race change` | everyone | Opens the book again to pick a new race. |
| `/do race change <player>` | operators | Makes another player pick again. |
| `/do race set <player> <race>` | operators | Sets a race directly. |
| `/do race list` | everyone | Lists every loaded race and its id. |
| `/do full maturity` | operators | Unlocks every maturity stage (testing). |
| `/do reset cooldown`, `/do no cooldown` | operators | Ability cooldown helpers (testing). |

## Races

| Race | Id | Abilities |
|---|---|---|
| Human | `humano` | — |
| Vampiric | `vampirica` | Vampiric Bite, Bat Form |
| Draconic | `draconica` | Fireball, Draconic Stomp |
| Brown Dragon *(Draconic variant)* | `dragao_marrom` | Green Fireball, Skyfall |
| Specter | `espectro` | Spectral Form |
| Dryad | `driade` | Bloom, Solar Ray |
| Mushroom Dame *(Dryad variant)* | `dama_cogumelo` | Venom Cauldron, Emergency Mushroom |
| Elf | `elfo` | Nimble Retreat, Hunter's Gaze |
| High Elf *(Elf variant)* | `elfo_alto` | Golden Lasso, Mirror Aegis |
| Night Elf | `elfo_noturno` | Midnight Blades, Shadow Step |
| Moon Elf *(Night Elf variant)* | `elfo_lua` | Eclipse Prison, Moonfall |
| Mystic Fox (fire and snow) | `huli_jing`, `huli_jing_neve` | Fox Form, Fox Illusions |
| Goat | `cabra` | Mountain Leap, Headbutt |
| Minotaur | `minotauro` | Bull's Fury, Boulder Throw |
| Gargoyle | `gargula` | Meteor |
| Undead | `morto_vivo` | Army of the Dead |
| Demon | `demonio` | passives only |
| Succubus *(Demon variant)* | `succubus` | Draining Kiss, Metamorphosis |
| Orc | `orc` | War Cry, Brutal Charge |
| Fairy | `fada` | Fairy Altar, Flower Sanctuary |
| Lilac | `lilac` | Golden Fist, Casino |
| Doll | `boneca` | Puppet Chain |
| Kitsune | `kitsune` | Spirit Orb, Wandering Orbs |
| Corrupted Kitsune *(Kitsune variant)* | `kitsune_corrompida` | Spirit Orb, Chain Prison |
| Mermaid | `sereia` | Song of the Tides, Tidal Wave |
| Phoenix | `fenix` | Solar Dive, Wings of Dawn |

Every trait and ability is described in detail in the book in game.

## Compatibility

- Built for Fabric API on 1.21.1 and works alongside large Fabric modpacks such as Cobbleverse.
- The internal mod id is `dakura_origins` (the project's original name). It is kept so existing worlds keep their
  chosen races, and add-ons refer to Datura Origins by this id.

---

## Creating races in your own mod

This section is for developers who want to add races without modifying Datura Origins. Everything below works from a
separate mod installed next to it.

### How the system works

Datura Origins is organised in three layers:

1. **Races** are JSON files. At startup the mod scans the `dakura_races/` folder of *every* installed mod and builds a
   race from each file. A race needs no code to appear in the book.
2. **Power types** are passive behaviours (attributes, immunities, auras, diets, flight...). A race lists the powers it
   uses in its `powers` array, each with a `type` id and that type's parameters.
3. **Ability types** are the active skills bound to the ability keys. A race lists up to two in `abilities`.

More than a hundred power and ability types ship with the mod and any race can use them. When you need behaviour that
does not exist yet, your mod registers a new type through the `dakura_origins` entrypoint and uses it from JSON exactly
like a built-in one.

Startup order:

1. Datura Origins registers its built-in types.
2. Every `dakura_origins` entrypoint (`DakuraOriginsPlugin.registerTypes()`) runs, so add-on types become available.
3. All `dakura_races/*.json` files from every mod are parsed.
4. The selection book is built from the loaded races, sorted by `order`.

### Step 1 — Create a Fabric mod

Start from the official [Fabric example mod](https://github.com/FabricMC/fabric-example-mod) for 1.21.1 with Java 21,
then add Datura Origins as a dependency. The simplest way is the release jar placed in a `libs/` folder:

```groovy
// build.gradle
dependencies {
    minecraft "com.mojang:minecraft:1.21.1"
    mappings "net.fabricmc:yarn:1.21.1+build.3:v2"
    modImplementation "net.fabricmc:fabric-loader:0.16.14"
    modImplementation "net.fabricmc.fabric-api:fabric-api:0.116.17+1.21.1"

    modCompileOnly files("libs/datura-origins-1.5.0.jar")
    modLocalRuntime files("libs/datura-origins-1.5.0.jar")
}
```

Declare the dependency in your `fabric.mod.json` so the game will not load your add-on without it:

```json
"depends": {
  "fabricloader": ">=0.16.0",
  "minecraft": "~1.21.1",
  "fabric-api": "*",
  "dakura_origins": ">=1.5.0"
}
```

An add-on that only contains JSON races does not need `modCompileOnly`; the `depends` entry is enough.

### Step 2 — Lay out the files

```
my-addon/
└─ src/main/resources/
   ├─ fabric.mod.json
   ├─ dakura_races/
   │  └─ ember_knight.json                        ← the race
   └─ assets/
      ├─ myaddon/textures/race/ember_knight.png   ← portrait shown in the book
      └─ dakura_origins/lang/
         ├─ en_us.json                             ← translations for the race
         └─ pt_br.json
```

- The file name does not matter; the `id` field inside it does. Use a unique id, ideally prefixed with your mod name
  (`myaddon_ember_knight`).
- The portrait can live in your own namespace and is referenced by its full identifier.
- Translation keys use the `dakura_origins` namespace because the book looks them up by race id. Minecraft merges
  language files from every mod, so your file only needs your own keys.

### Step 3 — Write the race JSON

```json
{
  "id": "myaddon_ember_knight",
  "order": 40,
  "name": "Ember Knight",
  "epithet": "The Oath That Still Burns",
  "icon": "myaddon:textures/race/ember_knight.png",
  "icon_size": [256, 256],
  "palette": {
    "ink": ["#120804", "#2A1208", "#5A2410", "#A8441A", "#F08A3A", "#FFE2B8"],
    "sky": "#0B0503",
    "glow": "#FFE2B8",
    "text": "#2A1208",
    "accent": "#A8441A",
    "muted": "#7A5A48"
  },
  "description": "A knight whose armour never cooled after the last war.",
  "traits": [
    { "type": "vantagem",    "text": "Immune to fire and lava." },
    { "type": "vanilla",     "text": "Sees in the dark." },
    { "type": "desvantagem", "text": "Slower in water." }
  ],
  "powers": [
    { "type": "dakura_origins:damage_immunity", "damage_tag": "minecraft:is_fire" },
    { "type": "dakura_origins:status_effect", "effect": "minecraft:night_vision" },
    { "type": "dakura_origins:conditional_attribute", "attribute": "minecraft:generic.movement_speed",
      "operation": "add_multiplied_base", "amount": -0.3, "when": "water" }
  ],
  "abilities": [
    {
      "type": "dakura_origins:fireball",
      "name": "Ember Shot",
      "description": "Throws a burning shot where you look.",
      "cooldown": 300
    }
  ]
}
```

| Field | Required | Meaning |
|---|---|---|
| `id` | yes | Unique race id, used by commands, save data and translation keys. |
| `name` | yes | Display name (fallback when no translation exists). |
| `palette` | yes | Page colours: six `ink` shades plus `sky`, `glow`, `text`, `accent` and `muted`. |
| `order` | no | Position in the book (default 99); ties are sorted by id. |
| `epithet` | no | Subtitle under the name. |
| `icon` | no | Portrait texture, default `dakura_origins:textures/race/<id>.png`. |
| `icon_size` | no | `[width, height]` of the portrait in pixels, used for clean scaling. |
| `description` | no | Lore text; `\n` starts a new paragraph. |
| `traits` | no | Lines shown in the book. `type` is `vantagem`/`pro` (advantage), `vanilla` (vanilla tweak, shown with the anvil icon), `desvantagem`/`con` (weakness) or `neutro`. Traits are tex[...]
| `powers` | no | Passive powers, each with a `type` and its parameters. |
| `abilities` | no | Up to two active abilities: `type`, `name`, `description`, `cooldown` (ticks) and the type's parameters. |
| `variant_of` | no | Base race id. The variant appears as a form on the base race's page. |
| `tag` | no | Profile used by the book's filters: `combat`, `exploration`, `casual`, `support` or `mixed`. |
| `nether_suppressed` | no | When `true`, the race keeps only its weaknesses while in the Nether. |

### Step 4 — Translations

Every visible string can be translated. Keys are built from the race id:

```json
{
  "dakura_origins.race.myaddon_ember_knight.name": "Ember Knight",
  "dakura_origins.race.myaddon_ember_knight.epithet": "The Oath That Still Burns",
  "dakura_origins.race.myaddon_ember_knight.description": "A knight whose armour never cooled after the last war.",
  "dakura_origins.race.myaddon_ember_knight.trait.0": "Immune to fire and lava.",
  "dakura_origins.race.myaddon_ember_knight.trait.1": "Sees in the dark.",
  "dakura_origins.race.myaddon_ember_knight.trait.2": "Slower in water.",
  "dakura_origins.race.myaddon_ember_knight.ability.0.name": "Ember Shot",
  "dakura_origins.race.myaddon_ember_knight.ability.0.description": "Throws a burning shot where you look."
}
```

Trait and ability numbers follow their position in the JSON arrays, starting at 0. Missing keys fall back to the text
in the JSON file, so a single-language add-on can skip this step.

### Step 5 — Reuse the built-in types

The quickest way to learn a type's parameters is to open a bundled race that uses it. They are all in
[`projeto/src/main/resources/dakura_races`](projeto/src/main/resources/dakura_races), and each type documents its
parameters in the Javadoc at the top of its class in
[`power/types`](projeto/src/main/java/com/dakura/origins/power/types) and
[`power/ability`](projeto/src/main/java/com/dakura/origins/power/ability).

| Power type | What it does | Main parameters |
|---|---|---|
| `dakura_origins:attribute` | Changes any entity attribute. | `attribute`, `operation` (`add_value`, `add_multiplied_base`, `add_multiplied_total`), `amount` |
| `dakura_origins:conditional_attribute` | Attribute change only while a condition holds. | as above plus `when`: `forest`, `night`, `day`, `water`, `high`, `nether`, `desert`, `no_gold` |
| `dakura_origins:status_effect` | Permanent hidden status effect. | `effect`, `amplifier` |
| `dakura_origins:damage_immunity` | Ignores a damage type tag. | `damage_tag` |
| `dakura_origins:damage_multiplier` | Scales damage from a damage type tag. | `damage_tag`, `multiplier` |
| `dakura_origins:flight` | Wing flight: double jump to take off, hold forward to thrust. | `speed`, `exhaustion`, `hover` |
| `dakura_origins:restricted_diet` | Refuses the listed foods. | `forbidden`, `message` |
| `dakura_origins:xp_multiplier` | More or less experience. | `factor` |
| `dakura_origins:regeneration` | Periodic healing. | `interval`, `amount` |
| `dakura_origins:vanilla_perk` | Small vanilla tweaks. | `mode`: `fortune`, `autosmelt`, `brew_double`, `crop_growth`, `no_trample`, `needs_roof`, `butcher`, `scare_animals` |
| `dakura_origins:cosmetic` | Turns on a built-in body part. | `feature` |
| `dakura_origins:flag` | Sets a synced flag that client code can read. | `flag`, `when` |

Abilities are reused the same way, for example `fireball`, `stomp`, `dash`, `charm`, `tether`, `mirror_shield`,
`puppet_chain`, `casino` or `blessing_altar`. The `type` string is always `dakura_origins:<name>` as registered in
[`PowerRegistry`](projeto/src/main/java/com/dakura/origins/power/PowerRegistry.java).

### Step 6 — Register your own types (Java)

Create a class implementing `DakuraOriginsPlugin` and declare it under the `dakura_origins` entrypoint:

```json
"entrypoints": {
  "main": [ "com.example.myaddon.MyAddon" ],
  "dakura_origins": [ "com.example.myaddon.MyAddonTypes" ]
}
```

```java
public final class MyAddonTypes implements DakuraOriginsPlugin {
    @Override
    public void registerTypes() {
        DaturaOriginsApi.registerPowerType(Identifier.of("myaddon", "ember_aura"), EmberAuraPower::new);
        DaturaOriginsApi.registerAbilityType(Identifier.of("myaddon", "ember_leap"), EmberLeapAbility::new);
    }
}
```

A **power** implements `com.dakura.origins.power.RacePower`; every method is optional:

```java
public final class EmberAuraPower implements RacePower {
    private final double radius;

    public EmberAuraPower(JsonObject json) {
        this.radius = JsonParams.num(json, "radius", 4f);
    }

    @Override
    public void tick(ServerPlayerEntity player) {
        if (player.age % 20 != 0) return;
        for (LivingEntity e : player.getWorld().getEntitiesByClass(LivingEntity.class,
                player.getBoundingBox().expand(radius), e -> e instanceof Monster)) {
            e.setOnFireFor(2);
        }
    }

    @Override public void onApply(ServerPlayerEntity player) { }
    @Override public void onRemove(ServerPlayerEntity player) { }
}
```

- `tick` runs every server tick while the race is active.
- `onApply` runs when the race is chosen and when the player joins; `onRemove` must undo anything it changed.
- Other hooks: `onKill`, `allowDamage` (return `false` to cancel incoming damage), `consumeRejection` and
  `useRejection` (return a message to block eating or using an item).

An **ability** implements `com.dakura.origins.power.Ability`:

```java
public final class EmberLeapAbility implements Ability {
    private final double power;

    public EmberLeapAbility(JsonObject json) {
        this.power = JsonParams.num(json, "power", 1.2f);
    }

    @Override
    public boolean activate(ServerPlayerEntity player) {
        Vec3d look = player.getRotationVec(1f);
        player.setVelocity(look.x * power, 0.6, look.z * power);
        player.velocityModified = true;
        return true;
    }
}
```

Returning `true` from `activate` starts the cooldown; `false` means nothing happened. Optional hooks: `chargeable`,
`holdStart` and `release` (hold-to-charge), `scroll` (mouse wheel), `attackKey`, `choose` (answers from a custom
screen) and `onRemove`. Key handling, cooldowns, the HUD and networking are provided by Datura Origins.

Use your types from JSON like any other:

```json
"powers":    [ { "type": "myaddon:ember_aura", "radius": 5 } ],
"abilities": [ { "type": "myaddon:ember_leap", "name": "Ember Leap", "cooldown": 200, "power": 1.4 } ]
```

### Step 7 — Communicate with Datura Origins at runtime

`com.dakura.origins.api.DaturaOriginsApi` is the facade meant for add-ons:

| Method | Use |
|---|---|
| `races()`, `race(id)` | Read every loaded race, or one by id. |
| `raceOf(player)` | The race of a player, on the server or the client. |
| `setRace(serverPlayer, id)` | Change a player's race from code. |
| `registerPowerType`, `registerAbilityType` | Register types from your `DakuraOriginsPlugin`. |

Helpers you can use inside your powers and abilities:

- `JsonParams` — typed reading of JSON parameters with defaults.
- `EntityFlags.set(player, "flag", true)` — flags synced to clients, read with `EntityFlags.clientHas(entity, "flag")`.
- `Control.lock(entity, ticks)` — full crowd control (no movement, attacks, items or abilities).
- `LegendKit` — damage helpers, sounds, client effects and multi-tick tasks (`LegendKit.run`).

### Step 8 — Visuals

- **Portrait:** any PNG; set `icon_size` to its real size so the book scales it cleanly.
- **Effects:** spawn vanilla particles and play sounds from your own code, as the bundled races do.
- **Body parts:** register a player `FeatureRenderer` in your client entrypoint and draw it when
  `EntityFlags.clientHas(player, "myaddon_feature")` is true, with a `dakura_origins:flag` power on the race setting
  that flag. Blockbench models exported as *Bedrock geometry* can be loaded with `GeoModel`, like the bundled tails
  and horns.

### Step 9 — Build, test and troubleshoot

1. Run `gradlew runClient` in your add-on project (with `modLocalRuntime` pointing to the Datura Origins jar).
2. Create a world: your race appears in the book at its `order` position.
3. `/do race list` confirms the id was loaded; `/do race set <player> <id>` switches to it quickly.
4. Read the log. Every problem names the race and file:
   - `Failed to read race <file> (<mod>)` — invalid JSON or a missing required field (`name`, `palette`).
   - `Race <id>: unknown power type '<type>'` — a typo in `type`, or your entrypoint did not run.
   - `Race '<id>' overridden by mod <mod>` — two mods share a race id; rename yours.
   - `Addon failed to register types` — an exception inside `registerTypes()`.
5. Build the release jar with `gradlew build`; it is written to `build/libs/`.

### Step 10 — Compatibility checklist

- Depend on `dakura_origins` in `fabric.mod.json` with the lowest version you tested.
- Keep race ids unique and stable; they are stored in player data.
- Undo everything in `onRemove` (attributes, flags, effects) so changing race leaves nothing behind.
- Run game logic on the server only (`!world.isClient`) and visuals on the client only.
- A bundled race can be replaced by shipping a file with the same id, but prefer new ids.

---

## Building from source

```
cd projeto
./gradlew build
```

The jar is written to `projeto/build/libs/`. Java 21 is required.

## Credits

- Inspired by [Origins](https://www.curseforge.com/minecraft/mc-mods/origins) by apace100.
- Fonts: EB Garamond and Cormorant Garamond, under the SIL Open Font License (see the `font` folder).
- Developed by Daturaasa.

## License

All rights reserved. You may play with the mod and create add-ons that depend on it. Redistributing modified copies of
Datura Origins requires permission.
