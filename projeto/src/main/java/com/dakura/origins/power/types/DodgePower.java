package com.dakura.origins.power.types;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:dodge" — Oni: chance de desviar de um golpe (corpo a corpo ou projétil), deslizando para o lado
 * num rastro de imagens. A chance sobe enquanto "boost_flag" estiver ativa. { "chance": 0.15, "boosted": 0.45, "boost_flag": "bloodlust" }
 */
public final class DodgePower implements RacePower {
	private final float chance, boosted;
	private final String boostFlag;

	public DodgePower(JsonObject json) {
		this.chance = JsonParams.num(json, "chance", 0.15f);
		this.boosted = JsonParams.num(json, "boosted", 0.45f);
		this.boostFlag = JsonParams.str(json, "boost_flag", "bloodlust");
	}

	@Override
	public boolean allowDamage(ServerPlayerEntity p, DamageSource source, float amount) {
		Entity atk = source.getAttacker();
		if (atk == null || atk == p || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.isIn(DamageTypeTags.IS_EXPLOSION)) return true;
		float c = EntityFlags.has(p, boostFlag) ? boosted : chance;
		if (p.getRandom().nextFloat() >= c) return true;
		Vec3d from = (source.getSource() != null ? source.getSource() : atk).getPos();
		Vec3d to = p.getPos().subtract(from).multiply(1, 0, 1);
		to = to.lengthSquared() < 1e-4 ? new Vec3d(1, 0, 0) : to.normalize();
		Vec3d side = new Vec3d(-to.z, 0, to.x).multiply(p.getRandom().nextBoolean() ? 1 : -1);
		p.setVelocity(side.x * 0.75 + to.x * 0.15, 0.12, side.z * 0.75 + to.z * 0.15);
		p.velocityModified = true;
		boolean left = side.dotProduct(new Vec3d(-Math.sin(Math.toRadians(p.getYaw() + 90)), 0, Math.cos(Math.toRadians(p.getYaw() + 90)))) > 0;
		ModNetworking.sendAround(new AnimPayload(p.getId(), left ? "dodge_l" : "dodge_r", 8), p);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:dodge", 0), p);
		if (source.getSource() != null && source.getSource() != atk) source.getSource().discard();
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.6f, 1.9f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_BREEZE_JUMP, SoundCategory.PLAYERS, 0.35f, 1.6f);
		return false;
	}
}
