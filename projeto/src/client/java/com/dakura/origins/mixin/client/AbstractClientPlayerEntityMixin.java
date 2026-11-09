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
		// Succubus disfarçada: usa a skin do jogador copiado
		var self = (AbstractClientPlayerEntity) (Object) this;
		var flags = com.dakura.origins.power.EntityFlags.CLIENT.get(self.getId());
		if (flags != null) for (String f : flags) {
			if (!f.startsWith(com.dakura.origins.power.ability.MimicAbility.PREFIX)) continue;
			try {
				var nh = net.minecraft.client.MinecraftClient.getInstance().getNetworkHandler();
				var entry = nh == null ? null : nh.getPlayerListEntry(java.util.UUID.fromString(f.substring(com.dakura.origins.power.ability.MimicAbility.PREFIX.length())));
				if (entry != null) {
					cir.setReturnValue(entry.getSkinTextures());
					return;
				}
			} catch (IllegalArgumentException ignored) {
			}
		}
		if (s.model() != SkinTextures.Model.SLIM && DakuraConfig.cosmetic((AbstractClientPlayerEntity) (Object) this, "slim_model")) {
			cir.setReturnValue(new SkinTextures(s.texture(), s.textureUrl(), s.capeTexture(), s.elytraTexture(), SkinTextures.Model.SLIM, s.secure()));
		}
	}
}
