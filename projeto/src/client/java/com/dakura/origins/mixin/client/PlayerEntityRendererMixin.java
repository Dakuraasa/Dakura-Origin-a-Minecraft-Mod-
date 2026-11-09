package com.dakura.origins.mixin.client;

import com.dakura.origins.client.render.FlightLean;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Pose de voo deitado (Fênix sempre; Serafim no voo rápido): inclina o corpo todo na direção do olhar, como a Elytra.
 * Gira em torno do meio do corpo (não dos pés), para o modelo ficar dentro da caixa do jogador.
 * Só entra em ação com a flag "fly_lean" do poder de voo; sem ela não faz nada.
 * {@code require = 0}: se algum nome de método mudar, o jogo continua abrindo e só esta pose deixa de existir.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
	@Inject(method = "setupTransforms(Lnet/minecraft/client/network/AbstractClientPlayerEntity;Lnet/minecraft/client/util/math/MatrixStack;FFFF)V",
			at = @At("TAIL"), require = 0)
	private void dakura$flightLean(AbstractClientPlayerEntity player, MatrixStack matrices, float animationProgress,
								   float bodyYaw, float tickDelta, float scale, CallbackInfo ci) {
		float lean = FlightLean.amount(player, animationProgress);
		if (lean <= 0.001f) return;
		float pitch = MathHelper.clamp(player.getPitch(tickDelta), -75f, 75f);
		matrices.translate(0.0, 0.9, 0.0);
		matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(lean * (-90f - pitch)));
		matrices.translate(0.0, -0.9, 0.0);
	}
}
