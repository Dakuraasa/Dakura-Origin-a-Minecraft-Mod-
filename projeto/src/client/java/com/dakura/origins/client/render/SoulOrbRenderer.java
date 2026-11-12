package com.dakura.origins.client.render;

import com.dakura.origins.entity.SoulOrbEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3f;

/** Alma do Demônio: o sprite da esfera de experiência vanilla, pulsando em vermelho, vinho e sangue. */
public class SoulOrbRenderer extends EntityRenderer<SoulOrbEntity> {
	private static final Identifier TEXTURE = Identifier.ofVanilla("textures/entity/experience_orb.png");
	private static final RenderLayer LAYER = RenderLayer.getItemEntityTranslucentCull(TEXTURE);
	private static final DustParticleEffect WINE = new DustParticleEffect(new Vector3f(0.45f, 0.02f, 0.08f), 0.6f);

	public SoulOrbRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
		this.shadowRadius = 0.15f;
		this.shadowOpacity = 0.75f;
	}

	@Override
	protected int getBlockLight(SoulOrbEntity e, BlockPos pos) {
		return MathHelper.clamp(super.getBlockLight(e, pos) + 7, 0, 15);
	}

	@Override
	public void render(SoulOrbEntity e, float yaw, float tickDelta, MatrixStack m, VertexConsumerProvider vcp, int light) {
		m.push();
		int i = e.getOrbSize();
		float u0 = (i % 4 * 16) / 64f, u1 = (i % 4 * 16 + 16) / 64f, v0 = (i / 4 * 16) / 64f, v1 = (i / 4 * 16 + 16) / 64f;
		float r = (e.age + tickDelta) / 2f;
		float pulse = (MathHelper.sin(r) + 1f) * 0.5f;
		// vermelho vivo <-> vinho escuro, com um toque de sangue
		int red = (int) (150 + 105 * pulse), green = (int) (8 + 22 * pulse), blue = (int) (24 + 26 * (1 - pulse));
		m.translate(0, 0.1f, 0);
		m.multiply(this.dispatcher.getRotation());
		m.scale(0.3f, 0.3f, 0.3f);
		VertexConsumer vc = vcp.getBuffer(LAYER);
		MatrixStack.Entry en = m.peek();
		vertex(vc, en, -0.5f, -0.25f, red, green, blue, u0, v1, light);
		vertex(vc, en, 0.5f, -0.25f, red, green, blue, u1, v1, light);
		vertex(vc, en, 0.5f, 0.75f, red, green, blue, u1, v0, light);
		vertex(vc, en, -0.5f, 0.75f, red, green, blue, u0, v0, light);
		m.pop();
		if (e.getWorld().random.nextInt(14) == 0)
			e.getWorld().addParticle(WINE, e.getX(), e.getY() + 0.2, e.getZ(), 0, 0.02, 0);
		super.render(e, yaw, tickDelta, m, vcp, light);
	}

	private static void vertex(VertexConsumer vc, MatrixStack.Entry en, float x, float y, int r, int g, int b, float u, float v, int light) {
		vc.vertex(en, x, y, 0).color(r, g, b, 128).texture(u, v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, 0, 1, 0);
	}

	@Override
	public Identifier getTexture(SoulOrbEntity e) {
		return TEXTURE;
	}
}
