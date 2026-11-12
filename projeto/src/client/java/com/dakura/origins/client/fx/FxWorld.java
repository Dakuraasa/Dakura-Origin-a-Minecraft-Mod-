package com.dakura.origins.client.fx;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Efeitos desenhados por código, sem partículas do Minecraft: malhas lisas e arredondadas (esferas, anéis,
 * discos, línguas de chama) com brilho aditivo, pulsação, crescimento e desaparecimento suave (easing),
 * e fragmentos de terreno com física (gravidade, giro, quique) que somem sozinhos sem mexer no mundo.
 */
public final class FxWorld {
	private enum Kind { ZONE_POISON, ZONE_FIRE, BOOM_GREEN, BOOM_POISON, CRATER, SPORES, CHARGE, DIVE, BREAK, BOOM_GOLD, BOOM_ARCANE,
		GOLD_TRAIL, SHIELD_FLASH, DUST, WARD_BURST, PUNCH, PUNCH_FINAL, BOOM_MANA, BOOM_VIOLET, FIRE_FIELD, FLOWER_CIRCLE, BLOOM }

	private static final class Shape {
		final Kind kind;
		final Vec3d pos;
		final float radius;
		final int life, grow;
		final Entity follow;
		final float seed;
		/** Direção do golpe (socos) ou variante. */
		float dx, dz, param;
		int age;

		Shape(Kind kind, Vec3d pos, float radius, int life, int grow, Entity follow, float seed) {
			this.kind = kind;
			this.pos = pos;
			this.radius = radius;
			this.life = Math.max(1, life);
			this.grow = Math.max(1, grow);
			this.follow = follow;
			this.seed = seed;
		}
	}

	private static final class Debris {
		BlockState state;
		double x, y, z, px, py, pz, vx, vy, vz;
		float rx, ry, sx, sy, scale;
		int age, life;
	}

	private static final List<Shape> SHAPES = new ArrayList<>();
	/** Laço Áureo: quem segura → alvo. */
	private static final java.util.Map<Integer, Integer> TETHER = new java.util.HashMap<>();
	private static final List<Debris> DEBRIS = new ArrayList<>();
	private static final int MAX_DEBRIS = 220;
	private static final net.minecraft.util.Identifier LILY_TEX = com.dakura.origins.DakuraOrigins.id("textures/entity/lily_pad.png");

	private FxWorld() {}

	public static void init() {
		WorldRenderEvents.AFTER_ENTITIES.register(FxWorld::render);
		ClientTickEvents.END_CLIENT_TICK.register(mc -> tick(mc));
	}

	// ================================================================== entrada (vinda dos pacotes de efeito)

	/** Efeitos soltos no mundo. Devolve true se o nome era deste sistema. */
	public static boolean fxAt(ClientWorld w, String name, Vec3d p, float a, float b, float c, Random rnd) {
		switch (name) {
			case "zone_poison" -> SHAPES.add(new Shape(Kind.ZONE_POISON, p, a, (int) b, (int) c, null, rnd.nextFloat() * 100));
			case "zone_greenfire" -> SHAPES.add(new Shape(Kind.ZONE_FIRE, p, a, (int) b, (int) c, null, rnd.nextFloat() * 100));
			// Dragão Marrom: sem esferas 3D, só partículas do próprio jogo
			case "boom_green" -> {
				w.addParticle(net.minecraft.particle.ParticleTypes.EXPLOSION_EMITTER, p.x, p.y, p.z, 0, 0, 0);
				for (int i = 0; i < 60 * Math.max(1f, a / 3f); i++) {
					double th = rnd.nextDouble() * Math.PI * 2, ph = rnd.nextDouble() * Math.PI - Math.PI / 2, sp = 0.15 + rnd.nextDouble() * 0.35;
					w.addParticle(i % 3 == 0 ? net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME : i % 3 == 1 ? net.minecraft.particle.ParticleTypes.LARGE_SMOKE : net.minecraft.particle.ParticleTypes.SOUL,
							p.x, p.y, p.z, Math.cos(th) * Math.cos(ph) * sp, Math.abs(Math.sin(ph)) * sp, Math.sin(th) * Math.cos(ph) * sp);
				}
			}
			case "boom_poison" -> {
				SHAPES.add(new Shape(Kind.BOOM_POISON, p, a, 18, 1, null, rnd.nextFloat() * 100));
				w.addParticle(net.minecraft.particle.ParticleTypes.EXPLOSION_EMITTER, p.x, p.y, p.z, 0, 0, 0);
				NativeFx.poison(w, p, Math.max(1.5f, a), (int) (40 + a * 12), rnd);
				for (int i = 0; i < 30; i++) w.addParticle(i % 2 == 0 ? net.minecraft.particle.ParticleTypes.WITCH : NativeFx.POISON, p.x, p.y, p.z,
						rnd.nextGaussian() * 0.35, rnd.nextDouble() * 0.4, rnd.nextGaussian() * 0.35);
			}
			case "poison_mist" -> NativeFx.poison(w, p, a, (int) (a * 6), rnd);
			case "crater" -> SHAPES.add(new Shape(Kind.CRATER, p, a, (int) b, 4, null, rnd.nextFloat() * 100));
			case "spores" -> {
				SHAPES.add(new Shape(Kind.SPORES, p, a, (int) b, 1, null, rnd.nextFloat() * 100));
				NativeFx.poison(w, p, a, (int) (a * 10), rnd);
			}
			case "debris" -> debris(w, p, (int) a, b, rnd);
			case "boom_gold" -> SHAPES.add(new Shape(Kind.BOOM_GOLD, p, a, 14, 1, null, rnd.nextFloat() * 100));
			case "boom_arcane" -> SHAPES.add(new Shape(Kind.BOOM_ARCANE, p, a, 16, 1, null, rnd.nextFloat() * 100));
			case "dust" -> SHAPES.add(new Shape(Kind.DUST, p, a, 26, 1, null, rnd.nextFloat() * 100));
			case "punch_hit", "punch_final" -> {
				boolean fin = name.equals("punch_final");
				Shape sh = new Shape(fin ? Kind.PUNCH_FINAL : Kind.PUNCH, p, fin ? 1.6f : 0.55f, fin ? 18 : 7, 1, null, rnd.nextFloat() * 100);
				sh.dx = b;
				sh.dz = c;
				sh.param = a;
				SHAPES.add(sh);
				if (fin) SHAPES.add(new Shape(Kind.DUST, p.add(0, -1, 0), 1.6f, 22, 1, null, rnd.nextFloat() * 100));
			}
			case "boom_mana" -> {
				boolean violet = b > 0;
				SHAPES.add(new Shape(violet ? Kind.BOOM_VIOLET : Kind.BOOM_MANA, p, a, 16, 1, null, rnd.nextFloat() * 100));
				if (violet) NativeFx.violetBurst(w, p, a, rnd);
				else NativeFx.manaBurst(w, p, a, rnd);
			}
			case "fire_field" -> {
			}
			case "flower_circle" -> SHAPES.add(new Shape(Kind.FLOWER_CIRCLE, p, a, (int) b, 20, null, rnd.nextFloat() * 100));
			default -> {
				return false;
			}
		}
		return true;
	}

	/** Efeitos presos a uma entidade. Devolve true se o nome era deste sistema. */
	public static boolean fx(ClientWorld w, String name, Entity e, int param, Random rnd) {
		switch (name) {
			// Dragão Marrom: preparação, mergulho e quebra só com partículas nativas (sem efeitos 3D verdes)
			case "charge_green" -> com.dakura.origins.client.anim.ClientEffects.schedule((ww, r, age) -> {
				if (e.isRemoved()) return false;
				Vec3d c = e.getPos();
				for (int i = 0; i < 3; i++) {
					double ang = r.nextDouble() * Math.PI * 2, rr = 1.6 + r.nextDouble();
					ww.addParticle(net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME, c.x + Math.cos(ang) * rr, c.y + 0.2 + r.nextDouble() * 1.8, c.z + Math.sin(ang) * rr,
							-Math.cos(ang) * 0.12, 0.02, -Math.sin(ang) * 0.12);
				}
				return age + 1 < Math.max(10, param);
			});
			case "dive_green" -> com.dakura.origins.client.anim.ClientEffects.schedule((ww, r, age) -> {
				if (e.isRemoved() || e.isOnGround()) return false;
				Vec3d c = e.getPos();
				for (int i = 0; i < 4; i++) ww.addParticle(i % 2 == 0 ? net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME : net.minecraft.particle.ParticleTypes.CAMPFIRE_COSY_SMOKE,
						c.x + r.nextGaussian() * 0.4, c.y + r.nextDouble() * 1.8, c.z + r.nextGaussian() * 0.4, 0, 0.05, 0);
				return age < 60;
			});
			case "grab_break" -> {
				Vec3d c = e.getPos().add(0, 1, 0);
				for (int i = 0; i < 30; i++) w.addParticle(i % 2 == 0 ? net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME : net.minecraft.particle.ParticleTypes.SMOKE, c.x, c.y, c.z,
						rnd.nextGaussian() * 0.25, rnd.nextGaussian() * 0.2, rnd.nextGaussian() * 0.25);
			}
			case "physgun" -> {
				TETHER.put(e.getId(), param);
				Entity tg = w.getEntityById(param);
				if (tg != null) for (int i = 0; i < 24; i++) w.addParticle(i % 3 == 0 ? net.minecraft.particle.ParticleTypes.FLASH
						: i % 3 == 1 ? net.minecraft.particle.ParticleTypes.END_ROD : net.minecraft.particle.ParticleTypes.WAX_ON,
						tg.getX(), tg.getY() + tg.getHeight() / 2, tg.getZ(), rnd.nextGaussian() * 0.2, rnd.nextGaussian() * 0.2, rnd.nextGaussian() * 0.2);
			}
			case "physgun_throw" -> SHAPES.add(new Shape(Kind.GOLD_TRAIL, e.getPos(), 0.8f, 40, 1, e, rnd.nextFloat() * 100));
			case "shield_flash" -> SHAPES.add(new Shape(Kind.SHIELD_FLASH, e.getPos(), 1.8f, 10, 1, e, rnd.nextFloat() * 100));
			case "bloom_heal" -> SHAPES.add(new Shape(Kind.BLOOM, e.getPos(), Math.max(0.5f, e.getWidth()), 16, 1, e, rnd.nextFloat() * 100));
			case "ward_melee", "ward_blink" -> SHAPES.add(new Shape(Kind.WARD_BURST, e.getPos(), name.equals("ward_melee") ? 3.5f : 2f,
					14, 1, e, rnd.nextFloat() * 100));
			default -> {
				return false;
			}
		}
		return true;
	}

	/** Lança pedaços do próprio chão em volta do ponto (só visual, o terreno não muda). */
	private static void debris(ClientWorld w, Vec3d c, int count, float radius, Random rnd) {
		for (int i = 0; i < count && DEBRIS.size() < MAX_DEBRIS; i++) {
			double a = rnd.nextDouble() * Math.PI * 2, d = rnd.nextDouble() * radius;
			BlockPos bp = BlockPos.ofFloored(c.x + Math.cos(a) * d, c.y - 0.5, c.z + Math.sin(a) * d);
			BlockState st = w.getBlockState(bp);
			for (int k = 0; k < 3 && st.isAir(); k++) st = w.getBlockState(bp = bp.down());
			if (st.isAir() || !st.isOpaque()) continue;
			Debris db = new Debris();
			db.state = st;
			db.x = db.px = bp.getX() + 0.5;
			db.y = db.py = bp.getY() + 1.0;
			db.z = db.pz = bp.getZ() + 0.5;
			double sp = 0.15 + rnd.nextDouble() * 0.3;
			db.vx = Math.cos(a) * sp;
			db.vz = Math.sin(a) * sp;
			db.vy = 0.45 + rnd.nextDouble() * 0.55;
			db.sx = (rnd.nextFloat() - 0.5f) * 40;
			db.sy = (rnd.nextFloat() - 0.5f) * 40;
			db.scale = 0.25f + rnd.nextFloat() * 0.35f;
			db.life = 50 + rnd.nextInt(30);
			DEBRIS.add(db);
		}
	}


	private static void render(WorldRenderContext ctx) {
		if (DEBRIS.isEmpty() && TETHER.isEmpty() && !anyFlagged(ctx.world())) return;
		MatrixStack ms = ctx.matrixStack();
		VertexConsumerProvider vcp = ctx.consumers();
		if (ms == null || vcp == null) return;
		Vec3d cam = ctx.camera().getPos();
		float pt = ctx.tickCounter().getTickDelta(true);
		ClientWorld world = ctx.world();

		if (!DEBRIS.isEmpty()) {
			var brm = MinecraftClient.getInstance().getBlockRenderManager();
			for (Debris d : DEBRIS) {
				double x = MathHelper.lerp(pt, d.px, d.x), y = MathHelper.lerp(pt, d.py, d.y), z = MathHelper.lerp(pt, d.pz, d.z);
				float fade = d.age > d.life - 10 ? (d.life - d.age) / 10f : 1f;
				ms.push();
				ms.translate(x - cam.x, y - cam.y, z - cam.z);
				ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(d.rx + d.sx * pt));
				ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(d.ry + d.sy * pt));
				float s = d.scale * fade;
				ms.scale(s, s, s);
				ms.translate(-0.5, -0.5, -0.5);
				int light = WorldRenderer.getLightmapCoordinates(world, BlockPos.ofFloored(x, y, z));
				brm.renderBlockAsEntity(d.state, ms, vcp, light, OverlayTexture.DEFAULT_UV);
				ms.pop();
			}
		}

		// vitória-régia da Fada sob os pés, boiando
		for (net.minecraft.entity.player.PlayerEntity pl : world.getPlayers()) {
			if (!com.dakura.origins.power.EntityFlags.clientHas(pl, "lily_pad")) continue;
			Vec3d p = pl.getLerpedPos(pt);
			float t = pl.age + pt;
			float half = 0.55f + pl.getScale() * 0.25f;
			ms.push();
			ms.translate(p.x - cam.x, p.y - cam.y + 0.015 + 0.012 * MathHelper.sin(t * 0.12f), p.z - cam.z);
			ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-MathHelper.lerpAngleDegrees(pt, pl.prevBodyYaw, pl.bodyYaw) + 15));
			ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
			VertexConsumer lv = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(LILY_TEX));
			MatrixStack.Entry en = ms.peek();
			Matrix4f mt = en.getPositionMatrix();
			int light = WorldRenderer.getLightmapCoordinates(world, pl.getBlockPos());
			lv.vertex(mt, -half, -half, 0).color(255, 255, 255, 255).texture(0, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, 0, 0, -1);
			lv.vertex(mt, -half, half, 0).color(255, 255, 255, 255).texture(0, 1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, 0, 0, -1);
			lv.vertex(mt, half, half, 0).color(255, 255, 255, 255).texture(1, 1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, 0, 0, -1);
			lv.vertex(mt, half, -half, 0).color(255, 255, 255, 255).texture(1, 0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, 0, 0, -1);
			ms.pop();
		}

		for (var en : TETHER.entrySet()) {
			Entity tg = world.getEntityById(en.getValue());
			if (tg == null) continue;
			Vec3d p = tg.getLerpedPos(pt);
			float side = Math.max(tg.getWidth(), tg.getHeight()) * 1.25f + 0.3f;
			ms.push();
			ms.translate(p.x - cam.x, p.y - cam.y + tg.getHeight() * 0.5, p.z - cam.z);
			ElfFx.prisonCube(ms, vcp, tg.age + pt, side);
			ms.pop();
		}
	}


	private static boolean anyFlagged(ClientWorld w) {
		if (w == null) return false;
		for (net.minecraft.entity.player.PlayerEntity pl : w.getPlayers())
			if (com.dakura.origins.power.EntityFlags.clientHas(pl, "lily_pad"))
				return true;
		return false;
	}

	// ================================================================== física e tempo

	private static void tick(MinecraftClient mc) {
		if (mc.world == null || mc.isPaused()) {
			if (mc.world == null) {
				SHAPES.clear();
				DEBRIS.clear();
			}
			return;
		}
		for (Iterator<Shape> it = SHAPES.iterator(); it.hasNext(); ) {
			Shape s = it.next();
			if (++s.age > s.life || (s.follow != null && s.follow.isRemoved())) it.remove();
			else NativeShapes.emit(mc.world, s.kind.name(), s.follow != null ? s.follow.getPos() : s.pos, s.radius, s.age, s.life, s.dx, s.dz);
		}
		for (net.minecraft.entity.player.PlayerEntity pl : mc.world.getPlayers()) NativeShapes.flagged(mc.world, pl);
		TETHER.entrySet().removeIf(en -> {
			Entity c = mc.world.getEntityById(en.getKey());
			return c == null || !com.dakura.origins.power.EntityFlags.clientHas(c, "physgun");
		});
		// Elfo Real segurando alguém: o raio dourado ganha faíscas, brilho e pó de ouro correndo pela linha
		for (var en : TETHER.entrySet()) {
			Entity c = mc.world.getEntityById(en.getKey()), tg = mc.world.getEntityById(en.getValue());
			if (c == null || tg == null) continue;
			Vec3d a0 = c.getEyePos().add(c.getRotationVec(1f).multiply(0.6)).add(0, -0.3, 0), b0 = tg.getPos().add(0, tg.getHeight() * 0.5, 0);
			net.minecraft.util.math.random.Random r = mc.world.random;
			for (int i = 0; i < 4; i++) {
				Vec3d q = a0.lerp(b0, r.nextDouble());
				mc.world.addParticle(i % 2 == 0 ? net.minecraft.particle.ParticleTypes.END_ROD : net.minecraft.particle.ParticleTypes.WAX_ON,
						q.x, q.y, q.z, r.nextGaussian() * 0.01, 0.01, r.nextGaussian() * 0.01);
			}
			if (r.nextInt(2) == 0) mc.world.addParticle(net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK, b0.x + r.nextGaussian() * 0.4,
					b0.y + r.nextGaussian() * 0.4, b0.z + r.nextGaussian() * 0.4, 0, 0, 0);
		}
		for (Iterator<Debris> it = DEBRIS.iterator(); it.hasNext(); ) {
			Debris d = it.next();
			d.px = d.x;
			d.py = d.y;
			d.pz = d.z;
			d.vy -= 0.06;
			d.x += d.vx;
			d.y += d.vy;
			d.z += d.vz;
			d.rx += d.sx;
			d.ry += d.sy;
			BlockPos bp = BlockPos.ofFloored(d.x, d.y - 0.1, d.z);
			if (d.vy < 0 && !mc.world.getBlockState(bp).getCollisionShape(mc.world, bp).isEmpty()) {
				d.y = bp.getY() + 1.0 + d.scale * 0.5;
				d.vy = -d.vy * 0.3;
				d.vx *= 0.6;
				d.vz *= 0.6;
				d.sx *= 0.5;
				d.sy *= 0.5;
			}
			if (++d.age > d.life) it.remove();
		}
	}

	// ================================================================== desenho

	/** Anel vertical de frente para a direção (dx, dz): a onda de choque de um golpe. */
	static void vring(Matrix4f m, VertexConsumer vc, float x, float y, float z, float dx, float dz, float r, float w, int rgb, float alpha) {
		if (alpha <= 0.01f || r <= 0) return;
		float l = MathHelper.sqrt(dx * dx + dz * dz);
		if (l < 1e-4f) {
			dx = 1;
			dz = 0;
			l = 1;
		}
		float sx = -dz / l, sz = dx / l; // eixo lateral do anel (o outro eixo é o vertical)
		int n = Math.max(16, (int) (r * 10));
		for (int i = 0; i < n; i++) {
			double a0 = 2 * Math.PI * i / n, a1 = 2 * Math.PI * (i + 1) / n;
			float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
			quad(m, vc, v(x + sx * c0 * r, y + s0 * r, z + sz * c0 * r), v(x + sx * c1 * r, y + s1 * r, z + sz * c1 * r),
					v(x + sx * c1 * (r + w), y + s1 * (r + w), z + sz * c1 * (r + w)), v(x + sx * c0 * (r + w), y + s0 * (r + w), z + sz * c0 * (r + w)), rgb, alpha);
		}
	}

	// ================================================================== primitivas (malhas)

	private static void quad(Matrix4f m, VertexConsumer vc, float[] a, float[] b, float[] c, float[] d, int rgb, float alpha) {
		int r = rgb >> 16 & 255, g = rgb >> 8 & 255, bl = rgb & 255, al = (int) (MathHelper.clamp(alpha, 0, 1) * 255);
		if (al <= 0) return;
		vc.vertex(m, a[0], a[1], a[2]).color(r, g, bl, al);
		vc.vertex(m, b[0], b[1], b[2]).color(r, g, bl, al);
		vc.vertex(m, c[0], c[1], c[2]).color(r, g, bl, al);
		vc.vertex(m, d[0], d[1], d[2]).color(r, g, bl, al);
		// verso (as malhas são vistas dos dois lados)
		vc.vertex(m, d[0], d[1], d[2]).color(r, g, bl, al);
		vc.vertex(m, c[0], c[1], c[2]).color(r, g, bl, al);
		vc.vertex(m, b[0], b[1], b[2]).color(r, g, bl, al);
		vc.vertex(m, a[0], a[1], a[2]).color(r, g, bl, al);
	}

	private static float[] v(float x, float y, float z) {
		return new float[]{x, y, z};
	}

	static void sphere(Matrix4f m, VertexConsumer vc, float x, float y, float z, float r, int rgb, float alpha) {
		if (alpha <= 0.01f || r <= 0) return;
		int lat = 8, lon = 12;
		for (int i = 0; i < lat; i++) {
			double t0 = Math.PI * i / lat - Math.PI / 2, t1 = Math.PI * (i + 1) / lat - Math.PI / 2;
			for (int j = 0; j < lon; j++) {
				double p0 = 2 * Math.PI * j / lon, p1 = 2 * Math.PI * (j + 1) / lon;
				quad(m, vc, pt(x, y, z, r, t0, p0), pt(x, y, z, r, t0, p1), pt(x, y, z, r, t1, p1), pt(x, y, z, r, t1, p0), rgb,
						alpha * (0.55f + 0.45f * (float) Math.cos(t0)));
			}
		}
	}

	/** Esfera pequena e leve (poucas faces) para pétalas, brasas, faíscas e pólen. */
	static void dot(Matrix4f m, VertexConsumer vc, float x, float y, float z, float r, int rgb, float alpha) {
		if (alpha <= 0.01f || r <= 0) return;
		int lat = 3, lon = 6;
		for (int i = 0; i < lat; i++) {
			double t0 = Math.PI * i / lat - Math.PI / 2, t1 = Math.PI * (i + 1) / lat - Math.PI / 2;
			for (int j = 0; j < lon; j++) {
				double p0 = 2 * Math.PI * j / lon, p1 = 2 * Math.PI * (j + 1) / lon;
				quad(m, vc, pt(x, y, z, r, t0, p0), pt(x, y, z, r, t0, p1), pt(x, y, z, r, t1, p1), pt(x, y, z, r, t1, p0), rgb, alpha);
			}
		}
	}

	private static float[] pt(float x, float y, float z, float r, double t, double p) {
		return v(x + (float) (Math.cos(t) * Math.cos(p) * r), y + (float) (Math.sin(t) * r), z + (float) (Math.cos(t) * Math.sin(p) * r));
	}

	static void ring(Matrix4f m, VertexConsumer vc, float x, float y, float z, float r, float w, int rgb, float alpha) {
		if (alpha <= 0.01f || r <= 0) return;
		int n = Math.max(16, (int) (r * 10));
		for (int i = 0; i < n; i++) {
			double a0 = 2 * Math.PI * i / n, a1 = 2 * Math.PI * (i + 1) / n;
			float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
			quad(m, vc, v(x + c0 * r, y, z + s0 * r), v(x + c1 * r, y, z + s1 * r), v(x + c1 * (r + w), y, z + s1 * (r + w)),
					v(x + c0 * (r + w), y, z + s0 * (r + w)), rgb, alpha);
		}
	}

	/** Disco com o centro mais forte e a borda suave. */
	static void disc(Matrix4f m, VertexConsumer vc, float x, float y, float z, float r, int rgb, float centerAlpha, float edgeAlpha) {
		if (r <= 0) return;
		int n = Math.max(16, (int) (r * 8)), rings = 4;
		for (int k = 0; k < rings; k++) {
			float r0 = r * k / rings, r1 = r * (k + 1) / rings;
			float al = MathHelper.lerp((k + 0.5f) / rings, centerAlpha, edgeAlpha);
			for (int i = 0; i < n; i++) {
				double a0 = 2 * Math.PI * i / n, a1 = 2 * Math.PI * (i + 1) / n;
				float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
				quad(m, vc, v(x + c0 * r0, y, z + s0 * r0), v(x + c1 * r0, y, z + s1 * r0), v(x + c1 * r1, y, z + s1 * r1),
						v(x + c0 * r1, y, z + s0 * r1), rgb, al);
			}
		}
	}

	/** Língua de chama: duas fitas cruzadas que afinam e ondulam, do tom da base ao da ponta. */
	static void flame(Matrix4f m, VertexConsumer vc, float x, float y, float z, float w, float h, float phase, int tip, int base, float alpha) {
		if (h <= 0.02f || alpha <= 0.01f) return;
		int seg = 5;
		for (int side = 0; side < 2; side++) {
			float dx = side == 0 ? 1 : 0, dz = side == 0 ? 0 : 1;
			for (int i = 0; i < seg; i++) {
				float t0 = i / (float) seg, t1 = (i + 1) / (float) seg;
				float w0 = w * (1 - t0) * (1 - t0 * 0.3f), w1 = w * (1 - t1) * (1 - t1 * 0.3f);
				float o0 = MathHelper.sin(phase + t0 * 4) * 0.12f * t0, o1 = MathHelper.sin(phase + t1 * 4) * 0.12f * t1;
				int col = mix(base, tip, (t0 + t1) / 2);
				quad(m, vc, v(x + o0 - dx * w0, y + t0 * h, z - dz * w0), v(x + o0 + dx * w0, y + t0 * h, z + dz * w0),
						v(x + o1 + dx * w1, y + t1 * h, z + dz * w1), v(x + o1 - dx * w1, y + t1 * h, z - dz * w1), col, alpha * (1 - t0 * 0.6f));
			}
		}
	}

	/** Rachadura no chão: uma fita em zigue-zague saindo do centro. */
	static void crack(Matrix4f m, VertexConsumer vc, float x, float y, float z, float angle, float len, int rgb, float alpha) {
		int seg = 6;
		float px = x, pz = z;
		for (int i = 1; i <= seg; i++) {
			float t = i / (float) seg;
			float a = angle + (float) Math.sin(i * 1.7 + angle * 3) * 0.35f;
			float nx = x + MathHelper.cos(a) * len * t, nz = z + MathHelper.sin(a) * len * t;
			float w = 0.12f * (1 - t) + 0.03f;
			float ox = -(nz - pz), oz = nx - px;
			float l = MathHelper.sqrt(ox * ox + oz * oz) + 1e-4f;
			ox = ox / l * w;
			oz = oz / l * w;
			quad(m, vc, v(px - ox, y, pz - oz), v(px + ox, y, pz + oz), v(nx + ox, y, nz + oz), v(nx - ox, y, nz - oz), rgb, alpha * (1 - t * 0.7f));
			px = nx;
			pz = nz;
		}
	}

	private static int mix(int a, int b, float t) {
		int r = (int) MathHelper.lerp(t, a >> 16 & 255, b >> 16 & 255);
		int g = (int) MathHelper.lerp(t, a >> 8 & 255, b >> 8 & 255);
		int bl = (int) MathHelper.lerp(t, a & 255, b & 255);
		return r << 16 | g << 8 | bl;
	}
}
