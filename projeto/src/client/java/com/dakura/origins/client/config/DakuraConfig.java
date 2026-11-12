package com.dakura.origins.client.config;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.types.CosmeticPower;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.Entity;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Configuração do cliente (config/dakura_origins.json). Hoje só tem "race_cosmetics":
 * liga/desliga TODAS as aparências opcionais das raças (CosmeticPower.FEATURES) — tiara,
 * orelhas, transparência, corpo esguio... Tamanho e altura não entram aqui.
 */
public final class DakuraConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("dakura_origins.json");
	public static boolean raceCosmetics = true;

	private DakuraConfig() {}

	public static void load() {
		try (Reader r = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
			JsonObject o = GSON.fromJson(r, JsonObject.class);
			if (o != null && o.has("race_cosmetics")) raceCosmetics = o.get("race_cosmetics").getAsBoolean();
		} catch (Exception ignored) {
			save();
		}
	}

	public static void save() {
		JsonObject o = new JsonObject();
		o.addProperty("race_cosmetics", raceCosmetics);
		try (Writer w = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
			GSON.toJson(o, w);
		} catch (Exception e) {
			DakuraOrigins.LOGGER.warn("Could not save the config: {}", e.getMessage());
		}
	}

	/** Esta entidade mostra esta aparência opcional? (respeita a opção do jogador) */
	public static boolean cosmetic(Entity e, String feature) {
		if (!raceCosmetics && CosmeticPower.FEATURES.contains(feature)) return false;
		return EntityFlags.clientHas(e, feature);
	}
}
