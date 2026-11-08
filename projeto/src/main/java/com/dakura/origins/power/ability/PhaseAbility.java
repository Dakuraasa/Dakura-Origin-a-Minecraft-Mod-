package com.dakura.origins.power.ability;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.power.Ability;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

/**
 * "dakura_origins:phase" — Forma Espectral: vira espectador (invisível, atravessa paredes),
 * sem limite de tempo; apertar de novo volta ao modo de jogo anterior (se houver espaço).
 * O modo anterior fica salvo no jogador, então sair do mundo nessa forma não perde nada.
 */
public final class PhaseAbility implements Ability {
	public static final AttachmentType<String> PREVIOUS_MODE = AttachmentRegistry.<String>create(
			DakuraOrigins.id("phase_previous_mode"), b -> b.persistent(Codec.STRING));

	public PhaseAbility(JsonObject json) {}

	public static void bootstrap() {}

	public static boolean isPhased(PlayerEntity player) {
		return player.getAttached(PREVIOUS_MODE) != null;
	}

	@Override
	public boolean activate(ServerPlayerEntity player) {
		if (!isPhased(player)) {
			if (player.isSpectator()) return false;
			player.setAttached(PREVIOUS_MODE, player.interactionManager.getGameMode().getName());
			effects(player);
			player.changeGameMode(GameMode.SPECTATOR);
			return true;
		}
		if (!player.getWorld().isSpaceEmpty(player, player.getDimensions(player.getPose()).getBoxAt(player.getPos()))) {
			player.sendMessage(Text.translatable("dakura_origins.message.phase_no_room").formatted(Formatting.GRAY), true);
			return false;
		}
		restore(player);
		effects(player);
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		if (isPhased(player)) restore(player);
	}

	private static void restore(ServerPlayerEntity player) {
		GameMode mode = GameMode.byName(player.getAttached(PREVIOUS_MODE), GameMode.SURVIVAL);
		player.removeAttached(PREVIOUS_MODE);
		player.changeGameMode(mode);
	}

	private static void effects(ServerPlayerEntity player) {
		com.dakura.origins.network.ModNetworking.sendAround(
				new com.dakura.origins.network.AnimPayload(player.getId(), "fx:phase", 0), player);
		player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PARTICLE_SOUL_ESCAPE.value(), SoundCategory.PLAYERS, 1.0f, 0.7f);
		player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.5f, 0.5f);
	}
}
