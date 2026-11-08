package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * "dakura_origins:flight" — voo de verdade (pulo duplo para levantar voo, como no criativo), mais lento,
 * gastando um pouco de fome enquanto voa. Sem fome, não decola. { "speed": 0.035, "exhaustion": 0.02 }
 */
public final class FlightPower implements RacePower {
	private final float speed, exhaustion;

	public FlightPower(JsonObject json) {
		this.speed = JsonParams.num(json, "speed", 0.035f);
		this.exhaustion = JsonParams.num(json, "exhaustion", 0.02f);
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		if (p.isCreative() || p.isSpectator()) return;
		boolean can = p.getHungerManager().getFoodLevel() > 0 && !com.dakura.origins.power.EntityFlags.has(p, StatuePower.FLAG)
				&& !com.dakura.origins.power.EntityFlags.has(p, "meteor");
		var a = p.getAbilities();
		boolean changed = false;
		if (a.allowFlying != can) {
			a.allowFlying = can;
			if (!can) a.flying = false;
			changed = true;
		}
		if (Math.abs(a.getFlySpeed() - speed) > 1e-4) {
			a.setFlySpeed(speed);
			changed = true;
		}
		if (changed) p.sendAbilitiesUpdate();
		if (a.flying) {
			p.addExhaustion(exhaustion);
			p.fallDistance = 0;
			if (p.age % 14 == 0) p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(),
					net.minecraft.sound.SoundEvents.ENTITY_PHANTOM_FLAP, net.minecraft.sound.SoundCategory.PLAYERS, 0.25f, 0.55f);
		}
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		if (p.isCreative() || p.isSpectator()) return;
		var a = p.getAbilities();
		a.allowFlying = false;
		a.flying = false;
		a.setFlySpeed(0.05f);
		p.sendAbilitiesUpdate();
	}
}
