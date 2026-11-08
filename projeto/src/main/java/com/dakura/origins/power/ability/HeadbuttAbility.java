package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:headbutt" — investida curta com os chifres: o primeiro alvo à frente é arremessado longe.
 * { "range": 3.5, "damage": 4, "knockback": 2.6, "lunge": 0.7 }
 */
public final class HeadbuttAbility implements Ability {
	private final double range, knockback, lunge;
	private final float damage;

	public HeadbuttAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 3.5f);
		this.damage = JsonParams.num(json, "damage", 4f);
		this.knockback = JsonParams.num(json, "knockback", 2.6f);
		this.lunge = JsonParams.num(json, "lunge", 0.7f);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		Vec3d look = p.getRotationVec(1f);
		Vec3d flat = new Vec3d(look.x, 0, look.z);
		if (flat.lengthSquared() < 1e-4) flat = new Vec3d(-Math.sin(Math.toRadians(p.getYaw())), 0, Math.cos(Math.toRadians(p.getYaw())));
		flat = flat.normalize();
		p.setVelocity(flat.x * lunge, Math.max(p.getVelocity().y, 0.1), flat.z * lunge);
		p.velocityModified = true;
		ModNetworking.sendAround(new AnimPayload(p.getId(), "ram", 12), p);

		Vec3d eye = p.getPos().add(0, p.getHeight() * 0.6, 0);
		LivingEntity best = null;
		double bestD = Double.MAX_VALUE;
		Box area = p.getBoundingBox().stretch(flat.multiply(range)).expand(0.8, 0.5, 0.8);
		for (Entity e : p.getWorld().getOtherEntities(p, area, x -> x instanceof LivingEntity l && l.isAlive() && !x.isSpectator())) {
			Vec3d to = e.getPos().add(0, e.getHeight() * 0.5, 0).subtract(eye);
			double d = to.length();
			if (d > range + e.getWidth() || to.normalize().dotProduct(flat) < 0.45) continue;
			if (d < bestD) {
				bestD = d;
				best = (LivingEntity) e;
			}
		}
		if (best != null) {
			best.damage(p.getDamageSources().playerAttack(p), damage);
			double kr = 1.0 - best.getAttributeValue(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE) * 0.6;
			best.setVelocity(flat.x * knockback * kr, 0.55 * kr, flat.z * knockback * kr);
			best.velocityModified = true;
			Vec3d at = best.getPos().add(0, best.getHeight() * 0.5, 0);
			p.getServerWorld().spawnParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 10, 0.25, 0.25, 0.25, 0.25);
			p.getServerWorld().spawnParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 5, 0.2, 0.2, 0.2, 0.05);
			p.getWorld().playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_GOAT_RAM_IMPACT, SoundCategory.PLAYERS, 1.0f, 0.9f);
		} else {
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_GOAT_PREPARE_RAM, SoundCategory.PLAYERS, 0.8f, 1.0f);
		}
		return true;
	}
}
