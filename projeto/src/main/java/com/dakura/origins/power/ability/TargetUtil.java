package com.dakura.origins.power.ability;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

/** Alvo vivo na mira (com linha de visão). */
final class TargetUtil {
	private TargetUtil() {}

	static LivingEntity aimed(ServerPlayerEntity p, double range, Predicate<Entity> extra) {
		Vec3d eye = p.getEyePos(), look = p.getRotationVec(1f), end = eye.add(look.multiply(range));
		EntityHitResult hit = ProjectileUtil.raycast(p, eye, end, p.getBoundingBox().stretch(look.multiply(range)).expand(1),
				e -> e instanceof LivingEntity l && l.isAlive() && !e.isSpectator() && extra.test(e), range * range);
		if (hit == null || !(hit.getEntity() instanceof LivingEntity t) || !p.canSee(t)) return null;
		return t;
	}

	static boolean boss(Entity e) {
		return e instanceof WitherEntity || e instanceof EnderDragonEntity;
	}
}
