package com.dakura.origins.power.types;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;

/** "dakura_origins:flower_crown" — tiara de flores na cabeça (desenhada no cliente, visível para todos). */
public final class FlowerCrownPower implements RacePower {
	public static final String FLAG = "flower_crown";

	public FlowerCrownPower(JsonObject json) {
		CosmeticPower.register(FLAG);
	}

	@Override
	public void onApply(ServerPlayerEntity player) {
		EntityFlags.set(player, FLAG, true);
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		EntityFlags.set(player, FLAG, false);
	}
}
