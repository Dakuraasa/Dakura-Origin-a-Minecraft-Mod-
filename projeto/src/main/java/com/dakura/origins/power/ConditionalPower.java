package com.dakura.origins.power;

import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Base para poderes que podem "desligar" sozinhos.
 * "requires_food": true → o poder só funciona com a barra de fome acima de zero.
 */
public abstract class ConditionalPower implements RacePower {
	protected final boolean requiresFood;

	protected ConditionalPower(JsonObject json) {
		this.requiresFood = JsonParams.bool(json, "requires_food", false);
	}

	protected boolean isActive(PlayerEntity player) {
		return !requiresFood || player.getHungerManager().getFoodLevel() > 0;
	}
}
