package com.dakura.origins.power.ability;

import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.HashSet;
import java.util.Set;

/**
 * "dakura_origins:tsunami" — Maremoto (Sereia): ergue uma onda gigante e enrolada que avança pela frente,
 * arrastando e afogando os inimigos e apagando o fogo. A Sereia surfa na crista.
 * { "length": 18, "width": 7, "damage": 7, "speed": 0.7 }
 */
public final class TsunamiAbility implements Ability {
	public static final String FLAG = "surfing";
	private final double length, width, speed;
	private final float damage;

	public TsunamiAbility(JsonObject json) {
		this.length = JsonParams.num(json, "length", 18f);
		this.width = JsonParams.num(json, "width", 7f);
		this.damage = JsonParams.num(json, "damage", 7f);
		this.speed = JsonParams.num(json, "speed", 0.7f);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		Vec3d dir = LegendKit.flat(p.getRotationVec(1f));
		Vec3d start = p.getPos().subtract(dir.multiply(1.5));
		Vec3d side = new Vec3d(-dir.z, 0, dir.x);
		int ticks = (int) Math.ceil(length / speed);
		Set<Integer> hit = new HashSet<>();
		float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
		LegendKit.fxAt(p.getServerWorld(), "tsunami", start, yaw, (float) width, ticks);
		LegendKit.anim(p, "surf", ticks);
		EntityFlags.set(p, FLAG, true);
		LegendKit.snd(p, SoundEvents.ITEM_TRIDENT_RIPTIDE_3, 1.0f, 0.7f);
		LegendKit.snd(p, SoundEvents.ENTITY_PLAYER_SPLASH_HIGH_SPEED, 1.2f, 0.6f);
		LegendKit.snd(p, SoundEvents.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, 1.0f, 0.6f);
		LegendKit.run(p, "tsunami", ticks, (pl, age) -> {
			Vec3d front = start.add(dir.multiply(age * speed + 1.5));
			// surfando: a Sereia é carregada um pouco atrás da crista
			Vec3d ride = front.subtract(dir.multiply(1.2)).subtract(pl.getPos());
			pl.setVelocity(ride.x * 0.5 + dir.x * speed * 0.6, Math.max(pl.getVelocity().y, 0.08), ride.z * 0.5 + dir.z * speed * 0.6);
			pl.velocityModified = true;
			pl.fallDistance = 0;
			Box box = new Box(front.subtract(side.multiply(width / 2)).add(-1.2, -1, -1.2), front.add(side.multiply(width / 2)).add(1.2, 3.5, 1.2));
			for (Entity e : pl.getWorld().getOtherEntities(pl, box)) {
				e.extinguish();
				if (e instanceof PlayerEntity) {
					e.addVelocity(dir.x * 0.25, 0.05, dir.z * 0.25);
					e.velocityModified = true;
					continue;
				}
				if (!(e instanceof LivingEntity l) || !l.isAlive()) continue;
				e.setVelocity(dir.x * (speed + 0.35), 0.35, dir.z * (speed + 0.35));
				e.velocityModified = true;
				if (hit.add(e.getId()) && LegendKit.enemy(pl, e)) {
					LegendKit.hurt(pl, l, damage, false);
					l.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 2));
					l.setAir(Math.max(0, l.getAir() - 120));
					LegendKit.fx(l, "splash_hit", 0);
				}
			}
			if (age % 6 == 0) LegendKit.sndAt(pl.getServerWorld(), front, SoundEvents.ENTITY_GENERIC_SPLASH, 1.0f, 0.5f + pl.getRandom().nextFloat() * 0.3f);
			return true;
		}, pl -> {
			EntityFlags.set(pl, FLAG, false);
			LegendKit.fx(pl, "wave_crash", 0);
			LegendKit.snd(pl, SoundEvents.ENTITY_GENERIC_SPLASH, 1.4f, 0.4f);
		});
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "tsunami");
	}
}
