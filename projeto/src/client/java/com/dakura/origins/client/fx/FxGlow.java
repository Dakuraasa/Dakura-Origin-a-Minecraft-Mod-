package com.dakura.origins.client.fx;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

/** Brilhos presos aos projéteis (desenhados com as mesmas malhas do FxWorld, em coordenadas do projétil). */
public final class FxGlow {
	private FxGlow() {}

	public static void magmaAura(MatrixStack ms, VertexConsumerProvider vcp, float t, Vec3d vel) {
		VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
		Matrix4f m = ms.peek().getPositionMatrix();
		float pulse = 0.8f + 0.2f * MathHelper.sin(t * 0.8f);
		FxWorld.sphere(m, vc, 0, 0.3f, 0, 0.55f * pulse, 0x6BFF5A, 0.35f);
		FxWorld.sphere(m, vc, 0, 0.3f, 0, 0.32f, 0xE6FFD0, 0.45f);
		// rastro: esferas cada vez menores para trás do movimento
		Vec3d back = vel.lengthSquared() > 1e-4 ? vel.normalize().multiply(-1) : new Vec3d(0, 1, 0);
		for (int i = 1; i <= 6; i++) {
			float d = i * 0.35f;
			FxWorld.sphere(m, vc, (float) back.x * d, 0.3f + (float) back.y * d, (float) back.z * d, 0.42f * (1 - i / 7f),
					i % 2 == 0 ? 0x6BFF5A : 0x2FBF3A, 0.4f * (1 - i / 7f));
		}
	}

	/** Esfera de energia arcana do Elfo: núcleo branco, casca azul pulsando, anéis girando e rastro. */
	public static void arcaneOrb(MatrixStack ms, VertexConsumerProvider vcp, float t, Vec3d vel) {
		VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
		Matrix4f m = ms.peek().getPositionMatrix();
		float pulse = 0.85f + 0.15f * MathHelper.sin(t * 0.9f);
		FxWorld.sphere(m, vc, 0, 0.3f, 0, 0.5f * pulse, 0x6FC8FF, 0.35f);
		FxWorld.sphere(m, vc, 0, 0.3f, 0, 0.28f, 0xF2FBFF, 0.8f);
		ms.push();
		ms.translate(0, 0.3, 0);
		ms.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(t * 14));
		FxWorld.ring(ms.peek().getPositionMatrix(), vc, 0, 0, 0, 0.55f, 0.06f, 0xB8E8FF, 0.8f);
		ms.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotationDegrees(70));
		FxWorld.ring(ms.peek().getPositionMatrix(), vc, 0, 0, 0, 0.6f, 0.05f, 0xDDF4FF, 0.6f);
		ms.pop();
		Vec3d back = vel.lengthSquared() > 1e-4 ? vel.normalize().multiply(-1) : new Vec3d(0, 1, 0);
		for (int i = 1; i <= 7; i++) {
			float d = i * 0.3f;
			FxWorld.sphere(m, vc, (float) back.x * d, 0.3f + (float) back.y * d, (float) back.z * d, 0.32f * (1 - i / 8f),
					i % 2 == 0 ? 0x6FC8FF : 0xDDF4FF, 0.45f * (1 - i / 8f));
		}
	}

	/** Esfera de mana da Kitsune: núcleo branco, casca azul pulsando, faíscas violeta e um rastro longo de cometa. */
	public static void manaOrb(MatrixStack ms, VertexConsumerProvider vcp, float t, Vec3d vel, float size) {
		manaOrb(ms, vcp, t, vel, size, false);
	}

	/** violet = Kitsune Corrompida: casca roxa, núcleo lilás, faíscas azuis e rastro roxo/azul. */
	public static void manaOrb(MatrixStack ms, VertexConsumerProvider vcp, float t, Vec3d vel, float size, boolean violet) {
		VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
		Matrix4f m = ms.peek().getPositionMatrix();
		float pulse = 0.85f + 0.15f * MathHelper.sin(t * 0.8f);
		float r = 0.42f * size;
		FxWorld.sphere(m, vc, 0, 0.3f, 0, r * pulse, violet ? 0x8A3DFF : 0x3F9BFF, 0.42f);
		FxWorld.sphere(m, vc, 0, 0.3f, 0, r * 0.55f, violet ? 0xE6D2FF : 0xF2FBFF, 0.9f);
		for (int i = 0; i < 4; i++) {
			double a = t * 0.35 + i * Math.PI / 2;
			FxWorld.dot(m, vc, (float) Math.cos(a) * r * 1.2f, 0.3f + MathHelper.sin(t * 0.3f + i) * r * 0.5f, (float) Math.sin(a) * r * 1.2f,
					0.05f * size, violet ? 0x5FA8FF : 0xC9B8FF, 0.9f);
		}
		Vec3d back = vel.lengthSquared() > 1e-4 ? vel.normalize().multiply(-1) : new Vec3d(0, 1, 0);
		for (int i = 1; i <= 10; i++) {
			float d = i * 0.22f * size;
			float k = 1 - i / 11f;
			FxWorld.sphere(m, vc, (float) back.x * d, 0.3f + (float) back.y * d, (float) back.z * d, r * 0.8f * k,
					violet ? (i % 2 == 0 ? 0x9A4DFF : 0x3F7BFF) : (i % 2 == 0 ? 0x5FB4FF : 0xB59CFF), 0.42f * k);
		}
	}

	public static void poisonDrip(MatrixStack ms, VertexConsumerProvider vcp, float t) {
		VertexConsumer vc = vcp.getBuffer(RenderLayer.getLightning());
		Matrix4f m = ms.peek().getPositionMatrix();
		for (int i = 0; i < 4; i++) {
			float ph = (t * 0.08f + i * 0.25f) % 1f;
			FxWorld.sphere(m, vc, MathHelper.sin(i * 2.1f) * 0.2f, 0.1f - ph * 0.8f, MathHelper.cos(i * 2.1f) * 0.2f,
					0.08f + 0.05f * (1 - ph), 0xB066FF, (1 - ph) * 0.6f);
		}
	}
}
