package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * "dakura_origins:desperate_regeneration" — quanto mais perto da morte, mais regenera (por segundo),
 * com teto. Com vida cheia, nada. { "max": 1.5, "interval": 20 }
 */
public final class DesperateRegenPower implements RacePower {
	private final float max;
	private final int interval;

	public DesperateRegenPower(JsonObject json) {
		this.max = JsonParams.num(json, "max", 1.5f);
		this.interval = Math.max(1, JsonParams.integer(json, "interval", 20));
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		if (p.age % interval != 0 || !p.isAlive() || p.getHealth() >= p.getMaxHealth()) return;
		float missing = 1f - p.getHealth() / p.getMaxHealth();
		float heal = Math.min(max, max * 1.4f * (float) Math.pow(missing, 1.6));
		if (heal >= 0.05f) p.heal(heal);
		if (missing > 0.6f && p.age % (interval * 3) == 0) p.getServerWorld().spawnParticles(
				net.minecraft.particle.ParticleTypes.CRIMSON_SPORE, p.getX(), p.getBodyY(0.5), p.getZ(), 6, 0.3, 0.5, 0.3, 0.0);
	}
}
