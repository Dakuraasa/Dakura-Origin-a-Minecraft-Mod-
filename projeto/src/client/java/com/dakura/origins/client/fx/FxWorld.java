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
		GOLD_TRAIL, SHIELD_FLASH, DUST, WARD_BURST, PUNCH, PUNCH_FINAL, BOOM_MANA, FIRE_FIELD, FLOWER_CIRCLE, BLOOM }

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
	/** Penas-adaga cravadas: {id da entidade, x, y, z (frações do corpo), giro, inclinação, tempo restante}. */
	private static final List<float[]> STUCK = new ArrayList<>();
	/** Laço Áureo: quem segura → alvo. */
	private static final java.util.Map<Integer, Integer> TETHER = new java.util.HashMap<>();
	private static final net.minecraft.util.Identifier SHIELD_TEX = com.dakura.origins.DakuraOrigins.id("textures/entity/golden_shield.png");
	private static final int SHIELD_FRAMES = 60, SHIELD_GRID = 8;
	private static final List<Debris> DEBRIS = new ArrayList<>();
	private static final int MAX_DEBRIS = 220;
	private static final net.minecraft.util.Identifier LILY_TEX = com.dakura.origins.DakuraOrigins.id("textures/entity/lily_pad.png");
	/** Estados que desenham algo contínuo em volta do jogador. */
	private static final String[] FLAGGED = {"mirror_shield", "gold_ward", "mana_orbs", "solar_dive", "lily_pad"};

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
			case "boom_green" -> SHAPES.add(new Shape(Kind.BOOM_GREEN, p, a, 16, 1, null, rnd.nextFloat() * 100));
			case "boom_poison" -> SHAPES.add(new Shape(Kind.BOOM_POISON, p, a, 18, 1, null, rnd.nextFloat() * 100));
			case "crater" -> SHAPES.add(new Shape(Kind.CRATER, p, a, (int) b, 4, null, rnd.nextFloat() * 100));
			case "spores" -> SHAPES.add(new Shape(Kind.SPORES, p, a, (int) b, 1, null, rnd.nextFloat() * 100));
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
			case "boom_mana" -> SHAPES.add(new Shape(Kind.BOOM_MANA, p, a, 16, 1, null, rnd.nextFloat() * 100));
			case "fire_field" -> {
				Shape sh = new Shape(Kind.FIRE_FIELD, p, a, (int) b, 10, null, rnd.nextFloat() * 100);
				sh.param = c;
				SHAPES.add(sh);
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
			case "charge_green" -> SHAPES.add(new Shape(Kind.CHARGE, e.getPos(), 2.2f, param, 1, e, rnd.nextFloat() * 100));
			case "dive_green" -> SHAPES.add(new Shape(Kind.DIVE, e.getPos(), 1.4f, 60, 1, e, rnd.nextFloat() * 100));
			case "grab_break" -> SHAPES.add(new Shape(Kind.BREAK, e.getPos(), 2.5f, 12, 1, e, rnd.nextFloat() * 100));
			case "feather_stuck" -> {
				Random r = Random.create(param);
				if (STUCK.size() < 160) STUCK.add(new float[]{e.getId(), r.nextFloat() - 0.5f, 0.3f + r.nextFloat() * 0.55f,
						r.nextFloat() - 0.5f, r.nextFloat() * 360f, -20f - r.nextFloat() * 40f, 200});
			}
			case "physgun" -> TETHER.put(e.getId(), param);
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
		}
		STUCK.removeIf(f -> --f[6] <= 0 || mc.world.getEntityById((int) f[0]) == null);
		TETHER.entrySet().removeIf(en -> {
			Entity c = mc.world.getEntityById(en.getKey());
			return c == null || !com.dakura.origins.power.EntityFlags.clientHas(c, "physgun");
		});
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

	private static void render(WorldRenderContext ctx) {
		if (SHAPES.isEmpty() && DEBRIS.isEmpty() && STUCK.isEmpty() && TETHER.isEmpty() && !anyFlagged(ctx.world())) return;
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

		if (!STUCK.isEmpty()) {
			VertexConsumer fv = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(com.dakura.origins.client.render.FeatherRenderer.TEX));
			for (float[] f : STUCK) {
				if (!(world.getEntityById((int) f[0]) instanceof net.minecraft.entity.LivingEntity le)) continue;
				Vec3d p = le.getLerpedPos(pt);
				float by = MathHelper.lerpAngleDegrees(pt, le.prevBodyYaw, le.bodyYaw);
				ms.push();
				ms.translate(p.x - cam.x, p.y - cam.y, p.z - cam.z);
				ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-by));
				ms.translate(f[1] * le.getWidth() * 0.9, f[2] * le.getHeight(), f[3] * le.getWidth() * 0.9);
				ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(f[4]));
				ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-f[5]));
				ms.translate(0.18, 0, 0);
				int light = WorldRenderer.getLightmapCoordinates(world, le.getBlockPos());
				com.dakura.origins.client.render.FeatherRenderer.draw(ms, fv, light, 0.26f);
				ms.pop();
			}
		}

		// Égide Espelhada: o círculo rúnico animado (GIF do Artur) como escudo emissivo à frente
		for (net.minecraft.entity.player.PlayerEntity pl : world.getPlayers()) {
			if (!com.dakura.origins.power.EntityFlags.clientHas(pl, "mirror_shield")) continue;
			Vec3d eye = pl.getLerpedPos(pt).add(0, pl.getStandingEyeHeight(), 0);
			float yaw = pl.getYaw(pt), pitch = pl.getPitch(pt);
			Vec3d look = Vec3d.fromPolar(pitch, yaw);
			Vec3d c = eye.add(look.multiply(1.4)).add(0, -0.35, 0);
			long ms0 = net.minecraft.util.Util.getMeasuringTimeMs();
			int frame = (int) ((ms0 / 70) % SHIELD_FRAMES);
			float u0 = (frame % SHIELD_GRID) / (float) SHIELD_GRID, v0 = (frame / SHIELD_GRID) / (float) SHIELD_GRID;
			float du = 1f / SHIELD_GRID;
			float pulse = 1f + 0.04f * MathHelper.sin(ms0 * 0.006f);
			float half = 1.55f * pl.getScale() * pulse;
			ms.push();
			ms.translate(c.x - cam.x, c.y - cam.y, c.z - cam.z);
			ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-yaw));
			ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
			VertexConsumer sv = vcp.getBuffer(RenderLayer.getEyes(SHIELD_TEX));
			MatrixStack.Entry en = ms.peek();
			Matrix4f mt = en.getPositionMatrix();
			for (int side = 0; side < 2; side++) {
				float z = side == 0 ? 0 : -0.01f;
				sv.vertex(mt, -half, -half, z).color(255, 255, 255, 255).texture(u0, v0 + du).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(en, 0, 0, -1);
				sv.vertex(mt, half, -half, z).color(255, 255, 255, 255).texture(u0 + du, v0 + du).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(en, 0, 0, -1);
				sv.vertex(mt, half, half, z).color(255, 255, 255, 255).texture(u0 + du, v0).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(en, 0, 0, -1);
				sv.vertex(mt, -half, half, z).color(255, 255, 255, 255).texture(u0, v0).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(en, 0, 0, -1);
				if (side == 0) {
					// verso
					sv.vertex(mt, -half, half, z).color(255, 255, 255, 255).texture(u0, v0).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(en, 0, 0, 1);
					sv.vertex(mt, half, half, z).color(255, 255, 255, 255).texture(u0 + du, v0).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(en, 0, 0, 1);
					sv.vertex(mt, half, -half, z).color(255, 255, 255, 255).texture(u0 + du, v0 + du).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(en, 0, 0, 1);
					sv.vertex(mt, -half, -half, z).color(255, 255, 255, 255).texture(u0, v0 + du).overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(en, 0, 0, 1);
				}
			}
			ms.pop();
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

		if (!SHAPES.isEmpty() || !TETHER.isEmpty() || anyFlagged(world)) {
			VertexConsumer glow = vcp.getBuffer(RenderLayer.getLightning());
			ms.push();
			ms.translate(-cam.x, -cam.y, -cam.z);
			Matrix4f m = ms.peek().getPositionMatrix();
			for (Shape s : SHAPES) draw(m, glow, s, pt);
			for (var en : TETHER.entrySet()) tether(m, glow, world, en.getKey(), en.getValue(), pt);
			for (net.minecraft.entity.player.PlayerEntity pl : world.getPlayers()) {
				if (com.dakura.origins.power.EntityFlags.clientHas(pl, "gold_ward")) ward(m, glow, pl, pt);
				if (com.dakura.origins.power.EntityFlags.clientHas(pl, "mirror_shield")) shieldGlow(m, glow, pl, pt);
				if (com.dakura.origins.power.EntityFlags.clientHas(pl, "mana_orbs")) manaOrbs(m, glow, pl, pt);
				if (com.dakura.origins.power.EntityFlags.clientHas(pl, "solar_dive")) phoenixBlaze(m, glow, pl, pt);
			}
			ms.pop();
			if (vcp instanceof VertexConsumerProvider.Immediate imm) imm.draw(RenderLayer.getLightning());
		}
	}

	private static float easeOut(float t) {
		t = MathHelper.clamp(t, 0, 1);
		return 1 - (1 - t) * (1 - t) * (1 - t);
	}

	private static void draw(Matrix4f m, VertexConsumer vc, Shape s, float pt) {
		float age = s.age + pt;
		float t = age / s.life;
		float fadeOut = MathHelper.clamp((s.life - age) / Math.min(20f, s.life * 0.3f), 0, 1);
		Vec3d p = s.follow != null ? s.follow.getLerpedPos(pt) : s.pos;
		float x = (float) p.x, y = (float) p.y, z = (float) p.z;
		switch (s.kind) {
			case ZONE_POISON -> {
				float r = s.radius * easeOut(age / s.grow);
				float pulse = 0.85f + 0.15f * MathHelper.sin(age * 0.15f + s.seed);
				disc(m, vc, x, y + 0.06f, z, r, 0x7A2BC8, 0.42f * fadeOut * pulse, 0.08f * fadeOut);
				ring(m, vc, x, y + 0.08f, z, r, 0.35f, 0xC07CFF, 0.6f * fadeOut);
				ring(m, vc, x, y + 0.1f + (age * 0.03f % 1.2f), z, r * (0.4f + (age * 0.01f % 0.6f)), 0.12f, 0xB066FF, 0.35f * fadeOut);
				// bolhas que sobem, incham e estouram
				for (int i = 0; i < 14; i++) {
					float ph = (age * 0.025f + i * 0.137f + s.seed) % 1f;
					double a = i * 2.39996 + s.seed;
					float d = r * (0.15f + 0.8f * ((i * 0.618f) % 1f));
					float bx = x + (float) Math.cos(a) * d, bz = z + (float) Math.sin(a) * d;
					float by = y + 0.1f + ph * 1.4f;
					float br = 0.12f + 0.22f * ph;
					sphere(m, vc, bx, by, bz, br, 0xD9A6FF, (1 - ph) * 0.55f * fadeOut);
				}
			}
			case ZONE_FIRE -> {
				float r = s.radius * easeOut(age / s.grow);
				disc(m, vc, x, y + 0.05f, z, r, 0x1F8F2A, 0.45f * fadeOut, 0.05f * fadeOut);
				ring(m, vc, x, y + 0.07f, z, r, 0.4f, 0x7CFF6A, 0.7f * fadeOut);
				int n = (int) (r * 6);
				for (int i = 0; i < n; i++) {
					double a = i * 2.39996 + s.seed;
					float d = r * (float) Math.sqrt(((i * 0.618f) % 1f));
					float fx = x + (float) Math.cos(a) * d, fz = z + (float) Math.sin(a) * d;
					float h = (0.5f + 0.6f * ((i * 0.37f) % 1f)) * (0.8f + 0.25f * MathHelper.sin(age * 0.6f + i));
					flame(m, vc, fx, y, fz, 0.25f, h * fadeOut, age * 0.1f + i, 0x9BFF6E, 0x0E6B1E, 0.85f * fadeOut);
				}
			}
			case BOOM_GREEN -> {
				float e = easeOut(t);
				sphere(m, vc, x, y, z, s.radius * (0.3f + 0.9f * e), 0x6BFF5A, (1 - t) * 0.6f);
				sphere(m, vc, x, y, z, s.radius * 0.5f * (1 - t * 0.5f), 0xE6FFD0, (1 - t) * 0.9f);
				ring(m, vc, x, y - 0.4f, z, s.radius * (0.4f + 1.4f * e), 0.5f, 0x8CFF7A, (1 - t) * 0.8f);
				for (int i = 0; i < 10; i++) {
					double a = i * Math.PI / 5 + s.seed;
					float d = s.radius * 0.6f * e;
					flame(m, vc, x + (float) Math.cos(a) * d, y - 0.5f, z + (float) Math.sin(a) * d, 0.35f,
							s.radius * 0.8f * (1 - t), i + age, 0xC8FFB0, 0x1C8A28, (1 - t));
				}
			}
			case BOOM_POISON -> {
				float e = easeOut(t);
				sphere(m, vc, x, y, z, s.radius * (0.3f + 0.8f * e), 0xA052F0, (1 - t) * 0.55f);
				ring(m, vc, x, y - 0.5f, z, s.radius * (0.3f + 1.2f * e), 0.45f, 0xD6A8FF, (1 - t) * 0.8f);
				for (int i = 0; i < 9; i++) {
					double a = i * 0.698 + s.seed;
					float d = s.radius * 0.7f * e;
					sphere(m, vc, x + (float) Math.cos(a) * d, y + 0.3f + e, z + (float) Math.sin(a) * d, 0.3f + 0.3f * (1 - t),
							0xE2C2FF, (1 - t) * 0.6f);
				}
			}
			case CRATER -> {
				float e = easeOut(age / s.grow);
				disc(m, vc, x, y + 0.04f, z, s.radius * e, 0x0B3D12, 0.5f * fadeOut, 0.3f * fadeOut);
				ring(m, vc, x, y + 0.06f, z, s.radius * e, 0.6f, 0x3FB548, 0.5f * fadeOut);
				for (int i = 0; i < 8; i++) {
					double a = i * Math.PI / 4 + s.seed;
					crack(m, vc, x, y + 0.05f, z, (float) a, s.radius * 1.25f * e, 0x6BFF5A, 0.5f * fadeOut);
				}
			}
			case SPORES -> {
				for (int i = 0; i < 18; i++) {
					float ph = (t + i * 0.055f) % 1f;
					double a = i * 2.39996 + s.seed + age * 0.02;
					float d = s.radius * (0.2f + 0.8f * ph);
					sphere(m, vc, x + (float) Math.cos(a) * d, y + ph * 3.5f, z + (float) Math.sin(a) * d, 0.12f + 0.1f * (1 - ph),
							i % 3 == 0 ? 0xFFE7B0 : 0xFF8A7A, (1 - ph) * 0.7f * fadeOut);
				}
			}
			case CHARGE -> {
				float pulse = 0.5f + 0.5f * MathHelper.sin(age * (0.3f + t * 0.8f));
				ring(m, vc, x, y + 0.1f, z, s.radius * (1.2f - t * 0.5f), 0.25f, 0x7CFF6A, 0.4f + 0.5f * pulse);
				ring(m, vc, x, y + 1.2f + t, z, s.radius * 0.7f, 0.15f, 0xC8FFB0, 0.3f + 0.4f * t);
				sphere(m, vc, x, y + 1.0f, z, 0.6f + 0.6f * t, 0x6BFF5A, 0.12f + 0.3f * t * pulse);
			}
			case DIVE -> {
				for (int i = 0; i < 6; i++) {
					float off = i * 0.9f;
					sphere(m, vc, x, y + 1.2f + off, z, s.radius * (1 - i * 0.12f), i % 2 == 0 ? 0x6BFF5A : 0xD8FFC8, 0.5f - i * 0.07f);
				}
			}
			case BREAK -> ring(m, vc, x, y + 1f, z, s.radius * easeOut(t) * 1.5f, 0.3f, 0xFFFFFF, (1 - t));
			case BOOM_GOLD -> {
				float e = easeOut(t);
				sphere(m, vc, x, y, z, s.radius * (0.3f + 0.8f * e), 0xFFC94A, (1 - t) * 0.6f);
				sphere(m, vc, x, y, z, s.radius * 0.4f * (1 - t * 0.5f), 0xFFF6C8, (1 - t) * 0.9f);
				ring(m, vc, x, y - 0.3f, z, s.radius * (0.3f + 1.3f * e), 0.35f, 0xFFE27A, (1 - t) * 0.8f);
			}
			case BOOM_ARCANE -> {
				float e = easeOut(t);
				sphere(m, vc, x, y, z, s.radius * (0.3f + 0.9f * e), 0x7FD6FF, (1 - t) * 0.55f);
				sphere(m, vc, x, y, z, s.radius * 0.4f * (1 - t * 0.5f), 0xEAF8FF, (1 - t) * 0.9f);
				ring(m, vc, x, y, z, s.radius * (0.4f + 1.2f * e), 0.3f, 0xB8E8FF, (1 - t) * 0.8f);
				for (int i = 0; i < 8; i++) {
					double a = i * Math.PI / 4 + s.seed;
					float d = s.radius * 1.2f * e;
					sphere(m, vc, x + (float) Math.cos(a) * d, y + 0.2f * MathHelper.sin(i), z + (float) Math.sin(a) * d, 0.12f * (1 - t), 0xDDF4FF, 1 - t);
				}
			}
			case GOLD_TRAIL -> {
				Vec3d v = s.follow != null ? s.follow.getVelocity() : Vec3d.ZERO;
				for (int i = 0; i < 6; i++) {
					Vec3d q = p.add(0, s.follow != null ? s.follow.getHeight() * 0.5 : 0.5, 0).subtract(v.multiply(i * 0.6));
					sphere(m, vc, (float) q.x, (float) q.y, (float) q.z, s.radius * (1 - i / 7f), i % 2 == 0 ? 0xFFD45E : 0xFFF6C8,
							(1 - t) * (0.6f - i * 0.08f));
				}
			}
			case SHIELD_FLASH -> {
				Vec3d look = s.follow != null ? s.follow.getRotationVec(pt) : new Vec3d(0, 0, 1);
				Vec3d c = p.add(0, s.follow != null ? s.follow.getStandingEyeHeight() - 0.35 : 1.2, 0).add(look.multiply(1.4));
				sphere(m, vc, (float) c.x, (float) c.y, (float) c.z, s.radius * (0.4f + 0.8f * easeOut(t)), 0xFFF1B0, (1 - t) * 0.7f);
			}
			case DUST -> {
				float e = easeOut(t);
				for (int i = 0; i < 16; i++) {
					double a = i * Math.PI / 8 + s.seed;
					float d = s.radius * (0.3f + 0.9f * e);
					sphere(m, vc, x + (float) Math.cos(a) * d, y + 0.25f + 0.5f * e * ((i % 3) / 2f), z + (float) Math.sin(a) * d,
							0.35f + 0.4f * e, 0x6E5B45, (1 - t) * 0.22f);
				}
				ring(m, vc, x, y + 0.05f, z, s.radius * (0.3f + 1.1f * e), 0.5f, 0x8C7356, (1 - t) * 0.35f);
			}
			case WARD_BURST -> {
				float e = easeOut(t);
				sphere(m, vc, x, y + 1f, z, s.radius * (0.3f + 0.7f * e), 0xFFD45E, (1 - t) * 0.45f);
				ring(m, vc, x, y + 0.1f, z, s.radius * (0.4f + 1.1f * e), 0.3f, 0xFFF1B0, (1 - t) * 0.9f);
			}
			case PUNCH, PUNCH_FINAL -> punch(m, vc, s, x, y, z, t);
			case BOOM_MANA -> {
				float e = easeOut(t);
				sphere(m, vc, x, y, z, s.radius * (0.3f + 0.85f * e), 0x4FA8FF, (1 - t) * 0.5f);
				sphere(m, vc, x, y, z, s.radius * 0.45f * (1 - t * 0.6f), 0xE8F6FF, (1 - t) * 0.95f);
				ring(m, vc, x, y, z, s.radius * (0.4f + 1.25f * e), 0.28f, 0x8FD0FF, (1 - t) * 0.85f);
				vring(m, vc, x, y, z, 1, 0, s.radius * (0.3f + 1.1f * e), 0.18f, 0xB59CFF, (1 - t) * 0.6f);
				vring(m, vc, x, y, z, 0, 1, s.radius * (0.3f + 1.0f * e), 0.18f, 0x9FE6FF, (1 - t) * 0.6f);
				for (int i = 0; i < 12; i++) {
					double a = i * 2.39996 + s.seed;
					float d = s.radius * 1.3f * e, up = (float) Math.sin(i * 1.3) * 0.6f * e;
					sphere(m, vc, x + (float) Math.cos(a) * d, y + up, z + (float) Math.sin(a) * d, 0.1f * (1 - t) + 0.03f, 0xDDF4FF, 1 - t);
				}
			}
			case FIRE_FIELD -> fireField(m, vc, s, x, y, z, age, fadeOut);
			case FLOWER_CIRCLE -> flowerCircle(m, vc, s, x, y, z, age, fadeOut);
			case BLOOM -> {
				float e = easeOut(t);
				for (int i = 0; i < 8; i++) {
					double a = i * Math.PI / 4 + s.seed + age * 0.08;
					float d = s.radius * (0.6f + 0.4f * e);
					sphere(m, vc, x + (float) Math.cos(a) * d, y + 0.2f + e * 1.6f, z + (float) Math.sin(a) * d, 0.08f,
							i % 2 == 0 ? 0xFFB7E3 : 0xC8FFB0, (1 - t) * 0.9f);
				}
				ring(m, vc, x, y + 0.1f + e * 0.8f, z, s.radius * (0.5f + 0.6f * e), 0.08f, 0xC8FFB0, (1 - t) * 0.7f);
			}
		}
	}

	/** Soco: clarão no ponto de contato, onda de choque de frente para o golpe e faíscas espirrando para trás do alvo. */
	private static void punch(Matrix4f m, VertexConsumer vc, Shape s, float x, float y, float z, float t) {
		boolean fin = s.kind == Kind.PUNCH_FINAL;
		float e = easeOut(t), k = 1 - t;
		float r = s.radius;
		int core = fin ? 0xFFFFFF : 0xFFF6E0, hot = fin ? 0xFFC94A : 0xFFE29A, wave = fin ? 0xFF9F4A : 0xFFF1C0;
		sphere(m, vc, x, y, z, r * (0.35f + 0.5f * e), hot, k * 0.6f);
		sphere(m, vc, x, y, z, r * 0.3f * (1 - t * 0.7f), core, k);
		vring(m, vc, x, y, z, s.dx, s.dz, r * (0.3f + 1.4f * e), fin ? 0.22f : 0.08f, wave, k * 0.9f);
		if (fin) {
			vring(m, vc, x + s.dx * 0.6f, y, z + s.dz * 0.6f, s.dx, s.dz, r * (0.2f + 2.2f * e), 0.14f, 0xFFF1B0, k * 0.7f);
			ring(m, vc, x, y - 0.9f, z, r * (0.3f + 2.2f * e), 0.35f, 0xFFC94A, k * 0.6f);
		}
		int n = fin ? 18 : 7;
		for (int i = 0; i < n; i++) {
			// faíscas saem num cone, na direção do soco
			double a = i * 2.39996 + s.seed + s.param;
			float spread = fin ? 1.1f : 0.7f;
			float ox = (float) Math.cos(a) * spread, oy = (float) Math.sin(a * 1.7) * spread;
			float d = r * (0.4f + (fin ? 3.2f : 1.8f) * e);
			float px = x + (s.dx + -s.dz * ox) * d, py = y + oy * d * 0.7f, pz = z + (s.dz + s.dx * ox) * d;
			dot(m, vc, px, py, pz, (fin ? 0.09f : 0.05f) * k + 0.02f, i % 3 == 0 ? 0xFFFFFF : hot, k);
		}
	}

	/** Chamas que tomam o chão em volta do impacto: brilho no solo, línguas de fogo, brasas subindo e fagulhas. */
	private static void fireField(Matrix4f m, VertexConsumer vc, Shape s, float x, float y, float z, float age, float fadeOut) {
		float grow = easeOut(age / s.grow);
		float r = s.radius * grow;
		boolean holy = s.param > 0.5f;
		int tip = holy ? 0xFFF1A0 : 0xFFD27A, mid = 0xFF8A2A, base = holy ? 0xD94A10 : 0xB0200C;
		disc(m, vc, x, y + 0.04f, z, r, 0xFF6A1A, 0.32f * fadeOut, 0.04f * fadeOut);
		ring(m, vc, x, y + 0.06f, z, r * 0.98f, 0.25f, 0xFF9F4A, 0.45f * fadeOut * (0.8f + 0.2f * MathHelper.sin(age * 0.4f)));
		int n = (int) (s.radius * 7);
		for (int i = 0; i < n; i++) {
			double a = i * 2.39996 + s.seed;
			float d = r * (float) Math.sqrt(((i * 0.618f + s.seed) % 1f));
			float fx = x + (float) Math.cos(a) * d, fz = z + (float) Math.sin(a) * d;
			float h = (0.45f + 0.75f * ((i * 0.37f) % 1f)) * (0.75f + 0.3f * MathHelper.sin(age * 0.7f + i * 1.3f)) * (1 - d / (r + 0.01f) * 0.4f);
			flame(m, vc, fx, y, fz, 0.18f + 0.1f * ((i * 0.53f) % 1f), h * fadeOut, age * 0.25f + i, tip, i % 2 == 0 ? base : mid, 0.85f * fadeOut);
		}
		// brasas que sobem, oscilam e se apagam
		for (int i = 0; i < n; i++) {
			float ph = (age * 0.03f + i * 0.173f + s.seed) % 1f;
			double a = i * 1.9 + s.seed;
			float d = r * ((i * 0.41f) % 1f);
			float ex = x + (float) Math.cos(a) * d + MathHelper.sin(age * 0.2f + i) * 0.15f * ph;
			float ez = z + (float) Math.sin(a) * d + MathHelper.cos(age * 0.2f + i) * 0.15f * ph;
			dot(m, vc, ex, y + 0.2f + ph * 2.6f, ez, 0.045f * (1 - ph) + 0.015f, ph < 0.4f ? 0xFFE08A : 0xFF7A2A, (1 - ph) * 0.95f * fadeOut);
		}
		// calor: um véu fraco ondulando acima das chamas
		for (int k = 0; k < 3; k++)
			ring(m, vc, x, y + 0.9f + k * 0.5f + 0.1f * MathHelper.sin(age * 0.15f + k), z, r * (0.7f - k * 0.15f), 0.4f, 0xFFB060, 0.06f * fadeOut);
	}

	/** Santuário da Fada: círculo mágico girando, runas, campo de flores encantadas desabrochando e uma parede de luz suave. */
	private static void flowerCircle(Matrix4f m, VertexConsumer vc, Shape s, float x, float y, float z, float age, float fadeOut) {
		float g = easeOut(age / s.grow);
		float r = s.radius * g;
		disc(m, vc, x, y + 0.03f, z, r, 0x9CFF8A, 0.16f * fadeOut, 0.05f * fadeOut);
		ring(m, vc, x, y + 0.05f, z, r, 0.22f, 0xE9FFB0, 0.75f * fadeOut);
		ring(m, vc, x, y + 0.05f, z, r * 0.82f, 0.08f, 0xFFD6F2, 0.6f * fadeOut);
		ring(m, vc, x, y + 0.05f, z, r * 0.45f, 0.06f, 0xE9FFB0, 0.5f * fadeOut);
		// runas: pontos de luz girando entre os anéis
		for (int i = 0; i < 16; i++) {
			double a = i * Math.PI / 8 + age * 0.015;
			float rr = r * 0.91f;
			dot(m, vc, x + (float) Math.cos(a) * rr, y + 0.08f, z + (float) Math.sin(a) * rr, 0.07f, i % 2 == 0 ? 0xFFF6B0 : 0xFFC4EC, 0.85f * fadeOut);
			double b = -i * Math.PI / 8 - age * 0.025;
			float r2 = r * 0.63f;
			dot(m, vc, x + (float) Math.cos(b) * r2, y + 0.07f, z + (float) Math.sin(b) * r2, 0.05f, 0xD8FFC0, 0.7f * fadeOut);
		}
		// flores: cinco pétalas e miolo, desabrochando uma a uma
		int n = (int) (s.radius * s.radius * 1.1f);
		int[] petals = {0xFFB7E3, 0xFFFFFF, 0xC9B8FF, 0xFFE07A, 0xFF9FB5};
		for (int i = 0; i < n; i++) {
			float open = MathHelper.clamp((age - i * 0.6f) / 14f, 0, 1);
			if (open <= 0) continue;
			double a = i * 2.39996 + s.seed;
			float d = r * 0.95f * (float) Math.sqrt((i + 0.5f) / n);
			float fx = x + (float) Math.cos(a) * d, fz = z + (float) Math.sin(a) * d;
			float sway = MathHelper.sin(age * 0.08f + i) * 0.03f;
			float fy = y + 0.12f + 0.06f * open;
			int col = petals[i % petals.length];
			float pr = 0.09f * easeOut(open);
			for (int k = 0; k < 5; k++) {
				double pa = k * Math.PI * 2 / 5 + i;
				dot(m, vc, fx + sway + (float) Math.cos(pa) * pr * 1.3f, fy, fz + (float) Math.sin(pa) * pr * 1.3f, pr, col, 0.8f * fadeOut);
			}
			dot(m, vc, fx + sway, fy + 0.02f, fz, pr * 0.7f, 0xFFE27A, 0.95f * fadeOut);
		}
		// pólen subindo
		for (int i = 0; i < 20; i++) {
			float ph = (age * 0.02f + i * 0.13f) % 1f;
			double a = i * 2.39996 + s.seed + age * 0.01;
			float d = r * ((i * 0.618f) % 1f);
			dot(m, vc, x + (float) Math.cos(a) * d, y + 0.2f + ph * 2.2f, z + (float) Math.sin(a) * d, 0.04f, 0xFFF6C0, (1 - ph) * 0.8f * fadeOut);
		}
		// parede de luz na borda: os monstros não passam
		for (int k = 0; k < 6; k++) {
			float hy = k * 0.35f + (age * 0.02f % 0.35f);
			ring(m, vc, x, y + hy, z, r, 0.05f, 0xD8FFC0, (1 - k / 6f) * 0.28f * fadeOut);
		}
	}

	/** Esferas de mana orbitando a Kitsune, cada uma com rastro de cometa. */
	private static void manaOrbs(Matrix4f m, VertexConsumer vc, net.minecraft.entity.player.PlayerEntity pl, float pt) {
		int n = pl instanceof net.minecraft.client.network.AbstractClientPlayerEntity ap
				? com.dakura.origins.client.render.GeoPartsRenderer.kitsuneTails(ap) : 3;
		Vec3d p = pl.getLerpedPos(pt);
		float t = pl.getWorld().getTime() + pt;
		float h = pl.getHeight() * 0.55f;
		for (int i = 0; i < n; i++) {
			for (int j = 7; j >= 0; j--) {
				double a = (t - j * 0.9f) * 0.15f + i * Math.PI * 2 / n;
				float ox = (float) Math.cos(a) * 1.3f, oz = (float) Math.sin(a) * 1.3f, oy = MathHelper.sin((float) a * 2) * 0.15f;
				float k = 1 - j / 8f;
				if (j == 0) {
					sphere(m, vc, (float) p.x + ox, (float) p.y + h + oy, (float) p.z + oz, 0.2f, 0x4FA8FF, 0.45f);
					sphere(m, vc, (float) p.x + ox, (float) p.y + h + oy, (float) p.z + oz, 0.11f, 0xF0FAFF, 0.95f);
				} else dot(m, vc, (float) p.x + ox, (float) p.y + h + oy, (float) p.z + oz, 0.15f * k, j % 2 == 0 ? 0x7FC4FF : 0xB59CFF, 0.45f * k);
			}
		}
	}

	/** Fênix mergulhando: o corpo envolto em labaredas. */
	private static void phoenixBlaze(Matrix4f m, VertexConsumer vc, net.minecraft.entity.player.PlayerEntity pl, float pt) {
		Vec3d p = pl.getLerpedPos(pt);
		float t = pl.age + pt;
		for (int i = 0; i < 10; i++) {
			double a = i * Math.PI / 5 + t * 0.2;
			float d = 0.45f;
			flame(m, vc, (float) p.x + (float) Math.cos(a) * d, (float) p.y + 0.2f, (float) p.z + (float) Math.sin(a) * d, 0.22f,
					1.2f + 0.4f * MathHelper.sin(t * 0.8f + i), t * 0.4f + i, 0xFFE08A, 0xD9300C, 0.8f);
		}
		sphere(m, vc, (float) p.x, (float) p.y + 1f, (float) p.z, 1.1f, 0xFF8A2A, 0.25f);
	}

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

	private static boolean anyFlagged(ClientWorld w) {
		if (w == null) return false;
		for (net.minecraft.entity.player.PlayerEntity pl : w.getPlayers())
			if (com.dakura.origins.power.EntityFlags.clientHas(pl, "mirror_shield") || com.dakura.origins.power.EntityFlags.clientHas(pl, "gold_ward"))
				return true;
		return false;
	}

	/** Laço Áureo (estilo Physgun): feixe dourado curvo da mão até o alvo + aura de contenção em volta dele. */
	private static void tether(Matrix4f m, VertexConsumer vc, ClientWorld w, int casterId, int targetId, float pt) {
		Entity c = w.getEntityById(casterId), tg = w.getEntityById(targetId);
		if (c == null || tg == null) return;
		float t = (w.getTime() % 100000) + pt;
		Vec3d look = c.getRotationVec(pt);
		Vec3d side = new Vec3d(-look.z, 0, look.x);
		side = side.lengthSquared() < 1e-4 ? new Vec3d(1, 0, 0) : side.normalize();
		Vec3d hand = c.getLerpedPos(pt).add(0, c.getStandingEyeHeight() - 0.45, 0).add(look.multiply(0.7)).add(side.multiply(-0.4));
		Vec3d to = tg.getLerpedPos(pt).add(0, tg.getHeight() * 0.5, 0);
		double dist = hand.distanceTo(to);
		// a curva sai na direção do olhar e "cai" até o alvo, como o feixe da Physgun
		Vec3d ctrl = hand.add(look.multiply(dist * 0.55));
		int n = (int) MathHelper.clamp(dist * 6, 10, 70);
		for (int i = 0; i <= n; i++) {
			double k = i / (double) n;
			Vec3d a = hand.multiply((1 - k) * (1 - k)).add(ctrl.multiply(2 * k * (1 - k))).add(to.multiply(k * k));
			float wob = 0.03f * MathHelper.sin((float) (k * 18 - t * 0.5));
			float r = 0.07f + 0.03f * MathHelper.sin((float) (k * 30 - t * 0.9));
			sphere(m, vc, (float) a.x, (float) a.y + wob, (float) a.z, r, 0xFFD45E, 0.55f);
			if (i % 6 == (int) (t * 0.5) % 6) sphere(m, vc, (float) a.x, (float) a.y, (float) a.z, 0.16f, 0xFFF6C8, 0.7f);
		}
		float rr = Math.max(0.7f, tg.getWidth() * 0.9f);
		float bx = (float) to.x, by = (float) tg.getLerpedPos(pt).y, bz = (float) to.z;
		sphere(m, vc, bx, (float) to.y, bz, Math.max(rr, tg.getHeight() * 0.6f), 0xFFC94A, 0.16f + 0.05f * MathHelper.sin(t * 0.3f));
		for (int k = 0; k < 3; k++) {
			float y = by + tg.getHeight() * (0.15f + 0.35f * k) + 0.1f * MathHelper.sin(t * 0.2f + k);
			ring(m, vc, bx, y, bz, rr * (1.05f + 0.1f * MathHelper.sin(t * 0.25f + k)), 0.08f, k == 1 ? 0xFFF1B0 : 0xFFC94A, 0.75f);
		}
		sphere(m, vc, (float) hand.x, (float) hand.y, (float) hand.z, 0.18f + 0.04f * MathHelper.sin(t * 0.6f), 0xFFF6C8, 0.8f);
	}

	/** Proteção dourada do Alto Elfo pronta: partículas de luz orbitando. */
	private static void ward(Matrix4f m, VertexConsumer vc, Entity e, float pt) {
		float t = (e.age + pt);
		Vec3d p = e.getLerpedPos(pt);
		for (int i = 0; i < 3; i++) {
			double a = t * 0.08 + i * Math.PI * 2 / 3;
			sphere(m, vc, (float) (p.x + Math.cos(a) * 0.75), (float) p.y + 0.4f + i * 0.5f + 0.1f * MathHelper.sin(t * 0.1f + i),
					(float) (p.z + Math.sin(a) * 0.75), 0.07f, 0xFFE27A, 0.8f);
		}
	}

	/** Brilho complementar do escudo: auréola e pulso aditivos atrás do círculo rúnico. */
	private static void shieldGlow(Matrix4f m, VertexConsumer vc, Entity e, float pt) {
		Vec3d look = e.getRotationVec(pt);
		Vec3d c = e.getLerpedPos(pt).add(0, e.getStandingEyeHeight() - 0.35, 0).add(look.multiply(1.42));
		float t = e.age + pt;
		sphere(m, vc, (float) c.x, (float) c.y, (float) c.z, 0.35f + 0.05f * MathHelper.sin(t * 0.3f), 0xFFE9A0, 0.35f);
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
