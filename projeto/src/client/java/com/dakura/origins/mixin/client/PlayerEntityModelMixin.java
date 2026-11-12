package com.dakura.origins.mixin.client;

import com.dakura.origins.client.config.DakuraConfig;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Mermaid: legs are hidden underwater while the fish tail is drawn in their place. */
@Mixin(PlayerEntityModel.class)
public abstract class PlayerEntityModelMixin {
	@Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void dakura$mermaidLegs(LivingEntity entity, float limbAngle, float limbDistance, float age, float headYaw, float headPitch, CallbackInfo ci) {
		if (!entity.isTouchingWater() || !DakuraConfig.cosmetic(entity, "mermaid_tail")) return;
		PlayerEntityModel<?> self = (PlayerEntityModel<?>) (Object) this;
		self.leftLeg.visible = false;
		self.rightLeg.visible = false;
		self.leftPants.visible = false;
		self.rightPants.visible = false;
	}
}
