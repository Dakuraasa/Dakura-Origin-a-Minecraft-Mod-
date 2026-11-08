package com.dakura.origins.power;

import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Uma habilidade ativa (ligada às teclas "Habilidade da raça 1/2").
 * Criada a partir do bloco "abilities" do JSON por um {@link PowerRegistry.AbilityFactory}.
 */
public interface Ability {
	/**
	 * O jogador apertou a tecla. Retorne true se a habilidade foi usada
	 * (o tempo de recarga só começa a contar nesse caso).
	 */
	boolean activate(ServerPlayerEntity player);

	/** Limpeza quando o jogador perde a raça, sai do servidor ou morre. */
	default void onRemove(ServerPlayerEntity player) {}

	/** true = a tecla é segurada: o cliente avisa quando começa a segurar e quando solta. */
	default boolean chargeable() {
		return false;
	}

	/** Começou a segurar a tecla (só para habilidades chargeable). */
	default void holdStart(ServerPlayerEntity player) {}

	/** Soltou a tecla depois de heldTicks. Retorne true se a habilidade foi usada (recarga começa). */
	default boolean release(ServerPlayerEntity player, int heldTicks) {
		return activate(player);
	}
}
