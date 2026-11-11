package com.dakura.origins.power.ability;

import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.sound.ModSounds;
import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:flower_sanctuary" — Santuário de Flores (Fada): abre um grande círculo mágico no chão. Nenhum
 * monstro consegue entrar (quem tenta é empurrado para fora e perde o alvo lá dentro), e um campo de flores
 * encantadas cura sem parar os jogadores e aliados que estiverem dentro.
 * { "radius": 7, "duration": 300, "heal": 1.0, "interval": 20 }
 */
public final class FlowerSanctuaryAbility implements Ability {
	private final float radius, heal;
	private final int duration, interval;

	public FlowerSanctuaryAbility(JsonObject json) {
		this.radius = JsonParams.num(json, "radius", 7f);
		this.duration = JsonParams.integer(json, "duration", 300);
		this.heal = JsonParams.num(json, "heal", 1f);
		this.interval = JsonParams.integer(json, "interval", 20);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		ServerWorld w = p.getServerWorld();
		Vec3d c = ground(w, p.getPos());
		LegendKit.fxAt(w, "flower_circle", c, radius, duration, 0);
		LegendKit.anim(p, "cast", 12);
		ModSounds.play(w, c, ModSounds.FLOWER_BLOOM, 1.2f, 1f);
		LegendKit.run(p, "sanctuary", duration, (pl, age) -> {
			double r2 = radius * radius;
			for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(c, c).expand(radius + 3, 4, radius + 3), LivingEntity::isAlive)) {
				double dx = e.getX() - c.x, dz = e.getZ() - c.z, d2 = dx * dx + dz * dz;
				if (e instanceof Monster) {
					// barreira: monstro que encosta no círculo é empurrado para fora
					if (d2 < (radius + 0.6) * (radius + 0.6)) {
						Vec3d out = LegendKit.flat(new Vec3d(dx, 0, dz));
						double push = d2 < r2 ? 0.55 : 0.3;
						e.setVelocity(out.x * push, Math.max(e.getVelocity().y, 0.08), out.z * push);
						e.velocityModified = true;
					}
					if (e instanceof MobEntity mob && mob.getTarget() != null) {
						LivingEntity t = mob.getTarget();
						double tx = t.getX() - c.x, tz = t.getZ() - c.z;
						if (tx * tx + tz * tz < r2) {
							mob.setTarget(null);
							mob.getNavigation().stop();
						}
					}
				} else if (d2 < r2 && age % interval == 0 && (e instanceof PlayerEntity || e instanceof TameableEntity tm && tm.isTamed())) {
					if (e.getHealth() < e.getMaxHealth()) {
						e.heal(heal);
						LegendKit.fx(e, "bloom_heal", 0);
					}
				}
			}
			return true;
		});
		return true;
	}

	private static Vec3d ground(ServerWorld w, Vec3d at) {
		var bp = net.minecraft.util.math.BlockPos.ofFloored(at);
		for (int i = 0; i < 6 && w.getBlockState(bp.down()).getCollisionShape(w, bp.down()).isEmpty(); i++) bp = bp.down();
		return new Vec3d(at.x, bp.getY(), at.z);
	}
}
