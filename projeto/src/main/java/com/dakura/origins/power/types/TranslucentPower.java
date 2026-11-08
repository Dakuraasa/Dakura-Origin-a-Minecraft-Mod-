package com.dakura.origins.power.types;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * "dakura_origins:translucent" — o corpo aparece semitransparente para todos (LivingEntityRendererMixin).
 * { "type": "dakura_origins:translucent" }
 */
public final class TranslucentPower implements RacePower {
	public static final String FLAG = "translucent";

	public TranslucentPower(JsonObject json) {
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
