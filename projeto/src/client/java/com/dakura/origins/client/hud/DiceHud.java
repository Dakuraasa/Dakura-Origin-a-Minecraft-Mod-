package com.dakura.origins.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.util.math.MathHelper;

/** Rerrolagem da Lilac: dois dados de seis lados em pixel art quicando e girando na tela até pararem. */
public final class DiceHud {
	private static long start = -1;
	private static int durMs, faceA, faceB;
	private static final int[][] PIPS = {{4}, {0, 8}, {0, 4, 8}, {0, 2, 6, 8}, {0, 2, 4, 6, 8}, {0, 2, 3, 5, 6, 8}};

	private DiceHud() {}

	public static void start(int ticks) {
		start = System.currentTimeMillis();
		durMs = Math.max(1000, ticks * 50);
		faceA = (int) (Math.random() * 6);
		faceB = (int) (Math.random() * 6);
	}

	public static void render(DrawContext ctx, RenderTickCounter counter) {
		if (start < 0 || MinecraftClient.getInstance().player == null) return;
		long el = System.currentTimeMillis() - start;
		if (el > durMs + 900) {
			start = -1;
			return;
		}
		float k = Math.min(1f, el / (float) durMs);
		int cx = ctx.getScaledWindowWidth() / 2, cy = ctx.getScaledWindowHeight() / 2 - 40;
		for (int d = 0; d < 2; d++) {
			float ph = el * 0.012f + d * 1.7f;
			float bounce = Math.abs(MathHelper.sin(ph)) * 26 * (1 - k);
			float rot = k >= 1 ? 0 : (float) (el * (0.02 + d * 0.006) * (1 - k));
			int face = k >= 1 ? (d == 0 ? faceA : faceB) : (int) ((el / 70 + d * 3) % 6);
			var m = ctx.getMatrices();
			m.push();
			m.translate(cx + (d == 0 ? -22 : 22), cy - bounce, 200);
			m.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotation(rot));
			die(ctx, face, k >= 1 && (el / 150) % 2 == 0);
			m.pop();
		}
	}

	private static void die(DrawContext ctx, int face, boolean shine) {
		ctx.fill(-12, -12, 12, 12, 0xFF3A2408);
		ctx.fill(-11, -11, 11, 11, shine ? 0xFFFFF6D8 : 0xFFF2E6C8);
		ctx.fill(-11, -11, 11, -9, 0xFFFFFFFF);
		ctx.fill(-11, 9, 11, 11, 0xFFC9B58A);
		ctx.fill(-11, -11, -9, 11, 0xFFD4A21E);
		ctx.fill(9, -11, 11, 11, 0xFFB8861B);
		for (int p : PIPS[face]) {
			int x = (p % 3 - 1) * 6, y = (p / 3 - 1) * 6;
			ctx.fill(x - 2, y - 2, x + 2, y + 2, face == 0 ? 0xFFC2182E : 0xFF1A1208);
		}
	}
}
