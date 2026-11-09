package com.dakura.origins.client.anim;

import com.dakura.origins.client.screen.SeraphAltarScreen;
import com.dakura.origins.power.EntityFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Efeitos da leva final (Boneca, Kitsune, Serafim, Oni). Mais exagerados que o resto do mod, de propósito:
 * energia amaldiçoada (Jujutsu Kaisen), dança de cortes (Malenia), chamas-fantasma, coro celestial e o
 * pentagrama do Akaza.
 */
final class FinaleEffects {
	// energia amaldiçoada: azul-escuro quase preto, com faíscas vermelhas
	static final DustParticleEffect CURSE = new DustParticleEffect(new Vector3f(0.06f, 0.05f, 0.22f), 1.8f);
	static final DustParticleEffect CURSE_EDGE = new DustParticleEffect(new Vector3f(0.35f, 0.25f, 1.0f), 0.9f);
	static final DustParticleEffect CURSE_RED = new DustParticleEffect(new Vector3f(0.95f, 0.1f, 0.2f), 0.8f);
	static final DustParticleEffect SLASH = new DustParticleEffect(new Vector3f(0.92f, 0.86f, 1.0f), 0.9f);
	static final DustParticleEffect SLASH_PINK = new DustParticleEffect(new Vector3f(1.0f, 0.55f, 0.75f), 0.8f);
	static final DustParticleEffect FOXFIRE = new DustParticleEffect(new Vector3f(0.45f, 0.78f, 1.0f), 1.4f);
	static final DustParticleEffect FOXFIRE_CORE = new DustParticleEffect(new Vector3f(0.9f, 0.97f, 1.0f), 1.0f);
	static final DustParticleEffect HOLY = new DustParticleEffect(new Vector3f(1.0f, 1.0f, 0.96f), 1.3f);
	static final DustParticleEffect HOLY_GOLD = new DustParticleEffect(new Vector3f(1.0f, 0.88f, 0.5f), 0.9f);
	static final DustParticleEffect BLOOD_GLOW = new DustParticleEffect(new Vector3f(1.0f, 0.08f, 0.15f), 1.3f);
	static final DustParticleEffect BLOOD_SOFT = new DustParticleEffect(new Vector3f(1.0f, 0.45f, 0.45f), 0.8f);

	private static final Map<Integer, int[]> THREADS = new HashMap<>();  // boneca → {alvo, ticks}
	private static final List<int[]> ALTAR = new ArrayList<>();          // opções do Altar Celestial

	private FinaleEffects() {}

	private static void expandingRing(Vec3d c, double maxR, int ticks, net.minecraft.particle.ParticleEffect fx, int points) {
		ClientEffects.schedule((world, rnd, age) -> {
			ClientEffects.ring(world, c, 0.3 + (maxR - 0.3) * (age + 1) / (double) ticks, points, fx);
			return age + 1 < ticks;
		});
	}

	/** Chama amaldiçoada subindo de um ponto. */
	private static void curseFlame(ClientWorld w, Vec3d p, Random rnd, int n, double spread) {
		for (int i = 0; i < n; i++) {
			w.addParticle(i % 4 == 0 ? CURSE_EDGE : CURSE, p.x + rnd.nextGaussian() * spread, p.y + rnd.nextDouble() * spread * 2,
					p.z + rnd.nextGaussian() * spread, 0, 0, 0);
		}
		if (rnd.nextInt(2) == 0) w.addParticle(ParticleTypes.SOUL_FIRE_FLAME, p.x, p.y, p.z, rnd.nextGaussian() * 0.02, 0.05, rnd.nextGaussian() * 0.02);
		if (rnd.nextInt(3) == 0) w.addParticle(CURSE_RED, p.x + rnd.nextGaussian() * spread, p.y + rnd.nextDouble() * spread, p.z + rnd.nextGaussian() * spread, 0, 0, 0);
	}

	/** Arco de corte (crescente) no plano inclinado. */
	static void crescent(ClientWorld w, Vec3d c, float yawDeg, float tiltDeg, double r, Random rnd) {
		double yaw = Math.toRadians(yawDeg), tilt = Math.toRadians(tiltDeg);
		Vec3d fwd = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw)), side = new Vec3d(Math.cos(yaw), 0, Math.sin(yaw));
		Vec3d up = new Vec3d(0, 1, 0);
		Vec3d axis2 = side.multiply(Math.cos(tilt)).add(up.multiply(Math.sin(tilt)));
		int n = 26;
		for (int i = 0; i <= n; i++) {
			double a = Math.toRadians(-80 + 160.0 * i / n);
			double rr = r * (0.75 + 0.25 * Math.sin(Math.PI * i / n));
			Vec3d q = c.add(fwd.multiply(Math.cos(a) * rr)).add(axis2.multiply(Math.sin(a) * rr));
			w.addParticle(i % 5 == 0 ? CURSE_EDGE : (i % 2 == 0 ? SLASH : SLASH_PINK), q.x, q.y, q.z, 0, 0, 0);
			if (i % 6 == 0) w.addParticle(ParticleTypes.SWEEP_ATTACK, q.x, q.y, q.z, 0, 0, 0);
		}
		if (rnd.nextInt(2) == 0) w.addParticle(ParticleTypes.CHERRY_LEAVES, c.x + rnd.nextGaussian() * r * 0.4, c.y, c.z + rnd.nextGaussian() * r * 0.4, 0, 0, 0);
	}

	/** Pentagrama vermelho do Akaza: círculo duplo + estrela, girando devagar. */
	private static void pentagram(ClientWorld w, Vec3d c, double r, double rot, boolean dense) {
		int ring = dense ? 48 : 30;
		for (int i = 0; i < ring; i++) {
			double a = rot + i * Math.PI * 2 / ring;
			w.addParticle(BLOOD_GLOW, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 0, 0, 0);
			if (i % 2 == 0) w.addParticle(BLOOD_SOFT, c.x + Math.cos(-a) * r * 0.82, c.y, c.z + Math.sin(-a) * r * 0.82, 0, 0, 0);
		}
		Vec3d[] pts = new Vec3d[5];
		for (int i = 0; i < 5; i++) {
			double a = rot - Math.PI / 2 + i * Math.PI * 2 / 5;
			pts[i] = c.add(Math.cos(a) * r * 0.82, 0, Math.sin(a) * r * 0.82);
		}
		int seg = dense ? 14 : 9;
		for (int i = 0; i < 5; i++) {
			Vec3d a = pts[i], b = pts[(i + 2) % 5];
			for (int k = 0; k <= seg; k++) {
				Vec3d q = a.lerp(b, k / (double) seg);
				w.addParticle(BLOOD_GLOW, q.x, q.y, q.z, 0, 0, 0);
			}
			// agulhas da bússola: pequenos raios nas pontas
			Vec3d tip = c.add(pts[i].subtract(c).multiply(1.35));
			w.addParticle(BLOOD_SOFT, tip.x, tip.y, tip.z, 0, 0, 0);
		}
	}

	static void fx(ClientWorld world, String name, Entity e, int param, Random rnd) {
		Vec3d base = e.getPos();
		switch (name) {
			case "cursed_tick" -> curseFlame(world, base.add(0, e.getHeight() * 0.6, 0).add(e.getRotationVec(1f).multiply(0.6)), rnd, 2 + param, 0.12);
			case "cursed_burst" -> {
				Vec3d c = base.add(0, e.getHeight() * 0.55, 0);
				expandingRing(c, 2.2, 5, CURSE_EDGE, 26);
				for (int i = 0; i < 30; i++) world.addParticle(i % 3 == 0 ? CURSE_RED : CURSE, c.x, c.y, c.z,
						rnd.nextGaussian() * 0.35, rnd.nextGaussian() * 0.35, rnd.nextGaussian() * 0.35);
				world.addParticle(ParticleTypes.FLASH, c.x, c.y, c.z, 0, 0, 0);
				for (int i = 0; i < 8; i++) world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y, c.z,
						rnd.nextGaussian() * 0.15, rnd.nextDouble() * 0.2, rnd.nextGaussian() * 0.15);
			}
			case "threads" -> THREADS.put(e.getId() * 31 + param, new int[]{e.getId(), param, 16});
			case "dance_dash" -> {
				for (int i = 0; i < 16; i++) world.addParticle(i % 2 == 0 ? CURSE : SLASH, base.x + rnd.nextGaussian() * 0.3,
						base.y + rnd.nextDouble() * 1.8, base.z + rnd.nextGaussian() * 0.3, 0, 0, 0);
				expandingRing(base.add(0, 0.1, 0), 2.5, 5, SLASH_PINK, 24);
			}
			case "dance_end" -> {
				expandingRing(base.add(0, 1.0, 0), 5.0, 8, SLASH, 40);
				expandingRing(base.add(0, 0.2, 0), 4.0, 6, CURSE_EDGE, 30);
				for (int i = 0; i < 20; i++) world.addParticle(ParticleTypes.CHERRY_LEAVES, base.x + rnd.nextGaussian() * 2,
						base.y + 1 + rnd.nextDouble() * 2, base.z + rnd.nextGaussian() * 2, 0, 0, 0);
			}
			case "foxfire_summon" -> {
				for (int i = 0; i < 9; i++) {
					double a = i * Math.PI * 2 / 9;
					Vec3d q = base.add(Math.cos(a) * 1.4, 1.2, Math.sin(a) * 1.4);
					for (int k = 0; k < 4; k++) world.addParticle(FOXFIRE, q.x, q.y, q.z, rnd.nextGaussian() * 0.03, 0.03, rnd.nextGaussian() * 0.03);
					world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, q.x, q.y, q.z, 0, 0.02, 0);
				}
				expandingRing(base.add(0, 0.1, 0), 3.0, 6, FOXFIRE, 30);
			}
			case "nine_tails" -> {
				double r = param / 10.0;
				expandingRing(base.add(0, 0.15, 0), r, 10, FOXFIRE, 48);
				expandingRing(base.add(0, 1.2, 0), r * 0.7, 8, FOXFIRE_CORE, 36);
				ClientEffects.spiral(world, base, 1.5, 5.0, 9, 54, FOXFIRE, ParticleTypes.SOUL_FIRE_FLAME, rnd);
				// o espírito da raposa: duas orelhas e um focinho de luz no alto, por um instante
				Vec3d head = base.add(0, 4.2, 0);
				for (int i = 0; i < 40; i++) {
					double a = rnd.nextDouble() * Math.PI * 2, rr = rnd.nextDouble() * 1.2;
					world.addParticle(FOXFIRE_CORE, head.x + Math.cos(a) * rr, head.y + Math.sin(a) * rr * 0.7, head.z, 0, 0, 0);
				}
				for (int s = -1; s <= 1; s += 2) for (int i = 0; i < 10; i++) {
					world.addParticle(FOXFIRE, head.x + s * (0.6 + i * 0.04), head.y + 0.8 + i * 0.1, head.z, 0, 0, 0);
				}
				world.addParticle(ParticleTypes.FLASH, head.x, head.y, head.z, 0, 0, 0);
			}
			case "holy_pillar" -> {
				for (int i = 0; i < 30; i++) {
					double y = rnd.nextDouble() * 6;
					world.addParticle(i % 3 == 0 ? HOLY_GOLD : HOLY, base.x + rnd.nextGaussian() * 0.25, base.y + y, base.z + rnd.nextGaussian() * 0.25, 0, -0.05, 0);
				}
				ClientEffects.ring(world, base.add(0, 0.1, 0), 0.8, 16, HOLY_GOLD);
			}
			case "seraph_choir" -> {
				double r = param / 10.0;
				expandingRing(base.add(0, 0.1, 0), r, 10, HOLY, 50);
				expandingRing(base.add(0, 2.4, 0), r * 0.5, 8, HOLY_GOLD, 30);
				for (int i = 0; i < 16; i++) world.addParticle(ParticleTypes.END_ROD, base.x + rnd.nextGaussian() * 1.5,
						base.y + 2 + rnd.nextDouble() * 3, base.z + rnd.nextGaussian() * 1.5, 0, -0.04, 0);
			}
			case "altar_opt" -> ALTAR.add(new int[]{param >> 4, param & 15});
			case "altar_open" -> {
				if (e == MinecraftClient.getInstance().player && !ALTAR.isEmpty()) {
					List<int[]> opts = new ArrayList<>(ALTAR);
					ALTAR.clear();
					MinecraftClient.getInstance().setScreen(new SeraphAltarScreen(opts));
				}
				ALTAR.clear();
			}
			case "altar_ritual" -> {
				ClientEffects.spiral(world, base, 1.0, 3.0, 2, 30, HOLY_GOLD, ParticleTypes.ENCHANT, rnd);
				for (int i = 0; i < 20; i++) world.addParticle(ParticleTypes.ENCHANT, base.x + rnd.nextGaussian() * 1.5,
						base.y + 2.5, base.z + rnd.nextGaussian() * 1.5, 0, -0.6, 0);
			}
			case "altar_done" -> {
				Vec3d hand = e.getEyePos().add(e.getRotationVec(1f).multiply(0.7)).add(0, -0.4, 0);
				for (int i = 0; i < 24; i++) world.addParticle(i % 2 == 0 ? HOLY_GOLD : HOLY, hand.x, hand.y, hand.z,
						rnd.nextGaussian() * 0.15, rnd.nextGaussian() * 0.15, rnd.nextGaussian() * 0.15);
				world.addParticle(ParticleTypes.FLASH, hand.x, hand.y, hand.z, 0, 0, 0);
			}
			case "compass" -> {
				Vec3d c = base.add(0, 0.06, 0);
				pentagram(world, c, 3.6, 0, true);
				expandingRing(c, 6.0, 7, BLOOD_GLOW, 50);
				expandingRing(c.add(0, 0.8, 0), 4.0, 6, ParticleTypes.CLOUD, 30);
				world.addParticle(ParticleTypes.FLASH, c.x, c.y + 1, c.z, 0, 0, 0);
				for (int i = 0; i < 24; i++) world.addParticle(ParticleTypes.CRIT, c.x, c.y + 0.2, c.z,
						rnd.nextGaussian() * 0.6, 0.3 + rnd.nextDouble() * 0.3, rnd.nextGaussian() * 0.6);
			}
			case "dodge" -> {
				// imagens residuais: silhueta vermelha que fica para trás
				for (int i = 0; i < 18; i++) world.addParticle(i % 2 == 0 ? BLOOD_SOFT : BLOOD_GLOW, base.x + rnd.nextGaussian() * 0.15,
						base.y + rnd.nextDouble() * e.getHeight(), base.z + rnd.nextGaussian() * 0.15, 0, 0, 0);
				for (int i = 0; i < 4; i++) world.addParticle(ParticleTypes.CLOUD, base.x, base.y + 0.1, base.z,
						rnd.nextGaussian() * 0.05, 0.01, rnd.nextGaussian() * 0.05);
			}
			default -> LegendEffects.fx(world, name, e, param, rnd);
		}
	}

	static void fxAt(ClientWorld world, String name, Vec3d p, float a, float b, float c, Random rnd) {
		switch (name) {
			case "dance_slash" -> {
				crescent(world, p, a, b, c, rnd);
				crescent(world, p.add(0, 0.3, 0), a + 120 + rnd.nextFloat() * 60, -b * 0.6f, c * 0.75, rnd);
			}
			case "foxfire_shot" -> {
				Vec3d to = new Vec3d(a, b, c);
				int n = (int) Math.min(30, p.distanceTo(to) * 3);
				for (int i = 0; i <= n; i++) {
					double k = i / (double) n;
					Vec3d q = p.lerp(to, k).add(0, Math.sin(k * Math.PI) * 0.8, 0);
					world.addParticle(i % 3 == 0 ? FOXFIRE_CORE : FOXFIRE, q.x, q.y, q.z, 0, 0, 0);
				}
				for (int i = 0; i < 10; i++) world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, to.x, to.y, to.z,
						rnd.nextGaussian() * 0.1, rnd.nextDouble() * 0.1, rnd.nextGaussian() * 0.1);
			}
			default -> LegendEffects.fxAt(world, name, p, a, b, c, rnd);
		}
	}

	static void tick(MinecraftClient mc, ClientWorld world, Vec3d cam, Random rnd) {
		LegendEffects.tick(mc, world, cam, rnd);
		long t = world.getTime();
		for (PlayerEntity pl : world.getPlayers()) {
			if (pl.squaredDistanceTo(cam) > 48 * 48) continue;
			Set<String> flags = EntityFlags.CLIENT.get(pl.getId());
			if (flags == null || flags.isEmpty()) continue;
			// chamas-fantasma orbitando a Kitsune
			for (String f : flags) {
				if (!f.startsWith("foxfire:")) continue;
				int n;
				try {
					n = Integer.parseInt(f.substring(8));
				} catch (NumberFormatException ex) {
					continue;
				}
				for (int i = 0; i < n; i++) {
					double ang = t * 0.12 + i * Math.PI * 2 / 9;
					double y = pl.getY() + 1.1 + Math.sin(t * 0.2 + i) * 0.25;
					double x = pl.getX() + Math.cos(ang) * 1.35, z = pl.getZ() + Math.sin(ang) * 1.35;
					world.addParticle(FOXFIRE, x, y, z, 0, 0, 0);
					if ((t + i) % 3 == 0) world.addParticle(FOXFIRE_CORE, x, y + 0.05, z, 0, 0.01, 0);
					if ((t + i) % 9 == 0) world.addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0, 0.01, 0);
				}
			}
			// dança da Boneca: aura amaldiçoada envolvendo o corpo
			if (flags.contains("dancing")) {
				curseFlame(world, pl.getPos().add(0, 0.2, 0), rnd, 3, 0.35);
				if (t % 2 == 0) world.addParticle(SLASH_PINK, pl.getX() + rnd.nextGaussian() * 0.6, pl.getY() + rnd.nextDouble() * 2,
						pl.getZ() + rnd.nextGaussian() * 0.6, 0, 0, 0);
			}
			// Boneca parada: fio amaldiçoado leve nas mãos, de vez em quando
			if (flags.contains("doll_key") && rnd.nextInt(30) == 0) curseFlame(world, pl.getPos().add(0, 0.9, 0), rnd, 1, 0.3);
			// Oni em sede de sangue: pentagrama segue os pés, olhos brilham e o vapor sobe
			if (flags.contains("bloodlust")) {
				if (t % 3 == 0) pentagram(world, pl.getPos().add(0, 0.06, 0), 3.0, t * 0.03, false);
				Vec3d eye = pl.getEyePos().add(pl.getRotationVec(1f).multiply(0.3));
				world.addParticle(BLOOD_GLOW, eye.x, eye.y, eye.z, 0, 0, 0);
				if (rnd.nextInt(3) == 0) world.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pl.getX() + rnd.nextGaussian() * 0.3,
						pl.getY() + 1.6, pl.getZ() + rnd.nextGaussian() * 0.3, 0, 0.02, 0);
			}
			// Serafim: penas de luz caindo devagar
			if (flags.contains("seraph") && rnd.nextInt(pl == mc.player ? 8 : 4) == 0) {
				world.addParticle(HOLY, pl.getX() + rnd.nextGaussian() * 0.7, pl.getY() + 2.2, pl.getZ() + rnd.nextGaussian() * 0.7, 0, -0.03, 0);
			}
		}
		// Fios da Marionete: linhas tremendo da mão da Boneca até cada alvo
		for (Iterator<int[]> it = THREADS.values().iterator(); it.hasNext(); ) {
			int[] th = it.next();
			Entity c = world.getEntityById(th[0]), tg = world.getEntityById(th[1]);
			if (c == null || tg == null || --th[2] <= 0) {
				it.remove();
				continue;
			}
			Vec3d from = c.getEyePos().add(c.getRotationVec(1f).multiply(0.5)).add(0, 0.25, 0), to = tg.getPos().add(0, tg.getHeight() * 0.6, 0);
			int n = (int) Math.min(30, from.distanceTo(to) * 3);
			for (int i = 0; i <= n; i++) {
				double k = i / (double) n;
				Vec3d q = from.lerp(to, k).add(0, Math.sin(k * Math.PI * 4 + t) * 0.05, 0);
				world.addParticle(i % 4 == 0 ? CURSE_RED : CURSE_EDGE, q.x, q.y, q.z, 0, 0, 0);
			}
			curseFlame(world, to, rnd, 2, 0.25);
		}
	}
}
