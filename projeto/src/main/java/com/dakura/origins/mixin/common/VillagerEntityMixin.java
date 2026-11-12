package com.dakura.origins.mixin.common;

import com.dakura.origins.power.types.MarkerPower;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Demônio: desconto sem precisar bater (menor nos aldeões que ele encantou); Succubus: desconto pelo encanto; Orc: preços maiores. */
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
		} else {
			if (!MarkerPower.has(player, "villager_discount")) return;
			// Demônio: desconto fixo de lábia infernal, sem precisar bater. A reputação ruim do vanilla é zerada.
			// Aldeão que ele encantou fica desconfiado: o desconto cai bastante (penalidade).
			for (TradeOffer offer : v.getOffers()) offer.clearSpecialPrice();
			frac = com.dakura.origins.power.ability.CharmAbility.charmedBy(v, player) ? 0.1 : 0.3;
		}
		if (frac <= 0) return;
		for (TradeOffer offer : v.getOffers()) {
			int cut = (int) Math.floor(frac * offer.getOriginalFirstBuyItem().getCount());
			if (cut > 0) offer.increaseSpecialPrice(-cut);
		}
	}
}
