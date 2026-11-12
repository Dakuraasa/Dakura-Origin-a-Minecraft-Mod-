package com.dakura.origins.power.ability;

import com.dakura.origins.power.Ability;
import com.dakura.origins.power.Control;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.sound.ModSounds;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.Brain;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.Angerable;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:puppet_chain" — the Doll binds the nearest enemy or player within "radius" blocks with the
 * Corrupted Kitsune's chain and controls it for "duration" ticks.
 * <p>
 * Mobs keep their own AI: only their targeting is overridden. They never target the Doll, follow her like a tamed wolf
 * and switch to whatever she attacks, fighting it with their own attacks (Wither skulls, Iron Golem slams, arrows...).
 * Players lose control of their character: they are walked after the Doll and strike her target with the item they hold.
 * One chain per Doll, one controller per target. { "radius": 6, "duration": 200 }
 */
public final class PuppetChainAbility implements Ability {
	private static final Map<UUID, Session> BY_OWNER = new HashMap<>();
	private static final Map<UUID, Session> BY_TARGET = new HashMap<>();
	private final double radius;
	private final int duration;

	private static final class Session {
		final UUID owner;
		final LivingEntity target;
		final int duration;
		int age;
		LivingEntity command;

		Session(UUID owner, LivingEntity target, int duration) {
			this.owner = owner;
			this.target = target;
			this.duration = duration;
		}
	}

	public PuppetChainAbility(JsonObject json) {
		this.radius = JsonParams.num(json, "radius", 6f);
		this.duration = JsonParams.integer(json, "duration", 200);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(PuppetChainAbility::tickAll);
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> {
			Entity attacker = source.getAttacker();
			if (attacker == null) return true;
			Session s = BY_TARGET.get(attacker.getUuid());
			if (s != null) return victim == s.command;
			command(attacker, victim);
			return true;
		});
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (!world.isClient) command(player, entity);
			return ActionResult.PASS;
		});
	}

	/** True when this mob is bound and must not aim at {@code target}. */
	public static boolean blocks(MobEntity mob, LivingEntity target) {
		if (BY_TARGET.isEmpty()) return false;
		Session s = BY_TARGET.get(mob.getUuid());
		return s != null && target != s.command;
	}

	public static boolean isBound(Entity e) {
		return e != null && BY_TARGET.containsKey(e.getUuid());
	}

	private static void command(Entity attacker, Entity victim) {
		if (BY_OWNER.isEmpty() || !(victim instanceof LivingEntity l) || !l.isAlive()) return;
		Session s = BY_OWNER.get(attacker.getUuid());
		if (s == null || victim == s.target || victim == attacker) return;
		s.command = l;
	}

	private static boolean candidate(ServerPlayerEntity p, Entity e) {
		if (!(e instanceof LivingEntity l) || e == p || !l.isAlive() || BY_TARGET.containsKey(e.getUuid())) return false;
		if (e instanceof ArmorStandEntity || e instanceof EnderDragonEntity || Control.isLocked(e) || CharmAbility.isCharmed(l)) return false;
		if (e instanceof PlayerEntity pl) return !pl.isSpectator() && !pl.isCreative();
		return e instanceof MobEntity;
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		if (BY_OWNER.containsKey(p.getUuid())) {
			p.sendMessage(Text.translatable("dakura_origins.message.puppet_busy").formatted(Formatting.GRAY), true);
			return false;
		}
		LivingEntity aimed = LegendKit.aim(p, radius, 0.9);
		LivingEntity target = aimed != null && candidate(p, aimed) && p.canSee(aimed) ? aimed : null;
		if (target == null) {
			double best = radius * radius;
			for (LivingEntity e : p.getWorld().getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(radius),
					e -> candidate(p, e) && p.canSee(e))) {
				double d = e.squaredDistanceTo(p);
				if (d <= best) {
					best = d;
					target = e;
				}
			}
		}
		if (target == null) {
			p.sendMessage(Text.translatable("dakura_origins.message.no_target").formatted(Formatting.GRAY), true);
			return false;
		}
		Session s = new Session(p.getUuid(), target, duration);
		BY_OWNER.put(p.getUuid(), s);
		BY_TARGET.put(target.getUuid(), s);
		EntityFlags.set(p, "chain#" + target.getId(), true);
		if (target instanceof MobEntity mob) {
			if (mob.getTarget() == p) mob.setTarget(null);
			if (mob instanceof Angerable a) a.stopAnger();
			forgetTarget(mob, null);
		}
		LegendKit.anim(p, "cast", 8);
		ServerWorld w = p.getServerWorld();
		Vec3d mid = target.getPos().add(0, target.getHeight() * 0.5, 0);
		LegendKit.fxAt(w, "dark_burst", mid, 1.2f, 0, 0);
		ModSounds.play(w, mid, ModSounds.CHAIN_BIND, 1.2f, 0.9f);
		if (target instanceof ServerPlayerEntity sp)
			sp.sendMessage(Text.translatable("dakura_origins.message.puppet_bound").formatted(Formatting.DARK_PURPLE), true);
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		Session s = BY_OWNER.get(p.getUuid());
		if (s != null) end(s, p);
	}

	// ------------------------------------------------------------------ control loop

	private static void tickAll(MinecraftServer server) {
		if (BY_OWNER.isEmpty()) return;
		Iterator<Session> it = new java.util.ArrayList<>(BY_OWNER.values()).iterator();
		while (it.hasNext()) {
			Session s = it.next();
			ServerPlayerEntity owner = server.getPlayerManager().getPlayer(s.owner);
			LivingEntity t = s.target;
			boolean lost = owner == null || !owner.isAlive() || owner.isSpectator() || !t.isAlive() || t.isRemoved()
					|| t.getWorld() != owner.getWorld() || (t instanceof ServerPlayerEntity sp && sp.isDisconnected());
			if (lost || ++s.age > s.duration) {
				end(s, owner);
				continue;
			}
			if (s.command != null && (!s.command.isAlive() || s.command.isRemoved() || s.command.getWorld() != t.getWorld()
					|| s.command.squaredDistanceTo(t) > 48 * 48)) s.command = null;
			if (t instanceof ServerPlayerEntity sp) drivePlayer(sp, owner, s);
			else if (t instanceof MobEntity mob) driveMob(mob, owner, s);
			if (s.age % 20 == 0) LegendKit.fx(t, "chain_pulse", 0);
		}
	}

	private static void driveMob(MobEntity mob, ServerPlayerEntity owner, Session s) {
		if (mob instanceof Angerable a && a.getAngryAt() != null && (s.command == null || !a.getAngryAt().equals(s.command.getUuid())))
			a.stopAnger();
		if (s.command != null) {
			if (mob.getTarget() != s.command) mob.setTarget(s.command);
			Brain<?> brain = mob.getBrain();
			if (brain.hasMemoryModule(MemoryModuleType.ATTACK_TARGET)) brain.remember(MemoryModuleType.ATTACK_TARGET, s.command);
			if (brain.hasMemoryModule(MemoryModuleType.ANGRY_AT)) brain.remember(MemoryModuleType.ANGRY_AT, s.command.getUuid(), 600L);
			return;
		}
		if (mob.getTarget() != null) mob.setTarget(null);
		forgetTarget(mob, null);
		double d = mob.squaredDistanceTo(owner);
		if (d > 24 * 24) {
			Vec3d at = besideOwner(owner);
			mob.refreshPositionAndAngles(at.x, at.y, at.z, mob.getYaw(), mob.getPitch());
			mob.getNavigation().stop();
		} else if (d > 4 * 4 && s.age % 10 == 0) {
			mob.getNavigation().startMovingTo(owner, 1.2);
			if (mob.getNavigation().isIdle()) mob.getMoveControl().moveTo(owner.getX(), owner.getY() + 0.5, owner.getZ(), 1.2);
		}
	}

	private static void forgetTarget(MobEntity mob, LivingEntity keep) {
		Brain<?> brain = mob.getBrain();
		if (brain.hasMemoryModule(MemoryModuleType.ATTACK_TARGET)) {
			var cur = brain.getOptionalRegisteredMemory(MemoryModuleType.ATTACK_TARGET);
			if (cur.isPresent() && cur.get() != keep) brain.forget(MemoryModuleType.ATTACK_TARGET);
		}
		if (brain.hasMemoryModule(MemoryModuleType.ANGRY_AT)) brain.forget(MemoryModuleType.ANGRY_AT);
	}

	private static void drivePlayer(ServerPlayerEntity sp, ServerPlayerEntity owner, Session s) {
		LivingEntity goal = s.command != null ? s.command : owner;
		double stop = s.command != null ? 2.2 : 2.8;
		Vec3d pos = sp.getPos(), to = goal.getPos().subtract(pos);
		double flat = Math.sqrt(to.x * to.x + to.z * to.z);
		Vec3d next = pos;
		if (flat > 24) {
			next = besideOwner(owner);
		} else if (flat > stop) {
			Vec3d dir = new Vec3d(to.x / flat, 0, to.z / flat);
			next = step(sp, pos, dir.multiply(Math.min(0.24, flat - stop)));
		} else {
			next = step(sp, pos, Vec3d.ZERO);
		}
		Vec3d look = goal.getEyePos().subtract(sp.getEyePos());
		float yaw = (float) (MathHelper.atan2(look.z, look.x) * 57.2957763671875) - 90f;
		float pitch = (float) -(MathHelper.atan2(look.y, Math.sqrt(look.x * look.x + look.z * look.z)) * 57.2957763671875);
		sp.setYaw(yaw);
		sp.setHeadYaw(yaw);
		sp.setPitch(pitch);
		Control.puppet(sp, 3, next);
		if (s.command != null && sp.squaredDistanceTo(s.command) <= 3.2 * 3.2 && sp.getAttackCooldownProgress(0.5f) >= 1f) {
			sp.attack(s.command);
			sp.swingHand(Hand.MAIN_HAND, true);
		}
	}

	/** One walking step with simple step-up and falling, so a controlled player follows the ground. */
	private static Vec3d step(ServerPlayerEntity sp, Vec3d pos, Vec3d move) {
		ServerWorld w = sp.getServerWorld();
		Vec3d next = pos.add(move);
		BlockPos feet = BlockPos.ofFloored(next.x, next.y + 0.05, next.z);
		if (solid(w, feet)) {
			if (!solid(w, feet.up()) && !solid(w, feet.up(2))) next = new Vec3d(next.x, feet.getY() + 1.0, next.z);
			else next = new Vec3d(pos.x, pos.y, pos.z);
		}
		BlockPos below = BlockPos.ofFloored(next.x, next.y - 0.05, next.z);
		if (!solid(w, below)) {
			BlockPos land = BlockPos.ofFloored(next.x, next.y - 0.45, next.z);
			next = solid(w, land) ? new Vec3d(next.x, land.getY() + 1.0, next.z) : next.add(0, -0.4, 0);
		}
		return next;
	}

	private static boolean solid(ServerWorld w, BlockPos pos) {
		return !w.getBlockState(pos).getCollisionShape(w, pos).isEmpty();
	}

	private static Vec3d besideOwner(ServerPlayerEntity owner) {
		Vec3d back = owner.getRotationVec(1f).multiply(-1.5, 0, -1.5);
		return owner.getPos().add(back);
	}

	private static void end(Session s, ServerPlayerEntity owner) {
		BY_OWNER.remove(s.owner);
		BY_TARGET.remove(s.target.getUuid());
		if (owner != null) {
			EntityFlags.set(owner, "chain#" + s.target.getId(), false);
			ModSounds.play(owner.getServerWorld(), s.target.getPos(), ModSounds.CHAIN_BIND, 0.7f, 1.4f);
		}
		LivingEntity t = s.target;
		if (t instanceof ServerPlayerEntity sp) {
			Control.release(sp);
		} else if (t instanceof MobEntity mob) {
			if (s.command != null && mob.getTarget() == s.command) mob.setTarget(null);
			forgetTarget(mob, null);
			if (mob instanceof Angerable a) a.stopAnger();
			mob.getNavigation().stop();
		}
	}
}
