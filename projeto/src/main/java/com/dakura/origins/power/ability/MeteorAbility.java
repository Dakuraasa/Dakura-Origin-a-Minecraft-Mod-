package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:meteor" — salta altíssimo, vira pedra e despenca: o impacto fere tudo em volta
 * (como o Pisão, mas sem arremessar ninguém). Sem dano de queda. { "launch": 1.9, "fall": 2.6, "radius": 4.5, "damage": 12 }
 */
public final class MeteorAbility implements Ability {
	public static final String FLAG = "meteor";
	private static final Map<UUID, int[]> ACTIVE = new HashMap<>(); // fase, idade
	private static final Map<UUID, MeteorAbility> OWNERS = new HashMap<>();
	private final double launch, fall, radius, push;
	private final float damage;

	public MeteorAbility(JsonObject json) {
		this.launch = JsonParams.num(json, "launch", 1.9f);
		this.fall = JsonParams.num(json, "fall", 2.6f);
		this.radius = JsonParams.num(json, "radius", 4.5f);
		this.damage = JsonParams.num(json, "damage", 12f);
		this.push = JsonParams.num(json, "push", 0f);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(MeteorAbility::tickAll);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		if (ACTIVE.containsKey(p.getUuid())) return false;
		if (p.getAbilities().flying) {
			p.getAbilities().flying = false;
			p.sendAbilitiesUpdate();
		}
		Vec3d look = p.getRotationVec(1f);
		p.setVelocity(look.x * 0.25, launch, look.z * 0.25);
		p.velocityModified = true;
		p.fallDistance = 0;
		ACTIVE.put(p.getUuid(), new int[]{0, 0});
		OWNERS.put(p.getUuid(), this);
		EntityFlags.set(p, FLAG, true);
		p.getServerWorld().spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.STONE.getDefaultState()),
				p.getX(), p.getY() + 0.2, p.getZ(), 16, 0.4, 0.1, 0.4, 0.1);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_BREEZE_JUMP, SoundCategory.PLAYERS, 0.8f, 0.7f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_STONE_PLACE, SoundCategory.PLAYERS, 0.6f, 0.6f);
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		ACTIVE.remove(p.getUuid());
		OWNERS.remove(p.getUuid());
		EntityFlags.set(p, FLAG, false);
	}

	private static void tickAll(MinecraftServer server) {
		if (ACTIVE.isEmpty()) return;
		Iterator<Map.Entry<UUID, int[]>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			var en = it.next();
			ServerPlayerEntity p = server.getPlayerManager().getPlayer(en.getKey());
			MeteorAbility ab = OWNERS.get(en.getKey());
			if (p == null || !p.isAlive() || ab == null) {
				if (p != null) EntityFlags.set(p, FLAG, false);
				it.remove();
				continue;
			}
			int[] st = en.getValue();
			st[1]++;
			p.fallDistance = 0;
			if (st[0] == 0) {
				if ((p.getVelocity().y <= 0.08 && st[1] > 4) || st[1] > 40) {
					st[0] = 1;
					p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_DEEPSLATE_PLACE, SoundCategory.PLAYERS, 0.9f, 0.5f);
				}
			} else {
				Vec3d v = p.getVelocity();
				p.setVelocity(v.x * 0.9, -ab.fall, v.z * 0.9);
				p.velocityModified = true;
				if ((p.isOnGround() && st[1] > 2) || p.isTouchingWater() || st[1] > 160) {
					it.remove();
					ab.impact(p);
				}
			}
		}
	}

	private void impact(ServerPlayerEntity p) {
		EntityFlags.set(p, FLAG, false);
		OWNERS.remove(p.getUuid());
		ServerWorld w = p.getServerWorld();
		Vec3d at = p.getPos();
		p.setVelocity(Vec3d.ZERO);
		p.velocityModified = true;
		if (!p.isTouchingWater()) {
			for (Entity e : w.getOtherEntities(p, new Box(at, at).expand(radius, 2.5, radius))) {
				if (!(e instanceof LivingEntity t) || !t.isAlive()) continue;
				double d = Math.sqrt(e.squaredDistanceTo(at));
				if (d > radius) continue;
				t.damage(p.getDamageSources().playerAttack(p), damage * (float) (1.0 - 0.5 * d / radius));
				if (push > 0) {
					Vec3d away = e.getPos().subtract(at).multiply(1, 0, 1);
					away = away.lengthSquared() < 1e-4 ? new Vec3d(1, 0, 0) : away.normalize();
					double f = push * (1.0 - 0.4 * d / radius);
					t.setVelocity(away.x * f, 0.35 + 0.15 * f, away.z * f);
					t.velocityModified = true;
				}
			}
		}
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:meteor", (int) Math.round(radius * 10)), p);
		w.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 0.7f, 0.8f);
		w.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.4f, 0.45f);
		w.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1.0f, 0.5f);
	}
}
