package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.FxPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:waterfowl_dance" — Dança das Aves Aquáticas (à la Malenia): a Boneca salta, gira numa dança
 * veloz e avança DUAS vezes, soltando uma chuva de cortes amaldiçoados numa área grande. Os cortes quebram
 * blocos frágeis (folhas, vidro, plantas, teias...). { "duration": 44, "radius": 4.5, "damage": 1.4, "interval": 3 }
 */
public final class WaterfowlDanceAbility implements Ability {
	public static final String FLAG = "dancing";
	private static final Map<UUID, int[]> ACTIVE = new HashMap<>();
	private static final Map<UUID, WaterfowlDanceAbility> OWNERS = new HashMap<>();
	private final int duration, interval;
	private final double radius;
	private final float damage;

	public WaterfowlDanceAbility(JsonObject json) {
		this.duration = JsonParams.integer(json, "duration", 44);
		this.radius = JsonParams.num(json, "radius", 4.5f);
		this.damage = JsonParams.num(json, "damage", 1.4f);
		this.interval = Math.max(1, JsonParams.integer(json, "interval", 3));
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(WaterfowlDanceAbility::tickAll);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		if (ACTIVE.containsKey(p.getUuid())) return false;
		ACTIVE.put(p.getUuid(), new int[]{0});
		OWNERS.put(p.getUuid(), this);
		EntityFlags.set(p, FLAG, true);
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 1, false, false, false));
		p.setVelocity(p.getVelocity().x * 0.3, 0.55, p.getVelocity().z * 0.3);
		p.velocityModified = true;
		ModNetworking.sendAround(new AnimPayload(p.getId(), "dance", duration + 4), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_BREEZE_CHARGE, SoundCategory.PLAYERS, 0.8f, 1.5f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1.0f, 0.6f);
		return true;
	}

	private static void tickAll(MinecraftServer server) {
		if (ACTIVE.isEmpty()) return;
		Iterator<Map.Entry<UUID, int[]>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			var en = it.next();
			ServerPlayerEntity p = server.getPlayerManager().getPlayer(en.getKey());
			WaterfowlDanceAbility ab = OWNERS.get(en.getKey());
			if (p == null || ab == null || !p.isAlive()) {
				it.remove();
				OWNERS.remove(en.getKey());
				if (p != null) EntityFlags.set(p, FLAG, false);
				continue;
			}
			int age = ++en.getValue()[0];
			p.fallDistance = 0;
			// os dois únicos avanços: logo no começo e no meio da dança
			if (age == 8 || age == ab.duration / 2 + 4) {
				Vec3d look = p.getRotationVec(1f), dir = new Vec3d(look.x, 0, look.z);
				dir = dir.lengthSquared() < 1e-4 ? Vec3d.ZERO : dir.normalize();
				p.setVelocity(dir.x * 1.35, 0.32, dir.z * 1.35);
				p.velocityModified = true;
				p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_BREEZE_JUMP, SoundCategory.PLAYERS, 0.9f, 1.3f);
				ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:dance_dash", 0), p);
			} else if (age > 8 && p.getVelocity().y < -0.05) {
				// paira no ar durante a dança, como a Malenia
				p.setVelocity(p.getVelocity().x * 0.9, -0.02, p.getVelocity().z * 0.9);
				p.velocityModified = true;
			}
			if (age >= 6 && age % ab.interval == 0) ab.slash(p, age);
			if (age >= ab.duration) {
				it.remove();
				OWNERS.remove(en.getKey());
				EntityFlags.set(p, FLAG, false);
				ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:dance_end", 0), p);
				p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 0.7f);
			}
		}
	}

	private void slash(ServerPlayerEntity p, int age) {
		ServerWorld w = p.getServerWorld();
		Vec3d c = p.getPos().add(0, p.getHeight() * 0.55, 0);
		for (Entity e : w.getOtherEntities(p, p.getBoundingBox().expand(radius, 2.5, radius))) {
			if (!(e instanceof LivingEntity le) || !le.isAlive() || e.squaredDistanceTo(c) > radius * radius) continue;
			le.timeUntilRegen = 0;
			le.damage(p.getDamageSources().playerAttack(p), damage);
			le.setVelocity(le.getVelocity().multiply(0.4));
		}
		// cortes cheios de pressão: quebram o que for frágil em volta
		if (w.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
			for (int i = 0; i < 10; i++) {
				BlockPos bp = BlockPos.ofFloored(c.x + (w.random.nextDouble() - 0.5) * radius * 2, c.y + (w.random.nextDouble() - 0.6) * 3,
						c.z + (w.random.nextDouble() - 0.5) * radius * 2);
				BlockState st = w.getBlockState(bp);
				if (st.isAir()) continue;
				float h = st.getHardness(w, bp);
				if (h >= 0 && h <= 0.35f && p.canModifyBlocks() && w.canPlayerModifyAt(p, bp)) w.breakBlock(bp, true, p);
			}
		}
		float yaw = w.random.nextFloat() * 360f, tilt = (w.random.nextFloat() - 0.5f) * 140f;
		ModNetworking.sendNear(w, c, 64, new FxPayload("dance_slash", c.x, c.y, c.z, yaw, tilt, (float) radius));
		if (age % (interval * 2) == 0) w.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.7f,
				1.3f + w.random.nextFloat() * 0.5f);
		else w.playSound(null, c.x, c.y, c.z, SoundEvents.ENTITY_PLAYER_ATTACK_NODAMAGE, SoundCategory.PLAYERS, 0.4f, 1.9f);
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		if (ACTIVE.remove(p.getUuid()) != null) EntityFlags.set(p, FLAG, false);
		OWNERS.remove(p.getUuid());
	}
}
