package com.dakura.origins.client.fx;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

/**
 * Partículas nativas do Minecraft que encorpam os efeitos próprios do mod (não os substituem): explosões de mana,
 * veneno, natureza. Quantidade equilibrada para não pesar.
 */
public final class NativeFx {
	static final DustColorTransitionParticleEffect MANA = new DustColorTransitionParticleEffect(
			new Vector3f(0.55f, 0.85f, 1f), new Vector3f(0.75f, 0.5f, 1f), 1.4f);
	static final DustParticleEffect MANA_WHITE = new DustParticleEffect(new Vector3f(0.92f, 0.97f, 1f), 1.1f);
	static final DustColorTransitionParticleEffect POISON = new DustColorTransitionParticleEffect(
			new Vector3f(0.75f, 0.3f, 1f), new Vector3f(0.35f, 0.9f, 0.3f), 1.5f);
	static final DustParticleEffect NATURE = new DustParticleEffect(new Vector3f(0.4f, 1f, 0.45f), 1.4f);

	private NativeFx() {}

	/** Explosão de mana: clarão, anel de faíscas, estrelas, brilho e flocos gelados (congelamento). */
	public static void manaBurst(ClientWorld w, Vec3d c, float size, Random r) {
		float s = Math.max(0.5f, size) * 1.8f;
		w.addParticle(ParticleTypes.FLASH, c.x, c.y, c.z, 0, 0, 0);
		w.addParticle(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 0, 0, 0);
		int n = (int) (36 * s);
		for (int i = 0; i < n; i++) {
			double th = r.nextDouble() * Math.PI * 2, ph = r.nextDouble() * Math.PI - Math.PI / 2, sp = (0.15 + r.nextDouble() * 0.35) * s;
			double vx = Math.cos(th) * Math.cos(ph) * sp, vy = Math.sin(ph) * sp, vz = Math.sin(th) * Math.cos(ph) * sp;
			ParticleEffect fx = switch (i % 6) {
				case 0 -> ParticleTypes.END_ROD;
				case 1 -> ParticleTypes.SNOWFLAKE;
				case 2 -> ParticleTypes.GLOW;
				case 3 -> ParticleTypes.ELECTRIC_SPARK;
				case 4 -> MANA_WHITE;
				default -> MANA;
			};
			w.addParticle(fx, c.x, c.y, c.z, vx, vy, vz);
		}
		ring(w, c, 1.4 * s, (int) (28 * s), ParticleTypes.END_ROD, 0.12);
		for (int i = 0; i < 8 * s; i++) w.addParticle(ParticleTypes.FIREWORK, c.x, c.y, c.z, r.nextGaussian() * 0.25, r.nextDouble() * 0.3, r.nextGaussian() * 0.25);
	}

	/** Explosão sombria (Kitsune Corrompida): fumaça negra, almas e faíscas brancas. */
	public static void darkBurst(ClientWorld w, Vec3d c, float size, Random r) {
		float s = Math.max(0.5f, size) * 1.8f;
		w.addParticle(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 0, 0, 0);
		for (int i = 0; i < 34 * s; i++) {
			double vx = r.nextGaussian() * 0.25 * s, vy = r.nextGaussian() * 0.25 * s, vz = r.nextGaussian() * 0.25 * s;
			ParticleEffect fx = switch (i % 5) {
				case 0 -> ParticleTypes.SCULK_SOUL;
				case 1 -> ParticleTypes.SQUID_INK;
				case 2 -> ParticleTypes.END_ROD;
				case 3 -> ParticleTypes.SNOWFLAKE;
				default -> ParticleTypes.LARGE_SMOKE;
			};
			w.addParticle(fx, c.x, c.y, c.z, vx, vy, vz);
		}
		ring(w, c, 1.6 * s, (int) (26 * s), ParticleTypes.SOUL_FIRE_FLAME, 0.1);
	}

	/** Veneno: bolhas, esporos, gotas e névoa roxa e verde. */
	public static void poison(ClientWorld w, Vec3d c, float radius, int count, Random r) {
		for (int i = 0; i < count; i++) {
			double a = r.nextDouble() * Math.PI * 2, d = Math.sqrt(r.nextDouble()) * radius;
			double x = c.x + Math.cos(a) * d, z = c.z + Math.sin(a) * d, y = c.y + r.nextDouble() * 0.6;
			ParticleEffect fx = switch (i % 6) {
				case 0 -> ParticleTypes.WITCH;
				case 1 -> ParticleTypes.SPORE_BLOSSOM_AIR;
				case 2 -> ParticleTypes.ITEM_SLIME;
				case 3 -> ParticleTypes.BUBBLE_POP;
				case 4 -> ParticleTypes.MYCELIUM;
				default -> POISON;
			};
			w.addParticle(fx, x, y, z, 0, 0.03 + r.nextDouble() * 0.04, 0);
		}
	}

	private static void ring(ClientWorld w, Vec3d c, double rad, int n, ParticleEffect fx, double sp) {
		for (int i = 0; i < n; i++) {
			double a = i * Math.PI * 2 / n;
			w.addParticle(fx, c.x + Math.cos(a) * rad * 0.3, c.y, c.z + Math.sin(a) * rad * 0.3, Math.cos(a) * sp * rad, 0.01, Math.sin(a) * sp * rad);
		}
	}
}
