package com.dakura.origins.power.ability;

import com.dakura.origins.sound.ModSounds;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/**
 * Fogo dos dragões, compartilhado: dano de explosão + dano de fogo real (queima de verdade, então a carne dos
 * animais cai assada) + área de fogo verde que continua queimando. Quem é imune a fogo só leva o impacto.
 */
public final class DragonFire {
	private DragonFire() {}

	/**
	 * Fogo de verdade num alvo: dano de fogo + o estado de queimadura do Minecraft (então quem morre queimando
	 * deixa a carne assada). Quem é imune a fogo não sofre nada disso.
	 */
	public static void burn(ServerWorld w, LivingEntity t, Entity attacker, float fireDamage, int seconds) {
		if (t.isFireImmune() || !t.isAlive()) return;
		t.setOnFireFor(seconds);
		if (fireDamage > 0) {
			t.timeUntilRegen = 0;
			t.damage(AreaZones.fire(w, attacker), fireDamage);
		}
	}

	/** Queima todos os seres vivos num raio (menos quem atacou). */
	public static void burnArea(ServerWorld w, Vec3d c, double radius, Entity attacker, float fireDamage, int seconds) {
		for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(c, c).expand(radius), LivingEntity::isAlive)) {
			if (e == attacker || e.getPos().distanceTo(c) > radius + e.getWidth()) continue;
			burn(w, e, attacker, fireDamage, seconds);
		}
	}

	/**
	 * Incêndio de verdade nos blocos em volta (fogo vanilla que se apaga sozinho), limitado a count focos e
	 * desligado se mobGriefing = false.
	 */
	public static void igniteGround(ServerWorld w, Vec3d c, int radius, int count) {
		if (!w.getGameRules().getBoolean(net.minecraft.world.GameRules.DO_MOB_GRIEFING)) return;
		net.minecraft.util.math.BlockPos center = net.minecraft.util.math.BlockPos.ofFloored(c);
		int placed = 0;
		for (int a = 0; a < count * 3 && placed < count; a++) {
			int dx = w.random.nextInt(radius * 2 + 1) - radius, dz = w.random.nextInt(radius * 2 + 1) - radius;
			if (dx * dx + dz * dz > radius * radius) continue;
			for (int dy = 2; dy >= -3; dy--) {
				net.minecraft.util.math.BlockPos pos = center.add(dx, dy, dz);
				if (w.getBlockState(pos).isAir() && net.minecraft.block.AbstractFireBlock.canPlaceAt(w, pos, net.minecraft.util.math.Direction.UP)) {
					w.setBlockState(pos, net.minecraft.block.AbstractFireBlock.getState(w, pos));
					placed++;
					break;
				}
			}
		}
	}

	/** true: explode sem a esfera 3D (quem chama usa outro efeito de impacto). */
	public static boolean quiet;

	public static void explode(ServerWorld w, Vec3d c, Entity owner, float radius, float blast, float fire, int fireSeconds,
							   int zoneTicks, float zoneRadius) {
		for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, new Box(c, c).expand(radius), LivingEntity::isAlive)) {
			if (e == owner) continue;
			double d = e.getPos().add(0, e.getHeight() / 2, 0).distanceTo(c);
			if (d > radius) continue;
			float k = (float) (1 - d / radius * 0.6);
			e.timeUntilRegen = 0;
			e.damage(w.getDamageSources().explosion(owner, owner), blast * k);
			if (!e.isFireImmune()) {
				e.timeUntilRegen = 0;
				e.damage(AreaZones.fire(w, owner), fire * k);
				e.setOnFireFor(fireSeconds);
			}
			Vec3d push = e.getPos().subtract(c);
			push = push.lengthSquared() < 1e-4 ? new Vec3d(0, 1, 0) : push.normalize();
			e.addVelocity(push.x * 0.9 * k, 0.45 * k + 0.2, push.z * 0.9 * k);
			e.velocityModified = true;
		}
		// fogo de verdade espalhado de forma irregular (sem área 3D desenhada)
		if (zoneTicks > 0) igniteGround(w, c, Math.max(2, Math.round(zoneRadius)), 10 + Math.round(zoneRadius * 3));
		if (!quiet) LegendKit.fxAt(w, "boom_green", c, radius, 0, 0);
		LegendKit.fxAt(w, "debris", c, (int) (radius * 5), radius, 0);
		ModSounds.play(w, c, ModSounds.MAGMA_EXPLODE, 1.4f, 0.9f + w.random.nextFloat() * 0.2f);
	}
}
