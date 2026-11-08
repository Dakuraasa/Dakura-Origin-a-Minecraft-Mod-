package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:charged_jump" — segure a tecla para carregar (como o pulo do cavalo) e solte para saltar.
 * { "max_charge": 30, "min_power": 0.6, "max_power": 1.55, "forward": 0.5 }
 */
public final class ChargedJumpAbility implements Ability {
	private final int maxCharge;
	private final double minPower, maxPower, forward;

	public ChargedJumpAbility(JsonObject json) {
		this.maxCharge = Math.max(1, JsonParams.integer(json, "max_charge", 30));
		this.minPower = JsonParams.num(json, "min_power", 0.6f);
		this.maxPower = JsonParams.num(json, "max_power", 1.55f);
		this.forward = JsonParams.num(json, "forward", 0.5f);
	}

	@Override
	public boolean chargeable() {
		return true;
	}

	@Override
	public void holdStart(ServerPlayerEntity p) {
		ModNetworking.sendAround(new AnimPayload(p.getId(), "crouch", maxCharge + 10), p);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		return release(p, 0);
	}

	@Override
	public boolean release(ServerPlayerEntity p, int held) {
		ModNetworking.sendAround(new AnimPayload(p.getId(), "crouch", 0), p);
		if (!p.isOnGround() && !p.isTouchingWater()) return false;
		double c = Math.min(1.0, held / (double) maxCharge);
		Vec3d look = p.getRotationVec(1f);
		Vec3d flat = new Vec3d(look.x, 0, look.z);
		flat = flat.lengthSquared() < 1e-4 ? Vec3d.ZERO : flat.normalize().multiply(forward * c);
		p.setVelocity(p.getVelocity().x + flat.x, minPower + (maxPower - minPower) * c, p.getVelocity().z + flat.z);
		p.velocityModified = true;
		p.fallDistance = 0;
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:goat_jump", (int) Math.round(c * 100)), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_GOAT_LONG_JUMP, SoundCategory.PLAYERS, 0.9f, 1.1f - 0.25f * (float) c);
		if (c > 0.7) p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_BREEZE_JUMP, SoundCategory.PLAYERS, 0.4f, 0.8f);
		return true;
	}
}
