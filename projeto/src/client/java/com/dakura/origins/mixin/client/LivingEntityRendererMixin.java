package com.dakura.origins.mixin.client;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.types.ClaustrophobiaPower;
import com.dakura.origins.power.types.TranslucentPower;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * - Tremor vanilla (o de quem congela) para o desconforto da Dracônica em lugares apertados.
 * - Espectro: corpo semitransparente, como um fantasma.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {
	@Unique
	private static boolean dakura$ghost = false;

	@Inject(method = "isShaking", at = @At("RETURN"), cancellable = true)
	private void dakura$crampedShake(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValueZ() && EntityFlags.clientHas(entity, ClaustrophobiaPower.FLAG)) {
			cir.setReturnValue(true);
		}
	}

	@Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At("HEAD"))
	private void dakura$ghostStart(LivingEntity entity, float yaw, float tickDelta, MatrixStack matrices,
								   VertexConsumerProvider vertices, int light, CallbackInfo ci) {
		dakura$ghost = EntityFlags.clientHas(entity, TranslucentPower.FLAG);
	}

	@Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At("RETURN"))
	private void dakura$ghostEnd(LivingEntity entity, float yaw, float tickDelta, MatrixStack matrices,
								 VertexConsumerProvider vertices, int light, CallbackInfo ci) {
		dakura$ghost = false;
	}

	/** Camada translúcida (a mesma que o vanilla usa para invisíveis vistos por aliados). */
	@SuppressWarnings("unchecked")
	@Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true)
	private void dakura$ghostLayer(LivingEntity entity, boolean showBody, boolean translucent, boolean showOutline,
								   CallbackInfoReturnable<RenderLayer> cir) {
		if (showBody && EntityFlags.clientHas(entity, TranslucentPower.FLAG)) {
			cir.setReturnValue(RenderLayer.getItemEntityTranslucentCull(
					((EntityRenderer<LivingEntity>) (Object) this).getTexture(entity)));
		}
	}

	/** Cor do modelo com 60% de opacidade. */
	@ModifyArg(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"),
			index = 4, require = 0)
	private int dakura$ghostAlpha(int color) {
		return dakura$ghost ? (color & 0x00FFFFFF) | 0x99000000 : color;
	}
}
