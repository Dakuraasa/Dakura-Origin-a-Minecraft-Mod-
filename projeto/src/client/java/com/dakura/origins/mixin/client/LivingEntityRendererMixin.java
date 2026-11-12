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
	@Unique
	private static int dakura$tint = -1;
	@Unique
	private static boolean dakura$stone = false;

	/** Vastaya camuflada na floresta: sem nome sobre a cabeça. */
	@Inject(method = "hasLabel(Lnet/minecraft/entity/LivingEntity;)Z", at = @At("HEAD"), cancellable = true, require = 0)
	private void dakura$hideLabel(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
		if (com.dakura.origins.power.types.ForestStealthPower.camouflaged(entity)) cir.setReturnValue(false);
	}

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
		dakura$ghost = com.dakura.origins.client.config.DakuraConfig.cosmetic(entity, TranslucentPower.FLAG);
		dakura$tint = entity instanceof com.dakura.origins.entity.HuliCloneEntity c ? (c.isSnow() ? 0xC8E6FF : 0xFFC9A0) : -1;
		dakura$stone = EntityFlags.clientHas(entity, com.dakura.origins.power.types.StatuePower.FLAG)
				|| EntityFlags.clientHas(entity, com.dakura.origins.power.ability.MeteorAbility.FLAG);
	}

	@Inject(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At("RETURN"))
	private void dakura$ghostEnd(LivingEntity entity, float yaw, float tickDelta, MatrixStack matrices,
								 VertexConsumerProvider vertices, int light, CallbackInfo ci) {
		dakura$ghost = false;
		dakura$tint = -1;
		dakura$stone = false;
	}

	/** Camada translúcida (a mesma que o vanilla usa para invisíveis vistos por aliados). */
	@SuppressWarnings("unchecked")
	@Inject(method = "getRenderLayer", at = @At("HEAD"), cancellable = true)
	private void dakura$ghostLayer(LivingEntity entity, boolean showBody, boolean translucent, boolean showOutline,
								   CallbackInfoReturnable<RenderLayer> cir) {
		if (showBody && (com.dakura.origins.client.config.DakuraConfig.cosmetic(entity, TranslucentPower.FLAG)
				|| entity instanceof com.dakura.origins.entity.HuliCloneEntity)) {
			cir.setReturnValue(RenderLayer.getItemEntityTranslucentCull(
					((EntityRenderer<LivingEntity>) (Object) this).getTexture(entity)));
		}
	}

	/** Invisível: nada do que o jogador carrega aparece (armadura, itens nas mãos, capa, asas, caudas, acessórios). */
	@org.spongepowered.asm.mixin.injection.Redirect(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/feature/FeatureRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/entity/Entity;FFFFFF)V"),
			require = 0)
	@SuppressWarnings({"unchecked", "rawtypes"})
	private void dakura$hideGearWhenInvisible(net.minecraft.client.render.entity.feature.FeatureRenderer feature, MatrixStack matrices,
			VertexConsumerProvider vcp, int light, net.minecraft.entity.Entity entity, float limbAngle, float limbDistance, float tickDelta,
			float animationProgress, float headYaw, float headPitch) {
		if (entity instanceof net.minecraft.entity.player.PlayerEntity && entity.isInvisible()) return;
		feature.render(matrices, vcp, light, entity, limbAngle, limbDistance, tickDelta, animationProgress, headYaw, headPitch);
	}

	/** Cor do modelo com 60% de opacidade. */
	@ModifyArg(method = "render(Lnet/minecraft/entity/LivingEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"),
			index = 4, require = 0)
	private int dakura$ghostAlpha(int color) {
		if (dakura$tint != -1) return 0xA8000000 | dakura$tint;   // ilusões da Huli Jing: translúcidas e tingidas
		if (dakura$stone) return (color & 0xFF000000) | 0x8C8A86;  // Gárgula em estátua ou meteoro: cinza pedra
		return dakura$ghost ? (color & 0x00FFFFFF) | 0x99000000 : color;
	}
}
