package com.dakura.origins.mixin.common;

import com.dakura.origins.power.types.MonsterTrucePower;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Espectro: monstros não escolhem o jogador como alvo, a menos que ele ataque primeiro. */
@Mixin(MobEntity.class)
public abstract class MobEntityMixin {
	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
	private void dakura$monsterTruce(LivingEntity target, CallbackInfo ci) {
		if (target != null && (MonsterTrucePower.blocks((MobEntity) (Object) this, target)
				|| target instanceof net.minecraft.entity.player.PlayerEntity p && (com.dakura.origins.power.types.StealthPower.hidden(p)
						|| com.dakura.origins.power.types.ForestStealthPower.blocks((MobEntity) (Object) this, p)
						|| com.dakura.origins.power.ability.FoxFormAbility.isFox(p))
				|| com.dakura.origins.power.ability.CharmAbility.blocks((MobEntity) (Object) this, target))) {
			((MobEntity) (Object) this).setTarget(null);
			ci.cancel();
		}
	}
}
