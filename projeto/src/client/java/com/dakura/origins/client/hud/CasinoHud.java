package com.dakura.origins.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Lilac's casino wheel: 12 pockets that spin, slow down and stop under the pointer on the outcome rolled by the server.
 * Pocket labels match {@code CasinoAbility.MULTS}.
 */
public final class CasinoHud {
	private static final String[] LABEL = {"✗", "—", "×2", "×3", "×4", "×5"};
	private static final int[] WHEEL = {0, 2, 1, 3, 0, 2, 1, 4, 0, 3, 1, 5};
	private static final net.minecraft.util.Identifier WHEEL_TEX = com.dakura.origins.DakuraOrigins.id("textures/gui/casino_wheel.png");
	private static final net.minecraft.util.Identifier POINTER_TEX = com.dakura.origins.DakuraOrigins.id("textures/gui/casino_pointer.png");
	private static long start = -1;
	private static int result, tier, spinMs, lastTop = -1, slot;

	private CasinoHud() {}

	public static void start(int res, int t, int spinTicks) {
		result = res;
		tier = t;
		spinMs = Math.max(1000, spinTicks * 50);
		start = System.currentTimeMillis();
		lastTop = -1;
		java.util.List<Integer> hits = new java.util.ArrayList<>();
		for (int i = 0; i < WHEEL.length; i++) if (WHEEL[i] == result) hits.add(i);
		slot = hits.isEmpty() ? 0 : hits.get((int) (Math.random() * hits.size()));
	}

	public static void render(DrawContext ctx, RenderTickCounter counter) {
		if (start < 0) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		long el = System.currentTimeMillis() - start;
		if (el > spinMs + 1600 || mc.player == null) {
			start = -1;
			return;
		}
		int n = WHEEL.length;
		double step = Math.PI * 2 / n;
		float tt = Math.min(1f, el / (float) spinMs);
		float ease = 1f - (float) Math.pow(1f - tt, 3);
		double total = (4 + tier) * Math.PI * 2 + slot * step;
		double angle = total * ease;
		int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
		int dia = Math.round(Math.min(sw, sh) * 0.66f);
		float rad = dia / 2f;
		int cx = sw / 2, cy = sh / 2 - 6;
		var mtx = ctx.getMatrices();
		mtx.push();
		mtx.translate(cx, cy, 200);
		mtx.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotation((float) -angle));
		mtx.push();
		float sc = dia / 384f;
		mtx.scale(sc, sc, 1f);
		ctx.drawTexture(WHEEL_TEX, -192, -192, 0, 0, 384, 384, 384, 384);
		mtx.pop();
		float ns = Math.max(0.6f, rad * 0.011f);
		for (int i = 0; i < n; i++) {
			String txt = LABEL[WHEEL[i]];
			mtx.push();
			mtx.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotation((float) (i * step)));
			mtx.translate(0, -rad * 0.785f, 1);
			mtx.scale(ns, ns, 1f);
			ctx.drawText(mc.textRenderer, txt, -mc.textRenderer.getWidth(txt) / 2, -4, 0xFFFFF3D6, true);
			mtx.pop();
		}
		mtx.pop();
		int top = (int) Math.floorMod(Math.round(angle / step), n);
		if (top != lastTop && tt < 1f) {
			lastTop = top;
			mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 0.35f, 1.2f + tt * 0.6f);
		}
		mtx.push();
		float ps = Math.max(1f, rad / 90f);
		mtx.translate(cx, cy - rad - 6 * ps, 300);
		mtx.scale(ps, ps, 1f);
		ctx.drawTexture(POINTER_TEX, -20, -8, 0, 0, 40, 56, 40, 56);
		mtx.pop();
		cy = (int) (cy + rad - 58);
		if (tt >= 1f) {
			boolean blink = (el / 150) % 2 == 0;
			Text res = Text.translatable(result == 0 ? "dakura_origins.casino.lose" : result == 1 ? "dakura_origins.casino.even"
					: "dakura_origins.casino.win", LABEL[result]).formatted(Formatting.BOLD);
			int col = result == 0 ? 0xFFFF6B6B : result == 1 ? 0xFFDDDDDD : (blink ? 0xFFFFE066 : 0xFFFFB020);
			ctx.drawText(mc.textRenderer, res, cx - mc.textRenderer.getWidth(res) / 2, cy + 70, col, true);
		} else {
			Text sp = Text.translatable("dakura_origins.casino.spinning");
			ctx.drawText(mc.textRenderer, sp, cx - mc.textRenderer.getWidth(sp) / 2, cy + 70, 0xFFE8D7A8, true);
		}
	}
}
