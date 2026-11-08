package com.dakura.origins.race;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.PowerRegistry;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Carrega as raças de resources/dakura_races/*.json de TODOS os mods instalados
 * (o Dakura Origins e qualquer addon). Para criar uma raça basta um JSON + um ícone.
 */
public final class RaceRegistry {
	public static final String DEFAULT_RACE = "humano";
	private static final Map<String, Race> RACES = new LinkedHashMap<>();
	private static List<Race> sorted = List.of();
	/** variante → raça base ("variant_of" no JSON). Variantes dividem a página da base. */
	private static final Map<String, String> VARIANT_OF = new java.util.HashMap<>();
	private static List<Race> pages = List.of();
	/** Perfil da raça ("tag" no JSON): combat, exploration, casual, support, mixed. Variantes herdam da base. */
	private static final Map<String, String> TAGS = new java.util.HashMap<>();

	private RaceRegistry() {}

	public static void load() {
		RACES.clear();
		VARIANT_OF.clear();
		TAGS.clear();
		for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
			Optional<Path> dir = mod.findPath("dakura_races");
			if (dir.isEmpty() || !Files.isDirectory(dir.get())) continue;
			try (Stream<Path> files = Files.list(dir.get())) {
				for (Path file : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
					try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
						JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
						Race race = parse(json);
						if (json.has("variant_of")) VARIANT_OF.put(race.id(), json.get("variant_of").getAsString());
						if (json.has("tag")) TAGS.put(race.id(), json.get("tag").getAsString());
						if (RACES.containsKey(race.id())) {
							DakuraOrigins.LOGGER.warn("Raça '{}' redefinida pelo mod {}", race.id(), mod.getMetadata().getId());
						}
						RACES.put(race.id(), race);
					} catch (Exception e) {
						DakuraOrigins.LOGGER.error("Erro lendo a raça {} ({}): {}", file.getFileName(), mod.getMetadata().getId(), e.getMessage());
					}
				}
			} catch (Exception e) {
				DakuraOrigins.LOGGER.error("Erro listando raças do mod {}", mod.getMetadata().getId(), e);
			}
		}
		List<Race> list = new ArrayList<>(RACES.values());
		list.sort(Comparator.comparingInt(Race::order).thenComparing(Race::id));
		sorted = Collections.unmodifiableList(list);
		pages = list.stream().filter(r -> !VARIANT_OF.containsKey(r.id())).toList();
	}

	/** Raças com página própria no livro (sem as variantes). */
	public static List<Race> pages() {
		return pages;
	}

	/** Primeira variante desta raça (ex.: Huli Jing → das Neves), se houver. */
	public static Optional<Race> variantOf(String baseId) {
		return sorted.stream().filter(r -> baseId.equals(VARIANT_OF.get(r.id()))).findFirst();
	}

	/** Todas as variantes desta raça, na ordem do livro. */
	public static List<Race> variantsOf(String baseId) {
		return sorted.stream().filter(r -> baseId.equals(VARIANT_OF.get(r.id()))).toList();
	}

	/** A base seguida das variantes (as "formas" que dividem a mesma página). */
	public static List<Race> forms(Race base) {
		List<Race> out = new ArrayList<>();
		out.add(base);
		out.addAll(variantsOf(base.id()));
		return out;
	}

	public static String tagOf(String id) {
		return TAGS.getOrDefault(id, TAGS.getOrDefault(baseOf(id), ""));
	}

	/** Base de uma variante (ou a própria raça). */
	public static String baseOf(String id) {
		return VARIANT_OF.getOrDefault(id, id);
	}

	public static List<Race> all() {
		return sorted;
	}

	public static Optional<Race> get(String id) {
		return id == null ? Optional.empty() : Optional.ofNullable(RACES.get(id));
	}

	public static List<String> ids() {
		return sorted.stream().map(Race::id).toList();
	}

	// ------------------------------------------------------------------ JSON

	private static Race parse(JsonObject o) {
		String id = o.get("id").getAsString().toLowerCase(Locale.ROOT);
		int order = o.has("order") ? o.get("order").getAsInt() : 99;
		String name = o.get("name").getAsString();
		String epithet = str(o, "epithet", "");
		Identifier icon = Identifier.of(str(o, "icon", DakuraOrigins.MOD_ID + ":textures/race/" + id + ".png"));
		int iw = 64, ih = 64;
		if (o.has("icon_size")) {
			JsonArray s = o.getAsJsonArray("icon_size");
			iw = s.get(0).getAsInt();
			ih = s.get(1).getAsInt();
		}

		JsonObject p = o.getAsJsonObject("palette");
		JsonArray inkArr = p.getAsJsonArray("ink");
		int[] ink = new int[6];
		for (int i = 0; i < 6; i++) ink[i] = color(inkArr.get(Math.min(i, inkArr.size() - 1)).getAsString());
		Race.Palette palette = new Race.Palette(ink,
				color(str(p, "sky", "#0B0718")),
				color(str(p, "glow", "#F3E6FF")),
				color(str(p, "text", "#123524")),
				color(str(p, "accent", "#A8445A")),
				color(str(p, "muted", "#5E806B")));

		List<Race.Trait> traits = new ArrayList<>();
		if (o.has("traits")) {
			for (JsonElement e : o.getAsJsonArray("traits")) {
				JsonObject t = e.getAsJsonObject();
				Race.TraitKind kind = switch (str(t, "type", "neutro").toLowerCase(Locale.ROOT)) {
					case "vantagem", "pro" -> Race.TraitKind.VANTAGEM;
					case "desvantagem", "con" -> Race.TraitKind.DESVANTAGEM;
					default -> Race.TraitKind.NEUTRO;
				};
				traits.add(new Race.Trait(kind, t.get("text").getAsString(), Race.key(id, "trait." + traits.size())));
			}
		}

		List<RacePower> powers = new ArrayList<>();
		if (o.has("powers")) {
			for (JsonElement e : o.getAsJsonArray("powers")) {
				JsonObject pj = e.getAsJsonObject();
				Identifier type = Identifier.of(pj.get("type").getAsString());
				PowerRegistry.PowerFactory factory = PowerRegistry.powerType(type);
				if (factory == null) {
					DakuraOrigins.LOGGER.error("Raça {}: tipo de poder desconhecido '{}'", id, type);
					continue;
				}
				powers.add(factory.create(pj));
			}
		}

		List<Race.AbilityInfo> abilities = new ArrayList<>();
		if (o.has("abilities")) {
			for (JsonElement e : o.getAsJsonArray("abilities")) {
				JsonObject a = e.getAsJsonObject();
				String typeStr = a.has("type") ? a.get("type").getAsString() : str(a, "id", "");
				Ability ability = null;
				if (!typeStr.isEmpty()) {
					PowerRegistry.AbilityFactory factory = PowerRegistry.abilityType(Identifier.of(typeStr));
					if (factory == null) DakuraOrigins.LOGGER.error("Raça {}: tipo de habilidade desconhecido '{}'", id, typeStr);
					else ability = factory.create(a);
				}
				abilities.add(new Race.AbilityInfo(typeStr, a.get("name").getAsString(),
						str(a, "description", ""), a.has("cooldown") ? a.get("cooldown").getAsInt() : 200, ability,
						Race.key(id, "ability." + abilities.size())));
			}
		}

		return new Race(id, order, name, epithet, icon, iw, ih, palette,
				str(o, "description", ""), List.copyOf(traits), List.copyOf(powers), List.copyOf(abilities));
	}

	private static String str(JsonObject o, String key, String fallback) {
		return o.has(key) ? o.get(key).getAsString() : fallback;
	}

	private static int color(String hex) {
		String h = hex.startsWith("#") ? hex.substring(1) : hex;
		return 0xFF000000 | Integer.parseInt(h, 16);
	}
}
