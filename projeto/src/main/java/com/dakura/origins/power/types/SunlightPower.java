package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * "dakura_origins:sunlight_damage" — a luz do sol queima (mesmas regras dos zumbis:
 * dia, céu aberto acima da cabeça, sem chuva/água; não funciona no Nether/End).
 * <pre>
 * { "type": "dakura_origins:sunlight_damage", "interval": 20, "damage": 1.5,
 *   "fire_seconds": 3, "helmet_protects": false }
 * </pre>
 */
public final class SunlightPower implements RacePower {
	private final int interval;
	private final float damage;
	private final int fireTicks;
	private final boolean helmetProtects;

	public SunlightPower(JsonObject json) {
		this.interval = Math.max(1, JsonParams.integer(json, "interval", 20));
		this.damage = JsonParams.num(json, "damage", 1.5f);
		this.fireTicks = Math.round(JsonParams.num(json, "fire_seconds", 3f) * 20);
		this.helmetProtects = JsonParams.bool(json, "helmet_protects", false);
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.age % interval != 0) return;
		if (!inSunlight(player)) return;

		player.setFireTicks(Math.max(player.getFireTicks(), fireTicks));
		if (damage > 0) player.damage(player.getDamageSources().onFire(), damage);
		if (player.getWorld() instanceof ServerWorld sw) {
			sw.spawnParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.2, player.getZ(),
					6, 0.25, 0.4, 0.25, 0.01);
		}
	}

	public boolean inSunlight(ServerPlayerEntity player) {
		if (player.isCreative() || player.isSpectator()) return false;
		World world = player.getWorld();
		if (!world.isDay() || player.isWet()) return false;
		if (helmetProtects && !player.getEquippedStack(EquipmentSlot.HEAD).isEmpty()) return false;
		BlockPos eyes = BlockPos.ofFloored(player.getX(), player.getEyeY(), player.getZ());
		return world.isSkyVisible(eyes);
	}
}
