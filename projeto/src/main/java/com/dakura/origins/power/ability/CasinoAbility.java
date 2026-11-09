package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * "dakura_origins:casino" — Cassino: aposta o item da mão numa roda. O resultado depende da raridade do item
 * (quanto mais raro, mais difícil): perder tudo, devolver, ×2, ×3 ou ×5. { "spin": 70 }
 */
public final class CasinoAbility implements Ability {
	/** Resultados da roda, na ordem: perde, ×1, ×2, ×3, ×5. */
	public static final int[] MULTS = {0, 1, 2, 3, 5};
	private static final float[][] ODDS = {
			{30, 25, 30, 12, 3},     // comum
			{38, 27, 25, 8, 2},      // incomum
			{45, 30, 20, 4, 1},      // raro / valioso
			{55, 30, 12, 2.5f, 0.5f} // épico
	};
	private static final Set<net.minecraft.item.Item> VALUABLE = Set.of(Items.DIAMOND, Items.EMERALD, Items.NETHERITE_INGOT,
			Items.NETHERITE_SCRAP, Items.ANCIENT_DEBRIS, Items.DIAMOND_BLOCK, Items.EMERALD_BLOCK, Items.GOLD_BLOCK,
			Items.NETHERITE_BLOCK, Items.TOTEM_OF_UNDYING, Items.NETHER_STAR, Items.ELYTRA, Items.ENCHANTED_GOLDEN_APPLE);
	private static final Map<UUID, Object[]> ACTIVE = new HashMap<>(); // {aposta, resultado, idade, giro}
	private final int spin;

	public CasinoAbility(JsonObject json) {
		this.spin = JsonParams.integer(json, "spin", 70);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(CasinoAbility::tickAll);
	}

	private static int tier(ItemStack s) {
		Rarity r = s.getRarity();
		int t = r == Rarity.EPIC ? 3 : r == Rarity.RARE ? 2 : r == Rarity.UNCOMMON ? 1 : 0;
		return VALUABLE.contains(s.getItem()) ? Math.max(t, 2) : t;
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		if (ACTIVE.containsKey(p.getUuid())) return false;
		ItemStack hand = p.getMainHandStack();
		if (hand.isEmpty()) {
			p.sendMessage(Text.translatable("dakura_origins.message.casino_empty").formatted(Formatting.GOLD), true);
			return false;
		}
		ItemStack bet = hand.copy();
		p.setStackInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
		int tier = tier(bet);
		float[] odds = ODDS[tier];
		float roll = p.getRandom().nextFloat() * 100f, acc = 0;
		int result = 0;
		for (int i = 0; i < odds.length; i++) {
			acc += odds[i];
			if (roll < acc) {
				result = i;
				break;
			}
		}
		ACTIVE.put(p.getUuid(), new Object[]{bet, result, new int[]{0}, this.spin});
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p, new AnimPayload(p.getId(), "fx:casino", result | (tier << 4) | (spin << 8)));
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:casino_world", 0), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_CHIME.value(), SoundCategory.PLAYERS, 1.0f, 1.2f);
		return true;
	}

	private static void tickAll(MinecraftServer server) {
		if (ACTIVE.isEmpty()) return;
		Iterator<Map.Entry<UUID, Object[]>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			var en = it.next();
			Object[] st = en.getValue();
			ServerPlayerEntity p = server.getPlayerManager().getPlayer(en.getKey());
			ItemStack bet = (ItemStack) st[0];
			if (p == null) {
				it.remove();
				continue;
			}
			int age = ++((int[]) st[2])[0];
			if (age < (int) st[3]) continue;
			it.remove();
			int result = (int) st[1], mult = MULTS[result];
			int total = bet.getCount() * mult;
			while (total > 0) {
				int n = Math.min(total, bet.getMaxCount());
				p.getInventory().offerOrDrop(bet.copyWithCount(n));
				total -= n;
			}
			var w = p.getServerWorld();
			if (mult == 0) {
				p.sendMessage(Text.translatable("dakura_origins.message.casino_lose").formatted(Formatting.DARK_RED), true);
				w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_DIDGERIDOO.value(), SoundCategory.PLAYERS, 1.0f, 0.6f);
				w.spawnParticles(ParticleTypes.SMOKE, p.getX(), p.getBodyY(0.8), p.getZ(), 14, 0.3, 0.3, 0.3, 0.02);
			} else if (mult == 1) {
				p.sendMessage(Text.translatable("dakura_origins.message.casino_even").formatted(Formatting.GRAY), true);
				w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.PLAYERS, 0.8f, 0.8f);
			} else {
				p.sendMessage(Text.translatable("dakura_origins.message.casino_win", mult).formatted(Formatting.GOLD, Formatting.BOLD), true);
				ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:jackpot", mult), p);
				w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f + mult * 0.08f);
				w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS, 1.0f, 1.5f);
			}
		}
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		Object[] st = ACTIVE.remove(p.getUuid());
		if (st != null) p.getInventory().offerOrDrop((ItemStack) st[0]);
	}
}
