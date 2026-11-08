package com.dakura.origins.power.ability;

import com.dakura.origins.network.FxPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * "dakura_origins:vine_snare" — vinhas correm pelo chão na direção do olhar; a primeira criatura
 * atingida fica presa (não anda nem pula) e depois muito lenta.
 * { "range": 16, "speed": 0.9, "root_ticks": 50, "slow_ticks": 80, "damage": 2 }
 */
public final class VineSnareAbility implements Ability {
	private static final List<Vine> VINES = new ArrayList<>();

	private static final class Vine {
		final VineSnareAbility a;
		final ServerPlayerEntity owner;
		final ServerWorld world;
		Vec3d pos;
		final Vec3d dir;
		double travelled;

		Vine(VineSnareAbility a, ServerPlayerEntity owner) {
			this.a = a;
			this.owner = owner;
			this.world = owner.getServerWorld();
			Vec3d look = owner.getRotationVec(1f);
			Vec3d flat = new Vec3d(look.x, 0, look.z);
			this.dir = flat.lengthSquared() < 1e-4 ? new Vec3d(1, 0, 0) : flat.normalize();
			this.pos = owner.getPos();
		}
	}

	private final double range, speed;
	private final int rootTicks, slowTicks;
	private final float damage;

	public VineSnareAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 16f);
		this.speed = JsonParams.num(json, "speed", 0.9f);
		this.rootTicks = JsonParams.integer(json, "root_ticks", 50);
		this.slowTicks = JsonParams.integer(json, "slow_ticks", 80);
		this.damage = JsonParams.num(json, "damage", 2f);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(VineSnareAbility::tickAll);
	}

	@Override
	public boolean activate(ServerPlayerEntity player) {
		VINES.add(new Vine(this, player));
		player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BLOCK_CAVE_VINES_PLACE, SoundCategory.PLAYERS, 1f, 0.8f);
		ModNetworking.sendAround(new com.dakura.origins.network.AnimPayload(player.getId(), "cast", 10), player);
		return true;
	}

	private static void tickAll(MinecraftServer server) {
		if (VINES.isEmpty()) return;
		VINES.removeIf(v -> !v.a.step(v));
	}

	/** @return false quando a vinha acaba */
	private boolean step(Vine v) {
		if (v.owner.isRemoved()) return false;
		Vec3d next = v.pos.add(v.dir.multiply(speed));
		// segue o relevo: procura o chão até 2 blocos acima/abaixo
		BlockPos bp = BlockPos.ofFloored(next);
		Double ground = null;
		for (int dy = 2; dy >= -2; dy--) {
			BlockPos p = bp.up(dy);
			if (v.world.isAir(p.up()) || !v.world.getBlockState(p.up()).isSolidBlock(v.world, p.up())) {
				if (v.world.getBlockState(p).isSolidBlock(v.world, p)) {
					ground = p.getY() + 1.0;
					break;
				}
			}
		}
		if (ground == null) return false;
		v.pos = new Vec3d(next.x, ground, next.z);
		v.travelled += speed;
		ModNetworking.sendNear(v.world, v.pos, 64, new FxPayload("vine", v.pos.x, v.pos.y, v.pos.z,
				(float) v.dir.x, 0, (float) v.dir.z));
		if (((int) (v.travelled / speed)) % 3 == 0) {
			v.world.playSound(null, v.pos.x, v.pos.y, v.pos.z, SoundEvents.BLOCK_ROOTS_PLACE, SoundCategory.PLAYERS,
					0.5f, 0.9f + v.world.random.nextFloat() * 0.3f);
		}
		for (var e : v.world.getOtherEntities(v.owner, new Box(v.pos, v.pos).expand(0.9, 1.2, 0.9))) {
			if (!(e instanceof LivingEntity t) || !t.isAlive()) continue;
			snare(v, t);
			return false;
		}
		return v.travelled < range;
	}

	private void snare(Vine v, LivingEntity t) {
		t.damage(v.owner.getDamageSources().playerAttack(v.owner), damage);
		t.setVelocity(0, Math.min(0, t.getVelocity().y), 0);
		t.velocityModified = true;
		t.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, rootTicks, 9, false, false, true), v.owner);
		t.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, rootTicks, 128, false, false, false), v.owner);
		t.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, rootTicks + slowTicks, 3, false, true, true), v.owner);
		ModNetworking.sendNear(v.world, t.getPos(), 64, new FxPayload("vine_snare", t.getX(), t.getY(), t.getZ(),
				t.getWidth(), t.getHeight(), rootTicks));
		v.world.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.BLOCK_HANGING_ROOTS_PLACE, SoundCategory.PLAYERS, 1f, 0.7f);
		v.world.playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.ITEM_BONE_MEAL_USE, SoundCategory.PLAYERS, 0.8f, 0.6f);
	}
}
