package com.dakura.origins.mixin.client;

import com.dakura.origins.power.EntityFlags;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Cor do contorno: dourado para a proteção do Alto Elfo. */
@Mixin(Entity.class)
public abstract class EntityTeamColorMixin {
	@Inject(method = "getTeamColorValue", at = @At("HEAD"), cancellable = true)
	private void dakura$goldWard(CallbackInfoReturnable<Integer> cir) {
		if (EntityFlags.clientHas((Entity) (Object) this, "gold_ward")) cir.setReturnValue(0xFFD45E);
	}
}
