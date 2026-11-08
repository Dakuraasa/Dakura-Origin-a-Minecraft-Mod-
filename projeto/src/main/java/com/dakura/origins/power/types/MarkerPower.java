package com.dakura.origins.power.types;

import com.dakura.origins.power.RacePower;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Poderes sem lógica própria, só consultados por mixins:
 * "dakura_origins:no_swim" (afunda e não nada) e "dakura_origins:powder_snow_walker" (anda sobre neve fofa).
 */
public final class MarkerPower implements RacePower {
	private final String kind;

	public MarkerPower(String kind) {
		this.kind = kind;
	}

	public static MarkerPower noSwim(JsonObject json) {
		return new MarkerPower("no_swim");
	}

	/** "dakura_origins:marker" { "kind": "villager_discount" } — genérico, lido por mixins. */
	public static MarkerPower generic(JsonObject json) {
		return new MarkerPower(com.dakura.origins.power.JsonParams.str(json, "kind", ""));
	}

	public static MarkerPower snowWalker(JsonObject json) {
		return new MarkerPower("powder_snow_walker");
	}

	public static boolean has(PlayerEntity p, String kind) {
		return RaceAccess.getRace(p).map(r -> r.powers().stream()
				.anyMatch(x -> x instanceof MarkerPower m && m.kind.equals(kind))).orElse(false);
	}
}
