package com.dakura.origins.mixin.common;

import com.dakura.origins.power.types.VanillaPerkPower;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BrewingStandBlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Dama Cogumelo: poções que ELA prepara saem em dobro (as cópias caem sobre o suporte). */
@Mixin(BrewingStandBlockEntity.class)
public abstract class BrewingStandMixin {
	@Inject(method = "craft", at = @At("TAIL"), require = 0)
	private static void dakura$doubleBrew(World world, BlockPos pos, DefaultedList<ItemStack> slots, CallbackInfo ci) {
		if (world.isClient || !VanillaPerkPower.brewedByAlchemist(world, pos)) return;
		for (int i = 0; i < 3; i++) {
			ItemStack s = slots.get(i);
			if (!s.isEmpty()) Block.dropStack(world, pos.up(), s.copy());
		}
	}
}
