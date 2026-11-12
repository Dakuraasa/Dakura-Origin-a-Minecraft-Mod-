package com.dakura.origins.mixin.common;

import com.dakura.origins.power.types.VanillaPerkPower;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.FleeEntityGoal;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vastaya Gata: os creepers a temem como temem gatos e mantêm distância. */
@Mixin(CreeperEntity.class)
public abstract class CreeperEntityMixin extends HostileEntity {
	protected CreeperEntityMixin(EntityType<? extends HostileEntity> type, World world) {
		super(type, world);
	}

	@Inject(method = "initGoals", at = @At("TAIL"), require = 0)
	private void dakura$fearCatGirl(CallbackInfo ci) {
		this.goalSelector.add(2, new FleeEntityGoal<>(this, PlayerEntity.class, e -> true, 7.0f, 1.0, 1.25,
				e -> e instanceof PlayerEntity p && VanillaPerkPower.has(p, "creeper_fear")));
	}
}
