package com.dakura.origins.power.types;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.block.Blocks;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "dakura_origins:statue" — parada no chão por alguns segundos, vira pedra: regenera vida e fome.
 * Mexer-se ou levar dano desfaz. { "still_ticks": 100, "interval": 20, "heal": 1.0, "food": 1 }
 */
public final class StatuePower implements RacePower {
	public static final String FLAG = "statue";
	private static final Map<UUID, Integer> STILL = new ConcurrentHashMap<>();
	private static final Map<UUID, Vec3d> LAST = new ConcurrentHashMap<>();
	private final int stillTicks, interval, food;
	private final float heal;

	public StatuePower(JsonObject json) {
		this.stillTicks = JsonParams.integer(json, "still_ticks", 100);
		this.interval = JsonParams.integer(json, "interval", 20);
		this.heal = JsonParams.num(json, "heal", 1f);
		this.food = JsonParams.integer(json, "food", 1);
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		Vec3d pos = p.getPos(), last = LAST.put(p.getUuid(), pos);
		boolean moved = last == null || last.squaredDistanceTo(pos) > 0.0004 || p.handSwinging || !p.isOnGround()
				|| p.getAbilities().flying;
		int now = moved ? 0 : STILL.getOrDefault(p.getUuid(), 0) + 1;
		STILL.put(p.getUuid(), now);
		boolean statue = EntityFlags.has(p, FLAG);
		if (!statue && now >= stillTicks) set(p, true);
		else if (statue && now == 0) set(p, false);
		if (statue && now % interval == 0) {
			if (p.getHealth() < p.getMaxHealth()) p.heal(heal);
			var h = p.getHungerManager();
			if (h.getFoodLevel() < 20) {
				h.setFoodLevel(Math.min(20, h.getFoodLevel() + food));
				h.setSaturationLevel(Math.min(h.getFoodLevel(), h.getSaturationLevel() + 0.5f));
			}
			if (p.age % 60 == 0) p.getServerWorld().spawnParticles(ParticleTypes.ENCHANT, p.getX(), p.getBodyY(0.7), p.getZ(),
					4, 0.3, 0.4, 0.3, 0.2);
		}
	}

	private static void set(ServerPlayerEntity p, boolean on) {
		EntityFlags.set(p, FLAG, on);
		p.getServerWorld().spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.STONE.getDefaultState()),
				p.getX(), p.getBodyY(0.5), p.getZ(), on ? 14 : 22, 0.3, 0.6, 0.3, 0.05);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), on ? SoundEvents.BLOCK_STONE_PLACE : SoundEvents.BLOCK_STONE_BREAK,
				SoundCategory.PLAYERS, 0.7f, on ? 0.6f : 0.8f);
		if (on) p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_GRINDSTONE_USE, SoundCategory.PLAYERS, 0.25f, 0.5f);
	}

	@Override
	public boolean allowDamage(ServerPlayerEntity p, DamageSource source, float amount) {
		if (EntityFlags.has(p, FLAG) && amount > 0) {
			STILL.put(p.getUuid(), 0);
			set(p, false);
		}
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		EntityFlags.set(p, FLAG, false);
		STILL.remove(p.getUuid());
		LAST.remove(p.getUuid());
	}
}
