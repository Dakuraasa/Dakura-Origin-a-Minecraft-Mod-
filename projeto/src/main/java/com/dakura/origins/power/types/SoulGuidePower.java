package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.power.ability.LegendKit;
import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:soul_guide" — Anúbis: cada criatura que morre pelas mãos dele tem a alma guiada até ele
 * (um rastro dourado), curando e dando um pouco de absorção. { "heal": 2, "absorption_ticks": 100 }
 */
public final class SoulGuidePower implements RacePower {
	private final float heal;
	private final int absorption;

	public SoulGuidePower(JsonObject json) {
		this.heal = JsonParams.num(json, "heal", 2f);
		this.absorption = JsonParams.integer(json, "absorption_ticks", 100);
	}

	@Override
	public void onKill(ServerPlayerEntity p, LivingEntity victim) {
		p.heal(heal);
		if (absorption > 0) p.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, absorption, 0, true, false, true));
		Vec3d from = victim.getPos().add(0, victim.getHeight() * 0.6, 0);
		Vec3d to = p.getPos().add(0, 1.0, 0);
		LegendKit.fxAt(p.getServerWorld(), "soul_flow", from, (float) to.x, (float) to.y, (float) to.z);
		LegendKit.snd(p, SoundEvents.PARTICLE_SOUL_ESCAPE, 1.0f, 0.8f);
		LegendKit.snd(p, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.6f);
	}
}
