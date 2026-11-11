package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.FireballPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.types.MaturityPower;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import net.minecraft.world.explosion.ExplosionBehavior;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * "dakura_origins:fireball" — dispara uma bola de fogo rápida (linha reta, sem gravidade).
 * <pre>
 * { "type": "dakura_origins:fireball", "speed": 1.6, "life": 50, "explosion_power": 4.0,
 *   "block_power": 1.5, "fire_count": 10, "fire_radius": 3 }
 * </pre>
 * - explosion_power 4 = dano de TNT nas criaturas (sem quebrar blocos, sem ferir quem lançou).
 * - block_power: uma segunda explosão pequena que abre uma cratera (respeita mobGriefing).
 * - Espalha até fire_count focos de fogo ao redor do impacto.
 * - maturity_damage: multiplicador do dano por estágio de maturidade (ex.: [1.0, 1.4, 1.8]).
 * O servidor simula o voo e a colisão; os clientes desenham o rastro sozinhos (FireballPayload).
 */
public final class FireballAbility implements Ability {
	private final double speed;
	private final int life;
	private final float explosionPower;
	private final float blockPower;
	private final int fireCount;
	private final int fireRadius;
	private final float[] maturityDamage;

	public FireballAbility(JsonObject json) {
		this.speed = JsonParams.num(json, "speed", 1.6f);
		this.life = JsonParams.integer(json, "life", 50);
		this.explosionPower = JsonParams.num(json, "explosion_power", 4f);
		this.blockPower = JsonParams.num(json, "block_power", 1.5f);
		this.fireCount = JsonParams.integer(json, "fire_count", 10);
		this.fireRadius = JsonParams.integer(json, "fire_radius", 3);
		this.maturityDamage = JsonParams.floats(json, "maturity_damage");
	}

	private static final class Shot {
		final int id;
		final FireballAbility config;
		final ServerPlayerEntity owner;
		final ServerWorld world;
		Vec3d pos;
		final Vec3d vel;
		int age;

		Shot(int id, FireballAbility config, ServerPlayerEntity owner, ServerWorld world, Vec3d pos, Vec3d vel) {
			this.id = id;
			this.config = config;
			this.owner = owner;
			this.world = world;
			this.pos = pos;
			this.vel = vel;
		}
	}

	private static final List<Shot> SHOTS = new ArrayList<>();
	private static int nextId = 1;

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(FireballAbility::tickAll);
	}

	@Override
	public boolean activate(ServerPlayerEntity player) {
		if (BreathAbility.isBreathing(player) || BatFormState.isBat(player)) return false;
		if (!(player.getWorld() instanceof ServerWorld world)) return false;

		Vec3d look = player.getRotationVec(1f);
		Vec3d start = player.getEyePos().add(look.multiply(0.6 + player.getWidth() * 0.5));
		Vec3d vel = look.multiply(speed);
		Shot shot = new Shot(nextId++, this, player, world, start, vel);
		SHOTS.add(shot);

		ModNetworking.sendNear(world, start, 128,
				new FireballPayload(shot.id, start.x, start.y, start.z, vel.x, vel.y, vel.z, FireballPayload.FLYING));
		ModNetworking.sendAround(new AnimPayload(player.getId(), "cast", 12), player);
		player.swingHand(Hand.MAIN_HAND, true);
		world.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.ENTITY_ENDER_DRAGON_SHOOT,
				SoundCategory.PLAYERS, 1.0f, 1.1f);
		world.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.ENTITY_BLAZE_SHOOT,
				SoundCategory.PLAYERS, 0.8f, 0.6f);
		return true;
	}

	// ================================================================== voo

	private static void tickAll(MinecraftServer server) {
		if (SHOTS.isEmpty()) return;
		Iterator<Shot> it = SHOTS.iterator();
		while (it.hasNext()) {
			Shot s = it.next();
			if (!s.config.step(s)) it.remove();
		}
	}

	/** @return false quando a bola de fogo acabou. */
	private boolean step(Shot s) {
		s.age++;
		BlockPos here = BlockPos.ofFloored(s.pos);
		if (!s.world.isChunkLoaded(here.getX() >> 4, here.getZ() >> 4)) return end(s, s.pos, false);

		Vec3d from = s.pos;
		Vec3d to = from.add(s.vel);
		BlockHitResult blockHit = s.world.raycast(new RaycastContext(from, to,
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.ANY, s.owner));
		Vec3d end = blockHit.getType() != HitResult.Type.MISS ? blockHit.getPos() : to;

		EntityHitResult entityHit = ProjectileUtil.getEntityCollision(s.world, s.owner, from, end,
				new Box(from, end).expand(1.0), e -> e != s.owner && e.isAlive() && e.canHit() && !e.isSpectator());

		if (entityHit != null) return end(s, entityHit.getPos(), true);
		if (blockHit.getType() != HitResult.Type.MISS) {
			boolean water = s.world.getFluidState(blockHit.getBlockPos()).isIn(FluidTags.WATER);
			Vec3d at = blockHit.getPos().subtract(s.vel.normalize().multiply(0.25));
			return end(s, at, !water);
		}
		s.pos = to;
		if (s.age >= life) return end(s, s.pos, false);
		return true;
	}

	/** Explode (ou se apaga) e avisa os clientes para pararem o rastro. Sempre retorna false. */
	private boolean end(Shot s, Vec3d at, boolean explode) {
		ModNetworking.sendNear(s.world, at, 128, new FireballPayload(s.id, at.x, at.y, at.z, s.vel.x, s.vel.y, s.vel.z,
				explode ? FireballPayload.EXPLODED : FireballPayload.FIZZLED));
		if (explode) explode(s, at);
		else s.world.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.8f, 1.0f);
		return false;
	}

	private void explode(Shot s, Vec3d at) {
		ServerWorld world = s.world;
		ServerPlayerEntity owner = s.owner;

		// 1) dano de TNT nas criaturas, sem quebrar blocos e sem ferir o próprio dragão
		float mult = MaturityPower.multiplier(owner, maturityDamage);
		world.createExplosion(owner, null, new OwnerSafeBehavior(owner, mult), at.x, at.y, at.z,
				explosionPower, false, World.ExplosionSourceType.NONE);
		// 2) cratera pequena e controlada (desligada se mobGriefing = false)
		if (blockPower > 0) {
			world.createExplosion(owner, null, new BlocksOnlyBehavior(), at.x, at.y, at.z,
					blockPower, false, World.ExplosionSourceType.MOB);
		}
		// 3) fogo de verdade: quem estava na explosão pega fogo (e a carne cai assada), e o chão em volta queima
		DragonFire.burnArea(world, at, explosionPower * 1.2, owner, 3f * mult, 6);
		spreadFire(world, BlockPos.ofFloored(at));
		// 4) empurrão moderado e proporcional (as explosões em si não arremessam mais ninguém para o céu)
		double r = explosionPower * 1.2;
		for (net.minecraft.entity.LivingEntity e : world.getEntitiesByClass(net.minecraft.entity.LivingEntity.class,
				new Box(at, at).expand(r), x -> x.isAlive() && x != owner)) {
			double d = e.getPos().distanceTo(at);
			if (d > r) continue;
			double k = 1 - d / r;
			Vec3d dir = LegendKit.flat(e.getPos().subtract(at));
			e.addVelocity(dir.x * 0.75 * k, 0.12 + 0.22 * k, dir.z * 0.75 * k);
			e.velocityModified = true;
		}
		LegendKit.fxAt(world, "fire_field", at, (float) r * 0.8f, 80, 0);

		// 5) o impacto visual (onda de choque, coluna de fogo, brasas) é desenhado pelos clientes
		world.playSound(null, at.x, at.y, at.z, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.0f, 0.5f);
	}

	private void spreadFire(ServerWorld world, BlockPos center) {
		int placed = 0;
		for (int attempt = 0; attempt < fireCount * 3 && placed < fireCount; attempt++) {
			int dx = world.random.nextInt(fireRadius * 2 + 1) - fireRadius;
			int dz = world.random.nextInt(fireRadius * 2 + 1) - fireRadius;
			for (int dy = 2; dy >= -3; dy--) {
				BlockPos pos = center.add(dx, dy, dz);
				if (world.getBlockState(pos).isAir() && AbstractFireBlock.canPlaceAt(world, pos, Direction.UP)) {
					world.setBlockState(pos, AbstractFireBlock.getState(world, pos));
					placed++;
					break;
				}
			}
		}
	}

	/** Explosão que não fere quem a criou; o dano cresce com a maturidade. */
	private static final class OwnerSafeBehavior extends ExplosionBehavior {
		private final Entity owner;
		private final float damageMultiplier;

		OwnerSafeBehavior(Entity owner, float damageMultiplier) {
			this.owner = owner;
			this.damageMultiplier = damageMultiplier;
		}

		@Override
		public boolean shouldDamage(Explosion explosion, Entity entity) {
			return entity != owner && super.shouldDamage(explosion, entity);
		}

		@Override
		public float calculateDamage(Explosion explosion, Entity entity) {
			return super.calculateDamage(explosion, entity) * damageMultiplier;
		}

		@Override
		public float getKnockbackModifier(Entity entity) {
			return 0f; // o empurrão é aplicado à parte, moderado
		}
	}

	/** Explosão que só mexe nos blocos (o dano já veio da primeira). */
	private static final class BlocksOnlyBehavior extends ExplosionBehavior {
		@Override
		public boolean shouldDamage(Explosion explosion, Entity entity) {
			return false;
		}

		@Override
		public float getKnockbackModifier(Entity entity) {
			return 0f;
		}
	}
}
