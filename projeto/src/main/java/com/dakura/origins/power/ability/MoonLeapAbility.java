package com.dakura.origins.power.ability;

import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:moon_leap" — Salto Lunar (Vastaya Coelho): segure para agachar e carregar (as orelhas tremem
 * e a lua cresce aos pés); solte para um salto gigantesco. Ao cair, uma cratera de luar explode em volta:
 * dano e arremesso proporcionais à carga. { "max_charge": 40, "damage": 12, "radius": 6 }
 */
public final class MoonLeapAbility implements Ability {
	public static final String CHARGE = "moon_charge", AIR = "moon_air";
	private final int maxCharge;
	private final float damage;
	private final double radius;

	public MoonLeapAbility(JsonObject json) {
		this.maxCharge = JsonParams.integer(json, "max_charge", 40);
		this.damage = JsonParams.num(json, "damage", 12f);
		this.radius = JsonParams.num(json, "radius", 6f);
	}

	@Override
	public boolean chargeable() {
		return true;
	}

	@Override
	public void holdStart(ServerPlayerEntity p) {
		EntityFlags.set(p, CHARGE, true);
		LegendKit.anim(p, "crouch", maxCharge + 20);
		LegendKit.snd(p, SoundEvents.ENTITY_RABBIT_ATTACK, 0.7f, 0.6f);
		LegendKit.run(p, "moon_charge", maxCharge * 3, (pl, age) -> {
			pl.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 5, 3, true, false, false));
			if (age % 8 == 0 && age <= maxCharge) LegendKit.snd(pl, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.6f, 0.6f + age / (float) maxCharge);
			if (age == maxCharge) LegendKit.fx(pl, "moon_full", 0);
			return true;
		}, pl -> EntityFlags.set(pl, CHARGE, false));
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		return release(p, maxCharge / 2);
	}

	@Override
	public boolean release(ServerPlayerEntity p, int heldTicks) {
		LegendKit.stop(p, "moon_charge");
		float k = Math.min(1f, Math.max(0.15f, heldTicks / (float) maxCharge));
		Vec3d look = LegendKit.flat(p.getRotationVec(1f));
		p.setVelocity(look.x * (0.4 + 0.9 * k), 0.9 + 1.4 * k, look.z * (0.4 + 0.9 * k));
		p.velocityModified = true;
		EntityFlags.set(p, AIR, true);
		LegendKit.anim(p, "leap", 20);
		LegendKit.fx(p, "moon_launch", (int) (k * 10));
		LegendKit.snd(p, SoundEvents.ENTITY_RABBIT_JUMP, 1.2f, 0.5f);
		LegendKit.snd(p, SoundEvents.ENTITY_BREEZE_JUMP, 1.0f, 0.8f);
		double r = radius * (0.5 + 0.5 * k);
		float dmg = damage * (0.35f + 0.65f * k);
		LegendKit.run(p, "moon_air", 200, (pl, age) -> {
			pl.fallDistance = 0;
			return age < 4 || !(pl.isOnGround() || pl.isTouchingWater());
		}, pl -> {
			EntityFlags.set(pl, AIR, false);
			Vec3d c = pl.getPos();
			for (LivingEntity e : LegendKit.enemies(pl, c, r)) {
				LegendKit.hurt(pl, e, dmg, true);
				Vec3d push = LegendKit.flat(e.getPos().subtract(c)).multiply(0.8);
				e.addVelocity(push.x, 0.9 * k + 0.3, push.z);
				e.velocityModified = true;
			}
			LegendKit.anim(pl, "land_pose", 12);
			LegendKit.fx(pl, "moon_crater", (int) (r * 10));
			LegendKit.snd(pl, SoundEvents.ENTITY_GENERIC_EXPLODE, 0.6f + 0.5f * k, 1.4f);
			LegendKit.snd(pl, SoundEvents.ENTITY_BREEZE_LAND, 1.0f, 0.7f);
			LegendKit.snd(pl, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 1.0f, 0.8f);
		});
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "moon_charge");
		LegendKit.stop(p, "moon_air");
	}
}
