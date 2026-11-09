package com.dakura.origins.client.anim;

import com.dakura.origins.power.EntityFlags;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Efeitos da última leva: Sereia (ondas, bolhas e canto), Anúbis (balança de Maat, areias do Duat, chacais),
 * Fênix (cometa solar, asas de fogo, renascimento) e Vastaya Coelho (estrelas de desenho animado, meia-lua,
 * cratera de luar). Exagerados e arredondados de propósito, fora do padrão do Minecraft.
 */
final class LegendEffects {
	// mar
	static final DustParticleEffect AQUA = new DustParticleEffect(new Vector3f(0.30f, 0.80f, 1.00f), 1.5f);
	static final DustParticleEffect FOAM = new DustParticleEffect(new Vector3f(0.94f, 0.99f, 1.00f), 1.3f);
	static final DustParticleEffect DEEP = new DustParticleEffect(new Vector3f(0.10f, 0.36f, 0.78f), 1.8f);
	static final DustParticleEffect PEARL = new DustParticleEffect(new Vector3f(1.00f, 0.82f, 0.92f), 0.9f);
	// deserto e submundo
	static final DustParticleEffect SAND = new DustParticleEffect(new Vector3f(0.93f, 0.78f, 0.45f), 1.4f);
	static final DustParticleEffect GOLD = new DustParticleEffect(new Vector3f(1.00f, 0.82f, 0.22f), 1.1f);
	static final DustParticleEffect LAPIS = new DustParticleEffect(new Vector3f(0.15f, 0.32f, 0.90f), 1.0f);
	static final DustParticleEffect HEART = new DustParticleEffect(new Vector3f(0.85f, 0.06f, 0.14f), 1.4f);
	static final DustParticleEffect SHADOW = new DustParticleEffect(new Vector3f(0.08f, 0.06f, 0.05f), 1.6f);
	// sol
	static final DustParticleEffect SUN = new DustParticleEffect(new Vector3f(1.00f, 0.80f, 0.25f), 1.7f);
	static final DustParticleEffect EMBER = new DustParticleEffect(new Vector3f(1.00f, 0.42f, 0.08f), 1.3f);
	static final DustParticleEffect SUN_CORE = new DustParticleEffect(new Vector3f(1.00f, 0.98f, 0.82f), 1.2f);
	static final DustParticleEffect CRIMSON = new DustParticleEffect(new Vector3f(0.90f, 0.16f, 0.06f), 1.5f);
	// lua
	static final DustParticleEffect MOON = new DustParticleEffect(new Vector3f(0.93f, 0.96f, 1.00f), 1.6f);
	static final DustParticleEffect MOON_BLUE = new DustParticleEffect(new Vector3f(0.58f, 0.74f, 1.00f), 1.2f);
	static final DustParticleEffect STAR = new DustParticleEffect(new Vector3f(1.00f, 0.95f, 0.55f), 1.2f);
	static final DustParticleEffect STAR_PINK = new DustParticleEffect(new Vector3f(1.00f, 0.60f, 0.78f), 1.1f);

	private static final ParticleEffect SAND_BLOCK = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.SAND.getDefaultState());
	private static final Map<Integer, Long> CHARGE_START = new HashMap<>();

	private LegendEffects() {}

	// ================================================================== formas

	private static void expanding(Vec3d c, double maxR, int ticks, ParticleEffect fx, int points) {
		ClientEffects.schedule((w, r, age) -> {
			ClientEffects.ring(w, c, 0.3 + (maxR - 0.3) * (age + 1) / (double) ticks, points, fx);
			return age + 1 < ticks;
		});
	}

	/** Círculo num plano vertical (normal = dir). */
	private static void vring(ClientWorld w, Vec3d c, Vec3d dir, double r, int n, ParticleEffect fx) {
		Vec3d[] b = ClientEffects.basis(dir.normalize());
		for (int i = 0; i < n; i++) {
			double a = i * Math.PI * 2 / n;
			Vec3d q = c.add(b[0].multiply(Math.cos(a) * r)).add(b[1].multiply(Math.sin(a) * r));
			w.addParticle(fx, q.x, q.y, q.z, 0, 0, 0);
		}
	}

	private static void line(ClientWorld w, Vec3d a, Vec3d b, int n, ParticleEffect fx) {
		for (int i = 0; i <= n; i++) {
			Vec3d q = a.lerp(b, i / (double) n);
			w.addParticle(fx, q.x, q.y, q.z, 0, 0, 0);
		}
	}

	/** Estrela de cinco pontas de desenho animado, de frente para a câmera (aprox.). */
	private static void star(ClientWorld w, Vec3d c, Vec3d facing, double r, ParticleEffect fx) {
		Vec3d[] b = ClientEffects.basis(facing.normalize());
		Vec3d[] pts = new Vec3d[10];
		for (int i = 0; i < 10; i++) {
			double a = -Math.PI / 2 + i * Math.PI / 5, rr = i % 2 == 0 ? r : r * 0.45;
			pts[i] = c.add(b[0].multiply(Math.cos(a) * rr)).add(b[1].multiply(-Math.sin(a) * rr));
		}
		for (int i = 0; i < 10; i++) line(w, pts[i], pts[(i + 1) % 10], 3, fx);
		w.addParticle(fx, c.x, c.y, c.z, 0, 0, 0);
	}

	/** Lua crescente gorda, num plano vertical. */
	private static void crescentMoon(ClientWorld w, Vec3d c, Vec3d facing, double r, int n) {
		Vec3d[] b = ClientEffects.basis(facing.normalize());
		for (int i = 0; i <= n; i++) {
			double a = Math.PI * 0.15 + Math.PI * 1.7 * i / n;
			double thick = Math.sin(Math.PI * i / n) * r * 0.45;
			for (int k = 0; k < 3; k++) {
				double rr = r - thick * k / 2.0;
				Vec3d q = c.add(b[0].multiply(Math.cos(a) * rr)).add(b[1].multiply(Math.sin(a) * rr));
				w.addParticle(k == 0 ? MOON : MOON_BLUE, q.x, q.y, q.z, 0, 0, 0);
			}
		}
	}

	/** Ankh dourado de pé. */
	private static void ankh(ClientWorld w, Vec3d base, double s, Vec3d facing) {
		Vec3d[] b = ClientEffects.basis(LegendKitClient.flat(facing));
		Vec3d up = new Vec3d(0, 1, 0), side = b[0];
		line(w, base, base.add(up.multiply(2.2 * s)), (int) (14 * s), GOLD);
		line(w, base.add(up.multiply(2.2 * s)).subtract(side.multiply(0.9 * s)), base.add(up.multiply(2.2 * s)).add(side.multiply(0.9 * s)), (int) (10 * s), GOLD);
		Vec3d loop = base.add(up.multiply(2.9 * s));
		for (int i = 0; i < 22; i++) {
			double a = i * Math.PI * 2 / 22;
			Vec3d q = loop.add(side.multiply(Math.cos(a) * 0.5 * s)).add(up.multiply(Math.sin(a) * 0.7 * s));
			w.addParticle(i % 4 == 0 ? SUN_CORE : GOLD, q.x, q.y, q.z, 0, 0, 0);
		}
	}

	/** Asa de fogo aberta (silhueta de penas), de um lado do corpo. */
	private static void fireWing(ClientWorld w, Vec3d shoulder, Vec3d side, Vec3d back, double span, Random rnd) {
		Vec3d up = new Vec3d(0, 1, 0);
		for (int f = 0; f < 9; f++) {
			double t = f / 8.0;
			Vec3d root = shoulder.add(side.multiply(span * 0.5 * t)).add(up.multiply(0.6 * Math.sin(t * Math.PI)));
			double len = span * (0.55 + 0.45 * Math.sin(t * Math.PI * 0.9)) * 0.7;
			Vec3d dir = side.multiply(0.4 + t).add(up.multiply(0.9 - t * 1.5)).add(back.multiply(0.25)).normalize();
			for (int k = 0; k < 8; k++) {
				Vec3d q = root.add(dir.multiply(len * k / 8.0));
				w.addParticle(k > 5 ? EMBER : (k % 2 == 0 ? SUN : SUN_CORE), q.x, q.y, q.z, 0, 0, 0);
			}
			if (rnd.nextInt(3) == 0) {
				Vec3d tip = root.add(dir.multiply(len));
				w.addParticle(ParticleTypes.FLAME, tip.x, tip.y, tip.z, 0, 0.02, 0);
			}
		}
	}

	/** Disco de luar no chão. */
	private static void moonDisc(ClientWorld w, Vec3d c, double r, Random rnd) {
		int rings = Math.max(1, (int) (r * 2));
		for (int k = 1; k <= rings; k++) {
			double rr = r * k / rings;
			int n = (int) (rr * 9) + 6;
			for (int i = 0; i < n; i += 2) {
				double a = i * Math.PI * 2 / n + rnd.nextDouble() * 0.3;
				w.addParticle(k == rings ? MOON : MOON_BLUE, c.x + Math.cos(a) * rr, c.y + 0.06, c.z + Math.sin(a) * rr, 0, 0, 0);
			}
		}
	}

	// ================================================================== efeitos presos a uma entidade

	static void fx(ClientWorld world, String name, Entity e, int param, Random rnd) {
		Vec3d base = e.getPos();
		Vec3d mid = base.add(0, e.getHeight() * 0.55, 0);
		switch (name) {
			// ---------------------------------------------------------------- Sereia
			case "sea_dive" -> {
				expanding(base.add(0, 0.2, 0), 2.2, 6, FOAM, 24);
				for (int i = 0; i < 20; i++) world.addParticle(ParticleTypes.SPLASH, base.x + rnd.nextGaussian() * 0.6, base.y + 0.5,
						base.z + rnd.nextGaussian() * 0.6, 0, 0.2, 0);
			}
			case "song_start" -> {
				double r = param / 10.0;
				expanding(base.add(0, 0.3, 0), r, 12, AQUA, 56);
				expanding(base.add(0, 1.4, 0), r * 0.7, 10, PEARL, 40);
				// cúpula de bolhas (pontos numa esfera)
				for (int i = 0; i < 90; i++) {
					double u = rnd.nextDouble() * Math.PI * 2, v = rnd.nextDouble() * Math.PI / 2;
					double rr = 2.4;
					world.addParticle(i % 3 == 0 ? FOAM : AQUA, base.x + Math.cos(u) * Math.cos(v) * rr, base.y + Math.sin(v) * rr,
							base.z + Math.sin(u) * Math.cos(v) * rr, 0, 0.01, 0);
				}
				for (int i = 0; i < 12; i++) world.addParticle(ParticleTypes.NOTE, base.x + rnd.nextGaussian(), base.y + 2 + rnd.nextDouble(),
						base.z + rnd.nextGaussian(), rnd.nextDouble(), 0, 0);
				world.addParticle(ParticleTypes.FLASH, mid.x, mid.y, mid.z, 0, 0, 0);
			}
			case "charmed" -> {
				Vec3d top = base.add(0, e.getHeight() + 0.4, 0);
				for (int i = 0; i < 8; i++) {
					double a = i * Math.PI / 4 + rnd.nextDouble() * 0.2;
					world.addParticle(PEARL, top.x + Math.cos(a) * 0.45, top.y + Math.sin(a * 2) * 0.08, top.z + Math.sin(a) * 0.45, 0, 0, 0);
				}
				world.addParticle(ParticleTypes.NOTE, top.x, top.y + 0.2, top.z, rnd.nextDouble(), 0, 0);
				if (rnd.nextBoolean()) world.addParticle(ParticleTypes.HEART, top.x, top.y + 0.3, top.z, 0, 0, 0);
			}
			case "splash_hit" -> {
				for (int i = 0; i < 24; i++) world.addParticle(i % 2 == 0 ? ParticleTypes.SPLASH : ParticleTypes.FISHING, mid.x, mid.y, mid.z,
						rnd.nextGaussian() * 0.25, rnd.nextDouble() * 0.3, rnd.nextGaussian() * 0.25);
				for (int i = 0; i < 10; i++) world.addParticle(FOAM, mid.x + rnd.nextGaussian() * 0.4, mid.y + rnd.nextGaussian() * 0.4,
						mid.z + rnd.nextGaussian() * 0.4, 0, 0, 0);
			}
			case "wave_crash" -> {
				Vec3d c = base.add(e.getRotationVec(1f).multiply(1, 0, 1).normalize().multiply(2));
				expanding(c.add(0, 0.2, 0), 4.5, 8, FOAM, 44);
				expanding(c.add(0, 0.5, 0), 3.5, 7, AQUA, 36);
				for (int i = 0; i < 50; i++) world.addParticle(i % 3 == 0 ? FOAM : ParticleTypes.SPLASH, c.x + rnd.nextGaussian() * 1.5,
						c.y + rnd.nextDouble() * 2.5, c.z + rnd.nextGaussian() * 1.5, 0, 0.1, 0);
				for (int i = 0; i < 10; i++) world.addParticle(ParticleTypes.CLOUD, c.x + rnd.nextGaussian() * 1.5, c.y + 0.4,
						c.z + rnd.nextGaussian() * 1.5, 0, 0.04, 0);
			}
			// ---------------------------------------------------------------- Anúbis
			case "scales" -> {
				final int dur = Math.max(10, param);
				ClientEffects.schedule((w, r, age) -> {
					if (e.isRemoved()) return false;
					Vec3d top = e.getPos().add(0, e.getHeight() + 1.3, 0);
					Vec3d cam = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
					Vec3d side = LegendKitClient.flat(top.subtract(cam)).crossProduct(new Vec3d(0, 1, 0)).normalize();
					double k = age / (double) dur;
					double tilt = Math.sin(age * 0.45) * 0.35 * (1 - k) + k * 0.4 * Math.signum(Math.sin(e.getId()));
					// coluna e travessão inclinado
					line(w, top.add(0, -0.9, 0), top.add(0, 0.25, 0), 6, GOLD);
					Vec3d l = top.add(side.multiply(-1.1)).add(0, -tilt, 0), rgt = top.add(side.multiply(1.1)).add(0, tilt, 0);
					line(w, l, rgt, 12, GOLD);
					w.addParticle(SUN_CORE, top.x, top.y + 0.3, top.z, 0, 0, 0);
					// pratos pendurados
					for (Vec3d end : new Vec3d[]{l, rgt}) {
						line(w, end, end.add(side.multiply(-0.35)).add(0, -0.55, 0), 3, GOLD);
						line(w, end, end.add(side.multiply(0.35)).add(0, -0.55, 0), 3, GOLD);
						for (int i = 0; i < 10; i++) {
							double a = i * Math.PI * 2 / 10;
							w.addParticle(GOLD, end.x + Math.cos(a) * 0.4, end.y - 0.58, end.z + Math.sin(a) * 0.4, 0, 0, 0);
						}
					}
					// o coração e a pena
					w.addParticle(HEART, l.x, l.y - 0.4, l.z, 0, 0, 0);
					w.addParticle(HEART, l.x + 0.08, l.y - 0.35, l.z, 0, 0, 0);
					w.addParticle(ParticleTypes.END_ROD, rgt.x, rgt.y - 0.35, rgt.z, 0, 0.005, 0);
					if (age % 4 == 0) w.addParticle(ParticleTypes.ENCHANT, top.x, top.y + 0.6, top.z, r.nextGaussian(), -0.5, r.nextGaussian());
					// correntes douradas prendendo o alvo
					Vec3d feet = e.getPos();
					for (int i = 0; i < 4; i++) {
						double a = i * Math.PI / 2 + age * 0.1;
						Vec3d anchor = feet.add(Math.cos(a) * 1.4, 0.05, Math.sin(a) * 1.4);
						if (age % 2 == 0) line(w, anchor, feet.add(0, e.getHeight() * 0.5, 0), 5, i % 2 == 0 ? GOLD : LAPIS);
					}
					return age + 1 < dur;
				});
			}
			case "judgment" -> {
				boolean heavy = param == 1;
				Vec3d c = base.add(0, e.getHeight() * 0.5, 0);
				Vec3d facing = MinecraftClient.getInstance().gameRenderer.getCamera().getPos().subtract(c);
				ClientEffects.schedule((w, r, age) -> {
					ankh(w, base.add(0, e.getHeight() + 0.3 + age * 0.05, 0), heavy ? 1.1 : 0.7, facing);
					return age < (heavy ? 18 : 10);
				});
				expanding(base.add(0, 0.1, 0), heavy ? 5 : 3, 8, GOLD, 40);
				for (int i = 0; i < (heavy ? 60 : 25); i++) world.addParticle(i % 3 == 0 ? ParticleTypes.SCULK_SOUL : GOLD, c.x, c.y, c.z,
						rnd.nextGaussian() * 0.3, rnd.nextDouble() * 0.4, rnd.nextGaussian() * 0.3);
				if (heavy) {
					// a alma é puxada para baixo, para as areias
					for (int i = 0; i < 30; i++) world.addParticle(ParticleTypes.SOUL, c.x + rnd.nextGaussian() * 0.4, c.y + rnd.nextDouble() * 2,
							c.z + rnd.nextGaussian() * 0.4, 0, -0.15, 0);
					world.addParticle(ParticleTypes.SONIC_BOOM, c.x, c.y, c.z, 0, 0, 0);
				}
				world.addParticle(ParticleTypes.FLASH, c.x, c.y, c.z, 0, 0, 0);
			}
			case "duat_open" -> {
				double r = param / 10.0;
				expanding(base.add(0, 0.08, 0), r, 10, GOLD, 60);
				expanding(base.add(0, 0.12, 0), r * 0.85, 10, LAPIS, 50);
				for (int i = 0; i < 70; i++) world.addParticle(i % 2 == 0 ? SAND_BLOCK : SAND, base.x + rnd.nextGaussian() * 1.2, base.y + 0.2,
						base.z + rnd.nextGaussian() * 1.2, rnd.nextGaussian() * 0.15, 0.5 + rnd.nextDouble() * 0.5, rnd.nextGaussian() * 0.15);
				ankh(world, base.add(0, 2.4, 0), 0.8, e.getRotationVec(1f));
				for (int i = 0; i < 6; i++) jackalHead(world, base.add(Math.cos(i) * 2.5, 0.8, Math.sin(i) * 2.5), new Vec3d(-Math.cos(i), 0, -Math.sin(i)));
			}
			case "duat_close" -> {
				for (int i = 0; i < 50; i++) world.addParticle(SAND_BLOCK, base.x + rnd.nextGaussian() * 2, base.y + rnd.nextDouble() * 2,
						base.z + rnd.nextGaussian() * 2, 0, -0.2, 0);
				expanding(base.add(0, 0.1, 0), 2.0, 5, GOLD, 24);
			}
			// ---------------------------------------------------------------- Fênix
			case "solar_rise" -> {
				for (int i = 0; i < 50; i++) world.addParticle(i % 3 == 0 ? ParticleTypes.FLAME : (i % 2 == 0 ? SUN : EMBER),
						base.x + rnd.nextGaussian() * 0.4, base.y + rnd.nextDouble() * 0.5, base.z + rnd.nextGaussian() * 0.4,
						rnd.nextGaussian() * 0.05, 0.4 + rnd.nextDouble() * 0.6, rnd.nextGaussian() * 0.05);
				expanding(base.add(0, 0.1, 0), 3.0, 6, SUN, 36);
				world.addParticle(ParticleTypes.EXPLOSION, base.x, base.y + 0.3, base.z, 0, 0, 0);
			}
			case "solar_charge" -> {
				Vec3d look = e.getRotationVec(1f);
				Vec3d c = mid.subtract(look.multiply(0.8));
				ClientEffects.schedule((w, r, age) -> {
					Vec3d cc = e.getPos().add(0, e.getHeight() * 0.55, 0).subtract(e.getRotationVec(1f).multiply(0.8));
					double rr = 1.4 + age * 0.12;
					vring(w, cc, e.getRotationVec(1f), rr, 40, SUN);
					vring(w, cc, e.getRotationVec(1f), rr * 0.7, 28, SUN_CORE);
					// raios do sol
					Vec3d[] b = ClientEffects.basis(e.getRotationVec(1f));
					for (int i = 0; i < 12; i++) {
						double a = i * Math.PI / 6 + age * 0.15;
						Vec3d d = b[0].multiply(Math.cos(a)).add(b[1].multiply(Math.sin(a)));
						line(w, cc.add(d.multiply(rr * 1.1)), cc.add(d.multiply(rr * 1.6)), 3, EMBER);
					}
					return age < 4;
				});
				world.addParticle(ParticleTypes.FLASH, c.x, c.y, c.z, 0, 0, 0);
			}
			case "solar_impact" -> {
				double r = param / 10.0;
				Vec3d c = base.add(0, 0.1, 0);
				expanding(c, r, 8, SUN, 64);
				expanding(c.add(0, 0.3, 0), r * 0.8, 7, EMBER, 48);
				expanding(c.add(0, 0.05, 0), r * 1.2, 10, ParticleTypes.FLAME, 40);
				for (int i = 0; i < 16; i++) {
					double a = i * Math.PI / 8;
					line(world, c.add(Math.cos(a) * 0.8, 0, Math.sin(a) * 0.8), c.add(Math.cos(a) * r * 1.3, 0, Math.sin(a) * r * 1.3), 8, i % 2 == 0 ? SUN : SUN_CORE);
				}
				for (int i = 0; i < 60; i++) world.addParticle(i % 2 == 0 ? ParticleTypes.FLAME : ParticleTypes.LAVA, c.x, c.y + 0.3, c.z,
						rnd.nextGaussian() * 0.5, 0.2 + rnd.nextDouble() * 0.6, rnd.nextGaussian() * 0.5);
				for (int i = 0; i < 3; i++) world.addParticle(ParticleTypes.EXPLOSION, c.x + rnd.nextGaussian(), c.y + 0.5, c.z + rnd.nextGaussian(), 0, 0, 0);
				// coluna de luz
				for (int i = 0; i < 40; i++) world.addParticle(i % 2 == 0 ? SUN_CORE : SUN, c.x + rnd.nextGaussian() * 0.3, c.y + rnd.nextDouble() * 7,
						c.z + rnd.nextGaussian() * 0.3, 0, 0.05, 0);
				world.addParticle(ParticleTypes.FLASH, c.x, c.y + 1, c.z, 0, 0, 0);
			}
			case "dawn_wings" -> {
				double r = param / 10.0;
				ClientEffects.schedule((w, rr, age) -> {
					float yaw = (float) Math.toRadians(e instanceof PlayerEntity pl ? pl.bodyYaw : e.getYaw());
					Vec3d fwd = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw)), side = new Vec3d(Math.cos(yaw), 0, Math.sin(yaw));
					Vec3d sh = e.getPos().add(0, 1.35, 0).subtract(fwd.multiply(0.35));
					double span = 3.2 + Math.min(age, 6) * 0.35;
					fireWing(w, sh.add(side.multiply(0.25)), side, fwd.multiply(-1), span, rr);
					fireWing(w, sh.subtract(side.multiply(0.25)), side.multiply(-1), fwd.multiply(-1), span, rr);
					return age < 18;
				});
				expanding(base.add(0, 0.2, 0), r, 10, EMBER, 60);
				expanding(base.add(0, 1.0, 0), r * 0.85, 10, SUN, 50);
				for (int i = 0; i < 40; i++) {
					double a = rnd.nextDouble() * Math.PI * 2, sp = 0.3 + rnd.nextDouble() * 0.4;
					world.addParticle(i % 3 == 0 ? ParticleTypes.FLAME : SUN_CORE, base.x, base.y + 1.0, base.z, Math.cos(a) * sp, 0.05, Math.sin(a) * sp);
				}
				world.addParticle(ParticleTypes.FLASH, mid.x, mid.y, mid.z, 0, 0, 0);
			}
			case "dawn_pulse" -> {
				double r = param / 10.0;
				expanding(base.add(0, 0.15, 0), r * 0.7, 6, SUN, 36);
				Vec3d sun = base.add(0, 3.0, 0);
				for (int i = 0; i < 16; i++) {
					double a = i * Math.PI / 8;
					world.addParticle(i % 2 == 0 ? SUN : SUN_CORE, sun.x + Math.cos(a) * 0.5, sun.y + Math.sin(a) * 0.5, sun.z, 0, 0, 0);
				}
				world.addParticle(ParticleTypes.FLAME, sun.x, sun.y, sun.z, 0, 0.02, 0);
			}
			case "sun_blessed" -> ClientEffects.spiral(world, base, 0.7, 2.0, 2, 18, SUN, ParticleTypes.WAX_ON, rnd);
			case "rebirth" -> {
				double r = param / 10.0;
				// ovo de chamas que se forma, racha e explode
				ClientEffects.schedule((w, rr, age) -> {
					Vec3d c = e.getPos().add(0, 1.0, 0);
					double k = Math.min(1, age / 14.0);
					if (age < 18) {
						for (int i = 0; i < 40; i++) {
							double u = rr.nextDouble() * Math.PI * 2, v = (rr.nextDouble() - 0.5) * Math.PI;
							double s = 1.4 - k * 0.5;
							w.addParticle(i % 4 == 0 ? CRIMSON : (i % 2 == 0 ? EMBER : SUN), c.x + Math.cos(u) * Math.cos(v) * s * 0.75,
									c.y + Math.sin(v) * s, c.z + Math.sin(u) * Math.cos(v) * s * 0.75, 0, 0, 0);
						}
						if (age > 8 && age % 2 == 0) w.addParticle(ParticleTypes.LAVA, c.x, c.y, c.z, 0, 0, 0);
						return true;
					}
					// estouro: pilar, anel, e a silhueta da fênix de asas abertas
					expanding(e.getPos().add(0, 0.1, 0), r, 10, ParticleTypes.FLAME, 50);
					expanding(e.getPos().add(0, 0.3, 0), r * 0.9, 9, SUN, 50);
					for (int i = 0; i < 80; i++) w.addParticle(i % 2 == 0 ? SUN_CORE : SUN, c.x + rr.nextGaussian() * 0.4, c.y + rr.nextDouble() * 9,
							c.z + rr.nextGaussian() * 0.4, 0, 0.08, 0);
					ClientEffects.schedule((w2, r2, a2) -> {
						Vec3d sh = e.getPos().add(0, 2.4 + a2 * 0.08, 0);
						fireWing(w2, sh, new Vec3d(1, 0, 0), new Vec3d(0, 0, 1), 6.0, r2);
						fireWing(w2, sh, new Vec3d(-1, 0, 0), new Vec3d(0, 0, 1), 6.0, r2);
						for (int i = 0; i < 6; i++) w2.addParticle(SUN_CORE, sh.x, sh.y + 0.4 + i * 0.12, sh.z, 0, 0, 0);
						return a2 < 16;
					});
					for (int i = 0; i < 4; i++) w.addParticle(ParticleTypes.EXPLOSION, c.x + rr.nextGaussian(), c.y, c.z + rr.nextGaussian(), 0, 0, 0);
					for (int i = 0; i < 30; i++) w.addParticle(ParticleTypes.TOTEM_OF_UNDYING, c.x, c.y, c.z, rr.nextGaussian() * 0.6,
							rr.nextDouble() * 0.8, rr.nextGaussian() * 0.6);
					w.addParticle(ParticleTypes.FLASH, c.x, c.y, c.z, 0, 0, 0);
					return false;
				});
			}
			// ---------------------------------------------------------------- Vastaya Coelho
			case "rabbit_leap" -> {
				world.addParticle(ParticleTypes.GUST, base.x, base.y + 0.3, base.z, 0, 0, 0);
				expanding(base.add(0, 0.1, 0), 1.8, 4, MOON, 20);
				for (int i = 0; i < 10; i++) world.addParticle(ParticleTypes.CLOUD, base.x + rnd.nextGaussian() * 0.4, base.y + 0.1,
						base.z + rnd.nextGaussian() * 0.4, rnd.nextGaussian() * 0.05, 0.05, rnd.nextGaussian() * 0.05);
			}
			case "kick_star" -> {
				Vec3d cam = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
				Vec3d c = mid.add(rnd.nextGaussian() * 0.35, rnd.nextGaussian() * 0.35, rnd.nextGaussian() * 0.35);
				star(world, c, cam.subtract(c), 0.45 + (param % 3) * 0.1, param % 2 == 0 ? STAR : STAR_PINK);
				for (int i = 0; i < 6; i++) world.addParticle(ParticleTypes.CRIT, c.x, c.y, c.z, rnd.nextGaussian() * 0.4, rnd.nextGaussian() * 0.4, rnd.nextGaussian() * 0.4);
				if (param % 2 == 1) world.addParticle(ParticleTypes.SWEEP_ATTACK, c.x, c.y, c.z, 0, 0, 0);
			}
			case "frenzy_hit" -> {
				Vec3d cam = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
				star(world, mid, cam.subtract(mid), 0.25 + param * 0.06, param >= 4 ? STAR_PINK : STAR);
				for (int i = 0; i < param; i++) {
					double a = i * Math.PI * 2 / Math.max(1, param);
					world.addParticle(MOON, mid.x + Math.cos(a) * 0.6, mid.y + 0.7, mid.z + Math.sin(a) * 0.6, 0, 0.02, 0);
				}
			}
			case "moon_kick" -> {
				Entity atk = world.getEntityById(param);
				Vec3d dir = atk != null ? LegendKitClient.flat(e.getPos().subtract(atk.getPos())) : new Vec3d(0, 0, 1);
				Vec3d side = dir.crossProduct(new Vec3d(0, 1, 0)).normalize();
				ClientEffects.schedule((w, r, age) -> {
					crescentMoon(w, e.getPos().add(0, e.getHeight() * 0.5, 0), side, 1.2 + age * 0.25, 30);
					return age < 5;
				});
				for (int i = 0; i < 20; i++) {
					Vec3d d = new Vec3d(rnd.nextGaussian(), rnd.nextGaussian(), rnd.nextGaussian()).normalize();
					line(world, mid.add(d.multiply(0.8)), mid.add(d.multiply(1.8)), 3, i % 2 == 0 ? MOON : STAR);
				}
				Vec3d cam = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
				star(world, mid, cam.subtract(mid), 0.9, STAR);
				world.addParticle(ParticleTypes.FLASH, mid.x, mid.y, mid.z, 0, 0, 0);
				world.addParticle(ParticleTypes.EXPLOSION, mid.x, mid.y, mid.z, 0, 0, 0);
			}
			case "moon_full" -> {
				moonDisc(world, base, 1.6, rnd);
				world.addParticle(ParticleTypes.FLASH, base.x, base.y + 0.2, base.z, 0, 0, 0);
				for (int i = 0; i < 12; i++) world.addParticle(ParticleTypes.END_ROD, base.x + rnd.nextGaussian() * 0.6, base.y + 0.1,
						base.z + rnd.nextGaussian() * 0.6, 0, 0.12, 0);
			}
			case "moon_launch" -> {
				world.addParticle(ParticleTypes.GUST_EMITTER_SMALL, base.x, base.y + 0.1, base.z, 0, 0, 0);
				expanding(base.add(0, 0.1, 0), 1.5 + param * 0.25, 6, MOON, 36);
				for (int i = 0; i < 20; i++) world.addParticle(i % 2 == 0 ? MOON : MOON_BLUE, base.x, base.y + 0.1, base.z,
						rnd.nextGaussian() * 0.25, 0.2 + rnd.nextDouble() * 0.3, rnd.nextGaussian() * 0.25);
			}
			case "moon_crater" -> {
				double r = param / 10.0;
				Vec3d c = base.add(0, 0.08, 0);
				expanding(c, r, 8, MOON, 60);
				expanding(c.add(0, 0.2, 0), r * 0.75, 7, MOON_BLUE, 44);
				ClientEffects.stomp(world, e, r, rnd);
				for (int i = 0; i < 10; i++) {
					double a = i * Math.PI / 5 + rnd.nextDouble() * 0.3;
					Vec3d s = c.add(Math.cos(a) * r * 0.6, 0.4 + rnd.nextDouble() * 1.4, Math.sin(a) * r * 0.6);
					star(world, s, new Vec3d(Math.cos(a), 0, Math.sin(a)), 0.3, i % 2 == 0 ? STAR : MOON);
				}
				for (int i = 0; i < 30; i++) world.addParticle(ParticleTypes.END_ROD, c.x, c.y + 0.3, c.z,
						rnd.nextGaussian() * 0.4, rnd.nextDouble() * 0.5, rnd.nextGaussian() * 0.4);
				world.addParticle(ParticleTypes.FLASH, c.x, c.y + 0.5, c.z, 0, 0, 0);
			}
			default -> {}
		}
	}

	/** Cabeça de chacal estilizada (focinho + orelhas pontudas), olhando para "dir". */
	private static void jackalHead(ClientWorld w, Vec3d c, Vec3d dir) {
		Vec3d d = LegendKitClient.flat(dir), side = d.crossProduct(new Vec3d(0, 1, 0)).normalize();
		Vec3d up = new Vec3d(0, 1, 0);
		line(w, c, c.add(d.multiply(0.7)).add(0, -0.15, 0), 5, SHADOW);
		line(w, c.add(0, -0.25, 0), c.add(d.multiply(0.6)).add(0, -0.28, 0), 4, SHADOW);
		for (int s = -1; s <= 1; s += 2) {
			Vec3d ear = c.add(side.multiply(0.18 * s));
			line(w, ear, ear.add(up.multiply(0.65)).subtract(d.multiply(0.12)), 5, SHADOW);
			w.addParticle(GOLD, ear.x, ear.y + 0.3, ear.z, 0, 0, 0);
		}
		Vec3d eye = c.add(d.multiply(0.25)).add(0, 0.08, 0);
		w.addParticle(GOLD, eye.x, eye.y, eye.z, 0, 0, 0);
	}

	// ================================================================== efeitos soltos no mundo

	static void fxAt(ClientWorld world, String name, Vec3d p, float a, float b, float c, Random rnd) {
		switch (name) {
			case "soul_flow" -> {
				Vec3d to = new Vec3d(a, b, c);
				ClientEffects.schedule((w, r, age) -> {
					double k = Math.min(1, age / 12.0);
					Vec3d q = p.lerp(to, k).add(0, Math.sin(k * Math.PI) * 1.2, 0);
					w.addParticle(ParticleTypes.SOUL_FIRE_FLAME, q.x, q.y, q.z, 0, 0, 0);
					w.addParticle(GOLD, q.x, q.y, q.z, 0, 0, 0);
					w.addParticle(ParticleTypes.SCULK_SOUL, q.x, q.y, q.z, 0, 0.01, 0);
					if (k >= 1) {
						for (int i = 0; i < 12; i++) w.addParticle(GOLD, to.x, to.y, to.z, r.nextGaussian() * 0.15, r.nextGaussian() * 0.15, r.nextGaussian() * 0.15);
						return false;
					}
					return true;
				});
			}
			case "jackal_dash" -> {
				Vec3d to = new Vec3d(a, b, c);
				Vec3d d = to.subtract(p);
				ClientEffects.schedule((w, r, age) -> {
					double k = Math.min(1, age / 5.0);
					Vec3d q = p.lerp(to, k).add(0, Math.sin(k * Math.PI) * 0.8, 0);
					jackalHead(w, q, d);
					for (int i = 0; i < 4; i++) w.addParticle(i % 2 == 0 ? SAND : ParticleTypes.SOUL, q.x - d.x * 0.05 * i, q.y, q.z - d.z * 0.05 * i, 0, 0, 0);
					if (k >= 1) {
						// mordida: duas arcadas que se fecham
						Vec3d side = LegendKitClient.flat(d).crossProduct(new Vec3d(0, 1, 0)).normalize();
						for (int i = 0; i < 9; i++) {
							double t = i / 8.0 - 0.5;
							w.addParticle(SUN_CORE, to.x + side.x * t, to.y + 0.25 - Math.abs(t) * 0.3, to.z + side.z * t, 0, -0.05, 0);
							w.addParticle(SUN_CORE, to.x + side.x * t, to.y - 0.25 + Math.abs(t) * 0.3, to.z + side.z * t, 0, 0.05, 0);
						}
						for (int i = 0; i < 12; i++) w.addParticle(SAND_BLOCK, to.x, to.y, to.z, r.nextGaussian() * 0.2, r.nextDouble() * 0.2, r.nextGaussian() * 0.2);
						return false;
					}
					return true;
				});
			}
			case "tsunami" -> {
				double yaw = Math.toRadians(a), width = b;
				int ticks = (int) c;
				Vec3d dir = new Vec3d(-Math.sin(yaw), 0, Math.cos(yaw)), side = new Vec3d(-dir.z, 0, dir.x);
				ClientEffects.schedule((w, r, age) -> {
					Vec3d front = p.add(dir.multiply(age * 0.7 + 1.5));
					double grow = Math.min(1, age / 6.0) * (age > ticks - 5 ? Math.max(0.2, (ticks - age) / 5.0) : 1);
					double h = 3.2 * grow;
					int across = (int) (width * 2.5);
					for (int i = 0; i <= across; i++) {
						double t = i / (double) across - 0.5;
						double hh = h * (1 - Math.abs(t) * 0.5);
						Vec3d col = front.add(side.multiply(t * width));
						// corpo da onda: uma curva que sobe e se enrola para a frente no topo
						for (int k = 0; k < 7; k++) {
							double ang = k / 6.0 * Math.PI * 1.25;
							double rr = hh * 0.5;
							Vec3d q = col.add(dir.multiply(-Math.cos(ang) * rr * 0.7 + rr * 0.3)).add(0, Math.sin(Math.min(ang, Math.PI / 2)) * hh * (ang > Math.PI / 2 ? 1 : 1) - (ang > Math.PI / 2 ? (ang - Math.PI / 2) * rr * 0.6 : 0), 0);
							w.addParticle(k >= 5 ? FOAM : (k >= 3 ? AQUA : DEEP), q.x, q.y, q.z, 0, 0, 0);
						}
						if (r.nextInt(3) == 0) {
							Vec3d crest = col.add(dir.multiply(hh * 0.3)).add(0, hh, 0);
							w.addParticle(ParticleTypes.SPLASH, crest.x, crest.y, crest.z, dir.x * 0.2, 0.1, dir.z * 0.2);
							w.addParticle(ParticleTypes.FALLING_WATER, crest.x + dir.x * 0.4, crest.y, crest.z + dir.z * 0.4, 0, 0, 0);
						}
						if (r.nextInt(4) == 0) w.addParticle(ParticleTypes.CLOUD, col.x, col.y + 0.1, col.z, dir.x * 0.1, 0.02, dir.z * 0.1);
					}
					if (age % 3 == 0) w.addParticle(ParticleTypes.NAUTILUS, front.x, front.y + h * 0.6, front.z, 0, 0, 0);
					return age + 1 < ticks;
				});
			}
			default -> {}
		}
	}

	// ================================================================== auras contínuas

	static void tick(MinecraftClient mc, ClientWorld world, Vec3d cam, Random rnd) {
		long t = world.getTime();
		for (PlayerEntity pl : world.getPlayers()) {
			if (pl.squaredDistanceTo(cam) > 48 * 48) continue;
			Set<String> flags = EntityFlags.CLIENT.get(pl.getId());
			if (flags == null || flags.isEmpty()) {
				CHARGE_START.remove(pl.getId());
				continue;
			}
			boolean self = pl == mc.player && mc.options.getPerspective().isFirstPerson();
			Vec3d pos = pl.getPos();
			// Sereia
			if (flags.contains("sea_wet") && pl.isSwimming() && rnd.nextInt(2) == 0) {
				world.addParticle(ParticleTypes.BUBBLE, pos.x + rnd.nextGaussian() * 0.3, pos.y + 0.3, pos.z + rnd.nextGaussian() * 0.3, 0, 0.05, 0);
				if (rnd.nextInt(4) == 0) world.addParticle(ParticleTypes.GLOW, pos.x + rnd.nextGaussian() * 0.5, pos.y + 0.3, pos.z + rnd.nextGaussian() * 0.5, 0, 0, 0);
			}
			if (flags.contains("sea_dry") && rnd.nextInt(6) == 0) {
				world.addParticle(ParticleTypes.WHITE_ASH, pos.x + rnd.nextGaussian() * 0.3, pos.y + 0.4 + rnd.nextDouble(), pos.z + rnd.nextGaussian() * 0.3, 0, 0, 0);
			}
			if (flags.contains("singing")) {
				double ang = t * 0.25;
				for (int i = 0; i < 2; i++) {
					double a = ang + i * Math.PI;
					world.addParticle(ParticleTypes.NOTE, pos.x + Math.cos(a) * 1.1, pos.y + 1.2 + Math.sin(t * 0.1 + i) * 0.4,
							pos.z + Math.sin(a) * 1.1, ((t / 4 + i * 6) % 24) / 24.0, 0, 0);
				}
				if (t % 20 == 0) expanding(pos.add(0, 0.2, 0), 6.0, 10, AQUA, 40);
				if (t % 4 == 0) world.addParticle(PEARL, pos.x + rnd.nextGaussian() * 0.6, pos.y + 1.6, pos.z + rnd.nextGaussian() * 0.6, 0, 0.03, 0);
			}
			if (flags.contains("surfing")) {
				for (int i = 0; i < 4; i++) world.addParticle(i % 2 == 0 ? FOAM : ParticleTypes.SPLASH, pos.x + rnd.nextGaussian() * 0.5, pos.y,
						pos.z + rnd.nextGaussian() * 0.5, 0, 0.1, 0);
			}
			// Anúbis
			if (flags.contains("duat")) {
				for (int i = 0; i < 10; i++) {
					double a = t * 0.18 + i * Math.PI * 2 / 10;
					double rr = 1.5 + (i % 3) * 1.9 + Math.sin(t * 0.05 + i) * 0.4;
					double y = pos.y + 0.2 + ((t + i * 7) % 30) / 30.0 * 2.5;
					world.addParticle(i % 3 == 0 ? SAND_BLOCK : SAND, pos.x + Math.cos(a) * rr, y, pos.z + Math.sin(a) * rr, -Math.sin(a) * 0.1, 0.02, Math.cos(a) * 0.1);
				}
				if (t % 4 == 0) {
					// anel de hieróglifos girando no chão
					for (int i = 0; i < 24; i++) {
						double a = i * Math.PI / 12 - t * 0.02;
						world.addParticle(i % 3 == 0 ? LAPIS : GOLD, pos.x + Math.cos(a) * 3.0, pos.y + 0.06, pos.z + Math.sin(a) * 3.0, 0, 0, 0);
					}
				}
				if (t % 10 == 0 && !self) ankh(world, pos.add(0, 2.3, 0), 0.4, pl.getRotationVec(1f));
			}
			if (flags.contains("anubis_ears") && rnd.nextInt(40) == 0) {
				world.addParticle(GOLD, pos.x + rnd.nextGaussian() * 0.4, pos.y + 1.8, pos.z + rnd.nextGaussian() * 0.4, 0, -0.02, 0);
			}
			// Fênix
			if (flags.contains("solar_dive")) {
				for (int i = 0; i < 6; i++) world.addParticle(i % 2 == 0 ? ParticleTypes.FLAME : (i % 3 == 0 ? SUN_CORE : SUN),
						pos.x + rnd.nextGaussian() * 0.35, pos.y + 0.8 + rnd.nextGaussian() * 0.4, pos.z + rnd.nextGaussian() * 0.35, 0, 0, 0);
				if (rnd.nextInt(3) == 0) world.addParticle(ParticleTypes.LAVA, pos.x, pos.y + 1, pos.z, 0, 0, 0);
			}
			if (flags.contains("reborn")) {
				double a = t * 0.3;
				world.addParticle(EMBER, pos.x + Math.cos(a) * 0.8, pos.y + 0.2 + (t % 20) / 10.0, pos.z + Math.sin(a) * 0.8, 0, 0.02, 0);
				world.addParticle(SUN, pos.x - Math.cos(a) * 0.8, pos.y + 0.2 + (t % 20) / 10.0, pos.z - Math.sin(a) * 0.8, 0, 0.02, 0);
			}
			if (flags.contains("phoenix_wings") && !self && rnd.nextInt(pl.isOnGround() ? 10 : 2) == 0) {
				float yaw = (float) Math.toRadians(pl.bodyYaw);
				Vec3d back = pos.add(Math.sin(yaw) * 0.4, 1.2 + rnd.nextDouble() * 0.6, -Math.cos(yaw) * 0.4);
				world.addParticle(rnd.nextBoolean() ? EMBER : ParticleTypes.SMALL_FLAME, back.x + rnd.nextGaussian() * 0.6, back.y,
						back.z + rnd.nextGaussian() * 0.6, 0, -0.02, 0);
			}
			// Vastaya Coelho
			if (flags.contains("moon_charge")) {
				long st = CHARGE_START.computeIfAbsent(pl.getId(), id -> t);
				double k = Math.min(1, (t - st) / 40.0);
				if (t % 2 == 0) moonDisc(world, pos, 0.4 + k * 1.4, rnd);
				if (k >= 1 && t % 5 == 0) world.addParticle(ParticleTypes.END_ROD, pos.x + rnd.nextGaussian() * 0.4, pos.y + 0.1, pos.z + rnd.nextGaussian() * 0.4, 0, 0.08, 0);
			} else CHARGE_START.remove(pl.getId());
			if (flags.contains("moon_air")) {
				world.addParticle(rnd.nextBoolean() ? MOON : STAR, pos.x + rnd.nextGaussian() * 0.2, pos.y + rnd.nextDouble() * 1.6, pos.z + rnd.nextGaussian() * 0.2, 0, 0, 0);
				if (rnd.nextInt(4) == 0) world.addParticle(ParticleTypes.END_ROD, pos.x, pos.y + 0.8, pos.z, 0, 0, 0);
			}
			if (flags.contains("rushing")) {
				Vec3d v = pl.getVelocity();
				for (int i = 0; i < 3; i++) {
					Vec3d q = pos.add(rnd.nextGaussian() * 0.5, 0.3 + rnd.nextDouble() * 1.4, rnd.nextGaussian() * 0.5);
					line(world, q, q.subtract(v.multiply(1.5)), 3, MOON);
				}
			}
		}
	}

	/** Pequenos utilitários vetoriais do cliente. */
	static final class LegendKitClient {
		static Vec3d flat(Vec3d v) {
			Vec3d f = new Vec3d(v.x, 0, v.z);
			return f.lengthSquared() < 1e-6 ? new Vec3d(0, 0, 1) : f.normalize();
		}
	}
}
