package com.dakura.origins.client.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * A roda do Cassino (Lilac): 12 fichas num círculo que gira, desacelera e para sob o ponteiro dourado
 * no resultado que o servidor sorteou. Depois mostra o prêmio por um instante.
 */
public final class CasinoHud {
	private static final String[] LABEL = {"✗", "×1", "×2", "×3", "×5"};
	private static final int[] COLOR = {0xFF7A1424, 0xFF5C5A63, 0xFF2F8A4C, 0xFF2E5FA8, 0xFFD4A21E};
	private static final int[] WHEEL = {0, 1, 2, 0, 1, 3, 0, 2, 1, 0, 4, 2};
	private static long start = -1;
	private static int result, tier, spinMs, lastTop = -1;

	private CasinoHud() {}

	public static void start(int res, int t, int spinTicks) {
		result = res;
		tier = t;
		spinMs = Math.max(1000, spinTicks * 50);
		start = System.currentTimeMillis();
		lastTop = -1;
	}

	private static int target() {
		for (int i = 0; i < WHEEL.length; i++) if (WHEEL[i] == result) return i;
		return 0;
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
		double total = (4 + tier) * Math.PI * 2 + target() * step;
		double angle = total * ease;
		int cx = ctx.getScaledWindowWidth() / 2, cy = 74, r = 44;

		// painel
		ctx.fill(cx - 62, cy - 62, cx + 62, cy + 66, 0xB0140C06);
		ctx.fill(cx - 62, cy - 62, cx + 62, cy - 61, 0xFFD4A21E);
		ctx.fill(cx - 62, cy + 65, cx + 62, cy + 66, 0xFFD4A21E);
		ctx.fill(cx - 62, cy - 62, cx - 61, cy + 66, 0xFFD4A21E);
		ctx.fill(cx + 61, cy - 62, cx + 62, cy + 66, 0xFFD4A21E);
		// luzes do aro piscando
		for (int i = 0; i < 24; i++) {
			double a = i * Math.PI / 12;
			int lx = cx + (int) Math.round(Math.cos(a) * (r + 12)), ly = cy + (int) Math.round(Math.sin(a) * (r + 12));
			boolean on = ((el / 120) + i) % 2 == 0;
			ctx.fill(lx - 1, ly - 1, lx + 2, ly + 2, on ? 0xFFFFE9A0 : 0xFF6B4A10);
		}
		// fichas
		int top = 0;
		double best = 9;
		for (int i = 0; i < n; i++) {
			double a = i * step - angle - Math.PI / 2;
			int x = cx + (int) Math.round(Math.cos(a) * r), y = cy + (int) Math.round(Math.sin(a) * r);
			double dTop = Math.abs(Math.atan2(Math.sin(a + Math.PI / 2), Math.cos(a + Math.PI / 2)));
			if (dTop < best) {
				best = dTop;
				top = i;
			}
			int c = COLOR[WHEEL[i]];
			ctx.fill(x - 10, y - 7, x + 10, y + 7, 0xFF1A1208);
			ctx.fill(x - 9, y - 6, x + 9, y + 6, c);
			ctx.fill(x - 9, y - 6, x + 9, y - 5, 0x40FFFFFF);
			Text l = Text.literal(LABEL[WHEEL[i]]).formatted(Formatting.BOLD);
			ctx.drawText(mc.textRenderer, l, x - mc.textRenderer.getWidth(l) / 2, y - 4, 0xFFFFFFFF, true);
		}
		if (top != lastTop && tt < 1f) {
			lastTop = top;
			mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 0.35f, 1.2f + tt * 0.6f);
		}
		// ponteiro
		for (int i = 0; i < 6; i++) ctx.fill(cx - 6 + i, cy - r - 22 + i, cx + 7 - i, cy - r - 21 + i, 0xFFFFD84A);
		// centro
		ctx.fill(cx - 16, cy - 16, cx + 16, cy + 16, 0xFF2A1A08);
		ctx.fill(cx - 15, cy - 15, cx + 15, cy + 15, 0xFFB8861B);
		ctx.fill(cx - 13, cy - 13, cx + 13, cy + 13, 0xFF3A2408);
		Text hub = Text.literal("$").formatted(Formatting.BOLD, Formatting.GOLD);
		ctx.getMatrices().push();
		ctx.getMatrices().translate(cx, cy - 6, 0);
		ctx.getMatrices().scale(1.6f, 1.6f, 1f);
		ctx.drawText(mc.textRenderer, hub, -mc.textRenderer.getWidth(hub) / 2, 0, 0xFFFFD84A, true);
		ctx.getMatrices().pop();
		// resultado
		if (tt >= 1f) {
			boolean blink = (el / 150) % 2 == 0;
			Text res = Text.translatable(result == 0 ? "dakura_origins.casino.lose" : result == 1 ? "dakura_origins.casino.even"
					: "dakura_origins.casino.win", LABEL[result]).formatted(Formatting.BOLD);
			int col = result == 0 ? 0xFFFF6B6B : result == 1 ? 0xFFDDDDDD : (blink ? 0xFFFFE066 : 0xFFFFB020);
			ctx.drawText(mc.textRenderer, res, cx - mc.textRenderer.getWidth(res) / 2, cy + 52, col, true);
		} else {
			Text sp = Text.translatable("dakura_origins.casino.spinning");
			ctx.drawText(mc.textRenderer, sp, cx - mc.textRenderer.getWidth(sp) / 2, cy + 52, 0xFFE8D7A8, true);
		}
	}
}
