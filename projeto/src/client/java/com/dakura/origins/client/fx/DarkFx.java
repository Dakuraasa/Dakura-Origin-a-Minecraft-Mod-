package com.dakura.origins.client.fx;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.power.EntityFlags;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Set;

/**
 * Kitsune Corrompida: o Cometa Negro (núcleo escuro sólido envolto em energia violeta, com cauda longa) e as
 * correntes 3D que ligam a Kitsune à presa (elos de metal escuro com volume, balanço e brilho pulsando).
 */
public final class DarkFx {
	private static final Identifier VOID_TEX = DakuraOrigins.id("textures/entity/void_core.png");
	private static final Identifier CHAIN_TEX = DakuraOrigins.id("textures/entity/dark_chain.png");

	private DarkFx() {}

	public static void init() {
		WorldRenderEvents.AFTER_ENTITIES.register(DarkFx::render);
	}

	// ------------------------------------------------------------------ cometa

	public static void comet(MatrixStack ms, VertexConsumerProvider vcp, float t, Vec3d vel, float size) {
		Matrix4f m = ms.peek().getPositionMatrix();
		float r = 0.3f * size, pulse = 0.9f + 0.1f * MathHelper.sin(t * 1.3f);
		// núcleo escuro sólido (não aditivo, para ficar realmente negro)
		VertexConsumer core = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(VOID_TEX));
		solidSphere(ms.peek(), core, 0, 0.3f, 0, r * pulse);
		VertexConsumer glow = vcp.getBuffer(RenderLayer.getLightning());
		FxWorld.sphere(m, glow, 0, 0.3f, 0, r * 1.45f, 0x7A2CFF, 0.38f);
		FxWorld.sphere(m, glow, 0, 0.3f, 0, r * 2.0f * pulse, 0xC23CFF, 0.16f);
		for (int i = 0; i < 6; i++) {
			double a = t * 0.6 + i * Math.PI / 3;
			FxWorld.dot(m, glow, (float) Math.cos(a) * r * 1.6f, 0.3f + MathHelper.sin(t * 0.5f + i) * r * 0.8f, (float) Math.sin(a) * r * 1.6f,
					0.045f * size, 0xE6B8FF, 0.95f);
		}
		Vec3d back = vel.lengthSquared() > 1e-4 ? vel.normalize().multiply(-1) : new Vec3d(0, 1, 0);
		for (int i = 1; i <= 22; i++) {
			float d = i * 0.24f * size, k = 1 - i / 23f;
			float wob = MathHelper.sin(t * 0.7f + i * 0.8f) * 0.05f * i * size;
			float x = (float) back.x * d + wob, y = 0.3f + (float) back.y * d, z = (float) back.z * d - wob;
			FxWorld.sphere(m, glow, x, y, z, r * 1.1f * k, i % 3 == 0 ? 0xD24CFF : 0x6A1FD8, 0.34f * k);
			if (i % 2 == 0 && i < 14) solidSphere(ms.peek(), core, x, y, z, r * 0.55f * k);
		}
	}

	// ------------------------------------------------------------------ correntes

	private static void render(WorldRenderContext ctx) {
		ClientWorld world = ctx.world();
		MatrixStack ms = ctx.matrixStack();
		VertexConsumerProvider vcp = ctx.consumers();
		if (world == null || ms == null || vcp == null || EntityFlags.CLIENT.isEmpty()) return;
		Vec3d cam = ctx.camera().getPos();
		float pt = ctx.tickCounter().getTickDelta(true);
		for (PlayerEntity pl : world.getPlayers()) {
			Set<String> flags = EntityFlags.CLIENT.get(pl.getId());
			if (flags == null) continue;
			for (String f : flags) {
				if (!f.startsWith("chain#")) continue;
				Entity t;
				try {
					t = world.getEntityById(Integer.parseInt(f.substring(6)));
				} catch (NumberFormatException ex) {
					continue;
				}
				if (t != null) chain(ms, vcp, world, cam, pl.getLerpedPos(pt).add(0, pl.getHeight() * 0.55, 0),
						t.getLerpedPos(pt).add(0, t.getHeight() * 0.5, 0), pl.age + pt);
			}
		}
	}

	private static void chain(MatrixStack ms, VertexConsumerProvider vcp, ClientWorld w, Vec3d cam, Vec3d a, Vec3d b, float t) {
		double len = a.distanceTo(b);
		if (len < 0.3) return;
		int links = Math.max(3, (int) (len / 0.3));
		double sag = Math.max(0.15, 1.6 - len * 0.08);
		VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(CHAIN_TEX));
		VertexConsumer glow = vcp.getBuffer(RenderLayer.getLightning());
		int light = WorldRenderer.getLightmapCoordinates(w, BlockPos.ofFloored(a.lerp(b, 0.5)));
		Vec3d prev = curve(a, b, 0, sag, t);
		for (int i = 1; i <= links; i++) {
			double u = i / (double) links;
			Vec3d p = curve(a, b, u, sag, t);
			Vec3d c = prev.lerp(p, 0.5), d = p.subtract(prev);
			float yaw = (float) Math.toDegrees(Math.atan2(d.x, d.z));
			float pitch = (float) Math.toDegrees(-Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
			ms.push();
			ms.translate(c.x - cam.x, c.y - cam.y, c.z - cam.z);
			ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(yaw));
			ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
			ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(i % 2 == 0 ? 0 : 90));
			link(ms.peek(), vc, light);
			// brilho violeta correndo pela corrente
			float wave = MathHelper.sin((float) (u * 12 - t * 0.35));
			if (wave > 0.6f) FxWorld.dot(ms.peek().getPositionMatrix(), glow, 0, 0, 0, 0.09f, 0xB45CFF, (wave - 0.6f) * 1.6f);
			ms.pop();
			prev = p;
		}
	}

	/** Ponto da corrente: linha reta com barriga (catenária aproximada) e um balanço leve. */
	private static Vec3d curve(Vec3d a, Vec3d b, double u, double sag, float t) {
		Vec3d p = a.lerp(b, u);
		double s = 4 * u * (1 - u);
		return p.add(Math.sin(t * 0.15 + u * 3) * 0.06 * s, -sag * s, Math.cos(t * 0.13 + u * 3) * 0.06 * s);
	}

	/** Um elo: anel retangular com volume (4 barras), comprimento no eixo z. */
	private static void link(MatrixStack.Entry e, VertexConsumer vc, int light) {
		float L = 0.2f, W = 0.1f, T = 0.035f;
		box(e, vc, light, -W, -T, -L, -W + 2 * T, T, L);
		box(e, vc, light, W - 2 * T, -T, -L, W, T, L);
		box(e, vc, light, -W, -T, -L, W, T, -L + 2 * T);
		box(e, vc, light, -W, -T, L - 2 * T, W, T, L);
	}

	private static void box(MatrixStack.Entry e, VertexConsumer vc, int light, float x0, float y0, float z0, float x1, float y1, float z1) {
		Matrix4f m = e.getPositionMatrix();
		float[][] f = {
				{x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0, 0, -1}, {x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, 0, 0, 1},
				{x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, -1, 0, 0}, {x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0},
				{x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0}, {x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1, 0}};
		float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
		for (float[] q : f) {
			for (int k = 0; k < 4; k++) vc.vertex(m, q[k * 3], q[k * 3 + 1], q[k * 3 + 2]).color(255, 255, 255, 255).texture(uv[k][0], uv[k][1])
					.overlay(OverlayTexture.DEFAULT_UV).light(light).normal(e, q[12], q[13], q[14]);
		}
	}

	private static void solidSphere(MatrixStack.Entry e, VertexConsumer vc, float x, float y, float z, float r) {
		Matrix4f m = e.getPositionMatrix();
		int lat = 6, lon = 10;
		for (int i = 0; i < lat; i++) {
			double t0 = Math.PI * i / lat - Math.PI / 2, t1 = Math.PI * (i + 1) / lat - Math.PI / 2;
			for (int j = 0; j < lon; j++) {
				double p0 = 2 * Math.PI * j / lon, p1 = 2 * Math.PI * (j + 1) / lon;
				double[][] pts = {{t0, p0}, {t0, p1}, {t1, p1}, {t1, p0}};
				for (int k = 3; k >= 0; k--) {
					float nx = (float) (Math.cos(pts[k][0]) * Math.cos(pts[k][1])), ny = (float) Math.sin(pts[k][0]), nz = (float) (Math.cos(pts[k][0]) * Math.sin(pts[k][1]));
					vc.vertex(m, x + nx * r, y + ny * r, z + nz * r).color(255, 255, 255, 255).texture(k == 1 || k == 2 ? 1 : 0, k >= 2 ? 1 : 0)
							.overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(e, nx, ny, nz);
				}
			}
		}
	}
}
