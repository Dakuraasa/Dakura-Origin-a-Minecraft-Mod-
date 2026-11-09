package com.dakura.origins.power.types;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * "dakura_origins:flag" — marca visível para todos enquanto a raça estiver ativa (aura, fogo sem chamas no corpo...).
 * { "flag": "aura#4CFF9A" } ou { "flag": "fireproof" }. "when": "night" liga só à noite.
 */
public final class FlagPower implements RacePower {
	private final String flag, when;

	public FlagPower(JsonObject json) {
		this.flag = JsonParams.str(json, "flag", "aura#FFFFFF");
		this.when = JsonParams.str(json, "when", "always");
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		if (p.age % 10 != 0) return;
		boolean on = switch (when) {
			case "night" -> !p.getWorld().isDay() || !p.getWorld().getDimension().hasSkyLight();
			case "day" -> p.getWorld().isDay() && p.getWorld().getDimension().hasSkyLight();
			case "low_health" -> p.getHealth() <= p.getMaxHealth() * 0.35f;
			default -> true;
		};
		if (on != EntityFlags.has(p, flag)) EntityFlags.set(p, flag, on);
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		EntityFlags.set(p, flag, false);
	}
}
