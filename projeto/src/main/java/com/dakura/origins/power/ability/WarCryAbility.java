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
 * "dakura_origins:war_cry" — Grito de Guerra: um rugido que estremece o chão. Aliados por perto ganham
 * força e resistência; monstros são empurrados, enfraquecidos e ficam lentos. { "radius": 10, "duration": 160 }
 */
public final class WarCryAbility implements Ability {
	private final double radius;
	private final int duration;

	public WarCryAbility(JsonObject json) {
		this.radius = JsonParams.num(json, "radius", 10f);
		this.duration = JsonParams.integer(json, "duration", 160);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		Vec3d c = p.getPos();
		for (Entity e : p.getWorld().getOtherEntities(null, p.getBoundingBox().expand(radius, 4, radius))) {
			if (!(e instanceof LivingEntity le) || !le.isAlive() || e.squaredDistanceTo(c) > radius * radius) continue;
			if (e instanceof PlayerEntity) {
				le.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, 0));
				le.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 0));
			} else if (e instanceof Monster) {
				le.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, duration * 2 / 3, 1));
				le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, duration / 2, 1));
				le.damage(p.getDamageSources().playerAttack(p), 2f);
				Vec3d away = e.getPos().subtract(c).multiply(1, 0, 1);
				away = away.lengthSquared() < 1e-4 ? new Vec3d(1, 0, 0) : away.normalize();
				le.setVelocity(away.x * 1.1, 0.35, away.z * 1.1);
				le.velocityModified = true;
			}
		}
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:warcry", (int) Math.round(radius * 10)), p);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "roar", 20), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_RAVAGER_ROAR, SoundCategory.PLAYERS, 1.0f, 0.8f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 0.25f, 1.4f);
		return true;
	}
}
