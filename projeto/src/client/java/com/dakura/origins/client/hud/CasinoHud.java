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
	private static final net.minecraft.util.Identifier WHEEL_TEX = com.dakura.origins.DakuraOrigins.id("textures/gui/casino_wheel.png");
	private static final net.minecraft.util.Identifier POINTER_TEX = com.dakura.origins.DakuraOrigins.id("textures/gui/casino_pointer.png");
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

		// roda em pixel art (desenho do Artur) girando sob o ponteiro fixo
		ctx.fill(cx - 62, cy - 62, cx + 62, cy + 66, 0x9A140C06);
		var mtx = ctx.getMatrices();
		mtx.push();
		mtx.translate(cx, cy, 0);
		mtx.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Z.rotation((float) -angle));
		float sc = 112f / 256f;
		mtx.scale(sc, sc, 1f);
		ctx.drawTexture(WHEEL_TEX, -128, -128, 0, 0, 256, 256, 256, 256);
		mtx.pop();
		int top = (int) Math.floorMod(Math.round(angle / step), n);
		if (top != lastTop && tt < 1f) {
			lastTop = top;
			mc.player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 0.35f, 1.2f + tt * 0.6f);
		}
		// ponteiro (a peça verde do desenho), preso no topo
		mtx.push();
		mtx.translate(cx, cy - 56, 50);
		mtx.scale(0.55f, 0.55f, 1f);
		ctx.drawTexture(POINTER_TEX, -10, -4, 0, 0, 19, 46, 19, 46);
		mtx.pop();
		// resultado
		if (tt >= 1f) {
			boolean blink = (el / 150) % 2 == 0;
			Text res = Text.translatable(result == 0 ? "dakura_origins.casino.lose" : result == 1 ? "dakura_origins.casino.even"
					: "dakura_origins.casino.win", LABEL[result]).formatted(Formatting.BOLD);
			int col = result == 0 ? 0xFFFF6B6B : result == 1 ? 0xFFDDDDDD : (blink ? 0xFFFFE066 : 0xFFFFB020);
			ctx.drawText(mc.textRenderer, res, cx - mc.textRenderer.getWidth(res) / 2, cy + 58, col, true);
		} else {
			Text sp = Text.translatable("dakura_origins.casino.spinning");
			ctx.drawText(mc.textRenderer, sp, cx - mc.textRenderer.getWidth(sp) / 2, cy + 58, 0xFFE8D7A8, true);
		}
	}
}
