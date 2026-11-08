package com.dakura.origins.power.types;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * "dakura_origins:berserk" — com pouca vida entra em fúria: regeneração e força fortes, bufando fumaça branca.
 * Sai quando a vida volta acima de "release". { "threshold": 8, "release": 12, "strength": 1, "regen": 1 }
 */
public final class BerserkPower implements RacePower {
	public static final String FLAG = "berserk";
	private final float threshold, release;
	private final int strength, regen;

	public BerserkPower(JsonObject json) {
		this.threshold = JsonParams.num(json, "threshold", 8f);
		this.release = JsonParams.num(json, "release", 12f);
		this.strength = JsonParams.integer(json, "strength", 1);
		this.regen = JsonParams.integer(json, "regen", 1);
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		if (p.age % 5 != 0 || !p.isAlive()) return;
		boolean on = EntityFlags.has(p, FLAG);
		if (!on && p.getHealth() <= threshold) {
			EntityFlags.set(p, FLAG, true);
			on = true;
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.PLAYERS, 0.55f, 1.15f);
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.0f, 0.5f);
		} else if (on && p.getHealth() >= release) {
			EntityFlags.set(p, FLAG, false);
			on = false;
		}
		if (on) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 30, strength, true, false, true));
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 30, regen, true, false, true));
			if (p.age % 40 == 0) p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_HORSE_BREATHE,
					SoundCategory.PLAYERS, 0.7f, 0.45f);
		}
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		EntityFlags.set(p, FLAG, false);
	}
}
