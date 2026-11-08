package com.dakura.origins.mixin.client;

import com.dakura.origins.client.hud.MaturityHud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Troca a barra (e o número) de experiência pela barra de maturidade, quando a raça tem uma. */
@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
	@Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
	private void dakura$maturityBar(DrawContext context, int x, CallbackInfo ci) {
		if (MaturityHud.render(context, x)) ci.cancel();
	}

	@Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
	private void dakura$hideLevel(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
		if (MaturityHud.active()) ci.cancel();
	}
}
