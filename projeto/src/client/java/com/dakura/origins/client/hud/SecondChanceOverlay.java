package com.dakura.origins.client.hud;

import com.dakura.origins.DakuraOrigins;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/**
 * Segunda Chance (Espectro): a caveira em pixel art salta no meio da tela, como a animação
 * do Totem da Imortalidade — surge girando, segura, cresce e some.
 */
public final class SecondChanceOverlay {
	private static final Identifier SKULL = DakuraOrigins.id("textures/gui/second_chance.png");
	private static final int TW = 23, TH = 29;
	private static final long DURATION_MS = 1800;
	private static long start = -1;

	private SecondChanceOverlay() {}

	public static void trigger() {
		start = System.currentTimeMillis();
	}

	public static void clear() {
		start = -1;
	}

	public static void render(DrawContext ctx, RenderTickCounter counter) {
		if (start < 0) return;
		float t = (System.currentTimeMillis() - start) / (float) DURATION_MS;
		if (t >= 1f) {
			start = -1;
			return;
		}
		float scale, alpha = 1f, spin;
		if (t < 0.22f) {
			float q = t / 0.22f;
			scale = 1.25f * (1f - (1f - q) * (1f - q));        // surge
			spin = (1f - q) * 200f;                            // girando
		} else if (t < 0.72f) {
			float q = (t - 0.22f) / 0.5f;
			scale = 1.25f - 0.25f * Math.min(1f, q * 3f);      // assenta
			spin = (float) Math.sin(q * Math.PI * 3) * 6f;     // balança
		} else {
			float q = (t - 0.72f) / 0.28f;
			scale = 1f + q * 0.9f;                             // avança para a tela
			alpha = 1f - q;
			spin = 0f;
		}
		int w0 = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
		// véu escuro avermelhado nas bordas, pulsando como um coração fraco
		float beat = 0.5f + 0.5f * (float) Math.sin(t * Math.PI * 6);
		int veil = Math.round(alpha * (0x50 + 0x30 * beat));
		int edge = (veil << 24) | 0x1A0006;
		int band = Math.max(8, h / 6);
		ctx.fillGradient(0, 0, w0, band, edge, 0x001A0006);
		ctx.fillGradient(0, h - band, w0, h, 0x001A0006, edge);
		ctx.fill(0, 0, w0, h, (Math.round(veil * 0.25f) << 24) | 0x0A0004);

		float px = Math.max(2f, (h * 0.42f) / TH);              // tamanho de cada pixel da arte
		float sx = TW * px * scale, sy = TH * px * scale;
		// tremor nervoso em pixels inteiros
		long tick = (System.currentTimeMillis() - start) / 45;
		float jx = ((tick * 7919) % 5 - 2) * 0.5f * (t < 0.72f ? 1f : 0.3f);
		float jy = ((tick * 104729) % 5 - 2) * 0.5f * (t < 0.72f ? 1f : 0.3f);
		RenderSystem.enableBlend();
		// "fantasmas" da caveira deslocados em vermelho e ciano (falha de assombração)
		drawSkull(ctx, w0, h, jx - 2.5f, jy, spin, sx, sy, 0.9f, 0.1f, 0.15f, alpha * 0.45f);
		drawSkull(ctx, w0, h, jx + 2.5f, jy, spin, sx, sy, 0.3f, 0.95f, 1f, alpha * 0.30f);
		// piscadas vermelhas rápidas
		boolean flick = (tick % 9 == 0) && t < 0.72f;
		drawSkull(ctx, w0, h, jx, jy, spin, sx, sy, 1f, flick ? 0.35f : 1f, flick ? 0.35f : 1f, alpha);
		ctx.setShaderColor(1f, 1f, 1f, 1f);
	}

	private static void drawSkull(DrawContext ctx, int w0, int h, float ox, float oy, float spin, float sx, float sy,
								  float r, float g, float b, float a) {
		ctx.setShaderColor(r, g, b, a);
		ctx.getMatrices().push();
		ctx.getMatrices().translate(w0 / 2f + ox, h / 2f + oy, 400f);
		ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spin));
		ctx.getMatrices().scale(sx / TW, sy / TH, 1f);
		ctx.drawTexture(SKULL, -TW / 2, -TH / 2, 0f, 0f, TW, TH, TW, TH);
		ctx.getMatrices().pop();
	}
}
