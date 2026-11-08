package com.dakura.origins.client.anim;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.FoxEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Raposa vanilla "de fachada" (como o morcego da Vampírica), copiando posição, rotação e passos do jogador. */
public final class FoxDisguise {
	private static final Map<Integer, FoxEntity> CACHE = new HashMap<>();

	private FoxDisguise() {}

	public static FoxEntity prepare(AbstractClientPlayerEntity p, boolean snow) {
		FoxEntity fox = CACHE.get(p.getId());
		if (fox == null || fox.getWorld() != p.getWorld()) {
			fox = new FoxEntity(EntityType.FOX, p.getWorld());
			CACHE.put(p.getId(), fox);
		}
		fox.setVariant(snow ? FoxEntity.Type.SNOW : FoxEntity.Type.RED);
		fox.setPos(p.getX(), p.getY(), p.getZ());
		fox.prevX = p.prevX;
		fox.prevY = p.prevY;
		fox.prevZ = p.prevZ;
		fox.lastRenderX = p.lastRenderX;
		fox.lastRenderY = p.lastRenderY;
		fox.lastRenderZ = p.lastRenderZ;
		fox.setYaw(p.getYaw());
		fox.prevYaw = p.prevYaw;
		fox.setPitch(p.getPitch());
		fox.prevPitch = p.prevPitch;
		fox.bodyYaw = p.bodyYaw;
		fox.prevBodyYaw = p.prevBodyYaw;
		fox.headYaw = p.headYaw;
		fox.prevHeadYaw = p.prevHeadYaw;
		fox.hurtTime = p.hurtTime;
		fox.age = p.age;
		fox.setOnGround(p.isOnGround());
		return fox;
	}

	/** Uma vez por tick: as patas acompanham o quanto o jogador andou. */
	public static void tick(ClientWorld world) {
		if (CACHE.isEmpty()) return;
		Iterator<Map.Entry<Integer, FoxEntity>> it = CACHE.entrySet().iterator();
		while (it.hasNext()) {
			var en = it.next();
			Entity p = world.getEntityById(en.getKey());
			if (p == null) {
				it.remove();
				continue;
			}
			double dx = p.getX() - p.prevX, dz = p.getZ() - p.prevZ;
			float speed = (float) Math.min(1.0, Math.sqrt(dx * dx + dz * dz) * 4.0);
			en.getValue().limbAnimator.updateLimbs(speed, 0.4f);
		}
	}

	public static void forget(int id) {
		CACHE.remove(id);
	}

	public static void clear() {
		CACHE.clear();
	}
}
