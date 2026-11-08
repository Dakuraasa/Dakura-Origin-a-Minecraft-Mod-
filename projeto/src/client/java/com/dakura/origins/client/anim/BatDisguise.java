package com.dakura.origins.client.anim;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.BatEntity;

import java.util.HashMap;
import java.util.Map;

/**
 * Um BatEntity vanilla "de fachada" por jogador transformado, só para desenhar.
 * Copia posição, rotação, idade e dano do jogador a cada frame — o resultado é
 * exatamente o modelo, a textura e a animação de voo do morcego do Minecraft.
 */
public final class BatDisguise {
	private static final Map<Integer, BatEntity> CACHE = new HashMap<>();

	private BatDisguise() {}

	public static BatEntity prepare(AbstractClientPlayerEntity player) {
		BatEntity bat = CACHE.get(player.getId());
		if (bat == null || bat.getWorld() != player.getWorld()) {
			bat = new BatEntity(EntityType.BAT, player.getWorld());
			bat.setRoosting(false);
			CACHE.put(player.getId(), bat);
		}
		bat.setPos(player.getX(), player.getY(), player.getZ());
		bat.prevX = player.prevX;
		bat.prevY = player.prevY;
		bat.prevZ = player.prevZ;
		bat.lastRenderX = player.lastRenderX;
		bat.lastRenderY = player.lastRenderY;
		bat.lastRenderZ = player.lastRenderZ;
		bat.setYaw(player.getYaw());
		bat.prevYaw = player.prevYaw;
		bat.setPitch(player.getPitch());
		bat.prevPitch = player.prevPitch;
		bat.bodyYaw = player.bodyYaw;
		bat.prevBodyYaw = player.prevBodyYaw;
		bat.headYaw = player.headYaw;
		bat.prevHeadYaw = player.prevHeadYaw;
		bat.hurtTime = player.hurtTime;
		bat.age = player.age;
		bat.roostingAnimationState.stop();
		bat.flyingAnimationState.startIfNotRunning(bat.age);
		return bat;
	}

	public static void forget(int entityId) {
		CACHE.remove(entityId);
	}

	public static void clear() {
		CACHE.clear();
	}
}
