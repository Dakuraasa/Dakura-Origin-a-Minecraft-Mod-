package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:nine_tails" — Bênção das Nove Caudas: as caudas se abrem em leque e o espírito da raposa
 * celestial desce num redemoinho de chamas azuis. Aliados ganham regeneração, velocidade e um escudo;
 * monstros são lançados ao ar, queimados e ficam tontos. { "radius": 10, "damage": 6, "duration": 200 }
 */
public final class NineTailsBlessingAbility implements Ability {
	private final double radius;
	private final float damage;
	private final int duration;

	public NineTailsBlessingAbility(JsonObject json) {
		this.radius = JsonParams.num(json, "radius", 10f);
		this.damage = JsonParams.num(json, "damage", 6f);
		this.duration = JsonParams.integer(json, "duration", 200);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		Vec3d c = p.getPos();
		for (Entity e : p.getWorld().getOtherEntities(null, p.getBoundingBox().expand(radius, 4, radius))) {
			if (!(e instanceof LivingEntity le) || !le.isAlive() || e.squaredDistanceTo(c) > radius * radius) continue;
			if (e instanceof PlayerEntity) {
				le.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, 1));
				le.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 0));
				le.addStatusEffect(new StatusEffectInstance(StatusEffects.ABSORPTION, duration, 1));
			} else if (e instanceof Monster) {
				le.damage(p.getDamageSources().indirectMagic(p, p), damage);
				le.setOnFireFor(4f);
				le.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 0));
				le.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1));
				Vec3d away = e.getPos().subtract(c).multiply(1, 0, 1);
				away = away.lengthSquared() < 1e-4 ? new Vec3d(1, 0, 0) : away.normalize();
				le.setVelocity(away.x * 0.6, 0.9, away.z * 0.6);
				le.velocityModified = true;
			}
		}
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fan", 30), p);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:nine_tails", (int) Math.round(radius * 10)), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.PLAYERS, 1.0f, 1.4f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_FOX_SCREECH, SoundCategory.PLAYERS, 0.6f, 1.2f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.8f, 1.5f);
		return true;
	}
}
