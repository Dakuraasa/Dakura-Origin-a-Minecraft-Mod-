package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:blessing_altar" — the Fairy's altar: opens a panel with random enchantments at random levels that fit
 * the item in the main hand; the player picks one. { "options": 3 }
 */
public final class BlessingAltarAbility implements Ability {
	private static final Map<UUID, Object[]> SESSIONS = new HashMap<>();
	private final int options;

	public BlessingAltarAbility(JsonObject json) {
		this.options = Math.max(1, Math.min(4, JsonParams.integer(json, "options", 3)));
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		ItemStack hand = p.getMainHandStack();
		if (hand.isEmpty()) {
			p.sendMessage(Text.translatable("dakura_origins.message.altar_empty").formatted(Formatting.WHITE), true);
			return false;
		}
		var reg = p.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT);
		List<RegistryEntry<Enchantment>> pool = new ArrayList<>();
		for (var e : reg.streamEntries().toList()) {
			if (e.value().isAcceptableItem(hand) && EnchantmentHelper.getLevel(e, hand) < e.value().getMaxLevel()
					&& EnchantmentHelper.isCompatible(EnchantmentHelper.getEnchantments(hand).getEnchantments(), e)) pool.add(e);
		}
		if (pool.isEmpty()) {
			p.sendMessage(Text.translatable("dakura_origins.message.altar_none").formatted(Formatting.WHITE), true);
			return false;
		}
		Collections.shuffle(pool, new java.util.Random(p.getRandom().nextLong()));
		List<int[]> opts = new ArrayList<>();
		List<RegistryEntry<Enchantment>> chosen = new ArrayList<>();
		for (int i = 0; i < Math.min(options, pool.size()); i++) {
			RegistryEntry<Enchantment> e = pool.get(i);
			int max = e.value().getMaxLevel();
			int lvl = 1 + p.getRandom().nextInt(max);
			chosen.add(e);
			opts.add(new int[]{reg.getRawId(e.value()), lvl});
		}
		SESSIONS.put(p.getUuid(), new Object[]{hand.getItem(), chosen, opts});
		for (int[] o : opts) ServerPlayNetworking.send(p, new AnimPayload(p.getId(), "fx:altar_opt", (o[0] << 4) | o[1]));
		ServerPlayNetworking.send(p, new AnimPayload(p.getId(), "fx:altar_open", opts.size()));
		ModNetworking.sendAround(new AnimPayload(p.getId(), "pray", 40), p);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:altar_ritual", 0), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.3f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.8f, 1.8f);
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public void choose(ServerPlayerEntity p, int option) {
		Object[] s = SESSIONS.remove(p.getUuid());
		if (s == null || option < 0) return;
		List<RegistryEntry<Enchantment>> chosen = (List<RegistryEntry<Enchantment>>) s[1];
		List<int[]> opts = (List<int[]>) s[2];
		ItemStack hand = p.getMainHandStack();
		if (option >= chosen.size() || hand.isEmpty() || hand.getItem() != s[0]) {
			p.sendMessage(Text.translatable("dakura_origins.message.altar_lost").formatted(Formatting.GRAY), true);
			return;
		}
		RegistryEntry<Enchantment> e = chosen.get(option);
		int lvl = Math.max(opts.get(option)[1], EnchantmentHelper.getLevel(e, hand));
		hand.addEnchantment(e, lvl);
		p.setStackInHand(Hand.MAIN_HAND, hand);
		p.sendMessage(Text.translatable("dakura_origins.message.altar_done", Enchantment.getName(e, lvl)).formatted(Formatting.GOLD), true);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:altar_done", 0), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.8f, 1.6f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.2f, 1.9f);
	}
}
