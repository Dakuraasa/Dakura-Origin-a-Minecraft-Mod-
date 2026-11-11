package com.dakura.origins.client.render;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.entity.FeatherEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

/**
 * Pena-adaga da Vastaya (ave): a sprite em pixel art do Artur, em duas lâminas cruzadas alinhadas com o voo
 * (a ponta da pena vai na frente), girando de leve como uma pena de verdade.
 */
public class FeatherRenderer extends EntityRenderer<FeatherEntity> {
	public static final Identifier TEX = DakuraOrigins.id("textures/entity/feather_dagger.png");

	public FeatherRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
	}

	@Override
	public void render(FeatherEntity e, float yaw, float tickDelta, MatrixStack m, VertexConsumerProvider vcp, int light) {
		m.push();
		m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.lerp(tickDelta, e.prevYaw, e.getYaw()) - 90f));
		m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.lerp(tickDelta, e.prevPitch, e.getPitch())));
		m.multiply(RotationAxis.POSITIVE_X.rotationDegrees((e.age + tickDelta) * 25f));
		draw(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEX)), light, 0.42f);
		m.pop();
		super.render(e, yaw, tickDelta, m, vcp, light);
	}

	/** Duas lâminas cruzadas com a diagonal da sprite (cabo → ponta) sobre o eixo +X. */
	public static void draw(MatrixStack m, VertexConsumer vc, int light, float half) {
		for (int plane = 0; plane < 2; plane++) {
			m.push();
			if (plane == 1) m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
			MatrixStack.Entry en = m.peek();
			Matrix4f mat = en.getPositionMatrix();
			// cantos da sprite: ponta (u1,v0) em +X, cabo (u0,v1) em -X
			v(vc, mat, en, -half, 0, 0f, 1f, light);
			v(vc, mat, en, 0, -half, 1f, 1f, light);
			v(vc, mat, en, half, 0, 1f, 0f, light);
			v(vc, mat, en, 0, half, 0f, 0f, light);
			m.pop();
		}
	}

	private static void v(VertexConsumer vc, Matrix4f mat, MatrixStack.Entry en, float x, float y, float u, float vv, int light) {
		vc.vertex(mat, x, y, 0).color(255, 255, 255, 255).texture(u, vv).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, 0, 0, 1);
	}

	@Override
	public Identifier getTexture(FeatherEntity e) {
		return TEX;
	}
}
