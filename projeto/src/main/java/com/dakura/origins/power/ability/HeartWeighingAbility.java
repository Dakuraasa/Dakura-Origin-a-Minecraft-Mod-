package com.dakura.origins.power.ability;

import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

/**
 * "dakura_origins:heart_weighing" — Pesagem do Coração (Anúbis): uma balança dourada gigante surge sobre o alvo,
 * com o coração dele de um lado e a pena de Maat do outro. O alvo fica preso enquanto a balança pende.
 * Se a vida dele estiver abaixo de "threshold", o coração é pesado demais: julgamento (execução em criaturas,
 * dano enorme em jogadores e chefes). Senão, sofre dano e definha. { "range": 16, "threshold": 0.35, "damage": 6 }
 */
public final class HeartWeighingAbility implements Ability {
	private final double range;
	private final float threshold, damage;
	private final int delay;

	public HeartWeighingAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 16f);
		this.threshold = JsonParams.num(json, "threshold", 0.35f);
		this.damage = JsonParams.num(json, "damage", 6f);
		this.delay = JsonParams.integer(json, "delay", 30);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		LivingEntity t = LegendKit.aim(p, range, 0.9);
		if (t == null) {
			p.sendMessage(Text.translatable("dakura_origins.message.no_target"), true);
			return false;
		}
		LegendKit.anim(p, "judge", delay + 10);
		LegendKit.fx(t, "scales", delay);
		LegendKit.snd(t, SoundEvents.BLOCK_BELL_USE, 1.0f, 0.5f);
		LegendKit.snd(t, SoundEvents.ENTITY_EVOKER_PREPARE_SUMMON, 0.8f, 0.6f);
		int id = t.getId();
		LegendKit.run(p, "weighing", delay + 1, (pl, age) -> {
			if (!(pl.getServerWorld().getEntityById(id) instanceof LivingEntity tg) || !tg.isAlive()) return false;
			tg.setVelocity(0, Math.min(tg.getVelocity().y, 0.02), 0);
			tg.velocityModified = true;
			tg.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 5, 6, true, false, false));
			if (age % 10 == 0) LegendKit.snd(tg, SoundEvents.BLOCK_CHAIN_PLACE, 0.8f, 0.6f + age * 0.01f);
			if (age < delay) return true;
			boolean heavy = tg.getHealth() <= tg.getMaxHealth() * threshold;
			boolean boss = tg instanceof PlayerEntity || tg.getMaxHealth() >= 100;
			if (heavy) {
				LegendKit.hurt(pl, tg, boss ? damage * 2.5f : Math.max(tg.getHealth() + 50, 100), false);
				tg.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 1));
				LegendKit.fx(tg, "judgment", 1);
				LegendKit.snd(tg, SoundEvents.ENTITY_WITHER_SPAWN, 0.5f, 1.6f);
				LegendKit.snd(tg, SoundEvents.ITEM_TOTEM_USE, 0.6f, 0.4f);
			} else {
				LegendKit.hurt(pl, tg, damage, false);
				tg.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 80, 0));
				tg.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1));
				LegendKit.fx(tg, "judgment", 0);
				LegendKit.snd(tg, SoundEvents.BLOCK_ANVIL_LAND, 0.5f, 1.4f);
			}
			LegendKit.snd(tg, SoundEvents.BLOCK_BELL_RESONATE, 1.0f, heavy ? 0.5f : 1.0f);
			return false;
		});
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "weighing");
	}
}
