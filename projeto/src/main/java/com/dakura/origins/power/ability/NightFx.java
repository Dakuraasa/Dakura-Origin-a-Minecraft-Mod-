package com.dakura.origins.power.ability;

import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/** Paleta dos elfos da noite com partículas nativas: roxo escuro, rosa, azul escuro e preto denso. */
public final class NightFx {
	public static final DustParticleEffect PURPLE = new DustParticleEffect(new Vector3f(0.35f, 0.08f, 0.55f), 1.4f);
	public static final DustParticleEffect PINK = new DustParticleEffect(new Vector3f(0.95f, 0.35f, 0.75f), 1.1f);
	public static final DustParticleEffect BLUE = new DustParticleEffect(new Vector3f(0.10f, 0.15f, 0.55f), 1.3f);
	public static final DustParticleEffect BLACK = new DustParticleEffect(new Vector3f(0.02f, 0.0f, 0.04f), 1.8f);
	private static final DustParticleEffect[] ALL = {PURPLE, PINK, BLUE, BLACK};

	private NightFx() {}

	public static void burst(ServerWorld w, Vec3d c, int n, double spread) {
		for (int i = 0; i < 4; i++) w.spawnParticles(ALL[i], c.x, c.y, c.z, n, spread, spread, spread, 0);
		w.spawnParticles(ParticleTypes.SQUID_INK, c.x, c.y, c.z, n / 2 + 1, spread * 0.6, spread * 0.6, spread * 0.6, 0.02);
		w.spawnParticles(ParticleTypes.REVERSE_PORTAL, c.x, c.y, c.z, n, spread, spread, spread, 0.05);
	}

	/** Feixe contínuo entre dois pontos (núcleo preto denso, espiral roxa/rosa/azul). */
	public static void beam(ServerWorld w, Vec3d a, Vec3d b, float t) {
		Vec3d d = b.subtract(a);
		double len = d.length();
		if (len < 0.1) return;
		Vec3d dir = d.multiply(1 / len);
		Vec3d side = dir.crossProduct(new Vec3d(0, 1, 0));
		if (side.lengthSquared() < 1e-4) side = new Vec3d(1, 0, 0);
		side = side.normalize();
		Vec3d up = side.crossProduct(dir).normalize();
		int steps = (int) (len * 4);
		for (int i = 0; i <= steps; i++) {
			double u = i / (double) steps;
			Vec3d p = a.add(d.multiply(u));
			w.spawnParticles(BLACK, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
			double ang = t * 0.5 + u * 14;
			for (int k = 0; k < 3; k++) {
				double aa = ang + k * Math.PI * 2 / 3;
				Vec3d o = side.multiply(Math.cos(aa) * 0.22).add(up.multiply(Math.sin(aa) * 0.22));
				w.spawnParticles(k == 0 ? PURPLE : k == 1 ? PINK : BLUE, p.x + o.x, p.y + o.y, p.z + o.z, 1, 0, 0, 0, 0);
			}
			if (i % 3 == 0) w.spawnParticles(ParticleTypes.SQUID_INK, p.x, p.y, p.z, 1, 0.05, 0.05, 0.05, 0.0);
		}
		w.spawnParticles(ParticleTypes.REVERSE_PORTAL, b.x, b.y, b.z, 6, 0.3, 0.4, 0.3, 0.05);
	}
}
