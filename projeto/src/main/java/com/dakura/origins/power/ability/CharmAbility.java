package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.WalkTarget;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:charm" — encanta o alvo na mira e o faz seguir o dono, sem atacá-lo.
 * Monstros de porte normal, aldeões e animais ficam encantados sem limite de tempo;
 * os demais (jogadores, chefes, criaturas grandes) por "short" ticks. { "range": 16, "short": 60, "max": 4 }
 */
public final class CharmAbility implements Ability {
	private static final Map<UUID, Charm> CHARMED = new LinkedHashMap<>();
	private static final DustParticleEffect CRIMSON = new DustParticleEffect(new Vector3f(0.75f, 0.1f, 0.22f), 0.9f);
	private final double range;
	private final int shortTicks, max;
	private static long ticks;

	private record Charm(UUID owner, LivingEntity target, long until) {}

	public CharmAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 16f);
		this.shortTicks = JsonParams.integer(json, "short", 60);
		this.max = JsonParams.integer(json, "max", 4);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(CharmAbility::tickAll);
	}

	/** Este monstro está encantado por este jogador (não pode mirá-lo). */
	public static boolean blocks(MobEntity mob, LivingEntity target) {
		if (CHARMED.isEmpty()) return false;
		Charm c = CHARMED.get(mob.getUuid());
		return c != null && c.owner.equals(target.getUuid());
	}

	private static boolean longCharm(LivingEntity t) {
		if (!(t instanceof MobEntity) || t instanceof WitherEntity || t instanceof EnderDragonEntity
				|| t instanceof WardenEntity || t instanceof ElderGuardianEntity) return false;
		if (t instanceof AnimalEntity || t instanceof MerchantEntity) return true;
		return t instanceof Monster && t.getWidth() <= 1.0f && t.getHeight() <= 2.6f;
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		Vec3d eye = p.getEyePos(), look = p.getRotationVec(1f), end = eye.add(look.multiply(range));
		EntityHitResult hit = ProjectileUtil.raycast(p, eye, end, p.getBoundingBox().stretch(look.multiply(range)).expand(1),
				e -> e instanceof LivingEntity l && l.isAlive() && !e.isSpectator(), range * range);
		if (hit == null || !(hit.getEntity() instanceof LivingEntity t) || !p.canSee(t)) {
			p.sendMessage(Text.translatable("dakura_origins.message.no_target").formatted(Formatting.GRAY), true);
			return false;
		}
		boolean lasting = longCharm(t);
		if (lasting) {
			long mine = CHARMED.values().stream().filter(c -> c.owner.equals(p.getUuid()) && c.until < 0).count();
			if (mine >= max) {
				CHARMED.entrySet().stream().filter(en -> en.getValue().owner.equals(p.getUuid()) && en.getValue().until < 0)
						.findFirst().ifPresent(en -> release(en.getValue()));
				CHARMED.values().removeIf(c -> !c.target.isAlive() || c.until == -2);
			}
		}
		CHARMED.put(t.getUuid(), new Charm(p.getUuid(), t, lasting ? -1 : ticks + shortTicks));
		if (t instanceof MobEntity m) {
			if (m.getTarget() == p) m.setTarget(null);
			m.setAttacker(null);
		}
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:charm", t.getId()), p, t);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "cast", 12), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, SoundCategory.PLAYERS, 0.5f, 1.3f);
		p.getWorld().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.9f, 0.6f);
		return true;
	}

	private static void release(Charm c) {
		if (c.target.getWorld() instanceof ServerWorld w && c.target.isAlive()) {
			w.spawnParticles(ParticleTypes.SMOKE, c.target.getX(), c.target.getBodyY(0.8), c.target.getZ(), 5, 0.2, 0.2, 0.2, 0.01);
		}
		CHARMED.put(c.target.getUuid(), new Charm(c.owner, c.target, -2));
	}

	private static void tickAll(MinecraftServer server) {
		ticks++;
		if (CHARMED.isEmpty()) return;
		Iterator<Map.Entry<UUID, Charm>> it = CHARMED.entrySet().iterator();
		while (it.hasNext()) {
			Charm c = it.next().getValue();
			LivingEntity t = c.target;
			ServerPlayerEntity owner = server.getPlayerManager().getPlayer(c.owner);
			boolean end = c.until == -2 || !t.isAlive() || t.isRemoved() || owner == null || !owner.isAlive()
					|| owner.getWorld() != t.getWorld() || owner.squaredDistanceTo(t) > 48 * 48
					|| (c.until >= 0 && ticks >= c.until)
					|| com.dakura.origins.race.RaceAccess.getRace(owner).map(r -> r.abilities().stream()
					.noneMatch(a -> a.ability() instanceof CharmAbility)).orElse(true);
			if (end) {
				if (c.until != -2 && t.isAlive() && t.getWorld() instanceof ServerWorld w)
					w.spawnParticles(ParticleTypes.SMOKE, t.getX(), t.getBodyY(0.8), t.getZ(), 5, 0.2, 0.2, 0.2, 0.01);
				it.remove();
				continue;
			}
			double d2 = t.squaredDistanceTo(owner);
			if (t instanceof MobEntity m) {
				if (m.getTarget() == owner) m.setTarget(null);
				m.getLookControl().lookAt(owner, 30, 30);
				if (ticks % 10 == 0 && d2 > 9) {
					if (m.getBrain().hasMemoryModule(MemoryModuleType.WALK_TARGET)) {
						m.getBrain().remember(MemoryModuleType.WALK_TARGET, new WalkTarget(owner, 0.7f, 2));
					} else m.getNavigation().startMovingTo(owner, 1.1);
				}
			} else if (t instanceof PlayerEntity && d2 > 4) {
				Vec3d pull = owner.getPos().subtract(t.getPos()).multiply(1, 0, 1).normalize().multiply(0.09);
				t.addVelocity(pull.x, 0, pull.z);
				t.velocityModified = true;
			}
			if (ticks % 30 == 0 && t.getWorld() instanceof ServerWorld w) {
				w.spawnParticles(CRIMSON, t.getX(), t.getBodyY(1.05), t.getZ(), 3, 0.25, 0.1, 0.25, 0);
				if (ticks % 90 == 0) w.spawnParticles(ParticleTypes.HEART, t.getX(), t.getBodyY(1.1), t.getZ(), 1, 0.1, 0.1, 0.1, 0);
			}
		}
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		CHARMED.values().removeIf(c -> c.owner.equals(p.getUuid()));
	}

	/** Ninguém deve ficar encantado por quem já saiu (o tick já cuida disso). */
	public static boolean charmed(Entity e) {
		return CHARMED.containsKey(e.getUuid());
	}
}
