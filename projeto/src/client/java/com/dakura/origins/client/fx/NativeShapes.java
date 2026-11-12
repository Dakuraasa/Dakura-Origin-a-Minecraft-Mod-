package com.dakura.origins.client.fx;

import com.dakura.origins.power.EntityFlags;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.component.type.FireworkExplosionComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

import java.util.List;

/**
 * Ability visuals built from vanilla particles: area zones, bursts, trails, shields and auras.
 * Repulsion and shield impacts use firework bursts.
 */
public final class NativeShapes {
	private static final DustParticleEffect PURPLE = dust(0x9A4CFF), GREEN = dust(0x55E06A), GOLD = dust(0xFFD24A),
			BLUE = dust(0x6FB8FF), VIOLET = dust(0xC07CFF), PINK = dust(0xFF9EC8), EARTH = dust(0x8C7356), WHITE = dust(0xF4F4FF);

	private NativeShapes() {}

	private static DustParticleEffect dust(int rgb) {
		return new DustParticleEffect(new Vector3f((rgb >> 16 & 255) / 255f, (rgb >> 8 & 255) / 255f, (rgb & 255) / 255f), 1.2f);
	}

	static void emit(ClientWorld w, String kind, Vec3d p, float radius, int age, int life, float dx, float dz) {
		Random r = w.random;
		switch (kind) {
			case "ZONE_POISON" -> zone(w, p, radius, age, PURPLE, ParticleTypes.WITCH, r);
			case "ZONE_FIRE" -> zone(w, p, radius, age, GREEN, ParticleTypes.SOUL_FIRE_FLAME, r);
			case "FIRE_FIELD" -> zone(w, p, radius, age, ParticleTypes.FLAME, ParticleTypes.SMOKE, r);
			case "FLOWER_CIRCLE" -> zone(w, p, radius, age, PINK, ParticleTypes.CHERRY_LEAVES, r);
			case "SPORES" -> {
				for (int i = 0; i < 3; i++) w.addParticle(ParticleTypes.SPORE_BLOSSOM_AIR, p.x + r.nextGaussian() * radius * 0.5,
						p.y + 0.3 + r.nextDouble() * 1.5, p.z + r.nextGaussian() * radius * 0.5, 0, 0.01, 0);
			}
			case "BOOM_GREEN" -> boom(w, p, radius, age, GREEN, 0x55E06A, r);
			case "BOOM_POISON" -> boom(w, p, radius, age, PURPLE, 0x9A4CFF, r);
			case "BOOM_GOLD" -> boom(w, p, radius, age, GOLD, 0xFFD24A, r);
			case "BOOM_ARCANE", "BOOM_MANA" -> boom(w, p, radius, age, BLUE, 0x6FB8FF, r);
			case "BOOM_VIOLET" -> boom(w, p, radius, age, VIOLET, 0xC07CFF, r);
			case "BREAK" -> boom(w, p, radius, age, WHITE, 0xF4F4FF, r);
			case "SHIELD_FLASH", "WARD_BURST" -> {
				if (age == 1) firework(w, p.add(0, 1, 0), 0xFFE27A, 0xFFFFFF, true);
			}
			case "CRATER", "DUST" -> {
				if (age <= 6) ring(w, p.add(0, 0.1, 0), radius * age / 6f, 18, age % 2 == 0 ? ParticleTypes.POOF : EARTH, r);
			}
			case "CHARGE" -> {
				for (int i = 0; i < 3; i++) {
					Vec3d from = p.add(r.nextGaussian() * radius, 0.5 + r.nextDouble() * 1.5, r.nextGaussian() * radius);
					Vec3d v = p.add(0, 1, 0).subtract(from).multiply(0.08);
					w.addParticle(GREEN, from.x, from.y, from.z, v.x, v.y, v.z);
				}
			}
			case "DIVE", "GOLD_TRAIL" -> {
				ParticleEffect fx = kind.equals("DIVE") ? ParticleTypes.FLAME : GOLD;
				for (int i = 0; i < 2; i++) w.addParticle(fx, p.x + r.nextGaussian() * 0.3, p.y + 0.5 + r.nextDouble(), p.z + r.nextGaussian() * 0.3, 0, 0.02, 0);
			}
			case "PUNCH", "PUNCH_FINAL" -> {
				if (age != 1) return;
				Vec3d c = p.add(dx, 1.1, dz);
				w.addParticle(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 0, 0, 0);
				for (int i = 0; i < 8; i++) w.addParticle(ParticleTypes.CRIT, c.x, c.y, c.z, r.nextGaussian() * 0.3, r.nextGaussian() * 0.2, r.nextGaussian() * 0.3);
				if (kind.equals("PUNCH_FINAL")) w.addParticle(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 0, 0, 0);
			}
			case "BLOOM" -> {
				if (age % 2 == 0) w.addParticle(ParticleTypes.CHERRY_LEAVES, p.x + r.nextGaussian() * radius, p.y + 0.4 + r.nextDouble() * 1.4,
						p.z + r.nextGaussian() * radius, 0, 0.02, 0);
				if (age % 4 == 0) w.addParticle(ParticleTypes.HAPPY_VILLAGER, p.x + r.nextGaussian() * radius, p.y + 1, p.z + r.nextGaussian() * radius, 0, 0, 0);
			}
			default -> {
			}
		}
	}

	/** Continuous auras of players with an active state (shields, wards, orbiting orbs, blaze). */
	static void flagged(ClientWorld w, PlayerEntity pl) {
		long t = w.getTime();
		Random r = w.random;
		if (EntityFlags.clientHas(pl, "mirror_shield")) {
			Vec3d look = pl.getRotationVec(1f);
			Vec3d c = pl.getEyePos().add(look.multiply(1.4)).add(0, -0.35, 0);
			Vec3d side = new Vec3d(-look.z, 0, look.x).normalize(), up = side.crossProduct(look).normalize();
			double rad = 1.3 * pl.getScale();
			for (int i = 0; i < 10; i++) {
				double a = t * 0.15 + i * Math.PI * 2 / 10;
				Vec3d q = c.add(side.multiply(Math.cos(a) * rad)).add(up.multiply(Math.sin(a) * rad));
				w.addParticle(i % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.WAX_ON, q.x, q.y, q.z, 0, 0, 0);
			}
			if (t % 3 == 0) w.addParticle(ParticleTypes.ENCHANT, c.x + r.nextGaussian() * 0.4, c.y + r.nextGaussian() * 0.4, c.z + r.nextGaussian() * 0.4, 0, 0, 0);
		}
		if (EntityFlags.clientHas(pl, "gold_ward") && t % 2 == 0) {
			double a = t * 0.2;
			for (int i = 0; i < 3; i++) {
				double b = a + i * Math.PI * 2 / 3;
				w.addParticle(GOLD, pl.getX() + Math.cos(b) * 0.9, pl.getY() + 0.2 + (t % 30) / 15.0, pl.getZ() + Math.sin(b) * 0.9, 0, 0, 0);
			}
		}
		if (EntityFlags.clientHas(pl, "mana_orbs")) {
			for (int i = 0; i < 3; i++) {
				double a = t * 0.12 + i * Math.PI * 2 / 3;
				w.addParticle(BLUE, pl.getX() + Math.cos(a) * 1.1, pl.getY() + 1.2 + Math.sin(t * 0.1 + i) * 0.2, pl.getZ() + Math.sin(a) * 1.1, 0, 0, 0);
			}
		}
		if (EntityFlags.clientHas(pl, "solar_dive")) {
			for (int i = 0; i < 4; i++) w.addParticle(i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.LAVA, pl.getX() + r.nextGaussian() * 0.5,
					pl.getY() + r.nextDouble() * 1.6, pl.getZ() + r.nextGaussian() * 0.5, 0, 0, 0);
		}
	}

	/** Vanilla firework burst (client-only particles, no damage). */
	public static void firework(ClientWorld w, Vec3d at, int color, int fade, boolean ball) {
		w.addFireworkParticle(at.x, at.y, at.z, 0, 0, 0, List.of(new FireworkExplosionComponent(
				ball ? FireworkExplosionComponent.Type.SMALL_BALL : FireworkExplosionComponent.Type.BURST,
				IntList.of(color), IntList.of(fade), false, false)));
	}

	private static void zone(ClientWorld w, Vec3d p, float radius, int age, ParticleEffect edge, ParticleEffect inner, Random r) {
		if (age % 3 != 0) return;
		ring(w, p.add(0, 0.12, 0), radius, Math.max(10, (int) (radius * 6)), edge, r);
		for (int i = 0; i < Math.max(2, (int) radius); i++) {
			double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * radius;
			w.addParticle(inner, p.x + Math.cos(a) * d, p.y + 0.15, p.z + Math.sin(a) * d, 0, 0.03, 0);
		}
	}

	private static void boom(ClientWorld w, Vec3d p, float radius, int age, ParticleEffect color, int rgb, Random r) {
		if (age == 1) {
			w.addParticle(radius > 2 ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, p.x, p.y + 0.5, p.z, 0, 0, 0);
			for (int i = 0; i < 24; i++) w.addParticle(color, p.x, p.y + 0.5, p.z, r.nextGaussian() * 0.3 * radius, r.nextGaussian() * 0.2 * radius, r.nextGaussian() * 0.3 * radius);
		}
		if (age <= 4) ring(w, p.add(0, 0.2, 0), radius * age / 4f, 16, color, r);
	}

	private static void ring(ClientWorld w, Vec3d c, double rad, int n, ParticleEffect fx, Random r) {
		for (int i = 0; i < n; i++) {
			double a = i * Math.PI * 2 / n + r.nextDouble() * 0.1;
			w.addParticle(fx, c.x + Math.cos(a) * rad, c.y, c.z + Math.sin(a) * rad, 0, 0.01, 0);
		}
	}

}
