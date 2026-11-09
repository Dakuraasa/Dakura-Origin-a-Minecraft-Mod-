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
	/** Huli Jing em forma de raposa: o botão de ataque vira a explosão de fogo/gelo. */
	@Inject(method = "doAttack", at = @At("HEAD"), cancellable = true, require = 0)
	private void dakura$foxBurst(CallbackInfoReturnable<Boolean> cir) {
		var mc = (MinecraftClient) (Object) this;
		if (mc.player == null || !com.dakura.origins.power.ability.FoxFormAbility.isFox(mc.player)
				&& !com.dakura.origins.power.ability.Channeling.active(mc.player)) return;
		cir.setReturnValue(false);
		if (com.dakura.origins.power.ability.Channeling.active(mc.player)) return;
		com.dakura.origins.client.DakuraOriginsClient.sendSpecial(com.dakura.origins.power.ability.FoxFormAbility.class, 4, 0);
	}

	@Inject(method = "hasOutline", at = @At("HEAD"), cancellable = true)
	private void dakura$keenSight(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (ClientEffects.keenOutline(entity)) cir.setReturnValue(true);
	}
}
