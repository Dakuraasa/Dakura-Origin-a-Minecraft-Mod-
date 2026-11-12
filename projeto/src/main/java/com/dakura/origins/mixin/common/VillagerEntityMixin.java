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
		VillagerEntity v = (VillagerEntity) (Object) this;
		if (MarkerPower.has(player, "villager_markup")) {
			// Orc: os aldeões cobram mais caro
			for (TradeOffer offer : v.getOffers()) offer.increaseSpecialPrice(Math.max(1, offer.getOriginalFirstBuyItem().getCount() / 4));
			return;
		}
		double frac;
		if (MarkerPower.has(player, "charm_discount")) {
			// Succubus: o desconto vem do encanto, não da violência
			frac = com.dakura.origins.power.ability.CharmAbility.charmedBy(v, player) ? 0.6 : 0.0;
		} else if (MarkerPower.has(player, "lucky_discount")) {
			// Lilac (Sorye): a sorte puxa o preço para baixo, sem depender de reputação
			frac = 0.3;
		} else {
			if (!MarkerPower.has(player, "villager_discount")) return;
			// Demônio: quanto mais o aldeão o teme (reputação negativa por ataques), maior o desconto.
			// A reputação ruim do vanilla aumentaria o preço: zera isso antes de aplicar o desconto.
			int rep = v.getReputation(player);
			for (TradeOffer offer : v.getOffers()) offer.clearSpecialPrice();
			frac = 0.2 + 0.4 * Math.max(0.0, Math.min(1.0, -rep / 60.0));
		}
		if (frac <= 0) return;
		for (TradeOffer offer : v.getOffers()) {
			int cut = (int) Math.floor(frac * offer.getOriginalFirstBuyItem().getCount());
			if (cut > 0) offer.increaseSpecialPrice(-cut);
		}
	}
}
