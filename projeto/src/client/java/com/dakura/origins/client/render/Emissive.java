package com.dakura.origins.client.render;

import com.dakura.origins.client.geo.GeoModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * Emissão de luz própria das peças do corpo, compatível com shaders:
 * 1) a peça é desenhada com luz máxima (não escurece à noite nem em cavernas);
 * 2) uma segunda passada na camada "eyes" (aditiva) soma o brilho por cima. É a mesma camada que os olhos
 *    da aranha e do Enderman usam, e que os pacotes de shader (Iris/OptiFine) tratam como emissiva, com bloom.
 * A intensidade, a cor e a pulsação são escolhidas por quem chama; {@link #GLOBAL} escala tudo.
 */
public final class Emissive {
	public static final int FULL_BRIGHT = 0xF000F0;
	/** Multiplicador geral do brilho (1 = padrão). */
	public static float GLOBAL = 1f;

	private Emissive() {}

	/** Desenha a peça com luz própria e um halo aditivo de intensidade glow (0..1) e cor tint (RGB). */
	public static void geo(GeoModel g, MatrixStack m, VertexConsumerProvider vcp, Identifier tex, float glow, int tint) {
		g.render(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(tex)), FULL_BRIGHT);
		float k = MathHelper.clamp(glow * GLOBAL, 0f, 1f);
		if (k > 0.01f) g.render(m, vcp.getBuffer(RenderLayer.getEyes(tex)), FULL_BRIGHT, argb(k, tint));
	}

	/** Só o halo aditivo (a peça já foi desenhada normalmente). */
	public static void halo(GeoModel g, MatrixStack m, VertexConsumerProvider vcp, Identifier tex, float glow, int tint) {
		float k = MathHelper.clamp(glow * GLOBAL, 0f, 1f);
		if (k > 0.01f) g.render(m, vcp.getBuffer(RenderLayer.getEyes(tex)), FULL_BRIGHT, argb(k, tint));
	}

	/** Pulsação suave entre min e max. */
	public static float pulse(float t, float speed, float min, float max) {
		return min + (max - min) * (0.5f + 0.5f * MathHelper.sin(t * speed));
	}

	public static int argb(float k, int rgb) {
		int r = (int) (((rgb >> 16) & 255) * k), gr = (int) (((rgb >> 8) & 255) * k), b = (int) ((rgb & 255) * k);
		return 0xFF000000 | (r << 16) | (gr << 8) | b;
	}
}
