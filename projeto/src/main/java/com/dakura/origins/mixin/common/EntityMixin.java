package com.dakura.origins.mixin.common;

import com.dakura.origins.power.types.MarkerPower;
import com.dakura.origins.power.types.StealthPower;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
	/** Huli Jing: passos sem som. */
	@Inject(method = "playStepSound", at = @At("HEAD"), cancellable = true)
	private void dakura$silentSteps(BlockPos pos, BlockState state, CallbackInfo ci) {
		if ((Object) this instanceof PlayerEntity p && StealthPower.silent(p)) ci.cancel();
	}

	/** Huli Jing: sem nado rápido (sprint na água). */
	@Inject(method = "updateSwimming", at = @At("HEAD"), cancellable = true)
	private void dakura$noSwimming(CallbackInfo ci) {
		if ((Object) this instanceof PlayerEntity p && MarkerPower.has(p, "no_swim")) {
			p.setSwimming(false);
			ci.cancel();
		}
	}
}
