package com.dakura.origins.client.render;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.race.Race;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

/**
 * A galáxia em pixel art do fundo: nebulosa que flui, estrelas piscando,
 * anel de eclipse e estrela-flare. As cores vêm da paleta da raça.
 *
 * Desempenho: a nebulosa inteira (com eclipse e estrelas) é pintada numa textura
 * pequena (1 pixel = 1 célula) só ~15 vezes por segundo e desenhada com UMA chamada.
 * O campo de "céu" é fixo e as nuvens grandes mudam devagar, então são recalculados
 * bem menos vezes que as fitas.
 */
public final class InkBackdrop {
	private static final Identifier TEXTURE_ID = DakuraOrigins.id("dynamic/nebula");
	private static final long REFRESH_MS = 66;   // ~15 fps de animação da nebulosa
	private static final int COVER_EVERY = 4;     // nuvens grandes: 1 a cada 4 atualizações
	private static final int EDGE_MARGIN = 10;

	private int cell, cols, rows, width, height;
	private int focusX, focusY, focusW, focusH;
	private float[] field = new float[0];
	private float[] sky = new float[0];
	private float[] cover = new float[0];
	private float[] nearCache = new float[0];
	private long lastCompute = -1;
	private int computeCount = 0;

	private NativeImageBackedTexture texture;
	private NativeImage image;

	/** Bordas da página já resolvidas (x0, y0, x1, y1, cor) — recalculadas junto com a nebulosa. */
	private int[] edgeRects = new int[0];
	private int edgeCount = 0;

	public void layout(int width, int height, int focusX, int focusY, int focusW, int focusH) {
		boolean resized = width != this.width || height != this.height || image == null;
		this.width = width;
		this.height = height;
		this.focusX = focusX;
		this.focusY = focusY;
		this.focusW = focusW;
		this.focusH = focusH;
		if (resized) {
			this.cell = Math.max(3, Math.round(width / 150f));
			this.cols = (width + cell - 1) / cell;
			this.rows = (height + cell - 1) / cell;
			int n = cols * rows;
			this.field = new float[n];
			this.sky = new float[n];
			this.cover = new float[n];
			this.nearCache = new float[n];
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					sky[r * cols + c] = PixelNoise.fbm((c * cell + cell / 2f) / 140f, (r * cell + cell / 2f) / 140f);
				}
			}
			allocateTexture();
		}
		// distância até o foco só muda quando o layout muda
		float diag = Math.min(width, height) * 0.55f;
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				float x = c * cell + cell / 2f, y = r * cell + cell / 2f;
				float dx = Math.max(Math.max(focusX - x, x - (focusX + focusW)), 0);
				float dy = Math.max(Math.max(focusY - y, y - (focusY + focusH)), 0);
				nearCache[r * cols + c] = 1f - Math.min(1f, (float) Math.sqrt(dx * dx + dy * dy) / diag);
			}
		}
		this.lastCompute = -1;
		this.computeCount = 0;
	}

	private void allocateTexture() {
		close();
		image = new NativeImage(cols, rows, false);
		texture = new NativeImageBackedTexture(image);
		MinecraftClient.getInstance().getTextureManager().registerTexture(TEXTURE_ID, texture);
	}

	/** Libera a textura (chamar quando a tela fecha). */
	public void close() {
		if (texture != null) {
			MinecraftClient.getInstance().getTextureManager().destroyTexture(TEXTURE_ID);
			texture = null;
			image = null;
		}
	}

	// ------------------------------------------------------------------ cálculo

	private void compute(Race.Palette pal, float t) {
		boolean updateCover = computeCount++ % COVER_EVERY == 0;
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				int i = r * cols + c;
				float nx = (c * cell + cell / 2f) / 110f, ny = (r * cell + cell / 2f) / 110f;
				if (updateCover) {
					float cov = PixelNoise.fbm(nx * 0.5f + 7.3f, ny * 0.5f - t * 0.015f);
					cover[i] = Math.max(0f, Math.min(1f, (cov - 0.35f) * 2.2f));
				}
				float wx = PixelNoise.fbm(nx * 0.9f + t * 0.04f, ny * 0.9f);
				float wy = PixelNoise.fbm(nx * 0.9f - 3.1f, ny * 0.9f + 4.7f + t * 0.035f);
				float f = PixelNoise.fbm(nx + 2.4f * wx, ny + 2.4f * wy - t * 0.025f);
				float rib = 1f - Math.abs((float) Math.sin(f * 9f + t * 0.45f));
				rib = (float) Math.pow(rib, 2.2);
				float near = nearCache[i];
				field[i] = rib * (0.40f + 0.70f * near) + cover[i] * 0.30f * (0.3f + near) + near * 0.04f - 0.06f;
			}
		}
		paintImage(pal, t);
		buildEdges(pal);
	}

	private void paintImage(Race.Palette pal, float t) {
		if (image == null) return;
		int sky2 = Race.Palette.mix(pal.sky(), pal.ink()[0], 0.45f);
		int starDim = Race.Palette.mix(pal.sky(), pal.glow(), 0.45f);
		for (int r = 0; r < rows; r++) {
			for (int c = 0; c < cols; c++) {
				int i = r * cols + c;
				int color = shade(pal, field[i]);
				if (color == 0) {
					float sk = sky[i];
					color = (sk > 0.56f && ((r + c) & 1) == 0) || sk > 0.66f ? sky2 : pal.sky();
					if (PixelNoise.hash(c * 7 + 3, r * 13 + 1) > 0.982f) {
						float v = (float) Math.sin(t * 2.2f + PixelNoise.hash(c, r) * 40f);
						if (v > 0.3f) color = pal.glow();
						else if (v > -0.3f) color = starDim;
					}
				}
				image.setColor(c, r, abgr(color));
			}
		}
		paintEclipse(pal, t);
		texture.upload();
	}

	/** Anel de eclipse + flare, pintados direto na textura. */
	private void paintEclipse(Race.Palette pal, float t) {
		float radius = Math.min(width, height) * 0.13f;
		float ex = width * 0.84f, ey = height * 0.58f;
		int halo = Race.Palette.mix(pal.ink()[3], pal.sky(), 0.2f);
		float thin = cell * 0.9f;
		int c0 = Math.max(0, (int) ((ex - radius * 1.6f) / cell)), c1 = Math.min(cols - 1, (int) ((ex + radius * 1.6f) / cell));
		int r0 = Math.max(0, (int) ((ey - radius * 1.6f) / cell)), r1 = Math.min(rows - 1, (int) ((ey + radius * 1.6f) / cell));
		for (int r = r0; r <= r1; r++) {
			for (int c = c0; c <= c1; c++) {
				float cx = c * cell + cell / 2f, cy = r * cell + cell / 2f;
				float d = (float) Math.hypot(cx - ex, cy - ey);
				int color = 0;
				if (d < radius - thin) color = (d > radius - thin * 2.2f && cx < ex) ? pal.ink()[1] : 0xFF06040C;
				else if (d < radius) color = pal.glow();
				else if (d < radius + thin) color = pal.ink()[5];
				else if (d < radius + thin * 2.6f && ((r + c) & 1) == 0) color = halo;
				if (color != 0) image.setColor(c, r, abgr(color));
			}
		}
		float pulse = 0.75f + 0.25f * (float) Math.sin(t * 1.7f);
		int fx = (int) ((ex - radius * 0.92f) / cell), fy = (int) ((ey - radius * 0.25f) / cell);
		int len = Math.round(radius * 1.6f * pulse / cell);
		for (int k = -len; k <= len; k++) {
			float a = 1f - Math.abs(k) / (float) (len + 1);
			int col = a > 0.66f ? 0xFFFFFFFF : a > 0.33f ? pal.glow() : pal.ink()[4];
			put(fx + k, fy, col);
			if (Math.abs(k) <= len * 0.8f) put(fx, fy + Math.round(k * 0.8f), col);
		}
		int diag = Math.max(1, len / 4);
		for (int k = 1; k <= diag; k++) {
			int col = k == 1 ? pal.glow() : pal.ink()[4];
			put(fx + k, fy + k, col);
			put(fx - k, fy - k, col);
			put(fx + k, fy - k, col);
			put(fx - k, fy + k, col);
		}
		for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) put(fx + dx, fy + dy, pal.glow());
		put(fx, fy, 0xFFFFFFFF);
	}

	private void put(int c, int r, int argb) {
		if (c >= 0 && r >= 0 && c < cols && r < rows) image.setColor(c, r, abgr(argb));
	}

	/** Pré-calcula os retângulos da tinta que invade a borda da página. */
	private void buildEdges(Race.Palette pal) {
		int c0 = Math.max(0, focusX / cell - 1), c1 = Math.min(cols - 1, (focusX + focusW) / cell + 1);
		int r0 = Math.max(0, focusY / cell - 1), r1 = Math.min(rows - 1, (focusY + focusH) / cell + 1);
		int needed = ((c1 - c0 + 1) * (r1 - r0 + 1)) * 5;
		if (edgeRects.length < needed) edgeRects = new int[needed];
		edgeCount = 0;
		for (int r = r0; r <= r1; r++) {
			for (int c = c0; c <= c1; c++) {
				int x0 = c * cell, y0 = r * cell;
				float cx = x0 + cell / 2f, cy = y0 + cell / 2f;
				float edge = Math.min(Math.min(cx - focusX, cy - focusY), Math.min(focusX + focusW - cx, focusY + focusH - cy));
				if (edge < 0 || edge >= EDGE_MARGIN) continue;
				int ax = Math.max(x0, focusX), ay = Math.max(y0, focusY);
				int bx = Math.min(x0 + cell, focusX + focusW), by = Math.min(y0 + cell, focusY + focusH);
				if (bx <= ax || by <= ay) continue;
				float s = field[r * cols + c];
				float threshold = 0.5f + (edge / EDGE_MARGIN) * 0.6f;
				if (s > threshold) {
					int color = shade(pal, s);
					addEdge(ax, ay, bx, by, color == 0 ? pal.ink()[0] : color);
				} else if (PixelNoise.hash(c * 31 + 5, r * 17 + 9) < 0.04f * (1f - edge / EDGE_MARGIN)) {
					addEdge(x0 + 1, y0 + 1, x0 + 3, y0 + 3, pal.ink()[1]);
				}
			}
		}
	}

	private void addEdge(int x0, int y0, int x1, int y1, int color) {
		int o = edgeCount * 5;
		edgeRects[o] = x0;
		edgeRects[o + 1] = y0;
		edgeRects[o + 2] = x1;
		edgeRects[o + 3] = y1;
		edgeRects[o + 4] = color;
		edgeCount++;
	}

	private static int shade(Race.Palette p, float s) {
		int[] ink = p.ink();
		if (s > 0.80f) return ink[5];
		if (s > 0.64f) return ink[4];
		if (s > 0.50f) return ink[3];
		if (s > 0.38f) return ink[2];
		if (s > 0.27f) return ink[1];
		if (s > 0.17f) return ink[0];
		return 0;
	}

	private static int abgr(int argb) {
		return (argb & 0xFF00FF00) | ((argb & 0xFF) << 16) | ((argb >> 16) & 0xFF);
	}

	// ------------------------------------------------------------------ desenho

	/** Fundo inteiro numa única chamada. */
	public void render(DrawContext ctx, Race.Palette pal, float t, boolean animatingPalette) {
		if (image == null) allocateTexture();
		long now = System.currentTimeMillis();
		long interval = animatingPalette ? 33 : REFRESH_MS;
		if (lastCompute < 0 || now - lastCompute >= interval) {
			compute(pal, t);
			lastCompute = now;
		}
		ctx.drawTexture(TEXTURE_ID, 0, 0, cols * cell, rows * cell, 0f, 0f, cols, rows, cols, rows);
	}

	/** Nebulosa invadindo a borda da página (desenhar depois da página). */
	public void renderPageEdges(DrawContext ctx) {
		for (int i = 0; i < edgeCount; i++) {
			int o = i * 5;
			ctx.fill(edgeRects[o], edgeRects[o + 1], edgeRects[o + 2], edgeRects[o + 3], edgeRects[o + 4]);
		}
	}
}
