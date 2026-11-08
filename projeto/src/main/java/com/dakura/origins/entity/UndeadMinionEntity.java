package com.dakura.origins.entity;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

/**
 * Zumbi invocado pelo Morto-vivo: não queima ao sol, não vira afogado, segue o dono e ataca
 * quem o dono atacar (ou quem atacar o dono). Some sozinho e nunca é salvo no mundo.
 */
public class UndeadMinionEntity extends ZombieEntity {
	private static final TrackedData<Optional<UUID>> OWNER = DataTracker.registerData(UndeadMinionEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
	private int life = 600;

	public UndeadMinionEntity(EntityType<? extends ZombieEntity> type, World world) {
		super(type, world);
		this.experiencePoints = 0;
		for (EquipmentSlot s : EquipmentSlot.values()) setEquipmentDropChance(s, 0f);
	}

	public static DefaultAttributeContainer.Builder createAttributes() {
		return ZombieEntity.createZombieAttributes().add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		super.initDataTracker(builder);
		builder.add(OWNER, Optional.empty());
	}

	@Override
	protected void initCustomGoals() {
		goalSelector.add(2, new ZombieAttackGoal(this, 1.15, false));
		goalSelector.add(7, new WanderAroundFarGoal(this, 0.9));
	}

	public void setup(PlayerEntity owner, int stage, int lifeTicks) {
		dataTracker.set(OWNER, Optional.of(owner.getUuid()));
		this.life = lifeTicks;
		setBaby(false);
		boolean iron = stage >= 2;
		equipStack(EquipmentSlot.HEAD, armor(iron ? Items.IRON_HELMET : Items.CHAINMAIL_HELMET, stage));
		equipStack(EquipmentSlot.CHEST, armor(iron ? Items.IRON_CHESTPLATE : Items.CHAINMAIL_CHESTPLATE, stage));
		equipStack(EquipmentSlot.LEGS, armor(iron ? Items.IRON_LEGGINGS : Items.CHAINMAIL_LEGGINGS, stage));
		equipStack(EquipmentSlot.FEET, armor(iron ? Items.IRON_BOOTS : Items.CHAINMAIL_BOOTS, stage));
		if (stage >= 1) {
			ItemStack sword = new ItemStack(stage >= 2 ? Items.IRON_SWORD : Items.STONE_SWORD);
			if (stage >= 3) enchant(sword, Enchantments.SHARPNESS, 2);
			equipStack(EquipmentSlot.MAINHAND, sword);
		}
		getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH).setBaseValue(20 + stage * 4);
		setHealth(getMaxHealth());
		getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25 + stage * 0.01);
	}

	private ItemStack armor(Item item, int stage) {
		ItemStack s = new ItemStack(item);
		if (stage >= 3) enchant(s, Enchantments.PROTECTION, 2);
		return s;
	}

	private void enchant(ItemStack s, RegistryKey<Enchantment> key, int level) {
		getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(key).ifPresent(e -> s.addEnchantment(e, level));
	}

	public Optional<UUID> ownerUuid() {
		return dataTracker.get(OWNER);
	}

	private PlayerEntity owner() {
		return ownerUuid().map(u -> getWorld().getPlayerByUuid(u)).orElse(null);
	}

	private boolean ally(Entity e) {
		if (e == null) return false;
		if (ownerUuid().map(u -> u.equals(e.getUuid())).orElse(false)) return true;
		return e instanceof UndeadMinionEntity m && m.ownerUuid().equals(ownerUuid());
	}

	@Override
	protected boolean burnsInDaylight() {
		return false;
	}

	@Override
	protected boolean canConvertInWater() {
		return false;
	}

	@Override
	public void setTarget(LivingEntity target) {
		if (ally(target)) return;
		super.setTarget(target);
	}

	@Override
	public void tick() {
		super.tick();
		if (getWorld().isClient) {
			if (random.nextInt(14) == 0) getWorld().addParticle(ParticleTypes.SOUL, getParticleX(0.4), getRandomBodyY(), getParticleZ(0.4), 0, 0.02, 0);
			return;
		}
		PlayerEntity owner = owner();
		if (--life <= 0 || owner == null || !owner.isAlive()) {
			vanish();
			return;
		}
		if (age % 10 == 0) {
			LivingEntity t = owner.getAttacking();
			if (t == null || !t.isAlive() || owner.age - owner.getLastAttackTime() > 100) t = owner.getAttacker();
			if (t != null && t.isAlive() && !ally(t)) setTarget(t);
			else if (getTarget() != null && (!getTarget().isAlive() || ally(getTarget()))) setTarget(null);
		}
		if (getTarget() == null && squaredDistanceTo(owner) > 64) getNavigation().startMovingTo(owner, 1.2);
		if (squaredDistanceTo(owner) > 48 * 48) refreshPositionAndAngles(owner.getX(), owner.getY(), owner.getZ(), getYaw(), 0);
	}

	@Override
	public boolean damage(DamageSource source, float amount) {
		if (ally(source.getAttacker())) return false;
		return super.damage(source, amount);
	}

	public void vanish() {
		if (getWorld() instanceof ServerWorld w && !isRemoved()) {
			w.spawnParticles(ParticleTypes.SOUL, getX(), getBodyY(0.5), getZ(), 8, 0.3, 0.5, 0.3, 0.02);
			w.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getBodyY(0.4), getZ(), 6, 0.25, 0.4, 0.25, 0.01);
			w.playSound(null, getX(), getY(), getZ(), net.minecraft.sound.SoundEvents.BLOCK_SOUL_SAND_BREAK,
					net.minecraft.sound.SoundCategory.HOSTILE, 0.7f, 0.8f);
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
