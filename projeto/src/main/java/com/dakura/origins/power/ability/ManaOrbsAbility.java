package com.dakura.origins.power.ability;

import com.dakura.origins.entity.GeoProjectileEntity;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.types.MaturityPower;
import com.dakura.origins.sound.ModSounds;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:mana_orbs" — Esferas Errantes (Kitsune): esferas de mana azul orbitam em volta dela, com rastro de
 * cometa (3, 6 ou 9, conforme a maturidade). No próximo golpe dela (ou ao usar de novo, na criatura da mira), todas
 * disparam como mísseis teleguiados, uma após a outra, perseguindo o alvo e explodindo com dano real.
 * { "duration": 400, "damage": 4, "range": 24, "maturity_damage": [1.0, 1.35, 1.75] }
 */
public final class ManaOrbsAbility implements Ability {
	public static final String FLAG = "mana_orbs";
	private static final Map<UUID, ManaOrbsAbility> ARMED = new HashMap<>();
	private final int duration;
	private final float damage;
	private final double range;
	private final float[] maturityDamage;

	public ManaOrbsAbility(JsonObject json) {
		this.duration = JsonParams.integer(json, "duration", 400);
		this.damage = JsonParams.num(json, "damage", 4f);
		this.range = JsonParams.num(json, "range", 24f);
		this.maturityDamage = JsonParams.floats(json, "maturity_damage");
	}

	public static void bootstrap() {
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (!world.isClient && player instanceof ServerPlayerEntity sp && entity instanceof LivingEntity le && le.isAlive()) {
				ManaOrbsAbility a = ARMED.get(sp.getUuid());
				if (a != null) a.launch(sp, le);
			}
			return ActionResult.PASS;
		});
	}

	/** 3, 6 ou 9 esferas conforme o estágio de maturidade. */
	public static int count(ServerPlayerEntity p) {
		return 3 * (1 + Math.min(2, MaturityPower.stage(p)));
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		if (ARMED.containsKey(p.getUuid())) {
			LivingEntity t = LegendKit.aim(p, range, 0.8);
			if (t == null) return false;
			launch(p, t);
			return false;
		}
		ARMED.put(p.getUuid(), this);
		EntityFlags.set(p, FLAG, true);
		LegendKit.anim(p, "cast", 10);
		ModSounds.play(p, ModSounds.MANA_CAST, 1f, 1.3f);
		LegendKit.run(p, "mana_orbs", duration, (pl, age) -> ARMED.containsKey(pl.getUuid()), pl -> {
			ARMED.remove(pl.getUuid());
			EntityFlags.set(pl, FLAG, false);
		});
		return true;
	}

	private void launch(ServerPlayerEntity p, LivingEntity target) {
		ARMED.remove(p.getUuid());
		EntityFlags.set(p, FLAG, false);
		ServerWorld w = p.getServerWorld();
		int n = count(p);
		float dmg = damage * MaturityPower.multiplier(p, maturityDamage);
		List<GeoProjectileEntity> orbs = new ArrayList<>();
		double phase = w.getTime() * 0.15;
		for (int i = 0; i < n; i++) {
			double a = phase + i * Math.PI * 2 / n;
			GeoProjectileEntity o = new GeoProjectileEntity(w, p, GeoProjectileEntity.MANA, 0, 1, 300);
			o.scripted = true;
			o.setSize(0.7f);
			o.setPosition(p.getX() + Math.cos(a) * 1.3, p.getY() + p.getHeight() * 0.55 + Math.sin(a * 2) * 0.15, p.getZ() + Math.sin(a) * 1.3);
			o.setVelocity(Math.cos(a) * 0.25, 0.25, Math.sin(a) * 0.25);
			w.spawnEntity(o);
			orbs.add(o);
		}
		ModSounds.play(p, ModSounds.MANA_CAST, 1.2f, 1.6f);
		int id = target.getId();
		LegendKit.run(p, "mana_missiles", 160, (pl, age) -> {
			LivingEntity tg = w.getEntityById(id) instanceof LivingEntity l && l.isAlive() ? l : null;
			boolean any = false;
			for (int i = 0; i < orbs.size(); i++) {
				GeoProjectileEntity o = orbs.get(i);
				if (o.isRemoved()) continue;
				any = true;
				if (age < 3 + i * 2) continue; // saem uma de cada vez
				if (tg == null) {
					LegendKit.fxAt(w, "boom_mana", o.getPos(), 0.6f, 0, 0);
					o.discard();
					continue;
				}
				Vec3d aimAt = tg.getPos().add(0, tg.getHeight() * 0.55, 0);
				Vec3d to = aimAt.subtract(o.getPos());
				double d = to.length();
				if (d < 0.9) {
					LegendKit.hurt(pl, tg, dmg, false);
					SpiritOrbAbility.freeze(tg, 30);
					SpiritOrbAbility.breakBlocks(w, pl, aimAt, 1.0f);
					LegendKit.fxAt(w, "boom_mana", aimAt, 1.3f, 0, 0);
					ModSounds.play(w, aimAt, ModSounds.MANA_BURST, 0.8f, 1.2f + i * 0.05f);
					o.discard();
					continue;
				}
				// curva teleguiada: vira aos poucos na direção do alvo e acelera
				double sp = Math.min(d, 0.8 + (age - i * 2) * 0.04);
				o.setVelocity(o.getVelocity().multiply(0.7).add(to.normalize().multiply(sp * 0.42)));
				o.velocityModified = true;
			}
			return any;
		}, pl -> orbs.forEach(o -> {
			if (!o.isRemoved()) o.discard();
		}));
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "mana_orbs");
		LegendKit.stop(p, "mana_missiles");
	}
}
