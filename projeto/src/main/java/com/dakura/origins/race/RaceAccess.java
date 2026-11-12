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

	/** Jogadores de raça "nether_suppressed" que estão no Nether agora (controlado pelo RaceManager). */
	public static final java.util.Set<UUID> IN_NETHER = java.util.concurrent.ConcurrentHashMap.newKeySet();

	public static Optional<Race> getRace(PlayerEntity player) {
		if (player.getWorld().isClient) {
			return player.getUuid().equals(clientPlayerId) ? RaceRegistry.get(clientRaceId) : Optional.empty();
		}
		Race b = BORROWED.get(player.getUuid());
		if (b != null) return Optional.of(b);
		String id = player.getAttached(RaceAttachments.RACE);
		if (IN_NETHER.contains(player.getUuid())) {
			Optional<Race> weak = RaceRegistry.netherForm(id);
			if (weak.isPresent()) return weak;
		}
		return RaceRegistry.get(id);
	}

	/** A raça escolhida de verdade (ignora a transformação). */
	public static Optional<Race> baseRace(PlayerEntity player) {
		return RaceRegistry.get(player.getAttached(RaceAttachments.RACE));
	}
}
