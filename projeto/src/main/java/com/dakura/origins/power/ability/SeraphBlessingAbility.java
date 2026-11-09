package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;

/**
 * "dakura_origins:seraph_blessing" — Coro Celestial: o Serafim junta as mãos, as seis asas se abrem e uma coluna
 * de luz cai sobre os aliados: cura grande, remove efeitos ruins e dá resistência e regeneração.
 * { "radius": 12, "heal": 10, "duration": 200 }
 */
public final class SeraphBlessingAbility implements Ability {
	private final double radius;
	private final float heal;
	private final int duration;

	public SeraphBlessingAbility(JsonObject json) {
		this.radius = JsonParams.num(json, "radius", 12f);
		this.heal = JsonParams.num(json, "heal", 10f);
		this.duration = JsonParams.integer(json, "duration", 200);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		for (PlayerEntity o : p.getWorld().getEntitiesByClass(PlayerEntity.class, p.getBoundingBox().expand(radius), x -> x.isAlive())) {
			if (o.squaredDistanceTo(p) > radius * radius) continue;
			o.heal(heal);
			for (var inst : new ArrayList<>(o.getStatusEffects())) {
				if (inst.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL) o.removeStatusEffect(inst.getEffectType());
			}
			o.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 1));
			o.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, 0));
			o.extinguish();
			ModNetworking.sendAround(new AnimPayload(o.getId(), "fx:holy_pillar", 0), o);
		}
		ModNetworking.sendAround(new AnimPayload(p.getId(), "pray", 30), p);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:seraph_choir", (int) Math.round(radius * 10)), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 1.0f, 1.8f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1.0f, 1.9f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.PLAYERS, 1.0f, 2.0f);
		return true;
	}
}
