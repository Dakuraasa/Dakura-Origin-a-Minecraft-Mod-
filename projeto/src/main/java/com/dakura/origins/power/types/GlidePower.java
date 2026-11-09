package com.dakura.origins.power.types;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:glide" — ao cair de um lugar alto, a asa se abre e ela plana como num élitro
 * (desce devagar e ganha impulso na direção do olhar). Agachar fecha a asa. { "min_fall": 3.5, "sink": 0.09 }
 */
public final class GlidePower implements RacePower {
	public static final String FLAG = "gliding";
	private final float minFall;
	private final double sink;

	public GlidePower(JsonObject json) {
		this.minFall = JsonParams.num(json, "min_fall", 3.5f);
		this.sink = JsonParams.num(json, "sink", 0.09f);
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		boolean on = EntityFlags.has(p, FLAG);
		boolean air = !p.isOnGround() && !p.isTouchingWater() && !p.getAbilities().flying && !p.hasVehicle() && !p.isSneaking();
		if (!on && air && p.fallDistance >= minFall && p.getVelocity().y < 0) {
			EntityFlags.set(p, FLAG, true);
			on = true;
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PARROT_FLY, SoundCategory.PLAYERS, 1.0f, 0.7f);
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_ELYTRA_FLYING, SoundCategory.PLAYERS, 0.15f, 1.6f);
		} else if (on && !air) {
			EntityFlags.set(p, FLAG, false);
			return;
		}
		if (!on) return;
		Vec3d look = p.getRotationVec(1f), v = p.getVelocity();
		Vec3d flat = new Vec3d(look.x, 0, look.z);
		double speed = Math.sqrt(v.x * v.x + v.z * v.z);
		double target = 0.55 + Math.max(0, -look.y) * 0.5;
		Vec3d push = flat.lengthSquared() < 1e-4 ? Vec3d.ZERO : flat.normalize().multiply(Math.min(0.06, Math.max(0, target - speed)));
		p.setVelocity(v.x * 0.98 + push.x, Math.max(v.y, -sink - Math.max(0, -look.y) * 0.25), v.z * 0.98 + push.z);
		p.velocityModified = true;
		p.fallDistance = 0;
		if (p.age % 16 == 0) p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PHANTOM_FLAP,
				SoundCategory.PLAYERS, 0.2f, 1.5f);
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		EntityFlags.set(p, FLAG, false);
	}
}
