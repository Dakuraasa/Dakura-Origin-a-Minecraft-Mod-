package com.dakura.origins.power.types;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "dakura_origins:cosmetic" — aparência opcional da raça, desenhada no cliente.
 * { "feature": "fox_ears" | "fox_ears_snow" | "slim_model" | ... }
 * Toda característica registrada aqui (e as de TranslucentPower e FlowerCrownPower) obedece
 * à opção "Aparências de raça" de cada jogador — raças novas entram sozinhas.
 * Tamanho e altura não são cosméticos e ficam sempre ativos.
 */
public final class CosmeticPower implements RacePower {
	/** Todas as características visuais opcionais conhecidas (preenchido ao carregar as raças). */
	public static final Set<String> FEATURES = ConcurrentHashMap.newKeySet();

	private final String feature;

	public CosmeticPower(JsonObject json) {
		this.feature = JsonParams.str(json, "feature", "");
		register(feature);
	}

	public static void register(String feature) {
		if (!feature.isEmpty()) FEATURES.add(feature);
	}

	@Override
	public void onApply(ServerPlayerEntity player) {
		EntityFlags.set(player, feature, true);
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		EntityFlags.set(player, feature, false);
	}
}
