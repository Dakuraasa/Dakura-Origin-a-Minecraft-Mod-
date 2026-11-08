package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:dash" — salto ágil para trás (ou na direção que anda, se "forward": true),
 * com velocidade breve depois. { "power": 1.3, "lift": 0.45, "forward": false }
 */
public final class ElfDashAbility implements Ability {
	private final double power, lift;
	private final boolean forward;

	public ElfDashAbility(JsonObject json) {
		this.power = JsonParams.num(json, "power", 1.3f);
		this.lift = JsonParams.num(json, "lift", 0.45f);
		this.forward = JsonParams.bool(json, "forward", false);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		Vec3d look = p.getRotationVec(1f);
		Vec3d flat = new Vec3d(look.x, 0, look.z);
		flat = flat.lengthSquared() < 1e-4 ? Vec3d.ZERO : flat.normalize().multiply(forward ? power : -power);
		p.setVelocity(flat.x, lift, flat.z);
		p.velocityModified = true;
		p.fallDistance = 0;
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 1, true, false, true));
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:dash", 0), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_BREEZE_JUMP, SoundCategory.PLAYERS, 0.7f, 1.3f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value(), SoundCategory.PLAYERS, 0.6f, 1.4f);
		return true;
	}
}
