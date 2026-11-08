package com.dakura.origins.mixin.common;

import com.dakura.origins.power.types.MarkerPower;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Huli Jing das Neves: anda sobre a neve fofa sem afundar. */
@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {
	@Inject(method = "canWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
	private static void dakura$snowWalker(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof PlayerEntity p && MarkerPower.has(p, "powder_snow_walker")) cir.setReturnValue(true);
	}
}
