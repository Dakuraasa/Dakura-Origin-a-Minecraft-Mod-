package com.dakura.origins.power.ability;

import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:tidal_song" — Canto das Marés (Sereia): uma canção hipnótica em ondas de bolhas e notas.
 * Monstros por perto esquecem o alvo e são puxados devagar, encantados; aliados se curam e respiram debaixo
 * d'água. { "radius": 10, "duration": 160, "heal": 1 }
 */
public final class TidalSongAbility implements Ability {
	public static final String FLAG = "singing";
	private final double radius;
	private final int duration;
	private final float heal;

	public TidalSongAbility(JsonObject json) {
		this.radius = JsonParams.num(json, "radius", 10f);
		this.duration = JsonParams.integer(json, "duration", 160);
		this.heal = JsonParams.num(json, "heal", 1f);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		EntityFlags.set(p, FLAG, true);
		LegendKit.anim(p, "sing", duration);
		LegendKit.fx(p, "song_start", (int) (radius * 10));
		LegendKit.snd(p, SoundEvents.BLOCK_CONDUIT_ACTIVATE, 1.0f, 1.4f);
		LegendKit.snd(p, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 0.9f);
		LegendKit.snd(p, SoundEvents.ENTITY_ALLAY_AMBIENT_WITH_ITEM, 0.9f, 0.7f);
		LegendKit.run(p, "tidal_song", duration, (pl, age) -> {
			Vec3d c = pl.getPos();
			for (LivingEntity e : LegendKit.enemies(pl, c, radius)) {
				if (e instanceof MobEntity m) {
					m.setTarget(null);
					m.getNavigation().stop();
				}
				Vec3d pull = c.subtract(e.getPos());
				if (pull.lengthSquared() > 4) {
					pull = pull.normalize().multiply(0.06);
					e.addVelocity(pull.x, 0, pull.z);
					e.velocityModified = true;
				}
				if (age % 20 == 0) {
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 2, true, false, true));
					e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 1, true, false, true));
					LegendKit.fx(e, "charmed", 0);
				}
			}
			if (age % 20 == 0) {
				for (PlayerEntity o : LegendKit.allies(pl, c, radius)) {
					o.heal(heal);
					o.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 400, 0, true, false, true));
					o.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 100, 0, true, false, true));
				}
				LegendKit.snd(pl, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, 0.8f + (age / 20 % 4) * 0.15f);
			}
			if (age % 40 == 10) LegendKit.snd(pl, SoundEvents.ENTITY_DOLPHIN_PLAY, 0.6f, 1.2f);
			return true;
		}, pl -> EntityFlags.set(pl, FLAG, false));
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "tidal_song");
		EntityFlags.set(p, FLAG, false);
	}
}
