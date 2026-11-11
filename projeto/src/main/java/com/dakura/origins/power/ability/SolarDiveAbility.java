package com.dakura.origins.power.ability;

import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:solar_dive" — Mergulho Solar (Fênix): sobe num jorro de chamas, para no alto com as asas
 * abertas e mergulha como um cometa na direção da mira. O impacto abre um anel de sol no chão (sem quebrar
 * blocos): dano, fogo e arremesso. { "radius": 5, "damage": 9, "rise": 1.3 }
 */
public final class SolarDiveAbility implements Ability {
	public static final String FLAG = "solar_dive";
	private final double radius, rise;
	private final float damage;

	public SolarDiveAbility(JsonObject json) {
		this.radius = JsonParams.num(json, "radius", 5f);
		this.damage = JsonParams.num(json, "damage", 9f);
		this.rise = JsonParams.num(json, "rise", 1.3f);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		EntityFlags.set(p, FLAG, true);
		p.setVelocity(p.getVelocity().x * 0.3, rise, p.getVelocity().z * 0.3);
		p.velocityModified = true;
		LegendKit.anim(p, "wings_up", 14);
		LegendKit.fx(p, "solar_rise", 0);
		LegendKit.snd(p, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 1.0f, 1.3f);
		LegendKit.snd(p, SoundEvents.ITEM_FIRECHARGE_USE, 1.0f, 0.7f);
		boolean[] diving = {false};
		LegendKit.run(p, "solar_dive", 70, (pl, age) -> {
			pl.fallDistance = 0;
			if (age < 12) return true;
			if (age < 16) {
				pl.setVelocity(0, 0.02, 0);
				pl.velocityModified = true;
				if (age == 12) {
					LegendKit.anim(pl, "dive", 40);
					LegendKit.fx(pl, "solar_charge", 0);
					LegendKit.snd(pl, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.0f, 1.6f);
				}
				return true;
			}
			Vec3d look = pl.getRotationVec(1f);
			Vec3d d = new Vec3d(look.x, Math.min(look.y, -0.45), look.z).normalize().multiply(1.9);
			if (!diving[0]) {
				diving[0] = true;
				LegendKit.snd(pl, SoundEvents.ENTITY_BLAZE_SHOOT, 1.2f, 0.6f);
				LegendKit.snd(pl, SoundEvents.ENTITY_PHANTOM_SWOOP, 1.0f, 0.8f);
			}
			pl.setVelocity(d);
			pl.velocityModified = true;
			boolean hitMob = !LegendKit.enemies(pl, pl.getPos(), 1.6).isEmpty();
			return !(pl.isOnGround() || pl.isTouchingWater() || hitMob || pl.horizontalCollision);
		}, pl -> {
			EntityFlags.set(pl, FLAG, false);
			if (!diving[0]) return;
			Vec3d c = pl.getPos();
			for (LivingEntity e : LegendKit.enemies(pl, c, radius)) {
				double k = 1 - Math.min(1, e.getPos().distanceTo(c) / radius) * 0.5;
				LegendKit.hurt(pl, e, (float) (damage * k), false);
				DragonFire.burn(pl.getServerWorld(), e, pl, 2f, 6);
				Vec3d push = LegendKit.flat(e.getPos().subtract(c)).multiply(1.1);
				e.addVelocity(push.x, 0.7, push.z);
				e.velocityModified = true;
			}
			pl.setVelocity(0, 0.35, 0);
			pl.velocityModified = true;
			LegendKit.fx(pl, "solar_impact", (int) (radius * 10));
			// o chão em volta do impacto pega fogo: chamas visuais, brasas e alguns focos de fogo de verdade
			DragonFire.igniteGround(pl.getServerWorld(), c, (int) radius, 7);
			com.dakura.origins.sound.ModSounds.play(pl, com.dakura.origins.sound.ModSounds.FIRE_ROAR, 1.2f, 1f);
			LegendKit.anim(pl, "land_pose", 12);
			LegendKit.snd(pl, SoundEvents.ENTITY_GENERIC_EXPLODE, 1.0f, 1.3f);
			LegendKit.snd(pl, SoundEvents.ITEM_FIRECHARGE_USE, 1.2f, 0.5f);
			LegendKit.snd(pl, SoundEvents.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.8f);
		});
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "solar_dive");
	}
}
