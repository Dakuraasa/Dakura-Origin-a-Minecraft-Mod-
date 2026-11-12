package com.dakura.origins.power.ability;

import com.dakura.origins.entity.GeoProjectileEntity;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.types.HitEffectPower;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:night_swords" — Lâminas da Meia-Noite (Elfo Noturno): três espadas de ferro envoltas em aura
 * roxa/rosa flutuam sobre a cabeça, pontas para a frente. No próximo golpe (ou usando de novo, na mira) disparam
 * teleguiadas, causando dano alto e decomposição (Wither). Mesma estrutura das Esferas Errantes da Kitsune.
 * { "duration": 400, "damage": 7, "range": 24, "wither": 5 }
 */
public final class NightSwordsAbility implements Ability {
	public static final String FLAG = "night_swords";
	private static final Map<UUID, NightSwordsAbility> ARMED = new HashMap<>();
	private final int duration, wither;
	private final float damage;
	private final double range;

	public NightSwordsAbility(JsonObject json) {
		this.duration = JsonParams.integer(json, "duration", 400);
		this.damage = JsonParams.num(json, "damage", 7f);
		this.range = JsonParams.num(json, "range", 24f);
		this.wither = JsonParams.integer(json, "wither", 5);
	}

	public static void bootstrap() {
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (!world.isClient && player instanceof ServerPlayerEntity sp && entity instanceof LivingEntity le && le.isAlive()) {
				NightSwordsAbility a = ARMED.get(sp.getUuid());
				if (a != null) a.launch(sp, le);
			}
			return ActionResult.PASS;
		});
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
		LegendKit.snd(p, SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value(), 1f, 0.6f);
		LegendKit.snd(p, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.6f);
		NightFx.burst(p.getServerWorld(), p.getEyePos().add(0, 0.4, 0), 6, 0.4);
		LegendKit.run(p, "night_swords", duration, (pl, age) -> ARMED.containsKey(pl.getUuid()), pl -> {
			ARMED.remove(pl.getUuid());
			EntityFlags.set(pl, FLAG, false);
		});
		return true;
	}

	private void launch(ServerPlayerEntity p, LivingEntity target) {
		ARMED.remove(p.getUuid());
		EntityFlags.set(p, FLAG, false);
		ServerWorld w = p.getServerWorld();
		List<GeoProjectileEntity> swords = new ArrayList<>();
		Vec3d look = LegendKit.flat(p.getRotationVec(1f)).normalize();
		Vec3d side = new Vec3d(-look.z, 0, look.x);
		for (int i = 0; i < 3; i++) {
			GeoProjectileEntity s = new GeoProjectileEntity(w, p, GeoProjectileEntity.SWORD, 0, 1, 300);
			s.scripted = true;
			s.setSize(1f);
			Vec3d at = p.getEyePos().add(0, 0.55 + (i == 1 ? 0.2 : 0), 0).add(side.multiply((i - 1) * 0.6));
			s.setPosition(at.x, at.y, at.z);
			s.setVelocity(look.multiply(0.3).add(0, 0.1, 0));
			w.spawnEntity(s);
			swords.add(s);
		}
		LegendKit.snd(p, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.6f);
		LegendKit.snd(p, SoundEvents.ENTITY_ILLUSIONER_CAST_SPELL, 0.8f, 1.3f);
		int id = target.getId();
		LegendKit.run(p, "night_swords_fly", 160, (pl, age) -> {
			LivingEntity tg = w.getEntityById(id) instanceof LivingEntity l && l.isAlive() ? l : null;
			boolean any = false;
			for (int i = 0; i < swords.size(); i++) {
				GeoProjectileEntity s = swords.get(i);
				if (s.isRemoved()) continue;
				any = true;
				if (age < 2 + i * 3) continue;
				if (tg == null) {
					NightFx.burst(w, s.getPos(), 3, 0.2);
					s.discard();
					continue;
				}
				Vec3d aimAt = tg.getPos().add(0, tg.getHeight() * 0.55, 0);
				Vec3d to = aimAt.subtract(s.getPos());
				double d = to.length();
				if (d < 0.9) {
					LegendKit.hurt(pl, tg, damage, false);
					HitEffectPower.applyWither(tg, wither);
					NightFx.burst(w, aimAt, 5, 0.3);
					LegendKit.sndAt(w, aimAt, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.8f + i * 0.1f);
					s.discard();
					continue;
				}
				double sp = Math.min(d, 0.9 + (age - i * 3) * 0.05);
				s.setVelocity(s.getVelocity().multiply(0.65).add(to.normalize().multiply(sp * 0.45)));
				s.velocityModified = true;
			}
			return any;
		}, pl -> swords.forEach(s -> {
			if (!s.isRemoved()) s.discard();
		}));
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "night_swords");
		LegendKit.stop(p, "night_swords_fly");
	}
}
