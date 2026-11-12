package com.dakura.origins.client.fx;

import com.dakura.origins.power.EntityFlags;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Elfos da noite: as três espadas de ferro flutuando sobre a cabeça do Elfo Noturno (pontas para a frente, aura
 * roxa/rosa), a espada voando como míssil e a lua branca do Elfo da Lua.
 */
public final class ElfFx {
	private static final ItemStack SWORD = new ItemStack(Items.IRON_SWORD);
	private static final DustParticleEffect PURPLE = new DustParticleEffect(new Vector3f(0.35f, 0.08f, 0.55f), 1.0f);
	private static final DustParticleEffect PINK = new DustParticleEffect(new Vector3f(0.95f, 0.35f, 0.75f), 0.8f);

	private ElfFx() {}

	public static void init() {
		WorldRenderEvents.AFTER_ENTITIES.register(ElfFx::render);
	}

	private static void render(WorldRenderContext ctx) {
		ClientWorld world = ctx.world();
		MatrixStack ms = ctx.matrixStack();
		VertexConsumerProvider vcp = ctx.consumers();
		if (world == null || ms == null || vcp == null || EntityFlags.CLIENT.isEmpty()) return;
		Vec3d cam = ctx.camera().getPos();
		float pt = ctx.tickCounter().getTickDelta(true);
		for (PlayerEntity pl : world.getPlayers()) {
			if (pl.isInvisible() || !EntityFlags.clientHas(pl, "night_swords")) continue;
			if (pl == MinecraftClient.getInstance().player && MinecraftClient.getInstance().options.getPerspective().isFirstPerson()) continue;
			float t = pl.age + pt;
			float yaw = MathHelper.lerpAngleDegrees(pt, pl.prevHeadYaw, pl.headYaw) * MathHelper.RADIANS_PER_DEGREE;
			Vec3d fwd = new Vec3d(-MathHelper.sin(yaw), 0, MathHelper.cos(yaw));
			Vec3d side = new Vec3d(-fwd.z, 0, fwd.x);
			Vec3d head = pl.getLerpedPos(pt).add(0, pl.getHeight() + 0.35, 0);
			for (int i = 0; i < 3; i++) {
				float bob = MathHelper.sin(t * 0.12f + i * 2.1f) * 0.06f;
				Vec3d at = head.add(side.multiply((i - 1) * 0.55)).add(0, (i == 1 ? 0.18 : 0) + bob, 0).add(fwd.multiply(i == 1 ? 0.12 : 0));
				ms.push();
				ms.translate(at.x - cam.x, at.y - cam.y, at.z - cam.z);
				sword(ms, vcp, fwd, t + i * 10, true);
				ms.pop();
				if (world.random.nextInt(6) == 0)
					world.addParticle(world.random.nextBoolean() ? PURPLE : PINK, at.x + world.random.nextGaussian() * 0.15, at.y,
							at.z + world.random.nextGaussian() * 0.15, 0, 0.01, 0);
				if (world.random.nextInt(14) == 0) world.addParticle(ParticleTypes.WITCH, at.x, at.y, at.z, 0, 0, 0);
				if (world.random.nextInt(18) == 0) world.addParticle(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 0, 0.02, 0);
			}
		}
	}

	/**
	 * Espada de ferro vanilla com a ponta apontando para "dir" e a lâmina em pé (plano vertical), sem nada 3D em volta:
	 * a aura é só de partículas nativas. Modelo cru do item (modo NONE): a ponta do sprite fica no canto (+x, +y).
	 */
	public static void sword(MatrixStack ms, VertexConsumerProvider vcp, Vec3d dir, float t, boolean idle) {
		Vec3d d = dir.lengthSquared() < 1e-6 ? new Vec3d(0, 0, 1) : dir.normalize();
		float yaw = (float) Math.atan2(d.x, d.z);
		float pitch = (float) Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z));
		ms.push();
		ms.multiply(RotationAxis.POSITIVE_Y.rotation(yaw));                       // 4) gira para a direção do olhar
		ms.multiply(RotationAxis.POSITIVE_X.rotation(MathHelper.HALF_PI - pitch)); // 3) +Y vira a frente (+Z), com a inclinação
		ms.multiply(RotationAxis.POSITIVE_Y.rotation(MathHelper.HALF_PI));        // 2) lâmina em pé
		ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45));                  // 1) ponta diagonal do sprite -> +Y
		ms.scale(0.85f, 0.85f, 0.85f);
		MinecraftClient.getInstance().getItemRenderer().renderItem(SWORD, ModelTransformationMode.NONE, 0xF000F0, OverlayTexture.DEFAULT_UV,
				ms, vcp, MinecraftClient.getInstance().world, 0);
		ms.pop();
	}

	/** Rastro e aura nativos (pó roxo/rosa, bruxaria, portal reverso e tinta) de uma espada em voo. */
	public static void swordTrail(net.minecraft.world.World w, Vec3d at) {
		var r = w.random;
		w.addParticle(r.nextBoolean() ? PURPLE : PINK, at.x + r.nextGaussian() * 0.08, at.y + r.nextGaussian() * 0.08, at.z + r.nextGaussian() * 0.08, 0, 0, 0);
		if (r.nextInt(2) == 0) w.addParticle(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 0, 0, 0);
		if (r.nextInt(3) == 0) w.addParticle(ParticleTypes.WITCH, at.x, at.y, at.z, 0, 0, 0);
		if (r.nextInt(5) == 0) w.addParticle(ParticleTypes.SQUID_INK, at.x, at.y, at.z, 0, 0, 0);
	}

	private static final net.minecraft.util.Identifier MOON_TEX = com.dakura.origins.DakuraOrigins.id("textures/entity/moon_cube.png");
	private static final net.minecraft.util.Identifier SPIRIT_TEX = com.dakura.origins.DakuraOrigins.id("textures/entity/spirit_cube.png");
	private static final net.minecraft.util.Identifier DARK_SPIRIT_TEX = com.dakura.origins.DakuraOrigins.id("textures/entity/dark_spirit_cube.png");

	public static void moon(MatrixStack ms, VertexConsumerProvider vcp, float t, float size) {
		cube(ms, vcp, MOON_TEX, t, 1.1f * size, 0xEEF2FF, 0.35f, 0.8f);
	}

	public static void spiritCube(MatrixStack ms, VertexConsumerProvider vcp, float t, float size, boolean dark) {
		cube(ms, vcp, dark ? DARK_SPIRIT_TEX : SPIRIT_TEX, t, 0.75f * size, dark ? 0x3A1450 : 0x9FD4FF, dark ? 0.3f : 0.32f, 2.2f);
	}

	private static final net.minecraft.util.Identifier SUN_TEX = com.dakura.origins.DakuraOrigins.id("textures/entity/sun_cube.png");
	private static final net.minecraft.util.Identifier LIGHT_TEX = com.dakura.origins.DakuraOrigins.id("textures/entity/light_cube.png");

	/** Golden pixel cube (Elf's dash shot). */
	public static void sunCube(MatrixStack ms, VertexConsumerProvider vcp, float t, float size) {
		cube(ms, vcp, SUN_TEX, t, 0.75f * size, 0xFFD24A, 0.32f, 2.2f);
	}

	/** Large translucent yellow cube drawn around an entity (High Elf's tether prison). */
	public static void prisonCube(MatrixStack ms, VertexConsumerProvider vcp, float t, float side) {
		float h = side / 2;
		ms.push();
		ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 1.2f));
		MatrixStack.Entry e = ms.peek();
		texBox(e, vcp.getBuffer(RenderLayer.getEntityTranslucentEmissive(LIGHT_TEX)), -h, -h, -h, h, h, h);
		float pulse = 0.85f + 0.15f * MathHelper.sin(t * 0.2f);
		glowBox(e.getPositionMatrix(), vcp.getBuffer(RenderLayer.getLightning()), h * 1.05f, 0xFFE27A, 0.14f * pulse);
		ms.pop();
	}

	private static void cube(MatrixStack ms, VertexConsumerProvider vcp, net.minecraft.util.Identifier tex, float t, float side, int glow, float glowA, float spin) {
		float h = side / 2;
		ms.push();
		ms.translate(0, 0.3f, 0);
		ms.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * 3.1f * spin));
		ms.multiply(RotationAxis.POSITIVE_X.rotationDegrees(t * 2.3f * spin));
		ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(t * 1.7f * spin));
		MatrixStack.Entry e = ms.peek();
		VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityTranslucentEmissive(tex));
		texBox(e, vc, -h, -h, -h, h, h, h);
		VertexConsumer g = vcp.getBuffer(RenderLayer.getLightning());
		float pulse = 0.85f + 0.15f * MathHelper.sin(t * 0.25f);
		glowBox(e.getPositionMatrix(), g, h * 1.12f, glow, glowA * pulse);
		glowBox(e.getPositionMatrix(), g, h * 1.3f, glow, glowA * 0.45f * pulse);
		ms.pop();
	}

	private static final float[][] FACES = {
			{0, 0, -1, -1, 0, 0, 0, 1, 0}, {0, 0, 1, 1, 0, 0, 0, 1, 0}, {-1, 0, 0, 0, 0, 1, 0, 1, 0},
			{1, 0, 0, 0, 0, -1, 0, 1, 0}, {0, 1, 0, 1, 0, 0, 0, 0, -1}, {0, -1, 0, 1, 0, 0, 0, 0, 1}};

	private static void texBox(MatrixStack.Entry e, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1) {
		Matrix4f m = e.getPositionMatrix();
		float h = (x1 - x0) / 2;
		float[][] uv = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
		for (float[] f : FACES) {
			float[][] c = corners(f, h);
			for (int k = 0; k < 4; k++)
				vc.vertex(m, c[k][0], c[k][1], c[k][2]).color(255, 255, 255, 255).texture(uv[k][0], uv[k][1])
						.overlay(OverlayTexture.DEFAULT_UV).light(0xF000F0).normal(e, f[0], f[1], f[2]);
		}
	}

	private static void glowBox(Matrix4f m, VertexConsumer vc, float h, int rgb, float a) {
		int r = rgb >> 16 & 255, gg = rgb >> 8 & 255, b = rgb & 255, al = (int) (MathHelper.clamp(a, 0, 1) * 255);
		for (float[] f : FACES) {
			float[][] c = corners(f, h);
			for (int k = 0; k < 4; k++) vc.vertex(m, c[k][0], c[k][1], c[k][2]).color(r, gg, b, al);
		}
	}

	private static float[][] corners(float[] f, float h) {
		Vector3f n = new Vector3f(f[0], f[1], f[2]), u = new Vector3f(f[3], f[4], f[5]), v = new Vector3f(f[6], f[7], f[8]);
		float[][] out = new float[4][];
		float[][] s = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
		for (int k = 0; k < 4; k++) {
			Vector3f p = new Vector3f(n).mul(h).add(new Vector3f(u).mul(s[k][0] * h)).add(new Vector3f(v).mul(s[k][1] * h));
			out[k] = new float[] {p.x, p.y, p.z};
		}
		return out;
	}
}
