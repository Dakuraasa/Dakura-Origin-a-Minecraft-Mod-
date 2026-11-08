package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * "dakura_origins:no_armor" — o corpo não cabe em armaduras convencionais.
 * <pre>
 * { "type": "dakura_origins:no_armor", "message": "Seu corpo não cabe nesta armadura." }
 * </pre>
 * Bloqueia equipar com clique direito e devolve ao inventário qualquer peça que entrar no slot.
 * Cabeças, abóboras e élitros não são "armadura" e continuam permitidos.
 */
public final class NoArmorPower implements RacePower {
	private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
	private final Text message;
	private final java.util.EnumSet<EquipmentSlot> slots = java.util.EnumSet.noneOf(EquipmentSlot.class);

	public NoArmorPower(JsonObject json) {
		this.message = JsonParams.message(json, "message", "dakura_origins.message.no_armor")
				.formatted(Formatting.DARK_RED);
		for (String s : JsonParams.strings(json, "slots")) {
			switch (s) {
				case "head" -> slots.add(EquipmentSlot.HEAD);
				case "chest" -> slots.add(EquipmentSlot.CHEST);
				case "legs" -> slots.add(EquipmentSlot.LEGS);
				case "feet" -> slots.add(EquipmentSlot.FEET);
				default -> {}
			}
		}
		if (slots.isEmpty()) slots.addAll(java.util.List.of(SLOTS));
	}

	@Override
	public Text useRejection(PlayerEntity player, ItemStack stack) {
		return stack.getItem() instanceof ArmorItem a && slots.contains(a.getSlotType()) ? message : null;
	}

	@Override
	public void onApply(ServerPlayerEntity player) {
		strip(player);
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.age % 10 == 0) strip(player);
	}

	private void strip(ServerPlayerEntity player) {
		if (player.isCreative() || player.isSpectator()) return;
		boolean removed = false;
		for (EquipmentSlot slot : slots) {
			ItemStack stack = player.getEquippedStack(slot);
			if (!stack.isEmpty() && stack.getItem() instanceof ArmorItem) {
				player.equipStack(slot, ItemStack.EMPTY);
				player.getInventory().offerOrDrop(stack);
				removed = true;
			}
		}
		if (removed) player.sendMessage(message, true);
	}
}
