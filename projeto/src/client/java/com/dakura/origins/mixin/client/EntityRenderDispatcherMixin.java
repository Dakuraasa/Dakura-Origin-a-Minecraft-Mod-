package com.dakura.origins.mixin.client;

import com.dakura.origins.client.anim.BatDisguise;
import com.dakura.origins.power.ability.BatFormState;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.BatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Jogador em forma de morcego é desenhado como o morcego vanilla (com sombra e tudo). */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
	@Inject(method = "render(Lnet/minecraft/entity/Entity;DDDFFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
			at = @At("HEAD"), cancellable = true)
	private void dakura$renderAsBat(Entity entity, double x, double y, double z, float yaw, float tickDelta,
									MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
		if (entity instanceof AbstractClientPlayerEntity player && BatFormState.isBat(player)) {
			BatEntity bat = BatDisguise.prepare(player);
			((EntityRenderDispatcher) (Object) this).render(bat, x, y, z, yaw, tickDelta, matrices, vertexConsumers, light);
			ci.cancel();
		} else if (entity instanceof AbstractClientPlayerEntity player && com.dakura.origins.power.ability.FoxFormAbility.isFox(player)) {
			var fox = com.dakura.origins.client.anim.FoxDisguise.prepare(player,
					com.dakura.origins.power.EntityFlags.clientHas(player, com.dakura.origins.power.ability.FoxFormAbility.SNOW_FLAG));
			((EntityRenderDispatcher) (Object) this).render(fox, x, y, z, yaw, tickDelta, matrices, vertexConsumers, light);
			ci.cancel();
		} else if (entity instanceof AbstractClientPlayerEntity player) {
			net.minecraft.entity.EntityType<?> type = com.dakura.origins.power.ability.MorphAbility.morphType(player);
			if (type == null) return;
			net.minecraft.entity.LivingEntity form = com.dakura.origins.client.anim.MorphDisguise.prepare(player, type);
			if (form == null) return;
			((EntityRenderDispatcher) (Object) this).render(form, x, y, z, yaw, tickDelta, matrices, vertexConsumers, light);
			ci.cancel();
		}
	}
}
