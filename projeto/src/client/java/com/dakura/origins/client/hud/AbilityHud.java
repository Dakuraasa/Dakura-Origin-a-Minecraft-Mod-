package com.dakura.origins.client.hud;

import com.dakura.origins.power.EntityFlags;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.client.DakuraOriginsClient;
import com.dakura.origins.client.anim.AbilityAnimations;
import com.dakura.origins.power.ability.BatFormState;
import com.dakura.origins.race.Race;
import com.dakura.origins.race.RaceRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Locale;
import java.util.Optional;

/**
 * Canto inferior esquerdo da tela, durante o jogo:
 *   [tecla 1] [tecla 2]   [livro]
 * Cada tecla mostra o keybind ATUAL (lido a cada quadro, então trocar a tecla nas
 * opções aparece na hora) e a recarga no estilo LoL: sombra girando no sentido horário
 * e os segundos restantes no meio; ao ficar pronta, um brilho rápido.
 * O livro abre a página de consulta da raça (pela tecla dele, ou clicando com o chat aberto).
 *
 * Custo: alguns retângulos por quadro; nada é alocado enquanto nada muda.
 */
public final class AbilityHud {
	private static final Identifier BOOK = DakuraOrigins.id("textures/gui/book.png");
	private static final int KEY = 22;          // tamanho da tecla
	private static final int GAP = 3;           // entre as teclas
	private static final int INFO_GAP = 7;      // entre as teclas e o livro
	private static final int MARGIN = 4;
	private static final long FLASH_MS = 300;

	// ------------------------------------------------------------------ recargas (cliente)

	private static final long[] READY_AT = new long[2];
	private static final int[] TOTAL = new int[2];
	private static final long[] FLASH_START = {-1, -1};
	private static final boolean[] COOLING = new boolean[2];

	// ------------------------------------------------------------------ cache de rótulos

	private static final String[] LABEL_KEY = new String[3];
	private static final Text[] LABEL = new Text[3];

	// posição do livro no último quadro (para o clique com o chat aberto)
	private static int infoX = -1, infoY = -1;

	private AbilityHud() {}

	/** CooldownPayload: slot -1 zera tudo. */
	public static void setCooldown(int slot, int total, int remaining) {
		MinecraftClient mc = MinecraftClient.getInstance();
		long now = mc.world != null ? mc.world.getTime() : 0;
		if (slot < 0) {
			for (int i = 0; i < READY_AT.length; i++) {
				READY_AT[i] = 0;
				TOTAL[i] = 0;
				COOLING[i] = false;
				FLASH_START[i] = -1;
			}
			return;
		}
		if (slot >= READY_AT.length) return;
		READY_AT[slot] = now + remaining;
		TOTAL[slot] = Math.max(1, total);
		COOLING[slot] = remaining > 0;
	}

	public static void clear() {
		setCooldown(-1, 0, 0);
		infoX = infoY = -1;
	}

	private static Optional<Race> race() {
		if (DakuraOriginsClient.mustChoose || DakuraOriginsClient.currentRace.isEmpty()) return Optional.empty();
		return RaceRegistry.get(DakuraOriginsClient.currentRace);
	}

	private static boolean visible(MinecraftClient mc) {
		if (mc.player == null || mc.world == null || mc.options.hudHidden) return false;
		// em espectador só aparece na Forma Espectral (para mostrar como voltar)
		return !mc.player.isSpectator() || race().map(r -> r.abilities().stream()
				.anyMatch(a -> a.type().equals("dakura_origins:phase"))).orElse(false);
	}

	// ------------------------------------------------------------------ desenho

	public static void render(DrawContext ctx, RenderTickCounter counter) {
		MinecraftClient mc = MinecraftClient.getInstance();
		infoX = infoY = -1;
		if (!visible(mc)) return;
		Optional<Race> opt = race();
		if (opt.isEmpty()) return;
		Race race = opt.get();

		int count = Math.min(2, race.abilities().size());
		int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
		int rowW = count * KEY + Math.max(0, count - 1) * GAP + (count > 0 ? INFO_GAP : 0) + KEY;
		// espaço livre à esquerda da hotbar (e do slot da mão secundária)
		boolean row = MARGIN + rowW <= w / 2 - 91 - 29 - 4;

		float now = mc.world.getTime() + counter.getTickDelta(false);
		long wall = System.currentTimeMillis();
		int accent = race.palette().ink()[4];
		INK = race.palette().ink();
		TextRenderer tr = mc.textRenderer;

		int x = MARGIN, y = h - MARGIN - KEY;
		KeyBinding[] binds = {DakuraOriginsClient.ABILITY_1, DakuraOriginsClient.ABILITY_2};
		for (int i = 0; i < count; i++) {
			drawAbilityKey(ctx, tr, x, y, i, binds[i], race.abilities().get(i), now, wall, accent, mc);
			if (row) x += KEY + GAP;
			else y -= KEY + GAP;
		}
		if (count > 0) {
			if (row) x += INFO_GAP - GAP;
			else y -= INFO_GAP - GAP;
		}
		drawInfoButton(ctx, tr, x, y, accent, mc);
	}

	private static void drawAbilityKey(DrawContext ctx, TextRenderer tr, int x, int y, int slot, KeyBinding bind,
									   Race.AbilityInfo info, float now, long wall, int accent, MinecraftClient mc) {
		float remaining = Math.max(0f, READY_AT[slot] - now);
		boolean cooling = remaining > 0f;
		if (COOLING[slot] && !cooling) FLASH_START[slot] = wall; // acabou de ficar pronta
		COOLING[slot] = cooling;

		// habilidade automática: não é botão (sem tecla, sem afundar); só mostra estado e recarga
		boolean auto = info.type().equals("dakura_origins:passive");
		boolean pressed = !auto && bind.isPressed() && mc.currentScreen == null;
		boolean active = isActive(info, mc);
		int border = active ? pulse(0xFFF2B33D, 0xFFFFF2C4, wall)
				: cooling ? 0xFF3A3846
				: accent;
		int faceTop = keycap(ctx, x, y, border, pressed);
		int fx = x + 2, fy = faceTop, fw = KEY - 4, fh = KEY - 7;

		if (cooling) {
			float frac = Math.min(1f, remaining / TOTAL[slot]);
			sweep(ctx, fx, fy, fw, fh, frac, 0xA8000000);
			// segundos restantes: inteiros, e com uma casa decimal no último segundo
			float s = remaining / 20f;
			String secs = s >= 1f ? String.valueOf((int) Math.ceil(s)) : String.format(Locale.ROOT, "%.1f", s);
			int sw = tr.getWidth(secs);
			ctx.drawText(tr, secs, fx + (fw - sw) / 2 + 1, fy + (fh - 8) / 2 + 1, 0xFFFFFFFF, true);
			// tecla pequena no canto
			if (!auto) drawLabel(ctx, tr, slot, bind, fx + fw, fy + fh, 0.5f, 0xFFB8B6C8, true);
		} else if (auto) {
			// "AUTO" pequeno no lugar da tecla
			ctx.getMatrices().push();
			ctx.getMatrices().translate(fx + fw / 2f, fy + fh / 2f - 2, 0);
			ctx.getMatrices().scale(0.5f, 0.5f, 1f);
			ctx.drawText(tr, "AUTO", -tr.getWidth("AUTO") / 2, 0, 0xFFB8B6C8, true);
			ctx.getMatrices().pop();
		} else {
			drawLabel(ctx, tr, slot, bind, fx + fw / 2, fy + fh / 2, 1f, 0xFFFFFFFF, false);
		}

		long since = FLASH_START[slot] < 0 ? Long.MAX_VALUE : wall - FLASH_START[slot];
		if (since < FLASH_MS) {
			int a = Math.round((1f - since / (float) FLASH_MS) * 0xB0);
			ctx.fill(fx, fy, fx + fw, fy + fh, (a << 24) | 0xFFFFFF);
		}
	}

	/** Habilidades com estado contínuo acendem a tecla enquanto estão ligadas. */
	private static boolean isActive(Race.AbilityInfo info, MinecraftClient mc) {
		if (mc.player == null) return false;
		return switch (info.type()) {
			case "dakura_origins:bat_form" -> BatFormState.CLIENT.contains(mc.player.getId());
			case "dakura_origins:breath" -> AbilityAnimations.active("breath").contains(mc.player.getId());
			case "dakura_origins:phase" -> mc.player.isSpectator();
			case "dakura_origins:passive" -> EntityFlags.clientHas(mc.player, "berserk");
			case "dakura_origins:morph" -> com.dakura.origins.power.ability.MorphAbility.morphType(mc.player) != null;
			case "dakura_origins:charged_jump", "dakura_origins:meteor" -> AbilityAnimations.active("crouch").contains(mc.player.getId())
					|| EntityFlags.clientHas(mc.player, "meteor");
			default -> false;
		};
	}

	private static void drawInfoButton(DrawContext ctx, TextRenderer tr, int x, int y, int accent, MinecraftClient mc) {
		infoX = x;
		infoY = y;
		boolean chat = mc.currentScreen instanceof ChatScreen;
		boolean hover = false;
		if (chat) {
			double mx = mc.mouse.getX() * ctx.getScaledWindowWidth() / mc.getWindow().getWidth();
			double my = mc.mouse.getY() * ctx.getScaledWindowHeight() / mc.getWindow().getHeight();
			hover = infoHit(mx, my);
		}
		boolean pressed = DakuraOriginsClient.RACE_PAGE.isPressed() && mc.currentScreen == null;
		int border = hover ? 0xFFFBE6EA : 0xFFF9C6CB;
		int faceTop = keycap(ctx, x, y, border, pressed);
		ctx.drawTexture(BOOK, x + 4, faceTop, 14, 14, 0f, 0f, 43, 43, 43, 43);
		drawLabel(ctx, tr, 2, DakuraOriginsClient.RACE_PAGE, x + KEY - 2, faceTop + KEY - 6, 0.5f, 0xFFFFFFFF, true);
		if (hover) {
			ctx.drawTooltip(tr, Text.translatable("dakura_origins.hud.page"), x + KEY, y);
		}
	}

	/** true se (mx, my), em coordenadas de interface, está sobre o livro. */
	public static boolean infoHit(double mx, double my) {
		return infoX >= 0 && mx >= infoX && mx < infoX + KEY && my >= infoY && my < infoY + KEY;
	}

	// ------------------------------------------------------------------ peças

	/**
	 * Tecla de teclado em pixel art: contorno, lateral escura embaixo (dá volume) e topo
	 * com reflexo. Apertada, ela "afunda" 1 pixel. @return y onde começa o rosto da tecla.
	 */
	/** Tons da raça atual (do escuro ao claro), usados para pintar as teclas. */
	private static int[] INK = {0x141219, 0x2A2733, 0x3A3846, 0x6A6880, 0xB8B6C8, 0xFFFFFF};

	/** Tecla com a cor da raça: aro arredondado, lateral escura, rosto em degradê, brilho e cantos de gema. */
	private static int keycap(DrawContext ctx, int x, int y, int border, boolean pressed) {
		int x2 = x + KEY, y2 = y + KEY;
		// sombra no chão
		ctx.fill(x + 2, y2, x2 - 1, y2 + 1, 0x50000000);
		// aro com cantos bem arredondados
		ctx.fill(x + 2, y, x2 - 2, y2, border);
		ctx.fill(x, y + 2, x2, y2 - 2, border);
		ctx.fill(x + 1, y + 1, x2 - 1, y2 - 1, border);
		// lateral (profundidade da tecla)
		int side = 0xF0000000 | Race.Palette.mix(INK[1], 0x000000, 0.35f);
		ctx.fill(x + 2, y + 1, x2 - 2, y2 - 1, side);
		ctx.fill(x + 1, y + 2, x2 - 1, y2 - 2, side);
		int top = pressed ? y + 3 : y + 2;
		int bottom = pressed ? y2 - 2 : y2 - 4;
		// rosto: degradê vertical do tom médio ao escuro
		int h = Math.max(1, bottom - top);
		for (int i = 0; i < h; i++) {
			int c = Race.Palette.mix(INK[3], INK[2], i / (float) h);
			int inset = (i == 0 || i == h - 1) ? 3 : 2;
			ctx.fill(x + inset, top + i, x2 - inset, top + i + 1, 0xF0000000 | c);
		}
		// brilho de cima e da esquerda, sombra interna embaixo
		ctx.fill(x + 3, top, x2 - 3, top + 1, 0x60FFFFFF);
		ctx.fill(x + 2, top + 1, x + 3, bottom - 1, 0x26FFFFFF);
		ctx.fill(x + 3, bottom - 1, x2 - 3, bottom, 0x30000000);
		// cantos de gema
		int gem = 0x90000000 | INK[5];
		ctx.fill(x + 3, top + 2, x + 4, top + 3, gem);
		ctx.fill(x2 - 4, top + 2, x2 - 3, top + 3, gem);
		return top + 1;
	}

	/**
	 * Sombra de recarga em forma de relógio: começa cobrindo tudo e vai sendo
	 * "comida" no sentido horário a partir do meio-dia. Colunas contíguas viram um retângulo só.
	 */
	private static void sweep(DrawContext ctx, int x, int y, int w, int h, float remainingFrac, int color) {
		if (remainingFrac >= 0.999f) {
			ctx.fill(x, y, x + w, y + h, color);
			return;
		}
		double cut = (1.0 - remainingFrac) * Math.PI * 2; // ângulo já liberado
		double cx = w / 2.0, cy = h / 2.0;
		for (int col = 0; col < w; col++) {
			int runStart = -1;
			for (int row = 0; row <= h; row++) {
				boolean dark = false;
				if (row < h) {
					double dx = col + 0.5 - cx, dy = row + 0.5 - cy;
					double a = Math.atan2(dx, -dy);
					if (a < 0) a += Math.PI * 2;
					dark = a >= cut;
				}
				if (dark && runStart < 0) runStart = row;
				else if (!dark && runStart >= 0) {
					ctx.fill(x + col, y + runStart, x + col + 1, y + row, color);
					runStart = -1;
				}
			}
		}
	}

	/**
	 * Nome da tecla atual. Lido do keybind a cada quadro (só recria o texto quando a tecla muda),
	 * reduzido para caber. corner = alinha pelo canto inferior direito em vez do centro.
	 */
	private static void drawLabel(DrawContext ctx, TextRenderer tr, int idx, KeyBinding bind, int ax, int ay,
								  float baseScale, int color, boolean corner) {
		String k = bind.getBoundKeyTranslationKey();
		if (!k.equals(LABEL_KEY[idx])) {
			LABEL_KEY[idx] = k;
			LABEL[idx] = bind.isUnbound() ? Text.literal("—") : bind.getBoundKeyLocalizedText();
		}
		Text label = LABEL[idx];
		int tw = tr.getWidth(label);
		float maxW = corner ? KEY - 4 : KEY - 6;
		float scale = baseScale;
		if (tw * scale > maxW) scale = Math.max(0.5f, maxW / tw);
		ctx.getMatrices().push();
		if (corner) {
			ctx.getMatrices().translate(ax - tw * scale, ay - 8 * scale, 0f);
		} else {
			ctx.getMatrices().translate(ax - tw * scale / 2f, ay - 4 * scale, 0f);
		}
		ctx.getMatrices().scale(scale, scale, 1f);
		ctx.drawText(tr, label, 0, 0, color, true);
		ctx.getMatrices().pop();
	}

	private static int pulse(int a, int b, long wall) {
		float t = 0.5f + 0.5f * (float) Math.sin(wall / 160.0);
		return Race.Palette.mix(a, b, t);
	}
}
