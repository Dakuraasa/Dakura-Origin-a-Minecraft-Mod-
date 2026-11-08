package com.dakura.origins.race;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Optional;
import java.util.UUID;

/**
 * "Qual a raça deste jogador?" — funciona nos dois lados.
 * Servidor: lê o attachment salvo no jogador.
 * Cliente: só conhece a raça do jogador local (sincronizada pelo servidor).
 */
public final class RaceAccess {
	public static volatile String clientRaceId = "";
	public static volatile UUID clientPlayerId = null;

	private RaceAccess() {}

	public static Optional<Race> getRace(PlayerEntity player) {
		if (player.getWorld().isClient) {
			return player.getUuid().equals(clientPlayerId) ? RaceRegistry.get(clientRaceId) : Optional.empty();
		}
		return RaceRegistry.get(player.getAttached(RaceAttachments.RACE));
	}
}
