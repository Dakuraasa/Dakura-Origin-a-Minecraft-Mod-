package com.dakura.origins.power.ability;

import com.dakura.origins.power.Ability;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** "dakura_origins:passive" — habilidade que funciona sozinha (só aparece na página e na HUD). */
public final class PassiveAbility implements Ability {
	public PassiveAbility(JsonObject json) {}

	@Override
	public boolean activate(ServerPlayerEntity player) {
		player.sendMessage(Text.translatable("dakura_origins.message.passive").formatted(Formatting.GRAY), true);
		return false;
	}
}
