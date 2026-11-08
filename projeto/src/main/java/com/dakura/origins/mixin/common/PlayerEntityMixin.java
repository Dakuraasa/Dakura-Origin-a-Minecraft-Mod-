package com.dakura.origins.mixin.common;

import com.dakura.origins.power.ability.BatFormState;
import com.dakura.origins.power.types.MaturityPower;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
	/** Em forma de morcego o jogador tem o tamanho (hitbox e olhos) do morcego vanilla. */
	@Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true, require = 0)
	private void dakura$batDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		if (BatFormState.isBat((PlayerEntity) (Object) this)) {
			cir.setReturnValue(EntityType.BAT.getDimensions());
		}
	}

	/** Raças com maturidade (Dracônica) não acumulam experiência vanilla. */
	@Inject(method = "addExperience", at = @At("HEAD"), cancellable = true)
	private void dakura$blockExperience(int amount, CallbackInfo ci) {
		if (MaturityPower.blocksExperience((PlayerEntity) (Object) this)) ci.cancel();
	}

	@Inject(method = "addExperienceLevels", at = @At("HEAD"), cancellable = true)
	private void dakura$blockLevels(int levels, CallbackInfo ci) {
		if (MaturityPower.blocksExperience((PlayerEntity) (Object) this)) ci.cancel();
	}
}
