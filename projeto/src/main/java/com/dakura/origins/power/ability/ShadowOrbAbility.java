package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.FxPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * "dakura_origins:shadow_orb" — esfera de sombra lenta e ondulante: quem for atingido fica cego por muito tempo
 * (cegueira + escuridão) e leva dano. { "speed": 0.9, "damage": 5, "blind": 200 }
 */
public final class ShadowOrbAbility implements Ability {
	private static final List<Orb> ORBS = new ArrayList<>();
	private final double speed;
	private final float damage;
	private final int blind;

	private static final class Orb {
		final ShadowOrbAbility ab;
		final ServerPlayerEntity owner;
		Vec3d pos, vel;
		int age;

		Orb(ShadowOrbAbility ab, ServerPlayerEntity owner, Vec3d pos, Vec3d vel) {
			this.ab = ab;
			this.owner = owner;
			this.pos = pos;
			this.vel = vel;
		}
	}

	public ShadowOrbAbility(JsonObject json) {
		this.speed = JsonParams.num(json, "speed", 0.9f);
		this.damage = JsonParams.num(json, "damage", 5f);
		this.blind = JsonParams.integer(json, "blind", 200);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(ShadowOrbAbility::tickAll);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		Vec3d look = p.getRotationVec(1f);
		ORBS.add(new Orb(this, p, p.getEyePos().add(look.multiply(0.8)).add(0, -0.2, 0), look.multiply(speed)));
		ModNetworking.sendAround(new AnimPayload(p.getId(), "cast", 12), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 0.35f, 1.8f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.5f, 0.5f);
		return true;
	}

	private static void tickAll(MinecraftServer server) {
		if (ORBS.isEmpty()) return;
		Iterator<Orb> it = ORBS.iterator();
		while (it.hasNext()) {
			Orb o = it.next();
			ServerWorld w = o.owner.getServerWorld();
			if (o.owner.isRemoved() || ++o.age > 60) {
				ModNetworking.sendNear(w, o.pos, 64, new FxPayload("shadow_burst", o.pos.x, o.pos.y, o.pos.z, 0, 0, 0));
				it.remove();
				continue;
			}
			// ondula levemente enquanto voa
			Vec3d wobble = new Vec3d(Math.sin(o.age * 0.5) * 0.03, Math.cos(o.age * 0.4) * 0.03, 0);
			Vec3d next = o.pos.add(o.vel).add(wobble);
			var hit = w.raycast(new RaycastContext(o.pos, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, o.owner));
			LivingEntity victim = null;
			for (Entity e : w.getOtherEntities(o.owner, new Box(o.pos, next).expand(0.6),
					x -> x instanceof LivingEntity l && l.isAlive() && !x.isSpectator())) {
				victim = (LivingEntity) e;
				break;
			}
			if (victim != null || hit.getType() != HitResult.Type.MISS) {
				Vec3d at = victim != null ? victim.getPos().add(0, victim.getHeight() / 2, 0) : hit.getPos();
				if (victim != null) {
					victim.damage(o.owner.getDamageSources().indirectMagic(o.owner, o.owner), o.ab.damage);
					victim.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, o.ab.blind, 0));
					victim.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, o.ab.blind / 2, 0));
					victim.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1));
				}
				ModNetworking.sendNear(w, at, 64, new FxPayload("shadow_burst", at.x, at.y, at.z, victim != null ? 1 : 0, 0, 0));
				w.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 0.7f, 1.4f);
				w.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_SCULK_SHRIEKER_BREAK, SoundCategory.PLAYERS, 0.8f, 0.8f);
				it.remove();
				continue;
			}
			o.pos = next;
			ModNetworking.sendNear(w, next, 64, new FxPayload("shadow_orb", next.x, next.y, next.z, (float) o.vel.x, (float) o.vel.y, (float) o.vel.z));
			if (o.age % 8 == 0) w.playSound(null, next.x, next.y, next.z, SoundEvents.BLOCK_SCULK_SENSOR_CLICKING, SoundCategory.PLAYERS, 0.3f, 0.6f);
		}
	}
}
