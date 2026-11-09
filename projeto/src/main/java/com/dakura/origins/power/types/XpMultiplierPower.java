package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;

/**
 * "dakura_origins:xp_multiplier" — multiplica a experiência ganha. { "factor": 1.5 }
 * Quem carrega uma Fada no ombro também ganha o dobro (lido pelo PlayerEntityMixin).
 */
public final class XpMultiplierPower implements RacePower {
	private final float factor;

	public XpMultiplierPower(JsonObject json) {
		this.factor = JsonParams.num(json, "factor", 1.5f);
	}

	public static float factor(PlayerEntity p) {
		float f = RaceAccess.getRace(p).flatMap(r -> r.powers().stream().filter(x -> x instanceof XpMultiplierPower)
				.map(x -> ((XpMultiplierPower) x).factor).findFirst()).orElse(1f);
		for (var passenger : p.getPassengerList()) {
			if (com.dakura.origins.power.ability.FairyPerchAbility.isFairy(passenger)) f *= 2f;
		}
		return f;
	}
}
