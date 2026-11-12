package com.dakura.origins.power.ability;

import com.dakura.origins.entity.GeoProjectileEntity;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * "dakura_origins:moonfall" — Queda da Lua (Elfo da Lua): ergue os braços e invoca uma lua branca enorme (estrutura das
 * esferas da Kitsune, maior, branca, lenta e teleguiada). Ao ativar, todos por perto ficam cegos e muito lentos; durante
 * a invocação ele não pode atacar. A lua cai no alvo da mira (ou no ponto mirado) abrindo uma cratera (respeita
 * mobGriefing) e causando dano muito alto.
 * { "windup": 50, "radius": 10, "damage": 22, "blast": 6, "crater": 4.5, "range": 40 }
 */
public final class MoonfallAbility implements Ability {
	private final int windup;
	private final double radius, range;
	private final float damage, blast, crater;

	public MoonfallAbility(JsonObject json) {
		this.windup = JsonParams.integer(json, "windup", 50);
		this.radius = JsonParams.num(json, "radius", 10f);
		this.range = JsonParams.num(json, "range", 40f);
		this.damage = JsonParams.num(json, "damage", 22f);
		this.blast = JsonParams.num(json, "blast", 6f);
		this.crater = JsonParams.num(json, "crater", 4.5f);
	}

	public static void bootstrap() {
		// invocando a lua: nada de ataques
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) ->
				!world.isClient && player instanceof ServerPlayerEntity sp && LegendKit.running(sp, "moonfall_cast") ? ActionResult.FAIL : ActionResult.PASS);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		if (LegendKit.running(p, "moonfall_cast") || LegendKit.running(p, "moonfall_fly")) return false;
		ServerWorld w = p.getServerWorld();
		for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(radius), x -> x != p && x.isAlive() && !x.isSpectator())) {
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 120, 1));
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 120, 5));
		}
		LegendKit.anim(p, "summon_up", windup + 10);
		LegendKit.snd(p, SoundEvents.BLOCK_BEACON_POWER_SELECT, 1.2f, 0.5f);
		LegendKit.snd(p, SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, 1f, 0.6f);
		GeoProjectileEntity moon = new GeoProjectileEntity(w, p, GeoProjectileEntity.MOON, 0, 1, 600);
		moon.scripted = true;
		moon.setSize(0.4f);
		Vec3d top = p.getPos().add(0, p.getHeight() + 3.5, 0);
		moon.setPosition(top.x, top.y, top.z);
		w.spawnEntity(moon);
		LegendKit.run(p, "moonfall_cast", windup, (pl, age) -> {
			if (moon.isRemoved()) return false;
			Vec3d at = pl.getPos().add(0, pl.getHeight() + 3.5, 0);
			moon.setPosition(at.x, at.y, at.z);
			moon.setVelocity(Vec3d.ZERO);
			moon.setSize(0.4f + 2.6f * age / windup);
			if (age % 4 == 0) w.spawnParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 4, 1.2, 1.2, 1.2, 0.02);
			if (age % 10 == 0) NightFx.burst(w, pl.getPos().add(0, 1, 0), 3, 0.6);
			return true;
		}, pl -> {
			if (moon.isRemoved()) return;
			LivingEntity target = LegendKit.aim(pl, range, 0.85);
			Vec3d point = aimPoint(pl);
			LegendKit.snd(pl, SoundEvents.ENTITY_WITHER_SHOOT, 1f, 0.5f);
			fly(pl, w, moon, target, point);
		});
		return true;
	}

	private Vec3d aimPoint(ServerPlayerEntity p) {
		Vec3d eye = p.getEyePos(), end = eye.add(p.getRotationVec(1f).multiply(range));
		BlockHitResult hit = p.getWorld().raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
		return hit.getType() == HitResult.Type.MISS ? end : hit.getPos();
	}

	private void fly(ServerPlayerEntity p, ServerWorld w, GeoProjectileEntity moon, LivingEntity target, Vec3d point) {
		int id = target == null ? -1 : target.getId();
		LegendKit.run(p, "moonfall_fly", 200, (pl, age) -> {
			if (moon.isRemoved()) return false;
			Vec3d goal = id >= 0 && w.getEntityById(id) instanceof LivingEntity t && t.isAlive() ? t.getPos().add(0, t.getHeight() * 0.5, 0) : point;
			Vec3d to = goal.subtract(moon.getPos());
			double d = to.length();
			boolean ground = !w.getBlockState(net.minecraft.util.math.BlockPos.ofFloored(moon.getPos())).getCollisionShape(w,
					net.minecraft.util.math.BlockPos.ofFloored(moon.getPos())).isEmpty();
			if (d < 1.6 || ground || age >= 199) {
				impact(pl, w, moon.getPos());
				moon.discard();
				return false;
			}
			double speed = Math.min(d, 0.35 + age * 0.012);
			moon.setVelocity(moon.getVelocity().multiply(0.8).add(to.normalize().multiply(speed * 0.25)));
			moon.velocityModified = true;
			if (age % 3 == 0) w.spawnParticles(ParticleTypes.END_ROD, moon.getX(), moon.getY() + 0.5, moon.getZ(), 3, 1, 1, 1, 0.01);
			return true;
		}, pl -> {
			if (!moon.isRemoved()) moon.discard();
		});
	}

	private void impact(ServerPlayerEntity p, ServerWorld w, Vec3d c) {
		SpiritOrbAbility.breakBlocks(w, p, c, crater);
		for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new net.minecraft.util.math.Box(c, c).expand(blast), x -> x != p && x.isAlive())) {
			double k = 1 - Math.min(1, e.getPos().distanceTo(c) / (blast + 1));
			LegendKit.hurt(p, e, (float) (damage * (0.45 + 0.55 * k)), false);
			e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 4));
			Vec3d push = e.getPos().subtract(c).normalize().multiply(1.2 * k).add(0, 0.5 * k, 0);
			e.addVelocity(push.x, push.y, push.z);
			e.velocityModified = true;
		}
		w.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 2, 1, 0.5, 1, 0);
		w.spawnParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 80, 2.5, 1.5, 2.5, 0.25);
		w.spawnParticles(ParticleTypes.FLASH, c.x, c.y + 1, c.z, 3, 0.5, 0.5, 0.5, 0);
		NightFx.burst(w, c.add(0, 1, 0), 30, 2.5);
		LegendKit.sndAt(w, c, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), 2f, 0.5f);
		LegendKit.sndAt(w, c, SoundEvents.BLOCK_BEACON_DEACTIVATE, 2f, 0.5f);
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "moonfall_cast");
		LegendKit.stop(p, "moonfall_fly");
	}
}
