package com.dakura.origins.mixin.common;

import com.dakura.origins.power.EntityFlags;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Demônios (flag "curse_immune"): a Maldição do Vínculo não prende a armadura no corpo. */
@Mixin(targets = "net.minecraft.screen.slot.ArmorSlot")
public abstract class ArmorSlotMixin {
	@Inject(method = "canTakeItems", at = @At("HEAD"), cancellable = true, require = 0)
	private void dakura$curseImmune(PlayerEntity player, CallbackInfoReturnable<Boolean> cir) {
		boolean immune = player instanceof ServerPlayerEntity sp ? EntityFlags.has(sp, "curse_immune") : EntityFlags.clientHas(player, "curse_immune");
		if (immune && !((Slot) (Object) this).getStack().isEmpty()) cir.setReturnValue(true);
	}
}
