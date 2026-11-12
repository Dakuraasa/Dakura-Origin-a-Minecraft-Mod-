package com.dakura.origins.client.anim;

import com.dakura.origins.client.hud.CasinoHud;
import com.dakura.origins.power.EntityFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Efeitos da segunda leva de raças: auras, floreio das habilidades, Laço Áureo, Égide, sombras, cassino...
 * Mais vistosos que o vanilla, mas leves (poucas partículas por tick, só perto da câmera).
 */
final class ExtraEffects {
	static final DustParticleEffect GOLD = new DustParticleEffect(new Vector3f(1.0f, 0.82f, 0.3f), 1.1f);
	static final DustParticleEffect ORANGE_LUX = new DustParticleEffect(new Vector3f(1.0f, 0.55f, 0.15f), 1.2f);
	static final DustParticleEffect BLUE_LUX = new DustParticleEffect(new Vector3f(0.35f, 0.7f, 1.0f), 1.0f);
	static final DustParticleEffect VIOLET = new DustParticleEffect(new Vector3f(0.6f, 0.2f, 1.0f), 1.4f);
	static final DustParticleEffect GOLD_SOFT = new DustParticleEffect(new Vector3f(1.0f, 0.93f, 0.62f), 0.7f);
	static final DustParticleEffect SHADOW = new DustParticleEffect(new Vector3f(0.16f, 0.05f, 0.26f), 1.7f);
	static final DustParticleEffect SHADOW_EDGE = new DustParticleEffect(new Vector3f(0.55f, 0.3f, 0.85f), 0.8f);
	static final DustParticleEffect PINK = new DustParticleEffect(new Vector3f(1.0f, 0.38f, 0.62f), 1.0f);
	static final DustParticleEffect FAIRY_A = new DustParticleEffect(new Vector3f(0.75f, 1.0f, 0.78f), 0.7f);
	static final DustParticleEffect FAIRY_B = new DustParticleEffect(new Vector3f(1.0f, 0.95f, 0.6f), 0.7f);
	static final DustParticleEffect RAGE = new DustParticleEffect(new Vector3f(0.55f, 0.25f, 0.12f), 1.4f);
	static final DustParticleEffect ICE = new DustParticleEffect(new Vector3f(0.7f, 0.9f, 1.0f), 1.3f);
	static final DustParticleEffect FIRE = new DustParticleEffect(new Vector3f(1.0f, 0.45f, 0.12f), 1.3f);

	private static final Map<Integer, Integer> TETHER = new HashMap<>();   // quem prende → alvo
	private static final Map<Integer, int[]> DRAIN = new HashMap<>();      // quem drena → {alvo, ticks}
	private static final Map<Integer, DustParticleEffect> AURA_CACHE = new HashMap<>();

	private ExtraEffects() {}

	private static DustParticleEffect dust(int rgb, float size) {
		return new DustParticleEffect(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), size);
	}

	private static Vec3d flatLook(Entity e) {
		Vec3d l = e.getRotationVec(1f);
		Vec3d f = new Vec3d(l.x, 0, l.z);
		return f.lengthSquared() < 1e-4 ? new Vec3d(0, 0, 1) : f.normalize();
	}

	/** Anel que se abre por alguns ticks (onda de choque). */
	private static void expandingRing(Vec3d c, double maxR, int ticks, ParticleEffect fx, int points) {
		ClientEffects.schedule((world, rnd, age) -> {
			double r = 0.3 + (maxR - 0.3) * (age + 1) / (double) ticks;
			ClientEffects.ring(world, c, r, points, fx);
			return age + 1 < ticks;
		});
	}

	static void fx(ClientWorld world, String name, Entity e, int param, Random rnd) {
		Vec3d base = e.getPos();
		switch (name) {
			// floreio em toda habilidade usada: anéis na cor da raça, um arco cortante à frente e brilhos subindo
			case "flourish" -> {
				DustParticleEffect c = dust(param, 1.0f), cs = dust(param, 0.6f);
				expandingRing(base.add(0, 0.08, 0), 1.6, 6, c, 18);
				Vec3d f = flatLook(e), side = new Vec3d(-f.z, 0, f.x);
				double y = e.getHeight() * 0.6;
				for (int i = 0; i <= 14; i++) {
					double a = Math.toRadians(-75 + i * (150 / 14.0));
					Vec3d q = base.add(0, y + Math.sin(a) * 0.25, 0).add(f.multiply(Math.cos(a) * 1.2)).add(side.multiply(Math.sin(a) * 1.2));
					world.addParticle(i % 3 == 0 ? c : cs, q.x, q.y, q.z, 0, 0, 0);
				}
				for (int i = 0; i < 6; i++) world.addParticle(ParticleTypes.END_ROD, base.x + rnd.nextGaussian() * 0.35,
						base.y + rnd.nextDouble() * e.getHeight(), base.z + rnd.nextGaussian() * 0.35, 0, 0.03, 0);
			}
			case "fox_burst" -> {
				boolean snow = param == 1;
				DustParticleEffect c = snow ? ICE : FIRE;
				expandingRing(base.add(0, 0.2, 0), 4.5, 8, c, 32);
				expandingRing(base.add(0, 1.0, 0), 3.0, 6, c, 24);
				ClientEffects.spiral(world, base, 0.8, 2.4, 3, 30, c, snow ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME, rnd);
				for (int i = 0; i < 20; i++) world.addParticle(snow ? ParticleTypes.SNOWFLAKE : ParticleTypes.LAVA, base.x, base.y + 0.6, base.z,
						rnd.nextGaussian() * 0.35, 0.2 + rnd.nextDouble() * 0.3, rnd.nextGaussian() * 0.35);
			}
			case "tether" -> TETHER.put(e.getId(), param);
			case "drain" -> DRAIN.put(e.getId(), new int[]{param, 55});
			case "mirror_hit" -> {
				Vec3d c = e.getEyePos().add(e.getRotationVec(1f).multiply(1.0)).add(0, -0.3, 0);
				for (int i = 0; i < 6; i++) {
					double a = i * Math.PI / 3;
					Vec3d[] b = ClientEffects.basis(e.getRotationVec(1f));
					Vec3d q = c.add(b[0].multiply(Math.cos(a) * 1.0)).add(b[1].multiply(Math.sin(a) * 1.0));
					world.addParticle(ParticleTypes.ELECTRIC_SPARK, q.x, q.y, q.z, 0, 0, 0);
				}
				Entity atk = world.getEntityById(param);
				if (atk != null) {
					Vec3d to = atk.getPos().add(0, atk.getHeight() * 0.6, 0);
					for (int i = 0; i <= 10; i++) {
						Vec3d q = c.lerp(to, i / 10.0);
						world.addParticle(GOLD, q.x, q.y, q.z, 0, 0, 0);
					}
					world.addParticle(ParticleTypes.FLASH, to.x, to.y, to.z, 0, 0, 0);
				}
			}
			case "warcry" -> {
				double r = param / 10.0;
				expandingRing(base.add(0, 0.1, 0), r, 9, RAGE, 40);
				expandingRing(base.add(0, 0.6, 0), r * 0.7, 7, ParticleTypes.CLOUD, 28);
				Vec3d mouth = e.getEyePos().add(e.getRotationVec(1f).multiply(0.4));
				for (int i = 0; i < 10; i++) world.addParticle(ParticleTypes.CLOUD, mouth.x, mouth.y, mouth.z,
						e.getRotationVec(1f).x * 0.3 + rnd.nextGaussian() * 0.05, 0.02, e.getRotationVec(1f).z * 0.3 + rnd.nextGaussian() * 0.05);
			}
			case "tempest_cut", "tempest_empowered" -> {
				boolean big = name.equals("tempest_empowered");
				Vec3d look = e.getRotationVec(1f), c = base.add(0, e.getHeight() * 0.6, 0);
				for (int i = 1; i <= (big ? 22 : 12); i++) {
					Vec3d p = c.add(look.multiply(i * 0.3));
					world.addParticle(big ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.CRIT, p.x, p.y, p.z, look.x * 0.2, 0.01, look.z * 0.2);
					if (i % 2 == 0) world.addParticle(ParticleTypes.SWEEP_ATTACK, p.x, p.y, p.z, 0, 0, 0);
				}
				if (big) {
					world.addParticle(ParticleTypes.FLASH, c.x + look.x * 2, c.y, c.z + look.z * 2, 0, 0, 0);
					for (int i = 0; i < 20; i++) world.addParticle(ParticleTypes.ENCHANTED_HIT, c.x + look.x * 2, c.y, c.z + look.z * 2,
							rnd.nextGaussian() * 0.3, rnd.nextGaussian() * 0.3, rnd.nextGaussian() * 0.3);
				}
			}
			case "gold_fist" -> {
				Vec3d c = base.add(0, e.getHeight() * 0.55, 0);
				ClientEffects.ring(world, c, 0.6, 16, GOLD);
				// explosão luxuosa: ouro, laranja e azul em camadas
				expandingRing(c, 1.8, 3, GOLD, 16);
				for (int i = 0; i < 10; i++) {
					double th = rnd.nextDouble() * Math.PI * 2, ph = rnd.nextDouble() * Math.PI - Math.PI / 2, sp = 0.15 + rnd.nextDouble() * 0.3;
					world.addParticle(i % 3 == 0 ? BLUE_LUX : i % 3 == 1 ? ORANGE_LUX : GOLD_SOFT, c.x, c.y, c.z, 0, 0, 0);
					world.addParticle(i % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.WAX_ON, c.x, c.y, c.z,
							Math.cos(th) * Math.cos(ph) * sp, Math.sin(ph) * sp, Math.sin(th) * Math.cos(ph) * sp);
				}
				world.addParticle(ParticleTypes.FLASH, c.x, c.y, c.z, 0, 0, 0);
				for (int i = 0; i < 4; i++) world.addParticle(ParticleTypes.WAX_ON, c.x, c.y, c.z,
						rnd.nextGaussian() * 0.3, rnd.nextDouble() * 0.3, rnd.nextGaussian() * 0.3);
				if (param == 1) {
					expandingRing(c, 3.0, 4, GOLD, 22);
					for (int i = 0; i < 6; i++) world.addParticle(ParticleTypes.FIREWORK, c.x, c.y, c.z,
							rnd.nextGaussian() * 0.25, rnd.nextGaussian() * 0.25, rnd.nextGaussian() * 0.25);
				}
			}
			case "jackpot" -> {
				FloatingText.add(e.getId(), param > 1 ? "×" + param + "!" : "JACKPOT", 0xFFD84A, 50);
				// fogos de artifício (só partículas do cliente: não causam dano)
				for (int i = 0; i < 2; i++) {
					final int k = i;
					ClientEffects.schedule((w, r, age) -> {
						if (age < k * 6) return true;
						Vec3d f = e.getPos().add(r.nextGaussian() * 2, 3.5 + r.nextDouble() * 2, r.nextGaussian() * 2);
						w.addFireworkParticle(f.x, f.y, f.z, 0, 0, 0, java.util.List.of(new net.minecraft.component.type.FireworkExplosionComponent(
								k % 2 == 0 ? net.minecraft.component.type.FireworkExplosionComponent.Type.LARGE_BALL : net.minecraft.component.type.FireworkExplosionComponent.Type.STAR,
								it.unimi.dsi.fastutil.ints.IntList.of(0xFFD23A, 0xFF8A1E, 0x4FA8FF), it.unimi.dsi.fastutil.ints.IntList.of(0xFFF3B0), false, false)));
						return false;
					});
				}
				Vec3d top = base.add(0, e.getHeight() + 0.3, 0);
				for (int i = 0; i < 10; i++) world.addParticle(i % 2 == 0 ? ParticleTypes.WAX_ON : ParticleTypes.FIREWORK, top.x, top.y, top.z,
						rnd.nextGaussian() * 0.18, 0.15 + rnd.nextDouble() * 0.25, rnd.nextGaussian() * 0.18);
				expandingRing(base.add(0, 0.1, 0), 2.2, 4, GOLD, 20);
			}
			case "dice_hud" -> {
				if (e == MinecraftClient.getInstance().player) com.dakura.origins.client.hud.DiceHud.start(param);
			}
			case "chain_pulse" -> {
				Vec3d c = base.add(0, e.getHeight() * 0.5, 0);
				for (int i = 0; i < 10; i++) world.addParticle(i % 2 == 0 ? SHADOW : VIOLET, c.x + rnd.nextGaussian() * 0.3, c.y + rnd.nextGaussian() * 0.4,
						c.z + rnd.nextGaussian() * 0.3, 0, 0.02, 0);
			}
			case "devour_bite" -> {
				Vec3d c = base.add(0, e.getHeight() * 0.5, 0);
				for (int i = 0; i < 40; i++) world.addParticle(i % 3 == 0 ? VIOLET : SHADOW, c.x, c.y, c.z,
						rnd.nextGaussian() * 0.25, rnd.nextGaussian() * 0.25, rnd.nextGaussian() * 0.25);
				world.addParticle(ParticleTypes.FLASH, c.x, c.y, c.z, 0, 0, 0);
			}
			case "devour_flame" -> {
				// fogo roxo envolvendo corpo e caudas por um instante (visual, não queima)
				final int dur = Math.max(10, param);
				ClientEffects.schedule((w, r, age) -> {
					if (e.isRemoved()) return false;
					Vec3d p = e.getPos();
					float k = 1 - age / (float) dur;
					for (int i = 0; i < 6; i++) {
						double a2 = r.nextDouble() * Math.PI * 2, rr = 0.35 + r.nextDouble() * 0.5;
						w.addParticle(i % 2 == 0 ? ParticleTypes.SOUL_FIRE_FLAME : VIOLET, p.x + Math.cos(a2) * rr, p.y + r.nextDouble() * 2.0 * k + 0.1,
								p.z + Math.sin(a2) * rr, 0, 0.04 + r.nextDouble() * 0.05, 0);
					}
					Vec3d back = e.getRotationVec(1f).multiply(-1, 0, -1).normalize();
					for (int i = 0; i < 3; i++) w.addParticle(VIOLET, p.x + back.x * (0.6 + r.nextDouble()), p.y + 0.6 + r.nextDouble() * 0.8,
							p.z + back.z * (0.6 + r.nextDouble()), 0, 0.05, 0);
					return age + 1 < dur;
				});
			}
			case "casino" -> {
				if (e == MinecraftClient.getInstance().player) CasinoHud.start(param & 15, (param >> 4) & 15, param >> 8);
			}
			case "casino_world" -> {
				ClientEffects.spiral(world, base, 0.7, 2.2, 2, 22, GOLD, ParticleTypes.WAX_ON, rnd);
			}
			case "fairy_dust" -> {
				double r = param / 10.0;
				for (int i = 0; i < 60; i++) {
					double a = rnd.nextDouble() * Math.PI * 2, d = Math.sqrt(rnd.nextDouble()) * r;
					world.addParticle(i % 3 == 0 ? FAIRY_A : i % 3 == 1 ? FAIRY_B : PINK, base.x + Math.cos(a) * d,
							base.y + 0.2 + rnd.nextDouble() * 2.2, base.z + Math.sin(a) * d, 0, 0, 0);
				}
				for (int i = 0; i < 12; i++) world.addParticle(ParticleTypes.END_ROD, base.x + rnd.nextGaussian() * r * 0.4,
						base.y + 2.5, base.z + rnd.nextGaussian() * r * 0.4, 0, -0.08, 0);
				expandingRing(base.add(0, 0.1, 0), r, 8, FAIRY_A, 36);
			}
			case "perch" -> {
				Entity host = world.getEntityById(param);
				if (host != null) for (int i = 0; i <= 10; i++) {
					Vec3d q = base.lerp(host.getPos().add(0, host.getHeight(), 0), i / 10.0).add(0, Math.sin(i / 10.0 * Math.PI) * 0.5, 0);
					world.addParticle(i % 2 == 0 ? FAIRY_A : FAIRY_B, q.x, q.y, q.z, 0, 0, 0);
				}
			}
			default -> FinaleEffects.fx(world, name, e, param, rnd);
		}
	}

	static void fxAt(ClientWorld world, String name, Vec3d p, float a, float b, float c, Random rnd) {
		switch (name) {
			case "dark_burst" -> {
				com.dakura.origins.client.fx.NativeFx.darkBurst(world, p, a, rnd);
				expandingRing(p, 1.5 * a, 5, VIOLET, 22);
				for (int i = 0; i < 30 * a; i++) world.addParticle(i % 2 == 0 ? SHADOW : VIOLET, p.x, p.y, p.z,
						rnd.nextGaussian() * 0.3, rnd.nextGaussian() * 0.3, rnd.nextGaussian() * 0.3);
				world.addParticle(ParticleTypes.FLASH, p.x, p.y, p.z, 0, 0, 0);
			}
			case "chest_reroll" -> {
				// baú brilhando: coluna dourada, anéis girando e faíscas elegantes
				final int dur = Math.max(10, (int) a);
				ClientEffects.schedule((w, r, age) -> {
					float k = age / (float) dur;
					double rr = 0.9 - k * 0.4;
					for (int i = 0; i < 3; i++) {
						double ang = age * 0.35 + i * Math.PI * 2 / 3;
						w.addParticle(i == 0 ? GOLD : i == 1 ? GOLD_SOFT : ORANGE_LUX, p.x + Math.cos(ang) * rr, p.y - 0.4 + k * 1.6, p.z + Math.sin(ang) * rr, 0, 0, 0);
					}
					if (age % 3 == 0) w.addParticle(ParticleTypes.END_ROD, p.x + r.nextGaussian() * 0.4, p.y + 0.6, p.z + r.nextGaussian() * 0.4, 0, 0.06, 0);
					if (age % 2 == 0) w.addParticle(ParticleTypes.WAX_ON, p.x + r.nextGaussian() * 0.5, p.y + r.nextDouble(), p.z + r.nextGaussian() * 0.5, 0, 0.05, 0);
					return age + 1 < dur;
				});
			}
			case "lucky_chest" -> {
				expandingRing(p, a > 0 ? 2.2 : 1.2, 5, GOLD, a > 0 ? 28 : 16);
				for (int i = 0; i < (a > 0 ? 40 : 16); i++) world.addParticle(i % 3 == 0 ? ParticleTypes.END_ROD : i % 3 == 1 ? GOLD_SOFT : BLUE_LUX,
						p.x, p.y + 0.4, p.z, rnd.nextGaussian() * 0.15, 0.1 + rnd.nextDouble() * 0.2, rnd.nextGaussian() * 0.15);
				if (a > 0) world.addParticle(ParticleTypes.FLASH, p.x, p.y + 0.5, p.z, 0, 0, 0);
			}
			case "shadow_orb" -> {
				for (int i = 0; i < 4; i++) world.addParticle(SHADOW, p.x + rnd.nextGaussian() * 0.12, p.y + rnd.nextGaussian() * 0.12,
						p.z + rnd.nextGaussian() * 0.12, 0, 0, 0);
				Vec3d vel = new Vec3d(a, b, c);
				if (vel.lengthSquared() > 1e-4) {
					Vec3d[] bs = ClientEffects.basis(vel.normalize());
					double t = world.getTime() * 0.6;
					for (int i = 0; i < 3; i++) {
						double ang = t + i * Math.PI * 2 / 3;
						Vec3d q = p.add(bs[0].multiply(Math.cos(ang) * 0.38)).add(bs[1].multiply(Math.sin(ang) * 0.38));
						world.addParticle(SHADOW_EDGE, q.x, q.y, q.z, 0, 0, 0);
					}
				}
				if (rnd.nextInt(2) == 0) world.addParticle(ParticleTypes.REVERSE_PORTAL, p.x, p.y, p.z, 0, 0, 0);
			}
			case "shadow_burst" -> {
				int n = a > 0 ? 40 : 22;
				for (int i = 0; i < n; i++) world.addParticle(i % 3 == 0 ? SHADOW_EDGE : SHADOW, p.x, p.y, p.z,
						rnd.nextGaussian() * 0.25, rnd.nextGaussian() * 0.25, rnd.nextGaussian() * 0.25);
				for (int i = 0; i < 8; i++) world.addParticle(ParticleTypes.SCULK_SOUL, p.x, p.y, p.z,
						rnd.nextGaussian() * 0.08, 0.05, rnd.nextGaussian() * 0.08);
				ClientEffects.ring(world, p, 1.2, 20, SHADOW_EDGE);
			}
			case "shadow_step" -> {
				Vec3d to = new Vec3d(a, b, c);
				for (Vec3d end : new Vec3d[]{p, to}) {
					for (int i = 0; i < 14; i++) world.addParticle(SHADOW, end.x + rnd.nextGaussian() * 0.25, end.y + rnd.nextDouble() * 1.8,
							end.z + rnd.nextGaussian() * 0.25, 0, 0, 0);
					for (int i = 0; i < 5; i++) world.addParticle(ParticleTypes.LARGE_SMOKE, end.x, end.y + 0.8, end.z,
							rnd.nextGaussian() * 0.04, 0.03, rnd.nextGaussian() * 0.04);
				}
				int n = (int) Math.min(30, p.distanceTo(to) * 3);
				for (int i = 0; i <= n; i++) {
					Vec3d q = p.lerp(to, i / (double) n).add(0, 1.0, 0);
					world.addParticle(SHADOW_EDGE, q.x, q.y, q.z, 0, 0, 0);
				}
			}
			default -> FinaleEffects.fxAt(world, name, p, a, b, c, rnd);
		}
	}

	static void tick(MinecraftClient mc, ClientWorld world, Vec3d cam, Random rnd) {
		long t = world.getTime();
		for (PlayerEntity pl : world.getPlayers()) {
			if (pl.squaredDistanceTo(cam) > 48 * 48) continue;
			Set<String> flags = EntityFlags.CLIENT.get(pl.getId());
			if (flags == null || flags.isEmpty()) continue;
			boolean self = pl == mc.player && mc.options.getPerspective().isFirstPerson();
			for (String f : flags) {
				if (!f.startsWith("aura#") || self && t % 3 != 0) continue;
				// aura: hélice suave subindo em volta do corpo, na cor da raça
				DustParticleEffect d = AURA_CACHE.computeIfAbsent(f.hashCode(), k -> {
					try {
						return dust(Integer.parseInt(f.substring(5), 16), 0.9f);
					} catch (NumberFormatException ex) {
						return GOLD_SOFT;
					}
				});
				double h = pl.getHeight();
				for (int i = 0; i < 2; i++) {
					double ang = t * 0.18 + i * Math.PI;
					double y = ((t * 0.04 + i * 0.5) % 1.0) * h;
					world.addParticle(d, pl.getX() + Math.cos(ang) * 0.6, pl.getY() + y, pl.getZ() + Math.sin(ang) * 0.6, 0, 0, 0);
				}
				if (rnd.nextInt(10) == 0) world.addParticle(d, pl.getX() + rnd.nextGaussian() * 0.4, pl.getY() + 0.05,
						pl.getZ() + rnd.nextGaussian() * 0.4, 0, 0, 0);
			}
			// Égide Espelhada: desenhada pelo FxWorld com o círculo rúnico animado (GIF)
			if (flags.contains("charging") && rnd.nextInt(2) == 0) {
				world.addParticle(RAGE, pl.getX() + rnd.nextGaussian() * 0.3, pl.getY() + 0.1, pl.getZ() + rnd.nextGaussian() * 0.3, 0, 0, 0);
				world.addParticle(ParticleTypes.CLOUD, pl.getX(), pl.getY() + 0.2, pl.getZ(), 0, 0.02, 0);
			}
			if (flags.contains("gliding") && rnd.nextInt(3) == 0) {
				Vec3d f = flatLook(pl), side = new Vec3d(-f.z, 0, f.x);
				Vec3d tip = pl.getPos().add(0, pl.getHeight() * 0.7, 0).add(side.multiply(-1.4)).add(f.multiply(-0.3));
				world.addParticle(ClientEffects.FEATHER, tip.x, tip.y, tip.z, 0, 0, 0);
			}
			if (flags.contains("fairy") && !pl.isOnGround() && rnd.nextInt(2) == 0) {
				world.addParticle(rnd.nextBoolean() ? FAIRY_A : FAIRY_B, pl.getX() + rnd.nextGaussian() * 0.15,
						pl.getY() + pl.getHeight() * 0.5, pl.getZ() + rnd.nextGaussian() * 0.15, 0, 0, 0);
			}
		}

		FinaleEffects.tick(mc, world, cam, rnd);

		// Laço Áureo: feixe ondulante da mão até o alvo + jaula de luz em volta dele
		for (Iterator<Map.Entry<Integer, Integer>> it = TETHER.entrySet().iterator(); it.hasNext(); ) {
			var en = it.next();
			Entity c = world.getEntityById(en.getKey()), tg = world.getEntityById(en.getValue());
			if (c == null || tg == null || !EntityFlags.clientHas(c, "channeling")) {
				it.remove();
				continue;
			}
			Vec3d look = c.getRotationVec(1f), side = new Vec3d(-look.z, 0, look.x).normalize();
			Vec3d hand = c.getEyePos().add(look.multiply(0.6)).add(side.multiply(-0.35)).add(0, -0.35, 0);
			Vec3d to = tg.getPos().add(0, tg.getHeight() * 0.5, 0);
			int n = (int) MathHelper.clamp(hand.distanceTo(to) * 3, 6, 40);
			for (int i = 0; i <= n; i++) {
				double k = i / (double) n;
				Vec3d q = hand.lerp(to, k).add(0, Math.sin(k * Math.PI * 3 + t * 0.6) * 0.07, 0);
				world.addParticle(i % 4 == 0 ? GOLD : GOLD_SOFT, q.x, q.y, q.z, 0, 0, 0);
			}
			double r = Math.max(0.6, tg.getWidth() * 0.9);
			for (int ring = 0; ring < 2; ring++) {
				double y = tg.getY() + (ring == 0 ? 0.1 : tg.getHeight() + 0.1);
				for (int i = 0; i < 12; i++) {
					double ang = i * Math.PI / 6 + t * (ring == 0 ? 0.12 : -0.12);
					world.addParticle(GOLD_SOFT, tg.getX() + Math.cos(ang) * r, y, tg.getZ() + Math.sin(ang) * r, 0, 0, 0);
				}
			}
			if (t % 2 == 0) for (int i = 0; i < 4; i++) {
				double ang = i * Math.PI / 2 + t * 0.12;
				world.addParticle(GOLD, tg.getX() + Math.cos(ang) * r, tg.getY() + rnd.nextDouble() * tg.getHeight(),
						tg.getZ() + Math.sin(ang) * r, 0, 0, 0);
			}
		}

		// Beijo Drenante: fio rosado saindo do alvo até a Succubus
		for (Iterator<Map.Entry<Integer, int[]>> it = DRAIN.entrySet().iterator(); it.hasNext(); ) {
			var en = it.next();
			Entity c = world.getEntityById(en.getKey()), tg = world.getEntityById(en.getValue()[0]);
			if (c == null || tg == null || --en.getValue()[1] <= 0) {
				it.remove();
				continue;
			}
			Vec3d from = tg.getPos().add(0, tg.getHeight() * 0.6, 0), to = c.getEyePos().add(0, -0.2, 0);
			for (int i = 0; i < 3; i++) {
				double k = ((t * 0.08 + i / 3.0) % 1.0);
				Vec3d q = from.lerp(to, k).add(0, Math.sin(k * Math.PI) * 0.4, 0);
				world.addParticle(i == 0 ? PINK : ClientEffects.CRIMSON, q.x, q.y, q.z, 0, 0, 0);
			}
			if (t % 10 == 0) world.addParticle(ParticleTypes.HEART, from.x, from.y + 0.4, from.z, 0, 0.05, 0);
		}
	}

	/** Égide Espelhada: dois anéis de runas girando em sentidos opostos + raios, na frente da mão estendida. */
	private static void mandala(ClientWorld world, PlayerEntity pl, long t) {
		Vec3d look = pl.getRotationVec(1f);
		Vec3d[] b = ClientEffects.basis(look);
		Vec3d c = pl.getEyePos().add(look.multiply(1.1)).add(0, -0.3, 0);
		for (int i = 0; i < 18; i++) {
			double a = i * Math.PI * 2 / 18 + t * 0.12;
			Vec3d q = c.add(b[0].multiply(Math.cos(a) * 0.95)).add(b[1].multiply(Math.sin(a) * 0.95));
			world.addParticle(i % 3 == 0 ? GOLD : GOLD_SOFT, q.x, q.y, q.z, 0, 0, 0);
		}
		for (int i = 0; i < 10; i++) {
			double a = i * Math.PI * 2 / 10 - t * 0.2;
			Vec3d q = c.add(b[0].multiply(Math.cos(a) * 0.55)).add(b[1].multiply(Math.sin(a) * 0.55));
			world.addParticle(GOLD, q.x, q.y, q.z, 0, 0, 0);
		}
		if (t % 2 == 0) for (int i = 0; i < 6; i++) {
			double a = i * Math.PI / 3 + t * 0.05, k = ((t * 0.1) % 1.0) * 0.9;
			Vec3d q = c.add(b[0].multiply(Math.cos(a) * k)).add(b[1].multiply(Math.sin(a) * k));
			world.addParticle(GOLD_SOFT, q.x, q.y, q.z, 0, 0, 0);
		}
	}
}
