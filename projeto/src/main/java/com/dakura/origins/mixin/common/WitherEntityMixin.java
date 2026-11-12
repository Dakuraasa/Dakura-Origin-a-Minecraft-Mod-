package com.dakura.origins.mixin.common;

import com.dakura.origins.power.ability.PuppetChainAbility;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A bound Wither aims all three heads at its controller's target instead of picking random ones. */
@Mixin(WitherEntity.class)
public abstract class WitherEntityMixin {
	@Inject(method = "mobTick", at = @At("TAIL"), require = 0)
	private void dakura$puppetHeads(CallbackInfo ci) {
		WitherEntity self = (WitherEntity) (Object) this;
		if (!PuppetChainAbility.isBound(self)) return;
		LivingEntity t = self.getTarget();
		int id = t != null && t.isAlive() ? t.getId() : 0;
		self.setTrackedEntityId(1, id);
		self.setTrackedEntityId(2, id);
	}
}
