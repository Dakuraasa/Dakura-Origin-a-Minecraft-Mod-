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
				if (world.random.nextInt(8) == 0)
					world.addParticle(world.random.nextBoolean() ? PURPLE : PINK, at.x + world.random.nextGaussian() * 0.15, at.y,
							at.z + world.random.nextGaussian() * 0.15, 0, 0.01, 0);
				if (world.random.nextInt(20) == 0) world.addParticle(ParticleTypes.WITCH, at.x, at.y, at.z, 0, 0, 0);
			}
		}
	}

	/** Espada de ferro vanilla com a ponta apontando para "dir", envolta em brilho roxo e rosa. */
	public static void sword(MatrixStack ms, VertexConsumerProvider vcp, Vec3d dir, float t, boolean idle) {
		Vec3d d = dir.lengthSquared() < 1e-6 ? new Vec3d(0, 0, 1) : dir.normalize();
		float yaw = (float) Math.atan2(d.x, d.z);
		float pitch = (float) Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z));
		ms.push();
		ms.multiply(RotationAxis.POSITIVE_Y.rotation(yaw));
		ms.multiply(RotationAxis.POSITIVE_X.rotation(MathHelper.HALF_PI - pitch));
		ms.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45));
		ms.scale(0.9f, 0.9f, 0.9f);
		MinecraftClient.getInstance().getItemRenderer().renderItem(SWORD, ModelTransformationMode.FIXED, 0xF000F0, OverlayTexture.DEFAULT_UV,
				ms, vcp, MinecraftClient.getInstance().world, 0);
		ms.pop();
		VertexConsumer glow = vcp.getBuffer(RenderLayer.getLightning());
		Matrix4f m = ms.peek().getPositionMatrix();
		float pulse = 0.8f + 0.2f * MathHelper.sin(t * 0.3f);
		FxWorld.sphere(m, glow, 0, 0, 0, 0.32f * pulse, 0x5A1A8C, 0.22f);
		FxWorld.sphere(m, glow, 0, 0, 0, 0.18f, 0xF25ABF, 0.18f);
		if (!idle) {
			for (int i = 1; i <= 6; i++) {
				float k = 1 - i / 7f;
				FxWorld.dot(m, glow, (float) -d.x * i * 0.22f, (float) -d.y * i * 0.22f, (float) -d.z * i * 0.22f, 0.12f * k,
						i % 2 == 0 ? 0xF25ABF : 0x4A1A9C, 0.5f * k);
			}
		}
	}

	/** A lua do Elfo da Lua: esfera branca grande e luminosa (estrutura das esferas da Kitsune). */
	public static void moon(MatrixStack ms, VertexConsumerProvider vcp, float t, float size) {
		VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
		Matrix4f m = ms.peek().getPositionMatrix();
		float pulse = 0.94f + 0.06f * MathHelper.sin(t * 0.2f);
		float r = 0.5f * size;
		FxWorld.sphere(m, vc, 0, 0.3f, 0, r * pulse, 0xFFFFFF, 0.9f);
		FxWorld.sphere(m, vc, 0, 0.3f, 0, r * 1.25f, 0xE6ECFF, 0.35f);
		FxWorld.sphere(m, vc, 0, 0.3f, 0, r * 1.7f * pulse, 0xB8C4FF, 0.14f);
		for (int i = 0; i < 8; i++) {
			double a = t * 0.05 + i * Math.PI / 4;
			FxWorld.dot(m, vc, (float) Math.cos(a) * r * 1.9f, 0.3f + MathHelper.sin(t * 0.07f + i) * r * 0.5f, (float) Math.sin(a) * r * 1.9f,
					0.08f * size, i % 2 == 0 ? 0xF25ABF : 0x6A5CFF, 0.7f);
		}
	}
}
