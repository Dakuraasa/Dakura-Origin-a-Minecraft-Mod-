package com.dakura.origins.power.types;

import com.dakura.origins.power.ConditionalPower;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * "dakura_origins:regeneration" — cura extra periódica.
 * <pre>
 * { "type": "dakura_origins:regeneration", "interval": 30, "amount": 1.0,
 *   "exhaustion": 0.5, "requires_food": true, "when": "always" }
 * </pre>
 * when: "always" | "in_heat" (só pegando fogo ou dentro de lava/fogo).
 */
public final class RegenerationPower extends ConditionalPower {
	private final int interval;
	private final float amount;
	private final float exhaustion;
	private final boolean onlyInHeat;

	public RegenerationPower(JsonObject json) {
		super(json);
		this.interval = Math.max(1, JsonParams.integer(json, "interval", 40));
		this.amount = JsonParams.num(json, "amount", 1f);
		this.exhaustion = JsonParams.num(json, "exhaustion", 0f);
		this.onlyInHeat = "in_heat".equals(JsonParams.str(json, "when", "always"));
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.age % interval != 0 || !isActive(player)) return;
		if (player.getHealth() >= player.getMaxHealth() || player.isDead()) return;
		if (onlyInHeat && !player.isOnFire() && !player.isInLava()) return;
		player.heal(amount);
		if (exhaustion > 0) player.addExhaustion(exhaustion);
	}
}
