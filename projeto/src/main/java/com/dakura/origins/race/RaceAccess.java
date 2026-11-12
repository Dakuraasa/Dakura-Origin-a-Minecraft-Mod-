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

	/** Succubus transformada: raça composta (poderes e 1ª habilidade da raça copiada + a destransformação). */
	public static final java.util.Map<UUID, Race> BORROWED = new java.util.concurrent.ConcurrentHashMap<>();

	public static Optional<Race> getRace(PlayerEntity player) {
		if (player.getWorld().isClient) {
			return player.getUuid().equals(clientPlayerId) ? RaceRegistry.get(clientRaceId) : Optional.empty();
		}
		Race b = BORROWED.get(player.getUuid());
		if (b != null) return Optional.of(b);
		return RaceRegistry.get(player.getAttached(RaceAttachments.RACE));
	}

	/** A raça escolhida de verdade (ignora a transformação). */
	public static Optional<Race> baseRace(PlayerEntity player) {
		return RaceRegistry.get(player.getAttached(RaceAttachments.RACE));
	}
}
