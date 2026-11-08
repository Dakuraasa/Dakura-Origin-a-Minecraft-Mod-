package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.server.network.ServerPlayerEntity;

/** "dakura_origins:slow_hunger" — a fome cai mais devagar. { "factor": 0.5 } */
public final class SlowHungerPower implements RacePower {
	private final float factor;
	private final java.util.Map<java.util.UUID, Float> last = new java.util.HashMap<>();

	public SlowHungerPower(JsonObject json) {
		this.factor = JsonParams.num(json, "factor", 0.5f);
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		HungerManager h = player.getHungerManager();
		float e = h.getExhaustion(), l = last.getOrDefault(player.getUuid(), e);
		if (e > l) h.setExhaustion(l + (e - l) * factor);
		last.put(player.getUuid(), h.getExhaustion());
	}
}
