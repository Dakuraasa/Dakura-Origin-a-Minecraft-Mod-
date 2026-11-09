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

	/** Raças imunes ao fogo (Dracônica, Demônio...): sem chamas desenhadas no corpo. */
	@Inject(method = "doesRenderOnFire", at = @At("HEAD"), cancellable = true, require = 0)
	private void dakura$noFireVisual(org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
		Entity self = (Entity) (Object) this;
		if (self instanceof PlayerEntity && self.getWorld().isClient
				&& com.dakura.origins.power.EntityFlags.clientHas(self, "fireproof")) cir.setReturnValue(false);
	}

	/** Fada sentada no ombro de outro jogador (em vez de em cima da cabeça). */
	@Inject(method = "getPassengerAttachmentPos", at = @At("HEAD"), cancellable = true, require = 0)
	private void dakura$shoulder(Entity passenger, net.minecraft.entity.EntityDimensions dims, float scale,
								 org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<net.minecraft.util.math.Vec3d> cir) {
		if ((Object) this instanceof PlayerEntity && com.dakura.origins.power.ability.FairyPerchAbility.isFairy(passenger)) {
			cir.setReturnValue(new net.minecraft.util.math.Vec3d(-0.36 * scale, dims.height() * 0.78, 0.02));
		}
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
