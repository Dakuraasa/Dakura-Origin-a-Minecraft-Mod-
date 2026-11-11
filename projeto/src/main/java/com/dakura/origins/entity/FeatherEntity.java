package com.dakura.origins.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pena-adaga da Vastaya (ave): flecha própria da raça (não existe em outro lugar do jogo). Fica cravada no
 * corpo do alvo (desenho próprio, não a flecha vanilla), aplica fraqueza e acumula dano no mesmo alvo
 * (+1 a cada 3 penas, por 5 s). Não pode ser recolhida e some sozinha.
 */
public class FeatherEntity extends PersistentProjectileEntity {
	private static final Map<UUID, long[]> STACKS = new ConcurrentHashMap<>(); // alvo → {penas, expira}

	public FeatherEntity(EntityType<? extends FeatherEntity> type, World world) {
		super(type, world);
	}

	public FeatherEntity(World world, LivingEntity owner) {
		super(ModEntities.FEATHER, owner, world, new ItemStack(Items.FEATHER), null);
		this.pickupType = PickupPermission.DISALLOWED;
	}

	@Override
	protected ItemStack getDefaultItemStack() {
		return new ItemStack(Items.FEATHER);
	}

	@Override
	protected SoundEvent getHitSound() {
		return SoundEvents.ENTITY_ARROW_HIT;
	}

	@Override
	protected void onEntityHit(EntityHitResult hit) {
		Entity e = hit.getEntity();
		long now = getWorld().getTime();
		long[] st = STACKS.get(e.getUuid());
		if (st == null || st[1] < now) st = new long[]{0, 0};
		double v = Math.max(0.1, getVelocity().length());
		double dmg = 2.0 + st[0] / 3;
		setDamage((dmg - 0.02) / v);
		e.timeUntilRegen = 0;
		super.onEntityHit(hit);
		if (e instanceof LivingEntity le) {
			// sem flechas vanilla presas: a pena cravada é desenhada pelo cliente
			if (le.getStuckArrowCount() > 0) le.setStuckArrowCount(le.getStuckArrowCount() - 1);
			le.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
					net.minecraft.entity.effect.StatusEffects.WEAKNESS, 100, st[0] >= 6 ? 1 : 0));
			com.dakura.origins.power.ability.LegendKit.fx(le, "feather_stuck", random.nextInt(1 << 20));
			st[0] = Math.min(9, st[0] + 1);
			st[1] = now + 100;
			STACKS.put(e.getUuid(), st);
		}
		if (STACKS.size() > 256) STACKS.entrySet().removeIf(en -> en.getValue()[1] < now);
	}

	@Override
	public void tick() {
		super.tick();
		if (!getWorld().isClient && age > 80) discard();
	}
}
