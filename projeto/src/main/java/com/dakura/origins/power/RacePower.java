package com.dakura.origins.power;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * Um poder passivo de raça (atributo, efeito, fraqueza, dieta...).
 *
 * Cada poder é criado a partir de um bloco "powers" do JSON da raça por um
 * {@link PowerRegistry.PowerFactory}. A mesma instância é compartilhada por todos
 * os jogadores daquela raça, então guarde estado por jogador fora dela (ou derive do jogador).
 * Todos os ganchos têm implementação vazia: cada poder só sobrescreve o que usa.
 */
public interface RacePower {
	/** O jogador virou esta raça, entrou no mundo ou renasceu. (servidor) */
	default void onApply(ServerPlayerEntity player) {}

	/** O jogador deixou de ser esta raça. Desfaça tudo o que o poder aplicou. (servidor) */
	default void onRemove(ServerPlayerEntity player) {}

	/** Chamado a cada tick do servidor (20x por segundo). Faça o mínimo possível aqui. */
	default void tick(ServerPlayerEntity player) {}

	/** O jogador matou uma criatura. (servidor) */
	default void onKill(ServerPlayerEntity player, LivingEntity victim) {}

	/** Retorne false para anular um dano recebido. (servidor) */
	default boolean allowDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		return true;
	}

	/**
	 * Este jogador pode comer este alimento? null = pode; senão, a mensagem da recusa.
	 * Roda no cliente e no servidor.
	 */
	@Nullable
	default Text consumeRejection(PlayerEntity player, ItemStack stack) {
		return null;
	}

	/**
	 * Este jogador pode usar (clique direito) este item? null = pode.
	 * Roda no cliente e no servidor.
	 */
	@Nullable
	default Text useRejection(PlayerEntity player, ItemStack stack) {
		return null;
	}
}
