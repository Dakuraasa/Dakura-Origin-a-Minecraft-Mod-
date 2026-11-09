package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.RangedWeaponItem;
import net.minecraft.item.TridentItem;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

/**
 * "dakura_origins:pacifist" — Serafim: não fere nenhuma criatura (golpes, flechas, tridentes e nem dano indireto
 * causado por ele). Em troca, nenhum mob o ataca (junto com monster_truce).
 */
public final class PacifistPower implements RacePower {
	private final Text message;

	public PacifistPower(JsonObject json) {
		this.message = JsonParams.message(json, "message", "dakura_origins.message.pacifist").formatted(Formatting.WHITE);
	}

	public static boolean is(PlayerEntity p) {
		return RaceAccess.getRace(p).map(r -> r.powers().stream().anyMatch(x -> x instanceof PacifistPower)).orElse(false);
	}

	public static void bootstrap() {
		AttackEntityCallback.EVENT.register((p, w, h, e, hit) -> {
			if (!is(p)) return ActionResult.PASS;
			if (!w.isClient) p.sendMessage(Text.translatable("dakura_origins.message.pacifist").formatted(Formatting.WHITE), true);
			return ActionResult.FAIL;
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((e, src, amount) ->
				!(src.getAttacker() instanceof PlayerEntity p && p != e && is(p)));
	}

	@Override
	public Text useRejection(PlayerEntity player, ItemStack stack) {
		return stack.getItem() instanceof RangedWeaponItem || stack.getItem() instanceof TridentItem ? message : null;
	}
}
