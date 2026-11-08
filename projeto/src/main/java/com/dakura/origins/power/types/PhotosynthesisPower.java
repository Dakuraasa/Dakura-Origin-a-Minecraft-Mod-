package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * "dakura_origins:photosynthesis" — sob o sol (dia, céu aberto, sem chuva): regenera mais e a fome enche sozinha.
 * { "heal": 1.0, "heal_interval": 40, "food_interval": 60 }
 */
public final class PhotosynthesisPower implements RacePower {
	private final float heal;
	private final int healInterval, foodInterval;

	public PhotosynthesisPower(JsonObject json) {
		this.heal = JsonParams.num(json, "heal", 1f);
		this.healInterval = Math.max(1, JsonParams.integer(json, "heal_interval", 40));
		this.foodInterval = Math.max(1, JsonParams.integer(json, "food_interval", 60));
	}

	public static boolean inSun(ServerPlayerEntity p) {
		var w = p.getWorld();
		return w.isDay() && !w.isRaining() && w.isSkyVisible(p.getBlockPos().up());
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (!inSun(player)) return;
		if (player.age % healInterval == 0 && player.getHealth() < player.getMaxHealth()) player.heal(heal);
		if (player.age % foodInterval == 0) player.getHungerManager().add(1, 0.5f);
	}
}
