package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

/**
 * "dakura_origins:environment_damage" — dano periódico num ambiente.
 * { "type": "dakura_origins:environment_damage", "when": "water" | "nether", "interval": 20, "damage": 4.0 }
 * water = chuva ou água. Não afeta quem está em modo espectador (Forma Espectral).
 */
public final class EnvironmentDamagePower implements RacePower {
	private final String when;
	private final int interval;
	private final float damage;
	private final boolean ignite;

	public EnvironmentDamagePower(JsonObject json) {
		this.when = JsonParams.str(json, "when", "water");
		this.interval = Math.max(1, JsonParams.integer(json, "interval", 20));
		this.damage = JsonParams.num(json, "damage", 4f);
		this.ignite = JsonParams.bool(json, "ignite", false);
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.age % interval != 0 || player.isSpectator() || player.isCreative()) return;
		boolean hit = switch (when) {
			case "nether" -> player.getWorld().getRegistryKey() == World.NETHER;
			default -> player.isTouchingWaterOrRain();
		};
		if (!hit) return;
		if (ignite) player.setOnFireFor(4f);
		else player.damage(player.getDamageSources().magic(), damage);
	}
}
