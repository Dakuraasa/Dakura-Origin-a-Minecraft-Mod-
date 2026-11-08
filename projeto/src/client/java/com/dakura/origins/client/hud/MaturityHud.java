package com.dakura.origins.client.hud;

import com.dakura.origins.power.types.MaturityPower;
import com.dakura.origins.race.Race;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.Optional;

/**
 * Barra de maturidade no lugar da barra de experiência: mesmo lugar, mesmo fundo
 * vanilla, preenchimento com a cor do estágio (muda aos poucos ao longo da progressão).
 */
public final class MaturityHud {
	private static final Identifier BACKGROUND = Identifier.ofVanilla("hud/experience_bar_background");
	private static final int WIDTH = 182, HEIGHT = 5;

	private MaturityHud() {}

	private static Optional<MaturityPower> power() {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		return player == null ? Optional.empty() : MaturityPower.of(player);
	}

	public static boolean active() {
		return power().isPresent();
	}

	/** @return true se desenhou (e a barra vanilla deve ser pulada). */
	public static boolean render(DrawContext ctx, int x) {
		Optional<MaturityPower> opt = power();
		if (opt.isEmpty()) return false;
		MaturityPower m = opt.get();
		int points = MaturityPower.clientPoints;
		int y = ctx.getScaledWindowHeight() - 32 + 3;

		ctx.drawGuiTexture(BACKGROUND, x, y, WIDTH, HEIGHT);

		int fill = Math.round(m.stageProgress(points) * WIDTH);
		if (fill <= 0) return true;
		int color = m.barColor(points);
		if (points >= m.maxPoints()) {
			// estágio final: um brilho lento, sem custo
			float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 600.0);
			color = Race.Palette.mix(color, 0xFFFFFFFF, 0.18f * pulse);
		}
		int dark = Race.Palette.mix(color, 0xFF000000, 0.45f);
		int light = Race.Palette.mix(color, 0xFFFFFFFF, 0.35f);
		ctx.fill(x, y, x + fill, y + HEIGHT, dark);
		if (fill > 2) {
			ctx.fill(x + 1, y + 1, x + fill - 1, y + HEIGHT - 1, color);
			ctx.fill(x + 1, y + 1, x + fill - 1, y + 2, light);
		}
		return true;
	}
}
