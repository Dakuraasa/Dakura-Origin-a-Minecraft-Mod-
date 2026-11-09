package com.dakura.origins.power.ability;

import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:rabbit_rush" — Chuva de Chutes (Vastaya Coelho): salta no alvo da mira e solta uma rajada de
 * chutes no ar (cada um com um estalo de estrela), fechando com uma meia-lua que arremessa o alvo para cima.
 * { "range": 12, "kicks": 8, "damage": 2.5, "finisher": 8 }
 */
public final class RabbitRushAbility implements Ability {
	public static final String FLAG = "rushing";
	private final double range;
	private final int kicks;
	private final float damage, finisher;

	public RabbitRushAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 12f);
		this.kicks = JsonParams.integer(json, "kicks", 8);
		this.damage = JsonParams.num(json, "damage", 2.5f);
		this.finisher = JsonParams.num(json, "finisher", 8f);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		LivingEntity t = LegendKit.aim(p, range, 0.85);
		if (t == null) {
			p.sendMessage(Text.translatable("dakura_origins.message.no_target"), true);
			return false;
		}
		int id = t.getId();
		EntityFlags.set(p, FLAG, true);
		LegendKit.anim(p, "kick_rush", 6 + kicks * 2 + 10);
		LegendKit.fx(p, "rabbit_leap", 0);
		LegendKit.snd(p, SoundEvents.ENTITY_RABBIT_JUMP, 1.0f, 0.6f);
		LegendKit.snd(p, SoundEvents.ENTITY_BREEZE_JUMP, 0.8f, 1.2f);
		int total = 6 + kicks * 2 + 4;
		LegendKit.run(p, "rabbit_rush", total, (pl, age) -> {
			if (!(pl.getServerWorld().getEntityById(id) instanceof LivingEntity tg) || !tg.isAlive()) return false;
			pl.fallDistance = 0;
			Vec3d toT = tg.getPos().subtract(pl.getPos());
			Vec3d dir = LegendKit.flat(toT);
			if (age < 6) {
				// o salto: vai até o alvo numa parábola rápida
				Vec3d goal = tg.getPos().subtract(dir.multiply(1.3)).add(0, 0.6, 0).subtract(pl.getPos());
				pl.setVelocity(goal.multiply(0.45));
				pl.velocityModified = true;
				return true;
			}
			// pairando na frente do alvo durante a rajada
			Vec3d hold = tg.getPos().subtract(dir.multiply(1.2)).add(0, 0.4, 0).subtract(pl.getPos());
			pl.setVelocity(hold.multiply(0.5));
			pl.velocityModified = true;
			tg.setVelocity(tg.getVelocity().multiply(0.2, 0, 0.2).add(0, 0.04, 0));
			tg.velocityModified = true;
			int k = (age - 6) / 2;
			if ((age - 6) % 2 == 0 && k < kicks) {
				LegendKit.hurt(pl, tg, damage, true);
				LegendKit.fx(tg, "kick_star", k);
				LegendKit.snd(tg, SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, 0.7f, 1.0f + k * 0.08f);
				if (k % 2 == 0) LegendKit.snd(tg, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 0.5f, 1.5f);
			}
			if (age == 6 + kicks * 2) {
				LegendKit.hurt(pl, tg, finisher, true);
				tg.setVelocity(dir.x * 0.6, 1.25, dir.z * 0.6);
				tg.velocityModified = true;
				LegendKit.anim(pl, "kick_high", 12);
				LegendKit.fx(tg, "moon_kick", pl.getId());
				LegendKit.snd(tg, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.6f);
				LegendKit.snd(tg, SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT, 0.8f, 1.4f);
				LegendKit.snd(tg, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 1.2f);
				return false;
			}
			return true;
		}, pl -> EntityFlags.set(pl, FLAG, false));
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "rabbit_rush");
	}
}
