package com.dakura.origins.mixin.client;

import com.dakura.origins.client.anim.ClientEffects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Olhar Aguçado (Elfo): contorno das criaturas próximas, só na tela de quem usou. */
@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
	@Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
	private void dakura$keenSight(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (ClientEffects.keenOutline(entity)) cir.setReturnValue(true);
	}
}
