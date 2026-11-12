package com.dakura.origins.power.ability;

import com.dakura.origins.power.Ability;
import com.dakura.origins.power.Control;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:moon_beam" — Prisão do Eclipse (Elfo da Lua): escolhe um inimigo a ~7 blocos, estende as duas mãos
 * e canaliza um feixe contínuo azul/rosa/roxo/preto. Um raio cai no alvo ao ser atingido; ele fica totalmente preso
 * pela duração, levando dano em pulsos, cegueira e decomposição (aplicados uma vez, com a duração inteira, sem piscar).
 * { "range": 7.5, "duration": 100, "damage": 2.0, "every": 10, "strike": 4.0 }
 */
public final class MoonBeamAbility implements Ability {
	private final double range;
	private final int duration, every;
	private final float damage, strike;

	public MoonBeamAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 7.5f);
		this.duration = JsonParams.integer(json, "duration", 100);
		this.every = JsonParams.integer(json, "every", 10);
		this.damage = JsonParams.num(json, "damage", 2f);
		this.strike = JsonParams.num(json, "strike", 4f);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		if (LegendKit.running(p, "moon_beam") || LegendKit.running(p, "moonfall_cast")) return false;
		LivingEntity t = LegendKit.aim(p, range, 0.75);
		if (t == null) {
			p.sendMessage(Text.translatable("dakura_origins.message.no_target").formatted(Formatting.GRAY), true);
			return false;
		}
		ServerWorld w = p.getServerWorld();
		float night = w.isNight() ? 1.5f : 1f;
		Vec3d anchor = t.getPos();
		LightningEntity bolt = EntityType.LIGHTNING_BOLT.create(w);
		if (bolt != null) {
			bolt.setCosmetic(true);
			bolt.refreshPositionAfterTeleport(anchor);
			w.spawnEntity(bolt);
		}
		LegendKit.hurt(p, t, strike * night, false);
		t.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, duration + 20, 0, false, true));
		t.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, duration, 1, false, true));
		LegendKit.anim(p, "reach", duration);
		LegendKit.snd(p, SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, 1f, 1.4f);
		LegendKit.snd(p, SoundEvents.BLOCK_BEACON_ACTIVATE, 1f, 0.6f);
		int id = t.getId();
		LegendKit.run(p, "moon_beam", duration, (pl, age) -> {
			if (!(w.getEntityById(id) instanceof LivingEntity tg) || !tg.isAlive() || tg.getWorld() != pl.getWorld()
					|| tg.squaredDistanceTo(pl) > (range + 6) * (range + 6)) return false;
			Control.lock(tg, 3, anchor);
			Vec3d hands = pl.getEyePos().add(LegendKit.flat(pl.getRotationVec(1f)).normalize().multiply(0.7)).add(0, -0.45, 0);
			NightFx.beam(w, hands, tg.getPos().add(0, tg.getHeight() * 0.55, 0), age);
			if (age % every == 0) {
				LegendKit.hurt(pl, tg, damage * night, false);
				LegendKit.sndAt(w, tg.getPos(), SoundEvents.ENTITY_WARDEN_HEARTBEAT, 0.8f, 1.6f);
			}
			if (age % 20 == 0) LegendKit.snd(pl, SoundEvents.BLOCK_BEACON_AMBIENT, 0.8f, 1.8f);
			return true;
		}, pl -> {
			LegendKit.anim(pl, "reach", 0);
			if (w.getEntityById(id) instanceof LivingEntity tg) Control.release(tg);
		});
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "moon_beam");
	}
}
