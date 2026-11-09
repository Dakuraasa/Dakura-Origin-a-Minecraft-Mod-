package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * "dakura_origins:brutal_charge" — Investida Brutal: dispara em linha reta atropelando tudo no caminho
 * e termina com uma pancada no chão. { "ticks": 14, "speed": 0.95, "damage": 7, "slam": 4 }
 */
public final class BrutalChargeAbility implements Ability {
	public static final String FLAG = "charging";
	private static final Map<UUID, Object[]> ACTIVE = new HashMap<>(); // {hab, dir, idade, atingidos}
	private final int ticks;
	private final double speed;
	private final float damage, slam;

	public BrutalChargeAbility(JsonObject json) {
		this.ticks = JsonParams.integer(json, "ticks", 14);
		this.speed = JsonParams.num(json, "speed", 0.95f);
		this.damage = JsonParams.num(json, "damage", 7f);
		this.slam = JsonParams.num(json, "slam", 4f);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(BrutalChargeAbility::tickAll);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		if (ACTIVE.containsKey(p.getUuid())) return false;
		Vec3d look = p.getRotationVec(1f), dir = new Vec3d(look.x, 0, look.z);
		if (dir.lengthSquared() < 1e-4) return false;
		ACTIVE.put(p.getUuid(), new Object[]{this, dir.normalize(), new int[]{0}, new HashSet<UUID>()});
		EntityFlags.set(p, FLAG, true);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "ram", ticks + 6), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_RAVAGER_STEP, SoundCategory.PLAYERS, 1.0f, 0.7f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_HORSE_BREATHE, SoundCategory.PLAYERS, 1.0f, 0.5f);
		return true;
	}

	@SuppressWarnings("unchecked")
	private static void tickAll(MinecraftServer server) {
		if (ACTIVE.isEmpty()) return;
		Iterator<Map.Entry<UUID, Object[]>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			var en = it.next();
			ServerPlayerEntity p = server.getPlayerManager().getPlayer(en.getKey());
			Object[] st = en.getValue();
			BrutalChargeAbility ab = (BrutalChargeAbility) st[0];
			Vec3d dir = (Vec3d) st[1];
			int age = ++((int[]) st[2])[0];
			Set<UUID> hit = (Set<UUID>) st[3];
			if (p == null || !p.isAlive()) {
				it.remove();
				if (p != null) EntityFlags.set(p, FLAG, false);
				continue;
			}
			if (age > ab.ticks || p.horizontalCollision) {
				it.remove();
				EntityFlags.set(p, FLAG, false);
				for (Entity e : p.getWorld().getOtherEntities(p, p.getBoundingBox().expand(2.5, 1, 2.5))) {
					if (e instanceof LivingEntity le && le.isAlive()) {
						le.damage(p.getDamageSources().playerAttack(p), ab.slam);
						le.addVelocity(0, 0.45, 0);
						le.velocityModified = true;
					}
				}
				ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:meteor", 25), p);
				p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.PLAYERS, 1.0f, 0.6f);
				p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.3f, 0.5f);
				continue;
			}
			p.setVelocity(dir.x * ab.speed, Math.min(p.getVelocity().y, 0.1), dir.z * ab.speed);
			p.velocityModified = true;
			for (Entity e : p.getWorld().getOtherEntities(p, p.getBoundingBox().expand(0.9))) {
				if (!(e instanceof LivingEntity le) || !le.isAlive() || !hit.add(e.getUuid())) continue;
				le.damage(p.getDamageSources().playerAttack(p), ab.damage);
				Vec3d side = new Vec3d(-dir.z, 0, dir.x).multiply(e.getPos().subtract(p.getPos()).dotProduct(new Vec3d(-dir.z, 0, dir.x)) >= 0 ? 1 : -1);
				le.setVelocity(dir.multiply(1.1).add(side.multiply(0.8)).add(0, 0.5, 0));
				le.velocityModified = true;
				p.getWorld().playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, SoundCategory.PLAYERS, 1.0f, 0.7f);
			}
			if (age % 3 == 0) p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_RAVAGER_STEP, SoundCategory.PLAYERS, 0.6f, 0.9f);
		}
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		if (ACTIVE.remove(p.getUuid()) != null) EntityFlags.set(p, FLAG, false);
	}
}
