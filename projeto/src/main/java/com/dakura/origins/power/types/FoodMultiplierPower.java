package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;

/**
 * "dakura_origins:food_multiplier" — quanto cada alimento enche (e sacia). 0.6 = precisa comer bem mais.
 * Aplicado pelo PlayerEntityMixin ao comer. { "factor": 0.6 }
 */
public final class FoodMultiplierPower implements RacePower {
	private final float factor;

	public FoodMultiplierPower(JsonObject json) {
		this.factor = JsonParams.num(json, "factor", 0.6f);
	}

	public static float factor(PlayerEntity p) {
		return RaceAccess.getRace(p).flatMap(r -> r.powers().stream().filter(x -> x instanceof FoodMultiplierPower)
				.map(x -> ((FoodMultiplierPower) x).factor).findFirst()).orElse(1f);
	}
}
