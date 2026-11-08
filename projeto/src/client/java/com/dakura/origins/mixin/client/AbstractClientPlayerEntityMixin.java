package com.dakura.origins.mixin.client;

import com.dakura.origins.client.config.DakuraConfig;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.util.SkinTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Aparência "slim_model": corpo esguio (modelo de braços finos) com a mesma skin. */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin {
	@Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
	private void dakura$slim(CallbackInfoReturnable<SkinTextures> cir) {
		SkinTextures s = cir.getReturnValue();
		if (s.model() != SkinTextures.Model.SLIM && DakuraConfig.cosmetic((AbstractClientPlayerEntity) (Object) this, "slim_model")) {
			cir.setReturnValue(new SkinTextures(s.texture(), s.textureUrl(), s.capeTexture(), s.elytraTexture(), SkinTextures.Model.SLIM, s.secure()));
		}
	}
}
