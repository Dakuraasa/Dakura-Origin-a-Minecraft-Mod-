package com.dakura.origins.client.anim;

import com.dakura.origins.network.FireballPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.particle.DustColorTransitionParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.RaycastContext;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Efeitos de fogo dracônico gerados no próprio cliente (nada disso passa pela rede):
 *  - Bola de Fogo: clarão no disparo, rastro (núcleo incandescente, cauda de chamas,
 *    hélice violeta, centelhas) e o impacto (clarão, onda de choque, coluna de fogo, brasas).
 *  - Sopro Dracônico: inspiração (brasas sugadas para a boca), torrente em camadas
 *    (núcleo branco-dourado, chamas, bainha violeta, brasas que esfriam em roxo) e
 *    o fogo "espirrando" na superfície atingida.
 * Só roda perto da câmera, e a opção "Partículas" do jogo continua reduzindo tudo.
 */
public final class ClientEffects {
	private static final double MAX_DIST_SQ = 64 * 64;
	private static final double BURST_DIST_SQ = 96 * 96;

	private static final Vector3f GOLD = new Vector3f(1.0f, 0.78f, 0.28f);
	private static final Vector3f ORANGE = new Vector3f(1.0f, 0.55f, 0.12f);
	private static final Vector3f VIOLET = new Vector3f(0.62f, 0.32f, 0.95f);
	private static final Vector3f DEEP_VIOLET = new Vector3f(0.36f, 0.14f, 0.62f);

	static final DustParticleEffect CORE = new DustParticleEffect(new Vector3f(1.0f, 0.95f, 0.7f), 1.9f);
	static final DustParticleEffect CORE_SMALL = new DustParticleEffect(new Vector3f(1.0f, 0.88f, 0.5f), 1.2f);
	/** Brasa que nasce laranja e esfria até o violeta da raça. */
	private static final DustColorTransitionParticleEffect EMBER = new DustColorTransitionParticleEffect(ORANGE, VIOLET, 1.1f);
	private static final DustColorTransitionParticleEffect EMBER_BIG = new DustColorTransitionParticleEffect(GOLD, DEEP_VIOLET, 1.7f);

	private static final class Ball {
		Vec3d pos;
		final Vec3d vel;
		int age;
		boolean green;

		Ball(Vec3d pos, Vec3d vel) {
			this.pos = pos;
			this.vel = vel;
			this.green = ClientEffects.green;
		}
	}

	private static final Map<Integer, Ball> BALLS = new HashMap<>();

	/** Efeitos curtos que duram alguns ticks (onda do pisão, pilar de almas). */
	public interface Timed {
		/** @return false quando acabou */
		boolean tick(ClientWorld world, Random rnd, int age);
	}

	private static final java.util.List<Timed> TIMED = new ArrayList<>();
	private static final java.util.List<Integer> TIMED_AGE = new ArrayList<>();

	public static void schedule(Timed t) {
		TIMED.add(t);
		TIMED_AGE.add(0);
	}

	static final DustParticleEffect BLOOD = new DustParticleEffect(new Vector3f(0.62f, 0.03f, 0.08f), 0.9f);
	static final DustParticleEffect BLOOD_DARK = new DustParticleEffect(new Vector3f(0.35f, 0.0f, 0.05f), 1.2f);
	static final DustParticleEffect NIGHT = new DustParticleEffect(new Vector3f(0.28f, 0.12f, 0.36f), 1.3f);
	static final DustParticleEffect GHOST = new DustParticleEffect(new Vector3f(0.78f, 0.9f, 1.0f), 1.0f);
	static final DustParticleEffect LEAF = new DustParticleEffect(new Vector3f(0.42f, 0.72f, 0.36f), 1.0f);
	static final DustParticleEffect VINE = new DustParticleEffect(new Vector3f(0.22f, 0.55f, 0.2f), 1.3f);
	static final DustParticleEffect SUN = new DustParticleEffect(new Vector3f(1.0f, 0.9f, 0.45f), 1.2f);
	static final DustParticleEffect SUN_CORE = new DustParticleEffect(new Vector3f(1.0f, 0.98f, 0.85f), 1.8f);

	/** Olhar Aguçado (Elfo): até quando e em que raio² os contornos aparecem. */
	public static long KEEN_UNTIL = 0;
	public static double KEEN_RADIUS_SQ = 0;

	public static boolean keenOutline(Entity e) {
		MinecraftClient mc = MinecraftClient.getInstance();
		return System.currentTimeMillis() < KEEN_UNTIL && mc.player != null && e != mc.player
				&& e instanceof LivingEntity && e.squaredDistanceTo(mc.player) < KEEN_RADIUS_SQ && !e.isInvisible();
	}

	/** Efeitos em pontos do mundo (FxPayload). */
	public static void fxAt(String name, Vec3d p, float a, float b, float c) {
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world == null || !near(p)) return;
		Random rnd = world.random;
		switch (name) {
			case "clone_poof" -> {
				boolean snow = a > 0.5f;
				spiral(world, p, 0.45, 1.9, 2, 16, snow ? FROST : EMBER_SOFT, snow ? ParticleTypes.SNOWFLAKE : ParticleTypes.FLAME, rnd);
				for (int i = 0; i < 6; i++) world.addParticle(ParticleTypes.POOF, p.x + rnd.nextGaussian() * 0.2,
						p.y + rnd.nextDouble() * 1.6, p.z + rnd.nextGaussian() * 0.2, 0, 0.02, 0);
			}
			case "vine" -> {
				for (int i = 0; i < 4; i++) world.addParticle(i < 3 ? VINE : LEAF, p.x + rnd.nextGaussian() * 0.15,
						p.y + 0.05 + rnd.nextDouble() * 0.2, p.z + rnd.nextGaussian() * 0.15, 0, 0, 0);
				if (rnd.nextInt(2) == 0) world.addParticle(ParticleTypes.COMPOSTER, p.x, p.y + 0.15, p.z, 0, 0.02, 0);
				net.minecraft.block.BlockState g = world.getBlockState(net.minecraft.util.math.BlockPos.ofFloored(p).down());
				if (!g.isAir()) world.addParticle(new net.minecraft.particle.BlockStateParticleEffect(ParticleTypes.BLOCK, g),
						p.x, p.y + 0.05, p.z, a * 0.1, 0.12, c * 0.1);
			}
			case "vine_snare" -> {
				final double r = Math.max(0.5, a * 0.7), h = Math.max(1.0, b);
				spiral(world, p, r, h, 3, 16, VINE, ParticleTypes.SPORE_BLOSSOM_AIR, rnd);
				ring(world, p.add(0, 0.05, 0), r + 0.2, 18, LEAF);
				final int ticks = (int) c;
				schedule((w, rr, age) -> {
					if (age > ticks) return false;
					if (age % 4 == 0) ring(w, p.add(0, 0.05 + (age / 4 % 3) * 0.25, 0), r, 8, VINE);
					return true;
				});
			}
			case "boulder" -> {
				for (int i = 0; i < 14; i++) world.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, p.x + rnd.nextGaussian() * 0.8,
						p.y + rnd.nextDouble(), p.z + rnd.nextGaussian() * 0.8, rnd.nextGaussian() * 0.02, 0.03, rnd.nextGaussian() * 0.02);
				ring(world, p.add(0, 0.15, 0), 1.6, 22, DUSTWAVE);
			}
			case "raise_dead" -> {
				net.minecraft.block.BlockState g = world.getBlockState(net.minecraft.util.math.BlockPos.ofFloored(p).down());
				if (g.isAir()) g = net.minecraft.block.Blocks.DIRT.getDefaultState();
				ParticleEffect deb = new net.minecraft.particle.BlockStateParticleEffect(ParticleTypes.BLOCK, g);
				for (int i = 0; i < 14; i++) world.addParticle(deb, p.x + rnd.nextGaussian() * 0.3, p.y + 0.1,
						p.z + rnd.nextGaussian() * 0.3, 0, 0.15 + rnd.nextDouble() * 0.15, 0);
				spiral(world, p, 0.4, 1.8, 2, 14, SOUL_DUST, ParticleTypes.SOUL, rnd);
			}
			default -> ExtraEffects.fxAt(world, name, p, a, b, c, rnd);
		}
	}

	static final DustParticleEffect SOUL_DUST = new DustParticleEffect(new Vector3f(0.45f, 0.78f, 0.8f), 1.0f);
	static final DustParticleEffect STONE_DUST = new DustParticleEffect(new Vector3f(0.62f, 0.6f, 0.57f), 1.2f);
	static final DustParticleEffect CRIMSON = new DustParticleEffect(new Vector3f(0.75f, 0.1f, 0.22f), 1.0f);
	static final DustParticleEffect FEATHER = new DustParticleEffect(new Vector3f(0.95f, 0.9f, 0.84f), 0.8f);

	static final DustParticleEffect FROST = new DustParticleEffect(new Vector3f(0.82f, 0.93f, 1.0f), 1.0f);
	static final DustParticleEffect EMBER_SOFT = new DustParticleEffect(new Vector3f(1.0f, 0.58f, 0.22f), 1.0f);

	static final DustParticleEffect DUSTWAVE = new DustParticleEffect(new Vector3f(0.82f, 0.76f, 0.66f), 1.6f);

	/**
	 * Efeitos disparados pelo servidor via AnimPayload "fx:nome" (um pacote, partículas locais).
	 * @param param número extra (alvo, raio×10, liga/desliga...)
	 */
	public static void fx(String name, int entityId, int param) {
		ClientWorld world = MinecraftClient.getInstance().world;
		if (world == null) return;
		Entity e = world.getEntityById(entityId);
		if (e == null || !near(e.getPos())) return;
		Random rnd = world.random;
		switch (name) {
			case "stomp" -> stomp(world, e, param / 10.0, rnd);
			case "bite_grab" -> ring(world, e.getPos().add(0, e.getStandingEyeHeight() * 0.85, 0), 0.45, 16, BLOOD_DARK);
			case "bite_drain" -> {
				Entity v = world.getEntityById(param);
				if (v != null) strand(world, e.getPos().add(0, e.getStandingEyeHeight() * 0.85, 0), v.getEyePos().add(0, -0.15, 0), rnd);
			}
			case "bat" -> spiral(world, e.getPos(), 0.55, 1.8, 2, 22, NIGHT, param == 1 ? ParticleTypes.SMOKE : ParticleTypes.ASH, rnd);
			case "phase" -> {
				spiral(world, e.getPos(), 0.6, 2.0, 3, 18, GHOST, ParticleTypes.SOUL, rnd);
				ring(world, e.getPos().add(0, 0.05, 0), 0.8, 20, GHOST);
			}
			case "second_chance" -> soulPillar(e);
			case "dash" -> {
				Vec3d b = e.getPos();
				for (int i = 0; i < 10; i++) world.addParticle(ParticleTypes.CLOUD, b.x + rnd.nextGaussian() * 0.25, b.y + 0.1,
						b.z + rnd.nextGaussian() * 0.25, rnd.nextGaussian() * 0.03, 0.02, rnd.nextGaussian() * 0.03);
				ring(world, b.add(0, 0.05, 0), 0.6, 14, LEAF);
			}
			case "goat_jump" -> {
				Vec3d b = e.getPos();
				int n = 8 + param / 8;
				for (int i = 0; i < n; i++) world.addParticle(ParticleTypes.CLOUD, b.x + rnd.nextGaussian() * 0.3, b.y + 0.1,
						b.z + rnd.nextGaussian() * 0.3, rnd.nextGaussian() * 0.04, 0.03, rnd.nextGaussian() * 0.04);
				if (param > 60) ring(world, b.add(0, 0.05, 0), 0.9, 18, ParticleTypes.SNOWFLAKE);
			}
			case "stone_guard" -> {
				Vec3d c = e.getPos().add(0, e.getHeight() * 0.55, 0);
				ring(world, c, 0.7, 18, STONE_DUST);
				for (int i = 0; i < 6; i++) world.addParticle(ParticleTypes.ENCHANTED_HIT, c.x + rnd.nextGaussian() * 0.3,
						c.y + rnd.nextGaussian() * 0.3, c.z + rnd.nextGaussian() * 0.3, 0, 0, 0);
			}
			case "meteor" -> {
				stomp(world, e, param / 10.0, rnd);
				for (int i = 0; i < 10; i++) world.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, e.getX() + rnd.nextGaussian() * 0.8,
						e.getY() + 0.2, e.getZ() + rnd.nextGaussian() * 0.8, 0, 0.03, 0);
			}
			case "charm" -> {
				Entity t = world.getEntityById(param);
				if (t != null) {
					Vec3d from = e.getPos().add(0, e.getHeight() * 0.75, 0), to = t.getPos().add(0, t.getHeight() * 0.6, 0);
					for (int i = 0; i <= 12; i++) {
						Vec3d q = from.lerp(to, i / 12.0).add(0, Math.sin(i / 12.0 * Math.PI) * 0.4, 0);
						world.addParticle(CRIMSON, q.x, q.y, q.z, 0, 0, 0);
					}
					ring(world, to.add(0, 0.4, 0), 0.45, 12, CRIMSON);
					world.addParticle(ParticleTypes.HEART, to.x, to.y + 0.5, to.z, 0, 0.05, 0);
				}
			}
			case "soul_take" -> {
				Entity k = world.getEntityById(param);
				if (k != null) {
					Vec3d from = e.getPos().add(0, e.getHeight() * 0.5, 0), to = k.getPos().add(0, k.getHeight() * 0.6, 0);
					for (int i = 0; i <= 8; i++) {
						Vec3d q = from.lerp(to, i / 8.0);
						world.addParticle(ParticleTypes.SOUL, q.x, q.y, q.z, 0, 0.01, 0);
					}
				}
			}
			case "morph" -> {
				spiral(world, e.getPos(), 0.55, 1.6, 2, 18, CRIMSON, ParticleTypes.SMOKE, rnd);
				for (int i = 0; i < 8; i++) world.addParticle(ParticleTypes.LARGE_SMOKE, e.getX() + rnd.nextGaussian() * 0.3,
						e.getY() + 0.3 + rnd.nextDouble() * 0.8, e.getZ() + rnd.nextGaussian() * 0.3, 0, 0.02, 0);
			}
			case "keen" -> {
				if (MinecraftClient.getInstance().player != null && entityId == MinecraftClient.getInstance().player.getId()) {
					KEEN_UNTIL = System.currentTimeMillis() + (param / 1000) * 50L;
					KEEN_RADIUS_SQ = (param % 1000) * (param % 1000);
				}
			}
			default -> ExtraEffects.fx(world, name, e, param, rnd);
		}
	}

	static void ring(ClientWorld world, Vec3d c, double r, int n, ParticleEffect fx) {
		for (int i = 0; i < n; i++) {
			double a = i * Math.PI * 2 / n;
			world.addParticle(fx, c.x + Math.cos(a) * r, c.y, c.z + Math.sin(a) * r, 0, 0, 0);
		}
	}

	/** Fio fino de sangue indo da presa até a boca. */
	private static void strand(ClientWorld world, Vec3d from, Vec3d to, Random rnd) {
		for (int i = 0; i <= 7; i++) {
			Vec3d p = from.lerp(to, i / 7.0).add(rnd.nextGaussian() * 0.03, Math.sin(i / 7.0 * Math.PI) * 0.12, rnd.nextGaussian() * 0.03);
			world.addParticle(i % 3 == 0 ? BLOOD_DARK : BLOOD, p.x, p.y, p.z, 0, 0, 0);
		}
		world.addParticle(ParticleTypes.FALLING_DRIPSTONE_LAVA, from.x, from.y - 0.1, from.z, 0, 0, 0);
	}

	/** Duas ou três hélices subindo em volta do corpo. */
	static void spiral(ClientWorld world, Vec3d base, double r, double h, int arms, int steps,
							   ParticleEffect fx, ParticleEffect extra, Random rnd) {
		for (int s = 0; s < steps; s++) {
			double t = s / (double) steps;
			for (int k = 0; k < arms; k++) {
				double a = t * Math.PI * 3 + k * Math.PI * 2 / arms;
				double rr = r * (1.0 - t * 0.4);
				world.addParticle(fx, base.x + Math.cos(a) * rr, base.y + t * h, base.z + Math.sin(a) * rr, 0, 0, 0);
			}
		}
		for (int i = 0; i < 6; i++) {
			world.addParticle(extra, base.x + rnd.nextGaussian() * 0.3, base.y + rnd.nextDouble() * h, base.z + rnd.nextGaussian() * 0.3,
					0, 0.03, 0);
		}
	}

	/** Pisão: estilhaços do chão, onda de poeira que se abre e um tremor no centro. */
	static void stomp(ClientWorld world, Entity e, double radius, Random rnd) {
		Vec3d c = e.getPos();
		net.minecraft.block.BlockState ground = world.getBlockState(e.getBlockPos().down());
		if (ground.isAir()) ground = net.minecraft.block.Blocks.DIRT.getDefaultState();
		ParticleEffect debris = new net.minecraft.particle.BlockStateParticleEffect(ParticleTypes.BLOCK, ground);
		for (int i = 0; i < 36; i++) {
			double a = rnd.nextDouble() * Math.PI * 2, sp = 0.25 + rnd.nextDouble() * 0.3;
			world.addParticle(debris, c.x + Math.cos(a) * 0.6, c.y + 0.1, c.z + Math.sin(a) * 0.6,
					Math.cos(a) * sp, 0.25 + rnd.nextDouble() * 0.3, Math.sin(a) * sp);
		}
		world.addParticle(ParticleTypes.EXPLOSION, c.x, c.y + 0.4, c.z, 0, 0, 0);
		final ParticleEffect deb = debris;
		schedule((w, r, age) -> {
			if (age > 7) return false;
			double rr = 0.6 + (radius - 0.6) * (age / 7.0);
			int n = (int) Math.round(10 + rr * 7);
			for (int i = 0; i < n; i++) {
				double a = i * Math.PI * 2 / n + r.nextDouble() * 0.08;
				double x = c.x + Math.cos(a) * rr, z = c.z + Math.sin(a) * rr;
				w.addParticle(DUSTWAVE, x, c.y + 0.12, z, 0, 0, 0);
				if (i % 4 == 0) w.addParticle(deb, x, c.y + 0.1, z, 0, 0.12, 0);
			}
			if (age % 2 == 0) w.addParticle(ParticleTypes.CRIT, c.x + r.nextGaussian(), c.y + 0.3, c.z + r.nextGaussian(), 0, 0.2, 0);
			return true;
		});
	}

	/** Segunda Chance: um pilar pálido de almas desce do céu sobre o Espectro. */
	private static void soulPillar(Entity e) {
		final int id = e.getId();
		schedule((w, r, age) -> {
			if (age > 14) return false;
			Entity t = w.getEntityById(id);
			if (t == null) return false;
			Vec3d b = t.getPos();
			double head = 14 - age;              // a ponta do pilar desce
			for (int i = 0; i < 8; i++) {
				double y = head + r.nextDouble() * Math.min(6, 14 - head + 1);
				w.addParticle(GHOST, b.x + r.nextGaussian() * 0.12, b.y + y, b.z + r.nextGaussian() * 0.12, 0, 0, 0);
			}
			w.addParticle(ParticleTypes.SOUL, b.x, b.y + head, b.z, 0, -0.25, 0);
			if (age == 14) {
				ring(w, b.add(0, 0.1, 0), 1.0, 24, GHOST);
				for (int i = 0; i < 10; i++) w.addParticle(ParticleTypes.SCULK_SOUL, b.x + r.nextGaussian() * 0.4,
						b.y + r.nextDouble() * 1.8, b.z + r.nextGaussian() * 0.4, 0, 0.04, 0);
			}
			return true;
		});
	}

	private ClientEffects() {}

	public static void fireball(int id, Vec3d pos, Vec3d vel, int state) {
		ClientWorld world = MinecraftClient.getInstance().world;
		green = id < 0;
		if (state == FireballPayload.FLYING) {
			BALLS.put(id, new Ball(pos, vel));
			if (world != null && near(pos)) muzzle(world, pos, vel, world.random);
			green = false;
			return;
		}
		BALLS.remove(id);
		if (world != null && near(pos)) {
			if (state == FireballPayload.EXPLODED) impact(world, pos, world.random);
			else fizzle(world, pos, world.random);
		}
		green = false;
	}

	public static void clear() {
		BALLS.clear();
		TIMED.clear();
		TIMED_AGE.clear();
	}

	private static boolean near(Vec3d pos) {
		return MinecraftClient.getInstance().gameRenderer.getCamera().getPos().squaredDistanceTo(pos) < BURST_DIST_SQ;
	}

	public static void tick(MinecraftClient client) {
		ClientWorld world = client.world;
		if (world == null || client.isPaused()) return;
		Vec3d cam = client.gameRenderer.getCamera().getPos();
		Random rnd = world.random;

		for (int i = TIMED.size() - 1; i >= 0; i--) {
			int age = TIMED_AGE.get(i);
			if (!TIMED.get(i).tick(world, rnd, age)) {
				TIMED.remove(i);
				TIMED_AGE.remove(i);
			} else TIMED_AGE.set(i, age + 1);
		}

		if (!BALLS.isEmpty()) {
			Iterator<Ball> it = BALLS.values().iterator();
			while (it.hasNext()) {
				Ball b = it.next();
				b.age++;
				if (b.age > 80) {
					it.remove();
					continue;
				}
				Vec3d prev = b.pos;
				b.pos = b.pos.add(b.vel);
				if (b.pos.squaredDistanceTo(cam) > MAX_DIST_SQ) continue;
				green = b.green;
				trail(world, b, prev, rnd);
				green = false;
			}
		}

		for (Integer id : new ArrayList<>(AbilityAnimations.active("breath"))) {
			Entity e = world.getEntityById(id);
			if (!(e instanceof LivingEntity le) || e.squaredDistanceTo(cam) > MAX_DIST_SQ) continue;
			float p = AbilityAnimations.progress(e, "breath", e.age);
			if (p < 0f) continue;
			if (p < 0.12f) inhale(world, le, rnd);
			else if (p <= 0.9f) breath(world, le, rnd, p);
		}

		for (var pl : world.getPlayers()) {
			if (pl.squaredDistanceTo(cam) > MAX_DIST_SQ) continue;
			var flags = com.dakura.origins.power.EntityFlags.CLIENT.get(pl.getId());
			if (flags == null) continue;
			// Kitsune com as Esferas Errantes prontas: as esferas orbitando ficam bem visíveis (faíscas e brilho)
			if (flags.contains("mana_orbs")) {
				int n = pl instanceof net.minecraft.client.network.AbstractClientPlayerEntity ap
						? com.dakura.origins.client.render.GeoPartsRenderer.kitsuneTails(ap) : 3;
				float tt = world.getTime();
				for (int i = 0; i < n; i++) {
					double a2 = tt * 0.15f + i * Math.PI * 2 / n;
					double ox = Math.cos(a2) * 1.3, oz = Math.sin(a2) * 1.3, oy = pl.getHeight() * 0.55 + Math.sin(a2 * 2) * 0.15;
					world.addParticle(i % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.GLOW, pl.getX() + ox, pl.getY() + oy, pl.getZ() + oz, 0, 0.005, 0);
					if (rnd.nextInt(3) == 0) world.addParticle(ParticleTypes.SNOWFLAKE, pl.getX() + ox, pl.getY() + oy, pl.getZ() + oz,
							rnd.nextGaussian() * 0.01, 0, rnd.nextGaussian() * 0.01);
				}
			}
			// Minotauro em fúria: bufadas de fumaça branca pelas narinas
			if (flags.contains("berserk") && pl.age % 12 == 0) {
				Vec3d look = pl.getRotationVec(1f), eye = pl.getEyePos().add(look.multiply(0.35)).add(0, -0.12, 0);
				for (int i = 0; i < 4; i++) world.addParticle(ParticleTypes.CLOUD, eye.x, eye.y, eye.z,
						look.x * 0.06 + rnd.nextGaussian() * 0.01, -0.01, look.z * 0.06 + rnd.nextGaussian() * 0.01);
			}
			// Gárgula despencando: rastro de poeira de pedra
			if (flags.contains("meteor") && pl.getVelocity().y < -0.5) {
				world.addParticle(STONE_DUST, pl.getX() + rnd.nextGaussian() * 0.2, pl.getY() + 1.0, pl.getZ() + rnd.nextGaussian() * 0.2, 0, 0, 0);
				world.addParticle(ParticleTypes.SMOKE, pl.getX(), pl.getY() + 1.4, pl.getZ(), 0, 0.05, 0);
			}
		}

		ExtraEffects.tick(client, world, cam, rnd);

		for (Integer id : new ArrayList<>(AbilityAnimations.active("solar"))) {
			Entity e = world.getEntityById(id);
			if (!(e instanceof LivingEntity le) || e.squaredDistanceTo(cam) > MAX_DIST_SQ) continue;
			float p = AbilityAnimations.progress(e, "solar", e.age);
			if (p < 0f) continue;
			if (p < 0.555f) solarCharge(world, le, rnd, p / 0.555f);
			else solarBeam(world, le, rnd);
		}
	}

	// ================================================================== Bola de Fogo

	/** Clarão de disparo: anel de chamas abrindo em volta da boca do tiro + baforada violeta. */
	private static void muzzle(ClientWorld world, Vec3d pos, Vec3d vel, Random rnd) {
		Vec3d dir = vel.normalize();
		Vec3d[] basis = basis(dir);
		for (int i = 0; i < 12; i++) {
			double a = i * Math.PI * 2 / 12;
			Vec3d r = basis[0].multiply(Math.cos(a)).add(basis[1].multiply(Math.sin(a)));
			Vec3d v = r.multiply(0.13).add(dir.multiply(0.08));
			add(world, ParticleTypes.FLAME, pos.x, pos.y, pos.z, v.x, v.y, v.z);
		}
		for (int i = 0; i < 6; i++) {
			add(world, ParticleTypes.DRAGON_BREATH, pos.x, pos.y, pos.z,
					dir.x * 0.18 + rnd.nextGaussian() * 0.04, dir.y * 0.18 + rnd.nextGaussian() * 0.04,
					dir.z * 0.18 + rnd.nextGaussian() * 0.04);
		}
		add(world, CORE, pos.x, pos.y, pos.z, 0, 0, 0);
	}

	private static void trail(ClientWorld world, Ball b, Vec3d prev, Random rnd) {
		// núcleo incandescente contínuo (3 posições por tick)
		for (int k = 0; k < 3; k++) {
			Vec3d p = prev.lerp(b.pos, k / 3.0);
			add(world, k == 0 ? CORE : CORE_SMALL, p.x, p.y, p.z, 0, 0, 0);
			add(world, ParticleTypes.FLAME, p.x + rnd.nextGaussian() * 0.07, p.y + rnd.nextGaussian() * 0.07,
					p.z + rnd.nextGaussian() * 0.07, 0, 0, 0);
		}
		// cauda de cometa: chamas escorrendo para trás
		for (int k = 0; k < 2; k++) {
			add(world, ParticleTypes.FLAME, b.pos.x, b.pos.y, b.pos.z,
					-b.vel.x * 0.07 + rnd.nextGaussian() * 0.035,
					-b.vel.y * 0.07 + rnd.nextGaussian() * 0.035,
					-b.vel.z * 0.07 + rnd.nextGaussian() * 0.035);
		}
		// hélice dupla: brasa (laranja → violeta) e sopro de dragão
		Vec3d dir = b.vel.normalize();
		Vec3d[] basis = basis(dir);
		double ang = b.age * 0.9;
		for (int h = 0; h < 2; h++) {
			double a = ang + h * Math.PI;
			Vec3d off = basis[0].multiply(Math.cos(a) * 0.34).add(basis[1].multiply(Math.sin(a) * 0.34));
			if (h == 0) add(world, EMBER, b.pos.x + off.x, b.pos.y + off.y, b.pos.z + off.z, 0, 0, 0);
			else add(world, ParticleTypes.DRAGON_BREATH, b.pos.x + off.x, b.pos.y + off.y, b.pos.z + off.z, 0, 0, 0);
		}
		// centelhas brancas soltas
		if (rnd.nextInt(2) == 0) {
			add(world, ParticleTypes.END_ROD, b.pos.x, b.pos.y, b.pos.z,
					rnd.nextGaussian() * 0.03, rnd.nextGaussian() * 0.03, rnd.nextGaussian() * 0.03);
		}
		if (b.age % 2 == 0) add(world, ParticleTypes.SMOKE, prev.x, prev.y, prev.z, 0, 0.01, 0);
		if (b.age % 3 == 0) add(world, ParticleTypes.LARGE_SMOKE, prev.x, prev.y, prev.z, 0, 0.015, 0);
		if (rnd.nextInt(4) == 0) add(world, ParticleTypes.LAVA, b.pos.x, b.pos.y, b.pos.z, 0, 0, 0);
	}

	/** Impacto: clarão, onda de choque rasteira, coluna de fogo, chuva de brasas e fumaça. */
	private static void impact(ClientWorld world, Vec3d at, Random rnd) {
		add(world, ParticleTypes.FLASH, at.x, at.y, at.z, 0, 0, 0);
		// onda de choque: fogo por fora, violeta por dentro
		for (int i = 0; i < 32; i++) {
			double a = i * Math.PI * 2 / 32 + rnd.nextDouble() * 0.1;
			double sp = 0.42 + rnd.nextDouble() * 0.12;
			add(world, ParticleTypes.FLAME, at.x, at.y + 0.15, at.z, Math.cos(a) * sp, 0.015, Math.sin(a) * sp);
		}
		for (int i = 0; i < 20; i++) {
			double a = i * Math.PI * 2 / 20;
			add(world, ParticleTypes.DRAGON_BREATH, at.x, at.y + 0.3, at.z, Math.cos(a) * 0.28, 0.02, Math.sin(a) * 0.28);
		}
		// coluna de fogo subindo
		for (int i = 0; i < 16; i++) {
			add(world, ParticleTypes.FLAME, at.x + rnd.nextGaussian() * 0.35, at.y, at.z + rnd.nextGaussian() * 0.35,
					rnd.nextGaussian() * 0.03, 0.14 + rnd.nextDouble() * 0.28, rnd.nextGaussian() * 0.03);
		}
		// brasas espalhadas que esfriam até o violeta
		for (int i = 0; i < 26; i++) {
			Vec3d p = at.add(rnd.nextGaussian() * 1.3, Math.abs(rnd.nextGaussian()) * 1.2, rnd.nextGaussian() * 1.3);
			if (i % 3 == 0) add(world, EMBER_BIG, p.x, p.y, p.z, 0, 0, 0);
			else add(world, EMBER, p.x, p.y, p.z, 0, 0, 0);
		}
		for (int i = 0; i < 12; i++) {
			add(world, ParticleTypes.LAVA, at.x, at.y + 0.2, at.z, 0, 0, 0);
		}
		for (int i = 0; i < 16; i++) {
			add(world, ParticleTypes.LARGE_SMOKE, at.x + rnd.nextGaussian() * 1.1, at.y + 0.4 + rnd.nextDouble(),
					at.z + rnd.nextGaussian() * 1.1, 0, 0.04 + rnd.nextDouble() * 0.04, 0);
		}
	}

	private static void fizzle(ClientWorld world, Vec3d at, Random rnd) {
		for (int i = 0; i < 12; i++) {
			add(world, ParticleTypes.LARGE_SMOKE, at.x + rnd.nextGaussian() * 0.3, at.y + rnd.nextGaussian() * 0.3,
					at.z + rnd.nextGaussian() * 0.3, 0, 0.03, 0);
		}
		for (int i = 0; i < 8; i++) {
			add(world, EMBER, at.x + rnd.nextGaussian() * 0.4, at.y + rnd.nextGaussian() * 0.4,
					at.z + rnd.nextGaussian() * 0.4, 0, 0, 0);
		}
	}

	/** Bola de fogo do Dragão Marrom (id negativo): mesmas partículas, paleta verde e azul. */
	private static boolean green;
	private static final DustParticleEffect G_CORE = new DustParticleEffect(new Vector3f(0.75f, 1.0f, 0.85f), 1.9f);
	private static final DustParticleEffect G_SMALL = new DustParticleEffect(new Vector3f(0.4f, 1.0f, 0.6f), 1.2f);
	private static final DustColorTransitionParticleEffect G_EMBER = new DustColorTransitionParticleEffect(
			new Vector3f(0.3f, 1f, 0.45f), new Vector3f(0.15f, 0.45f, 1f), 1.1f);
	private static final DustColorTransitionParticleEffect G_EMBER_BIG = new DustColorTransitionParticleEffect(
			new Vector3f(0.6f, 1f, 0.4f), new Vector3f(0.1f, 0.3f, 0.9f), 1.7f);

	private static void add(ClientWorld w, net.minecraft.particle.ParticleEffect fx, double x, double y, double z, double vx, double vy, double vz) {
		if (green) {
			if (fx == CORE) fx = G_CORE;
			else if (fx == CORE_SMALL) fx = G_SMALL;
			else if (fx == EMBER) fx = G_EMBER;
			else if (fx == EMBER_BIG) fx = G_EMBER_BIG;
			else if (fx == ParticleTypes.FLAME || fx == ParticleTypes.SMALL_FLAME) fx = ParticleTypes.SOUL_FIRE_FLAME;
			else if (fx == ParticleTypes.LAVA) fx = ParticleTypes.GLOW;
			else if (fx instanceof DustParticleEffect || fx instanceof DustColorTransitionParticleEffect) fx = G_SMALL;
		}
		w.addParticle(fx, x, y, z, vx, vy, vz);
	}

	// ================================================================== Sopro Dracônico

	private static Vec3d mouth(LivingEntity e, Vec3d look) {
		double scale = e.getScale();
		return e.getEyePos().add(look.multiply(0.35 * scale)).add(0, -0.08 * scale, 0);
	}

	/** Inspiração: brasas e fiapos de fogo sendo sugados para dentro da boca. */
	private static void inhale(ClientWorld world, LivingEntity e, Random rnd) {
		Vec3d look = e.getRotationVec(1f);
		Vec3d mouth = mouth(e, look);
		double r = 1.1 * e.getScale();
		for (int i = 0; i < 3; i++) {
			Vec3d from = mouth.add(look.multiply(0.6 * r)).add(rnd.nextGaussian() * r * 0.6,
					rnd.nextGaussian() * r * 0.4, rnd.nextGaussian() * r * 0.6);
			Vec3d v = mouth.subtract(from).multiply(0.14);
			if (i == 0) world.addParticle(ParticleTypes.SMALL_FLAME, from.x, from.y, from.z, v.x, v.y, v.z);
			else world.addParticle(ParticleTypes.DRAGON_BREATH, from.x, from.y, from.z, v.x, v.y, v.z);
		}
		world.addParticle(EMBER, mouth.x + rnd.nextGaussian() * 0.15, mouth.y + rnd.nextGaussian() * 0.15,
				mouth.z + rnd.nextGaussian() * 0.15, 0, 0, 0);
	}

	/** Torrente em camadas, que abre como um cone e espirra na superfície atingida. */
	private static void breath(ClientWorld world, LivingEntity e, Random rnd, float p) {
		Vec3d look = e.getRotationVec(1f);
		Vec3d mouth = mouth(e, look);
		// começa forte e afina um pouco no fim
		float strength = p > 0.8f ? 1f - (p - 0.8f) / 0.1f * 0.5f : 1f;

		// núcleo branco-dourado junto da boca
		for (int i = 0; i < 2; i++) {
			Vec3d c = mouth.add(look.multiply(0.25 + rnd.nextDouble() * 0.7));
			world.addParticle(i == 0 ? CORE : CORE_SMALL, c.x, c.y, c.z, 0, 0, 0);
		}
		// chamas principais: quanto mais rápidas, mais centradas
		int flames = Math.round(9 * strength);
		for (int i = 0; i < flames; i++) {
			double speed = 0.7 + rnd.nextDouble() * 0.5;
			double spread = 0.05 + (1.2 - speed) * 0.18;
			world.addParticle(ParticleTypes.FLAME, mouth.x, mouth.y, mouth.z,
					look.x * speed + rnd.nextGaussian() * spread,
					look.y * speed + rnd.nextGaussian() * spread,
					look.z * speed + rnd.nextGaussian() * spread);
		}
		// faíscas rápidas
		for (int i = 0; i < 2; i++) {
			double speed = 1.1 + rnd.nextDouble() * 0.3;
			world.addParticle(ParticleTypes.SMALL_FLAME, mouth.x, mouth.y, mouth.z,
					look.x * speed + rnd.nextGaussian() * 0.05, look.y * speed + rnd.nextGaussian() * 0.05,
					look.z * speed + rnd.nextGaussian() * 0.05);
		}
		// bainha violeta, mais larga e lenta, envolvendo a torrente
		for (int i = 0; i < 2; i++) {
			world.addParticle(ParticleTypes.DRAGON_BREATH, mouth.x, mouth.y, mouth.z,
					look.x * 0.5 + rnd.nextGaussian() * 0.09, look.y * 0.5 + rnd.nextGaussian() * 0.07,
					look.z * 0.5 + rnd.nextGaussian() * 0.09);
		}
		// brasas ao longo do jato, esfriando em roxo
		Vec3d along = mouth.add(look.multiply(1.0 + rnd.nextDouble() * 6.0));
		world.addParticle(EMBER, along.x + rnd.nextGaussian() * 0.3, along.y + rnd.nextGaussian() * 0.3,
				along.z + rnd.nextGaussian() * 0.3, 0, 0, 0);

		// onde o fogo bate: espalha pela superfície (um raycast por tick)
		BlockHitResult hit = world.raycast(new RaycastContext(mouth, mouth.add(look.multiply(9)),
				RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, e));
		if (hit.getType() == HitResult.Type.BLOCK) splash(world, hit, rnd);
		else if (rnd.nextInt(3) == 0) {
			Vec3d far = mouth.add(look.multiply(5.5 + rnd.nextDouble() * 3.5));
			world.addParticle(ParticleTypes.LARGE_SMOKE, far.x, far.y, far.z, 0, 0.04, 0);
		}
		if (rnd.nextInt(5) == 0) world.addParticle(ParticleTypes.LAVA, mouth.x, mouth.y, mouth.z, 0, 0, 0);
	}

	private static void splash(ClientWorld world, BlockHitResult hit, Random rnd) {
		Direction side = hit.getSide();
		Vec3d n = Vec3d.of(side.getVector());
		Vec3d at = hit.getPos().add(n.multiply(0.08));
		Vec3d[] basis = basis(n);
		for (int i = 0; i < 4; i++) {
			double a = rnd.nextDouble() * Math.PI * 2;
			double sp = 0.07 + rnd.nextDouble() * 0.1;
			Vec3d v = basis[0].multiply(Math.cos(a) * sp).add(basis[1].multiply(Math.sin(a) * sp)).add(n.multiply(0.02));
			world.addParticle(ParticleTypes.FLAME, at.x, at.y, at.z, v.x, v.y + 0.01, v.z);
		}
		if (rnd.nextInt(2) == 0) world.addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, 0, 0.05, 0);
		if (rnd.nextInt(3) == 0) world.addParticle(EMBER_BIG, at.x + rnd.nextGaussian() * 0.4, at.y + 0.2,
				at.z + rnd.nextGaussian() * 0.4, 0, 0, 0);
		if (rnd.nextInt(4) == 0) world.addParticle(ParticleTypes.LAVA, at.x, at.y, at.z, 0, 0, 0);
	}

	// ================================================================== Raio Solar (Dríade)

	/** Mão erguida da Dríade (braço direito). */
	private static Vec3d sunHand(LivingEntity e) {
		Vec3d l = e.getRotationVec(1f), f = new Vec3d(l.x, 0, l.z);
		f = f.lengthSquared() < 1e-4 ? new Vec3d(0, 0, 1) : f.normalize();
		Vec3d right = new Vec3d(-f.z, 0, f.x);
		return e.getPos().add(0, e.getHeight() + 0.45, 0).add(right.multiply(0.4)).add(f.multiply(0.1));
	}

	/** Carga: uma esfera de luz solar cresce na mão erguida, sugando faíscas e raios de luz em volta. */
	private static void solarCharge(ClientWorld world, LivingEntity e, Random rnd, float t) {
		Vec3d c = sunHand(e);
		double r = 0.15 + 0.5 * t;
		for (int i = 0; i < 8 + (int) (t * 10); i++) {
			double u = rnd.nextDouble() * 2 - 1, a = rnd.nextDouble() * Math.PI * 2, s = Math.sqrt(1 - u * u);
			world.addParticle(i % 3 == 0 ? SUN_CORE : SUN, c.x + s * Math.cos(a) * r, c.y + u * r, c.z + s * Math.sin(a) * r, 0, 0, 0);
		}
		world.addParticle(CORE, c.x, c.y, c.z, 0, 0, 0);
		// faíscas atraídas de longe para a esfera
		for (int i = 0; i < 3; i++) {
			double u = rnd.nextDouble() * 2 - 1, a = rnd.nextDouble() * Math.PI * 2, s = Math.sqrt(1 - u * u), d = 2.2 + rnd.nextDouble();
			Vec3d from = c.add(s * Math.cos(a) * d, u * d, s * Math.sin(a) * d);
			Vec3d v = c.subtract(from).multiply(0.12);
			world.addParticle(ParticleTypes.END_ROD, from.x, from.y, from.z, v.x, v.y, v.z);
		}
		// anel girando em volta da esfera
		double rot = e.age * 0.35;
		for (int i = 0; i < 8; i++) {
			double a = rot + i * Math.PI / 4;
			world.addParticle(SUN, c.x + Math.cos(a) * (r + 0.35), c.y + Math.sin(a * 2) * 0.08, c.z + Math.sin(a) * (r + 0.35), 0, 0, 0);
		}
		if (rnd.nextInt(2) == 0) world.addParticle(ParticleTypes.END_ROD, c.x, c.y + 4 + rnd.nextDouble() * 6, c.z, 0, -0.45, 0);
	}

	/** Raio: um canhão de luz largo (núcleo branco, duas hélices douradas, casca de brilho) e uma explosão onde bate. */
	private static void solarBeam(ClientWorld world, LivingEntity e, Random rnd) {
		Vec3d eye = e.getEyePos(), look = e.getRotationVec(1f);
		Vec3d from = sunHand(e).lerp(eye.add(look.multiply(0.9)), 0.7);
		Vec3d end = eye.add(look.multiply(24));
		BlockHitResult hit = world.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER,
				RaycastContext.FluidHandling.NONE, e));
		if (hit.getType() == HitResult.Type.BLOCK) end = hit.getPos();
		Vec3d dir = end.subtract(from);
		double len = dir.length();
		if (len < 0.1) return;
		dir = dir.normalize();
		Vec3d[] b = basis(dir);
		int n = (int) Math.min(64, len * 2.6);
		double spin = e.age * 0.7;
		for (int i = 0; i < n; i++) {
			double k = i / (double) n, d = k * len;
			Vec3d q = from.add(dir.multiply(d));
			world.addParticle(i % 2 == 0 ? CORE : SUN_CORE, q.x, q.y, q.z, 0, 0, 0);
			for (int h = 0; h < 2; h++) {
				double a = spin + d * 1.4 + h * Math.PI;
				Vec3d hq = q.add(b[0].multiply(Math.cos(a) * 0.38)).add(b[1].multiply(Math.sin(a) * 0.38));
				world.addParticle(SUN, hq.x, hq.y, hq.z, 0, 0, 0);
			}
		}
		for (int i = 0; i < 6; i++) {
			double d = rnd.nextDouble() * len, a = rnd.nextDouble() * Math.PI * 2;
			Vec3d q = from.add(dir.multiply(d)).add(b[0].multiply(Math.cos(a) * 0.65)).add(b[1].multiply(Math.sin(a) * 0.65));
			world.addParticle(SUN_CORE, q.x, q.y, q.z, 0, 0, 0);
		}
		// anel de disparo na boca do canhão
		for (int i = 0; i < 12; i++) {
			double a = i * Math.PI / 6 + spin;
			Vec3d q = from.add(b[0].multiply(Math.cos(a) * 0.55)).add(b[1].multiply(Math.sin(a) * 0.55));
			world.addParticle(SUN, q.x, q.y, q.z, 0, 0, 0);
		}
		if (hit.getType() == HitResult.Type.BLOCK) {
			for (int i = 0; i < 6; i++) world.addParticle(ParticleTypes.END_ROD, end.x, end.y, end.z,
					rnd.nextGaussian() * 0.15, rnd.nextGaussian() * 0.15, rnd.nextGaussian() * 0.15);
			for (int i = 0; i < 3; i++) world.addParticle(ParticleTypes.FLAME, end.x, end.y, end.z,
					rnd.nextGaussian() * 0.1, 0.06, rnd.nextGaussian() * 0.1);
			if (rnd.nextInt(2) == 0) world.addParticle(ParticleTypes.LARGE_SMOKE, end.x, end.y, end.z, 0, 0.05, 0);
			if (rnd.nextInt(5) == 0) world.addParticle(ParticleTypes.FLASH, end.x, end.y, end.z, 0, 0, 0);
		}
	}

	// ================================================================== util

	/** Dois vetores perpendiculares a dir (e entre si). */
	static Vec3d[] basis(Vec3d dir) {
		Vec3d side = dir.crossProduct(new Vec3d(0, 1, 0));
		if (side.lengthSquared() < 1e-4) side = new Vec3d(1, 0, 0);
		side = side.normalize();
		Vec3d up = side.crossProduct(dir).normalize();
		return new Vec3d[]{side, up};
	}
}
