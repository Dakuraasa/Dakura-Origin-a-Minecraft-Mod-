package com.dakura.origins.mixin.client;

import com.dakura.origins.power.ability.BatFormState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Em primeira pessoa, o morcego não tem mãos nem itens. */
@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
	@Inject(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
			at = @At("HEAD"), cancellable = true)
	private void dakura$hideHands(float tickDelta, MatrixStack matrices, VertexConsumerProvider.Immediate vertexConsumers,
								  ClientPlayerEntity player, int light, CallbackInfo ci) {
		if (BatFormState.isBat(player)) ci.cancel();
	}
}
