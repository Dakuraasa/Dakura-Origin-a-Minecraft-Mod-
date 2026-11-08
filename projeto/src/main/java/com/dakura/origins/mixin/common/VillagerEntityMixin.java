package com.dakura.origins.mixin.common;

import com.dakura.origins.power.types.MarkerPower;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Demônio: grande desconto com aldeões, que encolhe conforme a reputação cai (quanto mais bate, menor). */
@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {
	@Inject(method = "prepareOffersFor", at = @At("TAIL"), require = 0)
	private void dakura$demonDeal(PlayerEntity player, CallbackInfo ci) {
		if (!MarkerPower.has(player, "villager_discount")) return;
		VillagerEntity v = (VillagerEntity) (Object) this;
		int rep = v.getReputation(player);
		double frac = 0.5 * Math.max(0.0, Math.min(1.0, 1.0 + rep / 60.0));
		if (frac <= 0) return;
		for (TradeOffer offer : v.getOffers()) {
			int cut = (int) Math.floor(frac * offer.getOriginalFirstBuyItem().getCount());
			if (cut > 0) offer.increaseSpecialPrice(-cut);
		}
	}
}
