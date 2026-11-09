package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.ArrayList;

/**
 * "dakura_origins:fairy_dust" — Pó Encantado: uma chuva de brilho em volta. Jogadores (inclusive ela) são curados,
 * ganham um escudo de absorção e perdem os efeitos ruins; monstros flutuam sem controle e ficam marcados.
 * { "radius": 6, "heal": 6 }
 */
public final class FairyDustAbility implements Ability {
	private final double radius;
	private final float heal;

	public FairyDustAbility(JsonObject json) {
		this.radius = JsonParams.num(json, "radius", 6f);
		this.heal = JsonParams.num(json, "heal", 6f);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		for (Entity e : p.getWorld().getOtherEntities(null, p.getBoundingBox().expand(radius, 3, radius))) {
			if (!(e instanceof LivingEntity le) || !le.isAlive() || e.squaredDistanceTo(p) > radius * radius) continue;
			if (e instanceof PlayerEntity) {
				le.heal(heal);
				le.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, 300, 0));
				for (var inst : new ArrayList<>(le.getStatusEffects())) {
					if (inst.getEffectType().value().getCategory() == StatusEffectCategory.HARMFUL) le.removeStatusEffect(inst.getEffectType());
				}
			} else if (e instanceof Monster) {
				le.addStatusEffect(new StatusEffectInstance(StatusEffects.LEVITATION, 40, 1));
				le.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 200, 0));
			}
		}
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:fairy_dust", (int) Math.round(radius * 10)), p);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "cast", 12), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.2f, 1.9f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ALLAY_AMBIENT_WITH_ITEM, SoundCategory.PLAYERS, 1.0f, 1.3f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.4f, 2.0f);
		return true;
	}
}
