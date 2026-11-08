package com.dakura.origins.power.types;

import com.dakura.origins.power.RacePower;
import com.dakura.origins.race.RaceAccess;
import net.minecraft.entity.player.PlayerEntity;

/** Consulta rápida: a raça do jogador tem um poder deste tipo? */
final class SpectreSupport {
	private SpectreSupport() {}

	static boolean has(PlayerEntity player, Class<? extends RacePower> type) {
		return RaceAccess.getRace(player).map(r -> r.powers().stream().anyMatch(type::isInstance)).orElse(false);
	}
}
