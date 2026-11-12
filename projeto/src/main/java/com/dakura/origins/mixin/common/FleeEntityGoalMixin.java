package com.dakura.origins.mixin.common;

import com.dakura.origins.power.types.MarkerPower;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Races with the "animal_friend" marker do not scare animals away. */
@Mixin(FleeEntityGoal.class)
public abstract class FleeEntityGoalMixin {
	@Shadow @Final protected PathAwareEntity mob;
	@Shadow protected LivingEntity targetEntity;

	@Inject(method = "canStart", at = @At("RETURN"), cancellable = true, require = 0)
	private void dakura$animalFriend(CallbackInfoReturnable<Boolean> cir) {
		if (cir.getReturnValueZ() && mob instanceof AnimalEntity && targetEntity instanceof PlayerEntity p
				&& MarkerPower.has(p, "animal_friend")) cir.setReturnValue(false);
	}
}
