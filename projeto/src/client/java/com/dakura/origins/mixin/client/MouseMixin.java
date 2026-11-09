package com.dakura.origins.mixin.client;

import com.dakura.origins.client.DakuraOriginsClient;
import com.dakura.origins.power.ability.Channeling;
import com.dakura.origins.power.ability.TetherAbility;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Laço Áureo (Alto Elfo): enquanto prende alguém, a rodinha do mouse aproxima ou afasta o alvo. */
@Mixin(Mouse.class)
public abstract class MouseMixin {
	@Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true, require = 0)
	private void dakura$tetherScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.player == null || mc.currentScreen != null || vertical == 0 || !Channeling.active(mc.player)) return;
		if (DakuraOriginsClient.sendSpecial(TetherAbility.class, 3, vertical > 0 ? 1 : -1)) ci.cancel();
	}
}
