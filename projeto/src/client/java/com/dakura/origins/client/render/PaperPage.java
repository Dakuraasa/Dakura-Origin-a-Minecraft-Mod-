package com.dakura.origins.client.render;

import com.dakura.origins.DakuraOrigins;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * A página única em tons de jade: papel com grão, bordas envelhecidas,
 * moldura dupla e nuvens auspiciosas (xiangyun) nos cantos.
 *
 * Desempenho: papel, grão e moldura são pintados UMA vez numa textura do tamanho
 * da página (refeita só quando a janela muda de tamanho) e desenhados com uma chamada.
 */
public final class PaperPage {
	public static final int PAPER = 0xFFEEF4E2;
	private static final int PAPER_2 = 0xFFE1ECD6;
	private static final int PAPER_3 = 0xFFCFE0C9;
	private static final int BORDER_1 = 0xFF8FB89A;
	private static final int BORDER_2 = 0xFFC6DCC2;
	private static final int CLOUD = 0xFF6E9F82;
	private static final int INSET = 8;

	private static final Identifier TEXTURE_ID = DakuraOrigins.id("dynamic/paper");
	private static final Identifier[] CLOUDS = {
			DakuraOrigins.id("textures/gui/cloud_tl.png"), DakuraOrigins.id("textures/gui/cloud_tr.png"),
			DakuraOrigins.id("textures/gui/cloud_bl.png"), DakuraOrigins.id("textures/gui/cloud_br.png")};

	private NativeImageBackedTexture texture;
	private int cachedW = -1, cachedH = -1;

	private void rebuild(int w, int h) {
		close();
		NativeImage img = new NativeImage(w, h, false);
		img.fillRect(0, 0, w, h, abgr(PAPER));
		for (int y = 0; y < h; y += 2) {
			for (int x = 0; x < w; x += 2) {
				int edge = Math.min(Math.min(x, y), Math.min(w - x - 2, h - y - 2));
				float rnd = PixelNoise.hash(x * 3 + 11, y * 5 + 7);
				int color = 0;
				if (edge < 6 && rnd < (6 - edge) / 7f) color = rnd < (6 - edge) / 16f ? PAPER_3 : PAPER_2;
				else if (rnd < 0.04f) color = PAPER_2;
				if (color != 0) img.fillRect(x, y, Math.min(2, w - x), Math.min(2, h - y), abgr(color));
			}
		}
		outline(img, INSET, INSET, w - INSET * 2, h - INSET * 2, BORDER_1);
		outline(img, INSET + 2, INSET + 2, w - INSET * 2 - 4, h - INSET * 2 - 4, BORDER_2);

		texture = new NativeImageBackedTexture(img);
		MinecraftClient.getInstance().getTextureManager().registerTexture(TEXTURE_ID, texture);
		cachedW = w;
		cachedH = h;
	}

	private static void outline(NativeImage img, int x, int y, int w, int h, int color) {
		int c = abgr(color);
		img.fillRect(x + 3, y, w - 6, 1, c);
		img.fillRect(x + 3, y + h - 1, w - 6, 1, c);
		img.fillRect(x, y + 3, 1, h - 6, c);
		img.fillRect(x + w - 1, y + 3, 1, h - 6, c);
	}

	public void render(DrawContext ctx, int x, int y, int w, int h) {
		if (texture == null || w != cachedW || h != cachedH) rebuild(w, h);
		ctx.fill(x + 3, y + 4, x + w + 3, y + h + 4, 0x55000000); // sombra
		ctx.drawTexture(TEXTURE_ID, x, y, w, h, 0f, 0f, w, h, w, h);

		int cx0 = x + INSET - 3, cy0 = y + INSET - 3;
		int cx1 = x + w - INSET - 11, cy1 = y + h - INSET - 11;
		tint(ctx, CLOUD);
		ctx.drawTexture(CLOUDS[0], cx0, cy0, 14, 14, 0f, 0f, 14, 14, 14, 14);
		ctx.drawTexture(CLOUDS[1], cx1, cy0, 14, 14, 0f, 0f, 14, 14, 14, 14);
		ctx.drawTexture(CLOUDS[2], cx0, cy1, 14, 14, 0f, 0f, 14, 14, 14, 14);
		ctx.drawTexture(CLOUDS[3], cx1, cy1, 14, 14, 0f, 0f, 14, 14, 14, 14);
		ctx.setShaderColor(1f, 1f, 1f, 1f);
	}

	public void close() {
		if (texture != null) {
			MinecraftClient.getInstance().getTextureManager().destroyTexture(TEXTURE_ID);
			texture = null;
		}
	}

	public static void tint(DrawContext ctx, int c) {
		ctx.setShaderColor(((c >> 16) & 0xFF) / 255f, ((c >> 8) & 0xFF) / 255f, (c & 0xFF) / 255f, 1f);
	}

	private static int abgr(int argb) {
		return (argb & 0xFF00FF00) | ((argb & 0xFF) << 16) | ((argb >> 16) & 0xFF);
	}
}
