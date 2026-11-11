package com.dakura.origins.client.anim;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Metamorfose do Demônio: a criatura "de fachada" desenhada no lugar do jogador. */
public final class MorphDisguise {
	private static final Map<Integer, LivingEntity> CACHE = new HashMap<>();

	private MorphDisguise() {}

	public static LivingEntity prepare(AbstractClientPlayerEntity p, EntityType<?> type) {
		LivingEntity e = CACHE.get(p.getId());
		if (e == null || e.getType() != type || e.getWorld() != p.getWorld()) {
			Entity made = type.create(p.getWorld());
			if (!(made instanceof LivingEntity le)) return null;
			if (made instanceof MobEntity m) m.setAiDisabled(true);
			made.setSilent(true);
			made.setNoGravity(true);
			made.noClip = true;
			e = le;
			CACHE.put(p.getId(), e);
		}
		e.setPos(p.getX(), p.getY(), p.getZ());
		e.prevX = p.prevX;
		e.prevY = p.prevY;
		e.prevZ = p.prevZ;
		e.lastRenderX = p.lastRenderX;
		e.lastRenderY = p.lastRenderY;
		e.lastRenderZ = p.lastRenderZ;
		e.setYaw(p.getYaw());
		e.prevYaw = p.prevYaw;
		e.setPitch(p.getPitch());
		e.prevPitch = p.prevPitch;
		e.bodyYaw = p.bodyYaw;
		e.prevBodyYaw = p.prevBodyYaw;
		e.headYaw = p.headYaw;
		e.prevHeadYaw = p.prevHeadYaw;
		e.hurtTime = p.hurtTime;
		e.handSwingProgress = p.handSwingProgress;
		e.lastHandSwingProgress = p.lastHandSwingProgress;
		e.handSwinging = p.handSwinging;
		e.setOnGround(p.isOnGround());
		e.setSneaking(p.isSneaking());
		return e;
	}

	/** Uma vez por tick: as patas acompanham o quanto o jogador andou. */
	public static void tick(ClientWorld world) {
		if (CACHE.isEmpty()) return;
		Iterator<Map.Entry<Integer, LivingEntity>> it = CACHE.entrySet().iterator();
		while (it.hasNext()) {
			var en = it.next();
			Entity p = world.getEntityById(en.getKey());
			if (p == null) {
				it.remove();
				continue;
			}
			LivingEntity d = en.getValue();
			// a criatura de fachada roda o próprio tick do cliente: é ele que liga os estados de animação
			// (asas do morcego, abelha, papagaio, criaturas de outros mods...). Depois volta para o jogador.
			d.setPos(p.getX(), p.getY(), p.getZ());
			d.setVelocity(p.getVelocity());
			d.setOnGround(p.isOnGround());
			try {
				d.tick();
			} catch (Throwable ignored) {
			}
			d.setPos(p.getX(), p.getY(), p.getZ());
			d.setVelocity(p.getVelocity());
			if (d.isOnFire()) d.extinguish();
			double dx = p.getX() - p.prevX, dz = p.getZ() - p.prevZ;
			float speed = (float) Math.min(1.0, Math.sqrt(dx * dx + dz * dz) * 4.0);
			d.limbAnimator.updateLimbs(speed, 0.4f);
		}
	}

	public static void forget(int id) {
		CACHE.remove(id);
	}

	public static void clear() {
		CACHE.clear();
	}
}
