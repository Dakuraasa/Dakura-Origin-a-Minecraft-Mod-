package com.dakura.origins.power.ability;

import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * "dakura_origins:duat_sands" — Areias do Duat (Anúbis): abre o portal do submundo sob os pés. Um redemoinho de
 * areia dourada com hieróglifos gira à sua volta, cegando e puxando os inimigos; a cada segundo um chacal
 * espectral salta das areias e morde um deles. { "radius": 7, "duration": 160, "damage": 4 }
 */
public final class DuatSandsAbility implements Ability {
	public static final String FLAG = "duat";
	private final double radius;
	private final int duration;
	private final float damage;

	public DuatSandsAbility(JsonObject json) {
		this.radius = JsonParams.num(json, "radius", 7f);
		this.duration = JsonParams.integer(json, "duration", 160);
		this.damage = JsonParams.num(json, "damage", 4f);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		EntityFlags.set(p, FLAG, true);
		LegendKit.anim(p, "summon_up", 24);
		LegendKit.fx(p, "duat_open", (int) (radius * 10));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 1, true, false, true));
		LegendKit.snd(p, SoundEvents.ENTITY_WOLF_HOWL, 0.9f, 0.6f);
		LegendKit.snd(p, SoundEvents.ENTITY_EVOKER_CAST_SPELL, 1.0f, 0.6f);
		LegendKit.snd(p, SoundEvents.BLOCK_SAND_BREAK, 1.2f, 0.5f);
		LegendKit.run(p, "duat", duration, (pl, age) -> {
			Vec3d c = pl.getPos();
			List<LivingEntity> foes = LegendKit.enemies(pl, c, radius);
			for (LivingEntity e : foes) {
				Vec3d in = c.subtract(e.getPos());
				Vec3d tang = new Vec3d(-in.z, 0, in.x).normalize();
				Vec3d pull = in.normalize().multiply(0.05).add(tang.multiply(0.08));
				e.addVelocity(pull.x, 0, pull.z);
				e.velocityModified = true;
				if (age % 10 == 0) {
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 30, 1, true, false, true));
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0, true, false, true));
				}
			}
			if (age % 20 == 10 && !foes.isEmpty()) {
				LivingEntity v = foes.get(pl.getRandom().nextInt(foes.size()));
				LegendKit.hurt(pl, v, damage, false);
				v.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, 40, 0));
				Vec3d from = c.add(pl.getRandom().nextGaussian() * 1.5, 0.3, pl.getRandom().nextGaussian() * 1.5);
				Vec3d to = v.getPos().add(0, v.getHeight() * 0.5, 0);
				LegendKit.fxAt(pl.getServerWorld(), "jackal_dash", from, (float) to.x, (float) to.y, (float) to.z);
				LegendKit.snd(v, SoundEvents.ENTITY_WOLF_GROWL, 0.8f, 0.7f);
				LegendKit.snd(v, SoundEvents.ENTITY_EVOKER_FANGS_ATTACK, 0.8f, 1.1f);
			}
			if (age % 30 == 0) LegendKit.snd(pl, SoundEvents.BLOCK_SAND_STEP, 1.0f, 0.5f);
			return true;
		}, pl -> {
			EntityFlags.set(pl, FLAG, false);
			LegendKit.fx(pl, "duat_close", 0);
		});
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "duat");
	}
}
