package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * "dakura_origins:hit_effect" — todo golpe corpo a corpo (com qualquer arma) aplica um efeito.
 * { "mode": "fire" | "frost", "seconds": 4 }
 */
public final class HitEffectPower implements RacePower {
	private final boolean frost, wither;
	private final int seconds;

	public HitEffectPower(JsonObject json) {
		this.frost = "frost".equals(JsonParams.str(json, "mode", "fire"));
		this.wither = "wither".equals(JsonParams.str(json, "mode", "fire"));
		this.seconds = JsonParams.integer(json, "seconds", 4);
	}

	public static void bootstrap() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, base, taken, blocked) -> {
			if (blocked || !(source.getAttacker() instanceof PlayerEntity p) || source.getSource() != p) return;
			RaceAccess.getRace(p).ifPresent(r -> r.powers().stream().filter(x -> x instanceof HitEffectPower)
					.map(x -> (HitEffectPower) x).findFirst().ifPresent(h -> {
						if (h.wither) applyWither(target, h.seconds);
						else apply(target, h.frost, h.seconds);
					}));
			StealthPower.reveal(p);
		});
	}

	/** Decomposição (Wither) dos esqueletos wither. */
	public static void applyWither(LivingEntity target, int seconds) {
		if (!(target.getWorld() instanceof ServerWorld w) || !target.isAlive()) return;
		target.addStatusEffect(new StatusEffectInstance(StatusEffects.WITHER, seconds * 20, 0));
		w.spawnParticles(ParticleTypes.SMOKE, target.getX(), target.getBodyY(0.6), target.getZ(), 5, 0.2, 0.3, 0.2, 0.01);
		w.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_WITHER_SKELETON_STEP, SoundCategory.PLAYERS, 0.5f, 0.7f);
	}

	/** Fogo ou gelo num alvo (também usado pelos clones). */
	public static void apply(LivingEntity target, boolean frost, int seconds) {
		if (!(target.getWorld() instanceof ServerWorld w) || !target.isAlive()) return;
		if (frost) {
			target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, seconds * 20, 1));
			target.setFrozenTicks(Math.min(target.getMinFreezeDamageTicks() + 20, target.getFrozenTicks() + 60));
			w.spawnParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getBodyY(0.6), target.getZ(), 8, 0.25, 0.35, 0.25, 0.02);
			w.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BLOCK_POWDER_SNOW_STEP, SoundCategory.PLAYERS, 0.8f, 1.3f);
		} else {
			target.setOnFireFor(seconds);
			w.spawnParticles(ParticleTypes.FLAME, target.getX(), target.getBodyY(0.6), target.getZ(), 6, 0.2, 0.3, 0.2, 0.01);
			w.playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.35f, 1.6f);
		}
	}
}
