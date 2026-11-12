package com.dakura.origins.mixin.common;

import com.dakura.origins.power.types.VanillaPerkPower;
import net.minecraft.block.BlockState;
import net.minecraft.block.FarmlandBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Boneca: pés de pano nunca pisoteiam a terra arada. */
@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin {
	@Inject(method = "setToDirt", at = @At("HEAD"), cancellable = true, require = 0)
	private static void dakura$noTrample(Entity entity, BlockState state, World world, BlockPos pos, CallbackInfo ci) {
		if (entity instanceof PlayerEntity p && VanillaPerkPower.has(p, "no_trample")) ci.cancel();
	}
}
