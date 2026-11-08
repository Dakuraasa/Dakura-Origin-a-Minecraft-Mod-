package com.dakura.origins.power.ability;

import net.minecraft.entity.player.PlayerEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Quem está em forma de morcego. Consultado pelos dois lados
 * (hitbox, render, bloqueio de interações), por isso fica no código comum.
 */
public final class BatFormState {
	/** Servidor: por UUID. */
	public static final Set<UUID> SERVER = ConcurrentHashMap.newKeySet();
	/** Cliente: por id de entidade (sincronizado pelo BatStatePayload). */
	public static final Set<Integer> CLIENT = ConcurrentHashMap.newKeySet();

	private BatFormState() {}

	public static boolean isBat(PlayerEntity player) {
		return player.getWorld().isClient ? CLIENT.contains(player.getId()) : SERVER.contains(player.getUuid());
	}
}
