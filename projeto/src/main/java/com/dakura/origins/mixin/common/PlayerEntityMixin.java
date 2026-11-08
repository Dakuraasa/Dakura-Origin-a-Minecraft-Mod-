package com.dakura.origins.mixin.common;

import com.dakura.origins.power.ability.BatFormState;
import com.dakura.origins.power.types.MaturityPower;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
	/** Em forma de morcego o jogador tem o tamanho (hitbox e olhos) do morcego vanilla. */
	@Inject(method = "getBaseDimensions", at = @At("HEAD"), cancellable = true, require = 0)
	private void dakura$batDimensions(EntityPose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		if (BatFormState.isBat((PlayerEntity) (Object) this)) {
			cir.setReturnValue(EntityType.BAT.getDimensions());
		} else if (com.dakura.origins.power.ability.FoxFormAbility.isFox((PlayerEntity) (Object) this)) {
			cir.setReturnValue(EntityType.FOX.getDimensions());
		} else {
			EntityDimensions morph = com.dakura.origins.power.ability.MorphAbility.dimensions((PlayerEntity) (Object) this);
			if (morph != null) cir.setReturnValue(morph);
		}
	}

	@org.spongepowered.asm.mixin.Unique
	private int dakura$foodBefore;
	@org.spongepowered.asm.mixin.Unique
	private float dakura$satBefore;

	/** Cabra e Minotauro: cada alimento enche menos (precisam comer bastante). */
	@Inject(method = "eatFood", at = @At("HEAD"), require = 0)
	private void dakura$foodBefore(net.minecraft.world.World world, net.minecraft.item.ItemStack stack,
								   net.minecraft.component.type.FoodComponent food, CallbackInfoReturnable<net.minecraft.item.ItemStack> cir) {
		var h = ((PlayerEntity) (Object) this).getHungerManager();
		dakura$foodBefore = h.getFoodLevel();
		dakura$satBefore = h.getSaturationLevel();
	}

	@Inject(method = "eatFood", at = @At("RETURN"), require = 0)
	private void dakura$foodAfter(net.minecraft.world.World world, net.minecraft.item.ItemStack stack,
								  net.minecraft.component.type.FoodComponent food, CallbackInfoReturnable<net.minecraft.item.ItemStack> cir) {
		PlayerEntity p = (PlayerEntity) (Object) this;
		float f = com.dakura.origins.power.types.FoodMultiplierPower.factor(p);
		if (f == 1f) return;
		var h = p.getHungerManager();
		int gained = h.getFoodLevel() - dakura$foodBefore;
		float satGained = h.getSaturationLevel() - dakura$satBefore;
		if (gained > 0) h.setFoodLevel(dakura$foodBefore + Math.max(1, Math.round(gained * f)));
		if (satGained > 0) h.setSaturationLevel(Math.min(h.getFoodLevel(), dakura$satBefore + satGained * f));
	}

	/** Raças com maturidade (Dracônica) não acumulam experiência vanilla. */
	@Inject(method = "addExperience", at = @At("HEAD"), cancellable = true)
	private void dakura$blockExperience(int amount, CallbackInfo ci) {
		if (MaturityPower.blocksExperience((PlayerEntity) (Object) this)) ci.cancel();
	}

	@Inject(method = "addExperienceLevels", at = @At("HEAD"), cancellable = true)
	private void dakura$blockLevels(int levels, CallbackInfo ci) {
		if (MaturityPower.blocksExperience((PlayerEntity) (Object) this)) ci.cancel();
	}
}
