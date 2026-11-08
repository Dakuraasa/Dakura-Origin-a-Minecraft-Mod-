package com.dakura.origins.entity;

import com.dakura.origins.power.types.HitEffectPower;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

/**
 * Ilusão da Huli Jing: copia arma e armadura da dona, ataca o inimigo mais próximo (ou quem ela acertou,
 * ou quem a atacou), aplica fogo/gelo a cada golpe e some depois de alguns segundos. Nunca é salva no mundo.
 */
public class HuliCloneEntity extends PathAwareEntity {
	private static final TrackedData<Optional<UUID>> OWNER = DataTracker.registerData(HuliCloneEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
	private static final TrackedData<Boolean> SNOW = DataTracker.registerData(HuliCloneEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
	private int life = 140;

	public HuliCloneEntity(EntityType<? extends PathAwareEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 0;
		for (EquipmentSlot s : EquipmentSlot.values()) setEquipmentDropChance(s, 0f);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return MobEntity.createMobAttributes()
				.add(EntityAttributes.GENERIC_MAX_HEALTH, 20)
				.add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.32)
				.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3)
				.add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(OWNER, Optional.empty());
		builder.add(SNOW, false);
	}

	@Override
	protected void initGoals() {
		goalSelector.add(1, new MeleeAttackGoal(this, 1.25, true));
		goalSelector.add(5, new LookAroundGoal(this));
	}

	public void setup(PlayerEntity owner, boolean snow, int lifeTicks) {
		dataTracker.set(OWNER, Optional.of(owner.getUuid()));
		dataTracker.set(SNOW, snow);
		this.life = lifeTicks;
		getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(owner.getMaxHealth());
		setHealth(owner.getMaxHealth());
		getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE).setBaseValue(owner.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
		for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			equipStack(s, owner.getEquippedStack(s).copy());
		}
		setCustomName(owner.getName());
		setCustomNameVisible(false);
	}

	public Optional<UUID> ownerUuid() {
		return dataTracker.get(OWNER);
	}

	public boolean isSnow() {
		return dataTracker.get(SNOW);
	}

	private PlayerEntity owner() {
		return ownerUuid().map(u -> getWorld().getPlayerByUuid(u)).orElse(null);
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			if (random.nextInt(3) == 0) {
				getWorld().addParticle(isSnow() ? ParticleTypes.SNOWFLAKE : ParticleTypes.SMALL_FLAME,
						getParticleX(0.5), getRandomBodyY(), getParticleZ(0.5), 0, 0.01, 0);
			}
			return;
		}
		PlayerEntity owner = owner();
		if (--life <= 0 || owner == null || !owner.isAlive()) {
			vanish();
			return;
		}
		if (age % 10 == 0) pickTarget(owner);
		if (getTarget() == null && squaredDistanceTo(owner) > 16) getNavigation().startMovingTo(owner, 1.2);
	}

	private void pickTarget(PlayerEntity owner) {
		LivingEntity t = owner.getAttacking();
		if (!valid(t) || owner.age - owner.getLastAttackTime() > 100) t = owner.getAttacker();
		if (!valid(t)) {
			t = getWorld().getClosestEntity(HostileEntity.class, net.minecraft.entity.ai.TargetPredicate.DEFAULT, this,
					getX(), getY(), getZ(), getBoundingBox().expand(12));
		}
		if (valid(t)) setTarget(t);
	}

	private boolean valid(LivingEntity t) {
		return t != null && t.isAlive() && !(t instanceof HuliCloneEntity) && ownerUuid().map(u -> !u.equals(t.getUuid())).orElse(true);
	}

	@Override
	public boolean tryAttack(Entity target) {
		boolean hit = super.tryAttack(target);
		if (hit && target instanceof LivingEntity le) HitEffectPower.apply(le, isSnow(), 3);
		swingHand(net.minecraft.util.Hand.MAIN_HAND);
		return hit;
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (source.getAttacker() != null && ownerUuid().map(u -> u.equals(source.getAttacker().getUuid())).orElse(false)) return false;
		return super.damage(source, amount);
	}

	@Override
	public boolean isFireImmune() {
		return !isSnow();
	}

	@Override
	public void onDeath(DamageSource source) {
		vanish();
	}

	private void vanish() {
		if (getWorld() instanceof ServerWorld w && !isRemoved()) {
			w.spawnParticles(isSnow() ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME, getX(), getBodyY(0.5), getZ(), 14, 0.3, 0.5, 0.3, 0.03);
			w.spawnParticles(ParticleTypes.POOF, getX(), getBodyY(0.5), getZ(), 6, 0.25, 0.4, 0.25, 0.01);
			w.playSound(null, getX(), getY(), getZ(), net.minecraft.sound.SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE,
					net.minecraft.sound.SoundCategory.NEUTRAL, 0.35f, 1.6f);
		}
		discard();
	}

	@Override
	public boolean shouldSave() {
		return false;
	}

	@Override
	protected boolean shouldDropLoot() {
		return false;
	}

	@Override
	public boolean canImmediatelyDespawn(double distanceSquared) {
		return false;
	}
}
