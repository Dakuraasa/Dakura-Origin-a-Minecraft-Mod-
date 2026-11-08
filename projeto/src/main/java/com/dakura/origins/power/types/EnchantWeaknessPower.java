package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

/**
 * "dakura_origins:enchanted_weakness" — armas encantadas ferem muito mais (qualquer encantamento),
 * e nada encantado pode ser usado ou vestido. { "multiplier": 1.75 }
 */
public final class EnchantWeaknessPower implements RacePower {
	private static boolean reentry;
	private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND};
	private final float multiplier;
	private final Text message;

	public EnchantWeaknessPower(JsonObject json) {
		this.multiplier = JsonParams.num(json, "multiplier", 1.75f);
		this.message = JsonParams.message(json, "message", "dakura_origins.message.no_enchanted").formatted(Formatting.DARK_RED);
	}

	private static EnchantWeaknessPower of(PlayerEntity p) {
		if (p.isCreative()) return null;
		return RaceAccess.getRace(p).flatMap(r -> r.powers().stream().filter(x -> x instanceof EnchantWeaknessPower)
				.map(x -> (EnchantWeaknessPower) x).findFirst()).orElse(null);
	}

	public static void bootstrap() {
		AttackEntityCallback.EVENT.register((p, w, hand, e, hit) -> {
			EnchantWeaknessPower pw = of(p);
			if (pw == null || !p.getMainHandStack().hasEnchantments()) return ActionResult.PASS;
			if (!w.isClient) p.sendMessage(pw.message, true);
			return ActionResult.FAIL;
		});
		AttackBlockCallback.EVENT.register((p, w, hand, pos, dir) -> {
			EnchantWeaknessPower pw = of(p);
			if (pw == null || !p.getMainHandStack().hasEnchantments()) return ActionResult.PASS;
			if (!w.isClient) p.sendMessage(pw.message, true);
			return ActionResult.FAIL;
		});
	}

	@Override
	public Text useRejection(PlayerEntity player, ItemStack stack) {
		return stack.hasEnchantments() ? message : null;
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		if (p.age % 10 != 0 || p.isCreative() || p.isSpectator()) return;
		boolean removed = false;
		for (EquipmentSlot s : ARMOR) {
			ItemStack st = p.getEquippedStack(s);
			if (!st.isEmpty() && st.hasEnchantments()) {
				p.equipStack(s, ItemStack.EMPTY);
				p.getInventory().offerOrDrop(st);
				removed = true;
			}
		}
		if (removed) p.sendMessage(message, true);
	}

	@Override
	public boolean allowDamage(ServerPlayerEntity p, DamageSource source, float amount) {
		if (reentry || !(source.getAttacker() instanceof LivingEntity a) || source.getSource() != a
				|| !a.getMainHandStack().hasEnchantments()) return true;
		reentry = true;
		try {
			p.damage(source, amount * multiplier);
		} finally {
			reentry = false;
		}
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.7f, 0.6f);
		return false;
	}
}
