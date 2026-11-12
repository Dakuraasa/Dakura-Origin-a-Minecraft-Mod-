package com.dakura.origins.power.ability;

import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:steel_tempest" — Boneca (inspirado no Q do Yasuo): corte rápido e pontudo para a frente, com um
 * avanço curto. A cada 3 cortes seguidos, o 4º é fortalecido: ignora armadura, desgasta armaduras e escudos, causa
 * mais dano quanto menos vida o alvo tem e aplica um efeito aleatório. A contagem zera se ela ficar "reset_ticks"
 * sem cortar.
 * { "range": 5, "damage": 5, "dash": 0.9, "empower_after": 3, "reset_ticks": 100 }
 */
public final class SteelTempestAbility implements Ability {
	private static final Map<UUID, long[]> STACKS = new HashMap<>(); // {cortes seguidos, último tick}
	private final double range, dash;
	private final float damage;
	private final int empowerAfter, resetTicks;

	public SteelTempestAbility(JsonObject json) {
		range = JsonParams.num(json, "range", 5f);
		dash = JsonParams.num(json, "dash", 0.9f);
		damage = JsonParams.num(json, "damage", 5f);
		empowerAfter = JsonParams.integer(json, "empower_after", 3);
		resetTicks = JsonParams.integer(json, "reset_ticks", 100);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		ServerWorld w = p.getServerWorld();
		long now = w.getTime();
		long[] st = STACKS.computeIfAbsent(p.getUuid(), u -> new long[2]);
		if (now - st[1] > resetTicks) st[0] = 0;
		boolean empowered = st[0] >= empowerAfter;
		Vec3d look = p.getRotationVec(1f);
		Vec3d flat = LegendKit.flat(look);
		// avanço junto com o corte
		p.addVelocity(flat.x * dash, 0.08, flat.z * dash);
		p.velocityModified = true;
		// área pontuda: linha estreita à frente, que afina na ponta
		Vec3d eye = p.getEyePos();
		boolean hitAny = false;
		for (LivingEntity t : w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().stretch(look.multiply(range)).expand(1.2),
				e -> e != p && e.isAlive() && LegendKit.enemy(p, e))) {
			Vec3d to = t.getPos().add(0, t.getHeight() * 0.5, 0).subtract(eye);
			double along = to.dotProduct(look);
			if (along < 0 || along > range) continue;
			double side = to.subtract(look.multiply(along)).length();
			if (side > 1.3 - 0.7 * (along / range) + t.getWidth() * 0.5) continue;
			hitAny = true;
			t.timeUntilRegen = 0;
			if (empowered) empoweredHit(p, t);
			else t.damage(p.getDamageSources().playerAttack(p), damage);
		}
		LegendKit.fx(p, empowered ? "tempest_empowered" : "tempest_cut", 0);
		LegendKit.anim(p, "punch_rush", 6);
		LegendKit.snd(p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1.0f, empowered ? 0.7f : 1.4f);
		if (empowered) {
			LegendKit.snd(p, SoundEvents.ITEM_TRIDENT_RIPTIDE_2, 1.0f, 1.3f);
			st[0] = 0;
		} else if (hitAny) st[0]++;
		st[1] = now;
		return true;
	}

	private void empoweredHit(ServerPlayerEntity p, LivingEntity t) {
		float missing = 1f - t.getHealth() / Math.max(1f, t.getMaxHealth());
		float total = damage * 1.5f + damage * 2f * missing;
		// ignora armadura: dano mágico não passa pela proteção de armadura
		t.damage(p.getDamageSources().indirectMagic(p, p), total);
		for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
			ItemStack a = t.getEquippedStack(s);
			if (!a.isEmpty() && a.isDamageable()) a.damage(12, t, s);
		}
		for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
			ItemStack sh = t.getEquippedStack(s);
			if (sh.getItem() instanceof ShieldItem) {
				sh.damage(40, t, s);
				if (t instanceof PlayerEntity tp) tp.disableShield();
			}
		}
		var r = p.getRandom();
		switch (r.nextInt(5)) {
			case 0 -> t.setFrozenTicks(Math.max(t.getFrozenTicks(), 200));
			case 1 -> t.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON, 100, 1), p);
			case 2 -> t.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 120, 1), p);
			case 3 -> t.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 2), p);
			default -> t.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 100, 1), p);
		}
		t.takeKnockback(0.6, p.getX() - t.getX(), p.getZ() - t.getZ());
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		STACKS.remove(p.getUuid());
	}
}
