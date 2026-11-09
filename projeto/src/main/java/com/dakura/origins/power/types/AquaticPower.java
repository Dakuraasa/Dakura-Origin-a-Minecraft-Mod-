package com.dakura.origins.power.types;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.power.ability.LegendKit;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "dakura_origins:aquatic" — Sereia: na água ganha a graça do golfinho, o poder do conduíte e visão;
 * a cauda aparece. Fora da água (e sem chuva) por "dry_seconds", as escamas secam: lentidão e fraqueza
 * até se molhar de novo. { "dry_seconds": 90 }
 */
public final class AquaticPower implements RacePower {
	public static final String WET = "sea_wet", DRY = "sea_dry";
	private static final Map<UUID, Integer> DRY_TICKS = new ConcurrentHashMap<>();
	private final int dryTicks;

	public AquaticPower(JsonObject json) {
		this.dryTicks = Math.round(JsonParams.num(json, "dry_seconds", 90f) * 20);
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		boolean water = p.isTouchingWater();
		boolean wet = p.isTouchingWaterOrRain();
		int d = wet ? 0 : DRY_TICKS.getOrDefault(p.getUuid(), 0) + 1;
		DRY_TICKS.put(p.getUuid(), d);
		if (EntityFlags.has(p, WET) != water) {
			EntityFlags.set(p, WET, water);
			if (water) {
				LegendKit.snd(p, SoundEvents.ENTITY_DOLPHIN_SPLASH, 0.6f, 1.3f);
				LegendKit.fx(p, "sea_dive", 0);
			}
		}
		if (water && p.age % 20 == 0) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.DOLPHINS_GRACE, 60, 0, true, false, true));
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.CONDUIT_POWER, 260, 0, true, false, true));
		}
		boolean dry = d > dryTicks;
		if (EntityFlags.has(p, DRY) != dry) {
			EntityFlags.set(p, DRY, dry);
			if (dry) LegendKit.snd(p, SoundEvents.ENTITY_DOLPHIN_HURT, 0.5f, 0.8f);
		}
		if (dry && p.age % 20 == 0) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 0, true, false, true));
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 40, 0, true, false, true));
		}
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		DRY_TICKS.remove(p.getUuid());
		EntityFlags.set(p, WET, false);
		EntityFlags.set(p, DRY, false);
	}
}
