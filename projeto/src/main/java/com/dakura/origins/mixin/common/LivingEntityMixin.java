package com.dakura.origins.mixin.common;

import com.dakura.origins.power.types.InvertedPotionPower;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Espectro: Cura Instantânea fere e Dano Instantâneo cura, como num morto-vivo. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
	@Inject(method = "hasInvertedHealingAndHarm", at = @At("HEAD"), cancellable = true)
	private void dakura$invertedPotions(CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof PlayerEntity p && InvertedPotionPower.inverts(p)) cir.setReturnValue(true);
	}

	/** Huli Jing: não sabe nadar (pular na água não sobe). */
	@Inject(method = "swimUpward", at = @At("HEAD"), cancellable = true)
	private void dakura$noSwim(net.minecraft.registry.tag.TagKey<net.minecraft.fluid.Fluid> fluid, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
		if ((Object) this instanceof PlayerEntity p && com.dakura.origins.power.types.MarkerPower.has(p, "no_swim")) ci.cancel();
	}

	/** Dríade: imune a veneno e decomposição (Wither). */
	@Inject(method = "canHaveStatusEffect", at = @At("HEAD"), cancellable = true)
	private void dakura$effectImmunity(net.minecraft.entity.effect.StatusEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof PlayerEntity p && com.dakura.origins.power.types.EffectImmunityPower.blocks(p, effect)) {
			cir.setReturnValue(false);
		}
	}
}
