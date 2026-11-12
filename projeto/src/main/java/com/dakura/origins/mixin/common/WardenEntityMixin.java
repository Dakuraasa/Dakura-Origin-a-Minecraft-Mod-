package com.dakura.origins.mixin.common;

import com.dakura.origins.power.types.MarkerPower;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Elfo da Lua: o Warden o reconhece como criatura da escuridão e não o caça. */
@Mixin(WardenEntity.class)
public abstract class WardenEntityMixin {
	@Inject(method = "isValidTarget", at = @At("HEAD"), cancellable = true, require = 0)
	private void dakura$moonElf(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		if (entity instanceof PlayerEntity p && MarkerPower.has(p, "warden_friend")) cir.setReturnValue(false);
	}
}
