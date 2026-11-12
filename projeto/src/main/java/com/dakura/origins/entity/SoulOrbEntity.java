package com.dakura.origins.entity;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.types.MaturityPower;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Alma deixada por quem o Demônio mata. É a esfera de experiência do Minecraft (mesmo movimento, mesmo ímã, mesma
 * coleta), mas só os Demônios a atraem e ela vira maturidade, nunca experiência. Some depois de 5 minutos.
 */
public class SoulOrbEntity extends Entity {
	private static final TrackedData<Integer> SIZE = DataTracker.registerData(SoulOrbEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private int value = 1;
	private int orbAge;
	private PlayerEntity target;
	private boolean taken;

	public SoulOrbEntity(EntityType<? extends SoulOrbEntity> type, World world) {
		super(type, world);
	}

	public SoulOrbEntity(World world, Vec3d at, int value) {
		this(ModEntities.SOUL_ORB, world);
		this.value = Math.max(1, value);
		this.dataTracker.set(SIZE, sizeFor(this.value));
		setPosition(at.x, at.y, at.z);
		setYaw((float) (random.nextDouble() * 360));
		setVelocity((random.nextDouble() * 0.2 - 0.1) * 2, random.nextDouble() * 0.2 * 2, (random.nextDouble() * 0.2 - 0.1) * 2);
	}

	private static int sizeFor(int v) {
		return v >= 40 ? 7 : v >= 20 ? 6 : v >= 12 ? 5 : v >= 7 ? 4 : v >= 4 ? 3 : v >= 2 ? 1 : 0;
	}

	/** Índice do sprite da esfera de experiência (0..10), como no vanilla. */
	public int getOrbSize() {
		return this.dataTracker.get(SIZE);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(SIZE, 0);
	}

	@Override
	protected double getGravity() {
		return 0.03;
	}

	/** Mesmo comportamento da esfera de experiência vanilla, mas o ímã só pega Demônios. */
	@Override
	public void tick() {
		super.tick();
		prevX = getX();
		prevY = getY();
		prevZ = getZ();
		applyGravity();
		if (!getWorld().isSpaceEmpty(getBoundingBox())) pushOutOfBlocks(getX(), (getBoundingBox().minY + getBoundingBox().maxY) / 2.0, getZ());
		if (age % 20 == 1 || target != null && (!target.isAlive() || target.squaredDistanceTo(this) > 64 || target.isSpectator())) {
			target = null;
			PlayerEntity best = null;
			double bestD = 64;
			for (PlayerEntity p : getWorld().getPlayers()) {
				if (p.isSpectator() || !isDemon(p)) continue;
				double d = p.squaredDistanceTo(this);
				if (d < bestD) {
					bestD = d;
					best = p;
				}
			}
			target = best;
		}
		if (target != null) {
			Vec3d to = new Vec3d(target.getX() - getX(), target.getY() + target.getStandingEyeHeight() / 2.0 - getY(), target.getZ() - getZ());
			double d2 = to.lengthSquared();
			if (d2 < 64) {
				double f = 1.0 - Math.sqrt(d2) / 8.0;
				setVelocity(getVelocity().add(to.normalize().multiply(f * f * 0.1)));
			}
		}
		move(MovementType.SELF, getVelocity());
		float friction = 0.98f;
		if (isOnGround()) friction = getWorld().getBlockState(BlockPos.ofFloored(getX(), getY() - 1, getZ())).getBlock().getSlipperiness() * 0.98f;
		setVelocity(getVelocity().multiply(friction, 0.98, friction));
		if (isOnGround()) setVelocity(getVelocity().multiply(1, -0.9, 1));
		if (++orbAge >= 6000) discard();
	}

	private static boolean isDemon(PlayerEntity p) {
		return p.getWorld().isClient ? EntityFlags.clientHas(p, "soul_harvest") : p instanceof ServerPlayerEntity sp && EntityFlags.has(sp, "soul_harvest");
	}

	@Override
	public void onPlayerCollision(PlayerEntity player) {
		if (getWorld().isClient || taken || isRemoved() || !(player instanceof ServerPlayerEntity sp) || !isDemon(sp)) return;
		taken = true; // cada alma conta uma única vez
		MaturityPower.grant(sp, value);
		ServerWorld w = sp.getServerWorld();
		w.playSound(null, sp.getX(), sp.getY(), sp.getZ(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.15f,
				0.45f + random.nextFloat() * 0.25f);
		w.spawnParticles(new net.minecraft.particle.DustParticleEffect(new org.joml.Vector3f(0.55f, 0.02f, 0.06f), 1.0f),
				getX(), getY() + 0.2, getZ(), 4, 0.15, 0.15, 0.15, 0);
		discard();
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		return false;
	}

	@Override
	public boolean isAttackable() {
		return false;
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		nbt.putInt("Value", value);
		nbt.putInt("OrbAge", orbAge);
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		value = Math.max(1, nbt.getInt("Value"));
		orbAge = nbt.getInt("OrbAge");
		dataTracker.set(SIZE, sizeFor(value));
	}
}
