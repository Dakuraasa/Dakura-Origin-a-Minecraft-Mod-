package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.types.MaturityPower;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
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
 * "dakura_origins:stomp" — Pisão: ergue a perna, desce com tudo e a onda de choque
 * arremessa para cima quem estiver no raio. Não quebra blocos.
 * { "radius": 4, "damage": 8, "launch": 0.9, "windup": 9, "maturity_damage": [1, 1.4, 1.8] }
 */
public final class StompAbility implements Ability {
	private static final Map<UUID, Pending> PENDING = new HashMap<>();

	private record Pending(StompAbility ability, ServerPlayerEntity player, int fireAt) {}

	private final double radius;
	private final float damage;
	private final double launch;
	private final int windup;
	private final float[] maturityDamage;
	private static int serverTicks;

	public StompAbility(JsonObject json) {
		this.radius = JsonParams.num(json, "radius", 4f);
		this.damage = JsonParams.num(json, "damage", 8f);
		this.launch = JsonParams.num(json, "launch", 0.9f);
		this.windup = JsonParams.integer(json, "windup", 9);
		this.maturityDamage = JsonParams.floats(json, "maturity_damage");
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(StompAbility::tickAll);
	}

	@Override
	public boolean activate(ServerPlayerEntity player) {
		if (PENDING.containsKey(player.getUuid())) return false;
		PENDING.put(player.getUuid(), new Pending(this, player, serverTicks + windup));
		ModNetworking.sendAround(new AnimPayload(player.getId(), "stomp", windup + 8), player);
		player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENTITY_RAVAGER_STEP, SoundCategory.PLAYERS, 0.9f, 0.6f);
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		PENDING.remove(player.getUuid());
	}

	private static void tickAll(MinecraftServer server) {
		serverTicks++;
		if (PENDING.isEmpty()) return;
		Iterator<Pending> it = PENDING.values().iterator();
		while (it.hasNext()) {
			Pending p = it.next();
			if (p.player.isRemoved() || !p.player.isAlive()) {
				it.remove();
				continue;
			}
			if (serverTicks >= p.fireAt) {
				it.remove();
				p.ability.impact(p.player);
			}
		}
	}

	private void impact(ServerPlayerEntity owner) {
		ServerWorld world = owner.getServerWorld();
		Vec3d at = owner.getPos();
		double r = radius * Math.max(1.0, owner.getScale() * 0.4);
		float hit = damage * MaturityPower.multiplier(owner, maturityDamage);
		for (Entity e : world.getOtherEntities(owner, new Box(at, at).expand(r, 2.5, r))) {
			if (!(e instanceof LivingEntity target) || !target.isAlive()) continue;
			double d = Math.sqrt(e.squaredDistanceTo(at));
			if (d > r) continue;
			float falloff = (float) (1.0 - 0.5 * d / r);
			target.damage(owner.getDamageSources().playerAttack(owner), hit * falloff);
			Vec3d push = e.getPos().subtract(at).multiply(1, 0, 1);
			push = push.lengthSquared() < 1e-4 ? Vec3d.ZERO : push.normalize().multiply(0.35 * falloff);
			double kr = target instanceof LivingEntity l ? 1.0 - l.getAttributeValue(net.minecraft.entity.attribute.EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE) * 0.5 : 1.0;
			target.addVelocity(push.x * kr, launch * falloff * kr, push.z * kr);
			target.velocityModified = true;
		}
		ModNetworking.sendAround(new AnimPayload(owner.getId(), "fx:stomp", (int) Math.round(r * 10)), owner);
		// peso do dragão: pedaços do chão voam (só visual, o terreno não muda) e uma nuvem de poeira sobe
		LegendKit.fxAt(world, "debris", at, (int) Math.round(r * 7), (float) r, 0);
		LegendKit.fxAt(world, "dust", at, (float) r, 0, 0);
		world.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 0.6f, 1.3f);
		world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.35f, 0.5f);
		world.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.PLAYERS, 0.9f, 0.6f);
	}
}
