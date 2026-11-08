package com.dakura.origins.power.types;

import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * "dakura_origins:no_hunger" — já está morto: a fome nunca baixa (sem regeneração extra de saturação).
 * { "type": "dakura_origins:no_hunger" }
 */
public final class NoHungerPower implements RacePower {
	public NoHungerPower(JsonObject json) {}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.age % 10 != 0) return;
		HungerManager h = player.getHungerManager();
		if (h.getFoodLevel() < 18) h.setFoodLevel(18);
		h.setExhaustion(0f);
	}
}
