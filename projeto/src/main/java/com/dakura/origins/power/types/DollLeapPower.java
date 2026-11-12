package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:doll_leap" — Boneca: com pouca vida, salta para longe e se reposiciona (o mesmo salto da Égide
 * Dourada do Elfo Real), sem disparar nenhum ataque. { "health": 6, "cooldown": 600 }
 */
public final class DollLeapPower implements RacePower {
	private static final Map<UUID, Long> READY_AT = new HashMap<>();
	private final float health;
	private final int cooldown;

	public DollLeapPower(JsonObject json) {
		health = JsonParams.num(json, "health", 6f);
		cooldown = JsonParams.integer(json, "cooldown", 600);
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		if (p.getHealth() > health || p.isSpectator() || p.isCreative()) return;
		long now = p.getServerWorld().getTime();
		if (now < READY_AT.getOrDefault(p.getUuid(), 0L)) return;
		READY_AT.put(p.getUuid(), now + cooldown);
		GoldenWardPower.escape(p, 6, 11);
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		READY_AT.remove(p.getUuid());
	}
}
