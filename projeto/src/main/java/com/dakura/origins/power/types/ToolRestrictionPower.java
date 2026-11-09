package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.item.ToolMaterials;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

/** "dakura_origins:noble_tools" — ferramentas e armas só de ouro ou diamante (o resto ela despreza). */
public final class ToolRestrictionPower implements RacePower {
	private final Text message;

	public ToolRestrictionPower(JsonObject json) {
		this.message = JsonParams.message(json, "message", "dakura_origins.message.noble_tools").formatted(Formatting.GOLD);
	}

	private static boolean cheap(ItemStack s) {
		return s.getItem() instanceof ToolItem t && t.getMaterial() != ToolMaterials.GOLD && t.getMaterial() != ToolMaterials.DIAMOND
				&& t.getMaterial() != ToolMaterials.NETHERITE;
	}

	private static ToolRestrictionPower of(PlayerEntity p) {
		if (p.isCreative()) return null;
		return RaceAccess.getRace(p).flatMap(r -> r.powers().stream().filter(x -> x instanceof ToolRestrictionPower)
				.map(x -> (ToolRestrictionPower) x).findFirst()).orElse(null);
	}

	public static void bootstrap() {
		AttackEntityCallback.EVENT.register((p, w, h, e, hit) -> {
			ToolRestrictionPower t = of(p);
			if (t == null || !cheap(p.getMainHandStack())) return ActionResult.PASS;
			if (!w.isClient) p.sendMessage(t.message, true);
			return ActionResult.FAIL;
		});
		AttackBlockCallback.EVENT.register((p, w, h, pos, dir) -> {
			ToolRestrictionPower t = of(p);
			if (t == null || !cheap(p.getMainHandStack())) return ActionResult.PASS;
			if (!w.isClient) p.sendMessage(t.message, true);
			return ActionResult.FAIL;
		});
	}

	@Override
	public Text useRejection(PlayerEntity player, ItemStack stack) {
		return cheap(stack) ? message : null;
	}
}
