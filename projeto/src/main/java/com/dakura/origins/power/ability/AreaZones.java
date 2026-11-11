package com.dakura.origins.power.ability;

import com.dakura.origins.block.ModBlocks;
import com.dakura.origins.power.types.ToxicAffinityPower;
import com.dakura.origins.race.RaceAccess;
import com.dakura.origins.sound.ModSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryKey;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Áreas que ficam no chão por um tempo: o veneno roxo da Dama Cogumelo e o fogo verde dos dragões.
 * O efeito é real (dano, queimadura, cegueira, lentidão); o visual é desenhado no cliente (FxWorld).
 */
public final class AreaZones {
	public enum Kind { POISON, GREEN_FIRE }

	private static final class Zone {
		final ServerWorld world;
		final Vec3d center;
		final float maxRadius;
		final int growTicks, life;
		final Kind kind;
		final UUID owner;
		final float damage;
		BlockPos cauldron;
		int age;

		Zone(ServerWorld world, Vec3d center, float maxRadius, int growTicks, int life, Kind kind, UUID owner, float damage) {
			this.world = world;
			this.center = center;
			this.maxRadius = maxRadius;
			this.growTicks = Math.max(1, growTicks);
			this.life = life;
			this.kind = kind;
			this.owner = owner;
			this.damage = damage;
		}

		float radius() {
			return maxRadius * Math.min(1f, (age + 1) / (float) growTicks);
		}
	}

	private static final List<Zone> ZONES = new ArrayList<>();

	private AreaZones() {}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(AreaZones::tick);
	}

	/** Cria a área e manda o desenho para os jogadores por perto. Devolve a zona (para prender um caldeirão nela). */
	public static void create(ServerWorld world, Vec3d center, float radius, int growTicks, int life, Kind kind, Entity owner,
							  float damage, BlockPos cauldron) {
		Zone z = new Zone(world, center, radius, growTicks, life, kind, owner != null ? owner.getUuid() : null, damage);
		z.cauldron = cauldron;
		ZONES.add(z);
		LegendKit.fxAt(world, kind == Kind.POISON ? "zone_poison" : "zone_greenfire", center, radius, life, growTicks);
	}

	/** Dano de fogo de verdade (queima, cozinha a carne dos animais e respeita quem é imune a fogo). */
	public static DamageSource fire(ServerWorld world, Entity attacker) {
		return source(world, DamageTypes.IN_FIRE, attacker);
	}

	public static DamageSource poison(ServerWorld world, Entity attacker) {
		return source(world, DamageTypes.MAGIC, attacker);
	}

	private static DamageSource source(ServerWorld world, RegistryKey<DamageType> key, Entity attacker) {
		return new DamageSource(world.getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(key), attacker);
	}

	/** Esta criatura ignora o veneno (e se fortalece nele)? */
	public static boolean poisonFriend(Entity e) {
		return e instanceof PlayerEntity p && RaceAccess.getRace(p)
				.map(r -> r.powers().stream().anyMatch(x -> x instanceof ToxicAffinityPower)).orElse(false);
	}

	private static void tick(MinecraftServer server) {
		if (ZONES.isEmpty()) return;
		for (Zone z : new ArrayList<>(ZONES)) {
			z.age++;
			if (z.age > z.life || z.world.getServer() != server) {
				end(z);
				continue;
			}
			if (z.age % 10 != 0) continue;
			float r = z.radius();
			Entity owner = z.owner != null ? z.world.getEntity(z.owner) : null;
			Box box = new Box(z.center, z.center).expand(r, 2.5, r);
			for (LivingEntity e : z.world.getEntitiesByClass(LivingEntity.class, box, LivingEntity::isAlive)) {
				double dx = e.getX() - z.center.x, dz = e.getZ() - z.center.z;
				if (dx * dx + dz * dz > r * r) continue;
				if (z.kind == Kind.POISON) poisonTick(z, e, owner);
				else fireTick(z, e, owner);
			}
			if (z.kind == Kind.POISON && z.age % 40 == 0) ModSounds.play(z.world, z.center, ModSounds.POISON_BUBBLE, 0.6f, 0.8f + z.world.random.nextFloat() * 0.4f);
		}
	}

	private static void poisonTick(Zone z, LivingEntity e, Entity owner) {
		if (poisonFriend(e)) {
			// a Dama Cogumelo se fortalece no próprio veneno: escudo dourado e cura contínua, sem dano nem lentidão
			if (e.getAbsorptionAmount() < 4f) e.setAbsorptionAmount(4f);
			if (z.age % 20 == 0) e.heal(1f);
			return;
		}
		if (e == owner) return;
		if (z.age % 20 == 0) {
			e.timeUntilRegen = 0;
			e.damage(poison(z.world, owner), z.damage);
		}
		e.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 50, 0, true, false, true));
		e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 1, true, false, true));
	}

	private static void fireTick(Zone z, LivingEntity e, Entity owner) {
		if (e == owner || e.isFireImmune()) return;
		e.setOnFireFor(4f);
		if (z.age % 20 == 0) {
			e.timeUntilRegen = 0;
			e.damage(fire(z.world, owner), z.damage);
		}
	}

	private static void end(Zone z) {
		ZONES.remove(z);
		if (z.cauldron != null) {
			BlockState bs = z.world.getBlockState(z.cauldron);
			if (bs.isOf(ModBlocks.TOXIC_CAULDRON)) z.world.removeBlock(z.cauldron, false);
		}
	}
}
