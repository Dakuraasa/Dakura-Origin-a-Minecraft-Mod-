package com.dakura.origins.power.ability;

import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:tether" — Laço Áureo (Alto Elfo), no estilo da Physgun do Garry's Mod: um feixe dourado
 * agarra o alvo na mira e o mantém suspenso à frente, seguindo o olhar, por até 10 s. A câmera e o movimento
 * ficam livres. A rodinha aproxima/afasta; o botão de ataque arremessa o alvo com força na direção da mira,
 * e ele sofre dano de impacto ao bater no chão, em paredes ou em outras criaturas. Usar de novo solta.
 * { "range": 20, "duration": 200, "throw": 2.8, "impact": 4 }
 */
public final class TetherAbility implements Ability {
	public static final String FLAG = "physgun";
	private static final Map<UUID, Session> ACTIVE = new HashMap<>();
	private static final Map<LivingEntity, Thrown> THROWN = new HashMap<>();
	private final double range, throwSpeed;
	private final int duration;
	private final float impact;

	private static final class Session {
		final ServerPlayerEntity p;
		final LivingEntity t;
		double dist;
		int age;
		final int duration;
		final double throwSpeed;
		final float impact;

		Session(ServerPlayerEntity p, LivingEntity t, double dist, int duration, double throwSpeed, float impact) {
			this.p = p;
			this.t = t;
			this.dist = dist;
			this.duration = duration;
			this.throwSpeed = throwSpeed;
			this.impact = impact;
		}
	}

	private record Thrown(ServerPlayerEntity by, float impact, int[] age, Vec3d[] lastVel) {}

	public TetherAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 20f);
		this.duration = JsonParams.integer(json, "duration", 200);
		this.throwSpeed = JsonParams.num(json, "throw", 2.8f);
		this.impact = JsonParams.num(json, "impact", 4f);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(TetherAbility::tickAll);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		Session old = ACTIVE.get(p.getUuid());
		if (old != null) {
			ACTIVE.remove(p.getUuid());
			release(old, false);
			return false;
		}
		LivingEntity t = TargetUtil.aimed(p, range, e -> !TargetUtil.boss(e));
		if (t == null) {
			p.sendMessage(Text.translatable("dakura_origins.message.no_target").formatted(Formatting.GRAY), true);
			return false;
		}
		double d = MathHelper.clamp(p.getEyePos().distanceTo(t.getPos().add(0, t.getHeight() / 2, 0)), 3, 12);
		ACTIVE.put(p.getUuid(), new Session(p, t, d, duration, throwSpeed, impact));
		EntityFlags.set(p, FLAG, true);
		if (t instanceof MobEntity m) m.setTarget(null);
		LegendKit.fx(p, "physgun", t.getId());
		LegendKit.anim(p, "reach", duration);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.8f, 1.6f);
		p.getWorld().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1.0f, 1.3f);
		return true;
	}

	@Override
	public void scroll(ServerPlayerEntity p, int delta) {
		Session s = ACTIVE.get(p.getUuid());
		if (s != null) s.dist = MathHelper.clamp(s.dist + Math.signum(delta) * 0.9, 2, 14);
	}

	/** Botão de ataque com o alvo preso: arremesso violento na direção da mira. */
	@Override
	public void attackKey(ServerPlayerEntity p) {
		Session s = ACTIVE.remove(p.getUuid());
		if (s != null) release(s, true);
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		Session s = ACTIVE.remove(p.getUuid());
		if (s != null) release(s, false);
	}

	public static boolean holding(ServerPlayerEntity p) {
		return ACTIVE.containsKey(p.getUuid());
	}

	private static void release(Session s, boolean hurl) {
		EntityFlags.set(s.p, FLAG, false);
		LegendKit.anim(s.p, "reach", 0);
		if (!s.t.isAlive()) return;
		if (hurl) {
			Vec3d dir = s.p.getRotationVec(1f);
			s.t.setVelocity(dir.multiply(s.throwSpeed).add(0, 0.25, 0));
			s.t.velocityModified = true;
			THROWN.put(s.t, new Thrown(s.p, s.impact, new int[]{0}, new Vec3d[]{s.t.getVelocity()}));
			LegendKit.fx(s.t, "physgun_throw", 0);
			LegendKit.anim(s.p, "throw", 10);
			LegendKit.snd(s.t, SoundEvents.ITEM_TRIDENT_THROW, 1.2f, 0.7f);
			LegendKit.snd(s.p, SoundEvents.BLOCK_BEACON_DEACTIVATE, 0.8f, 1.8f);
		} else {
			s.t.setVelocity(s.t.getVelocity().multiply(0.3));
			s.t.velocityModified = true;
		}
	}

	private static void tickAll(MinecraftServer server) {
		if (!ACTIVE.isEmpty()) {
			Iterator<Session> it = ACTIVE.values().iterator();
			while (it.hasNext()) {
				Session s = it.next();
				s.age++;
				boolean end = s.age >= s.duration || !s.p.isAlive() || s.p.isRemoved() || !s.t.isAlive() || s.t.isRemoved()
						|| s.t.getWorld() != s.p.getWorld() || s.p.distanceTo(s.t) > 30;
				if (end) {
					it.remove();
					release(s, false);
					continue;
				}
				// suspenso à frente da mira, seguindo o olhar sem prender a câmera
				Vec3d hold = s.p.getEyePos().add(s.p.getRotationVec(1f).multiply(s.dist)).add(0, -s.t.getHeight() / 2, 0);
				Vec3d vel = hold.subtract(s.t.getPos()).multiply(0.5);
				if (vel.length() > 1.8) vel = vel.normalize().multiply(1.8);
				s.t.setVelocity(vel);
				s.t.velocityModified = true;
				s.t.fallDistance = 0;
				if (s.t instanceof MobEntity m) {
					m.setTarget(null);
					m.getNavigation().stop();
				}
				if (s.age % 20 == 0) s.t.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 30, 1, true, false, true));
				if (s.age % 10 == 0) LegendKit.snd(s.t, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 0.4f, 1.8f);
			}
		}
		if (!THROWN.isEmpty()) {
			Iterator<Map.Entry<LivingEntity, Thrown>> it = THROWN.entrySet().iterator();
			while (it.hasNext()) {
				var en = it.next();
				LivingEntity t = en.getKey();
				Thrown th = en.getValue();
				int age = ++th.age()[0];
				if (!t.isAlive() || age > 60) {
					it.remove();
					continue;
				}
				Vec3d before = th.lastVel()[0];
				boolean hitWall = t.horizontalCollision, hitGround = age > 3 && t.isOnGround();
				var others = t.getWorld().getOtherEntities(t, t.getBoundingBox().expand(0.3),
						o -> o instanceof LivingEntity && o != th.by() && o.isAlive());
				if ((hitWall || hitGround || !others.isEmpty()) && before.length() > 0.5) {
					float dmg = th.impact() + (float) before.length() * 5f;
					t.timeUntilRegen = 0;
					t.damage(th.by().getDamageSources().playerAttack(th.by()), dmg);
					for (var o : others) {
						o.timeUntilRegen = 0;
						o.damage(th.by().getDamageSources().playerAttack(th.by()), dmg * 0.6f);
						o.addVelocity(before.x * 0.5, 0.3, before.z * 0.5);
						o.velocityModified = true;
					}
					if (t.getWorld() instanceof ServerWorld sw) {
						LegendKit.fxAt(sw, "boom_gold", t.getPos().add(0, t.getHeight() / 2, 0), 1.8f, 0, 0);
						LegendKit.fxAt(sw, "debris", t.getPos(), 8, 1.5f, 0);
					}
					LegendKit.snd(t, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.2f, 0.6f);
					LegendKit.snd(t, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.2f);
					it.remove();
					continue;
				}
				th.lastVel()[0] = t.getVelocity();
			}
		}
	}
}
