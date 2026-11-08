package com.dakura.origins.client.screen;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.client.DakuraOriginsClient;
import com.dakura.origins.client.render.DakuraFonts;
import com.dakura.origins.client.render.InkBackdrop;
import com.dakura.origins.client.render.PageLayout;
import com.dakura.origins.client.render.PaperPage;
import com.dakura.origins.client.render.PixelNoise;
import com.dakura.origins.network.ChooseRacePayload;
import com.dakura.origins.power.types.MaturityPower;
import com.dakura.origins.race.Race;
import com.dakura.origins.race.RaceRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * Tela de origens:
 *  1) FECHADO – galáxia animada, título com a sereia e o livro flutuando.
 *  2) ABRINDO – o livro cresce num clarão.
 *  3) ABERTO  – a página única da raça (jade), com a nebulosa invadindo as bordas.
 * A tela fica travada até o jogador escolher.
 *
 * Também serve de página de consulta ("sua raça", aberta pela HUD): mesma página, mesmo visual,
 * direto aberta, sem setas nem escolha, com vantagens/fraquezas agrupadas, controles e recargas.
 */
public class RaceSelectionScreen extends Screen {
	private static final Identifier TITLE = DakuraOrigins.id("textures/gui/title.png");
	private static final Identifier MERMAID = DakuraOrigins.id("textures/gui/mermaid.png");
	private static final Identifier BOOK = DakuraOrigins.id("textures/gui/book.png");
	private static final Identifier ARROW_LEFT = DakuraOrigins.id("textures/gui/arrow_left.png");
	private static final Identifier ARROW_RIGHT = DakuraOrigins.id("textures/gui/arrow_right.png");
	private static final int TITLE_TW = 720, TITLE_TH = 240;
	private static final int MERMAID_W = 79, MERMAID_H = 78;
	private static final int BOOK_W = 43, BOOK_H = 43;
	private static final long OPEN_MS = 750, FLIP_MS = 420;
	private static final int PAD = 16;

	private enum State { CLOSED, OPENING, OPEN }

	private final List<Race> races;
	/** true = página de consulta da própria raça (fecha com ESC, não escolhe nada). */
	private final boolean viewOnly;
	private static final long POP_MS = 260;
	private final InkBackdrop backdrop = new InkBackdrop();
	private final PaperPage paper = new PaperPage();
	private final long openedAt = System.currentTimeMillis();

	private State state = State.CLOSED;
	private long stateStart = System.currentTimeMillis();
	private int index;
	private int flipFrom, flipTo;
	private long flipStart = -1;

	private PageLayout layout;
	private float scroll;
	private boolean armed, waiting;

	// geometria
	private int titleX, titleY, titleW, titleH;
	private boolean titleWhenOpen;
	private int closedScale, closedX, closedY, closedW, closedH;
	private int pageX, pageY, pageW, pageH;
	private int btnX, btnY, btnW, btnH;
	private int leftArrowX, rightArrowX, arrowY;

	public RaceSelectionScreen(String preselect) {
		super(Text.translatable("dakura_origins.screen.title"));
		this.viewOnly = false;
		this.races = RaceRegistry.all();
		for (int i = 0; i < races.size(); i++) {
			if (races.get(i).id().equals(preselect)) index = i;
		}
	}

	private RaceSelectionScreen(Race race) {
		super(Text.translatable("dakura_origins.screen.title"));
		this.viewOnly = true;
		this.races = List.of(race);
		this.state = State.OPEN;
	}

	public boolean isViewOnly() {
		return viewOnly;
	}

	/** Página de consulta da raça atual (null se o jogador ainda não tem raça). */
	public static RaceSelectionScreen info(String raceId) {
		return RaceRegistry.get(raceId).map(RaceSelectionScreen::new).orElse(null);
	}

	// ================================================================== layout

	@Override
	protected void init() {
		// título (texto elegante + sereia à esquerda)
		titleW = Math.min(Math.round(width * 0.5f), 220);
		titleH = Math.round(titleW * TITLE_TH / (float) TITLE_TW);
		int blockW = MERMAID_W - 8 + titleW;
		titleX = (width - blockW) / 2 + MERMAID_W - 8;
		titleY = Math.max(4, 4 + (MERMAID_H - titleH) / 2);
		int titleBottom = Math.max(titleY + titleH, 4 + MERMAID_H);

		// livro fechado
		closedScale = Math.max(1, Math.min(3, (height - titleBottom - 46) / BOOK_H));
		closedW = BOOK_W * closedScale;
		closedH = BOOK_H * closedScale;
		closedX = (width - closedW) / 2;
		closedY = titleBottom + Math.max(6, (height - titleBottom - 30 - closedH) / 2);

		// página aberta (o título só aparece junto se houver espaço)
		titleWhenOpen = height >= 330;
		pageY = titleWhenOpen ? titleBottom + 4 : 10;
		pageH = height - pageY - 30;
		pageW = Math.max(230, Math.min(300, Math.round(pageH * 1.0f)));
		pageW = Math.min(pageW, width - 90);
		pageX = (width - pageW) / 2;

		btnW = viewOnly ? 110 : 150;
		btnH = 18;
		btnX = (width - btnW) / 2;
		btnY = pageY + pageH + 7;

		arrowY = pageY + pageH / 2 - 15;
		leftArrowX = pageX - 30;
		rightArrowX = pageX + pageW + 12;

		applyBackdropFocus();
		layout = null;
		scroll = 0;
	}

	private void applyBackdropFocus() {
		if (state == State.CLOSED) backdrop.layout(width, height, closedX, closedY, closedW, closedH);
		else backdrop.layout(width, height, pageX, pageY, pageW, pageH);
	}

	private int contentW() { return pageW - PAD * 2; }
	private int contentTop() { return pageY + PAD + 2; }
	private int viewportH() { return pageH - PAD * 2 - 12; }

	private PageLayout layoutFor(Race race) {
		int innerW = contentW();
		if (layout == null || !layout.raceId.equals(race.id()) || layout.innerWidth() != innerW) {
			List<Text> keys = List.of(
					DakuraOriginsClient.ABILITY_1.getBoundKeyLocalizedText(),
					DakuraOriginsClient.ABILITY_2.getBoundKeyLocalizedText());
			layout = PageLayout.build(race, innerW, textRenderer, keys, viewOnly ? details() : null);
		}
		return layout;
	}

	private PageLayout.Details details() {
		String status = null;
		if (client != null && client.player != null) {
			var m = MaturityPower.of(client.player);
			if (m.isPresent()) {
				int pts = MaturityPower.clientPoints;
				status = I18n.translate("dakura_origins.page.maturity", m.get().stageOf(pts) + 1, m.get().stageCount(),
						Math.round(m.get().totalProgress(pts) * 100));
			}
		}
		return new PageLayout.Details(DakuraOriginsClient.RACE_PAGE.getBoundKeyLocalizedText(), status);
	}

	private int maxScroll(PageLayout l) {
		return Math.max(0, l.height() - viewportH());
	}

	// ================================================================== desenho

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		if (races.isEmpty()) {
			ctx.fill(0, 0, width, height, 0xFF0B0718);
			ctx.drawCenteredTextWithShadow(textRenderer, Text.translatable("dakura_origins.screen.empty"), width / 2, height / 2, 0xFFFFFFFF);
			return;
		}
		long now = System.currentTimeMillis();
		float t = (now - openedAt) / 1000f;

		float flip = 1f;
		if (flipStart >= 0) {
			flip = Math.min(1f, (now - flipStart) / (float) FLIP_MS);
			if (flip >= 1f) flipStart = -1;
		}
		boolean flipping = flipStart >= 0;
		Race shown = races.get(flipping && flip < 0.5f ? flipFrom : index);
		Race.Palette pal = flipping
				? races.get(flipFrom).palette().lerp(races.get(flipTo).palette(), ease(flip))
				: races.get(index).palette();

		backdrop.render(ctx, pal, t, flipping);

		switch (state) {
			case CLOSED -> renderClosed(ctx, mouseX, mouseY, pal, t, 1f, 0f);
			case OPENING -> renderOpening(ctx, mouseX, mouseY, pal, t, shown, now);
			case OPEN -> renderOpen(ctx, mouseX, mouseY, pal, t, shown, flipping ? flip : -1f, viewOnly ? pop(now) : 1f);
		}
	}

	// ---------------------------------------------------------------- fechado

	private void renderClosed(DrawContext ctx, int mouseX, int mouseY, Race.Palette pal, float t, float scale, float lift) {
		drawTitle(ctx, t);

		boolean hover = state == State.CLOSED && inside(mouseX, mouseY, closedX - 6, closedY - 6, closedW + 12, closedH + 12);
		float bob = (float) Math.sin(t * 1.6f) * 4f - (hover ? 3f : 0f) - lift;
		float cx = closedX + closedW / 2f, cy = closedY + closedH / 2f + bob;

		// halo pulsante
		float pulse = 0.85f + 0.15f * (float) Math.sin(t * 2.1f);
		int haloBase = Math.round(closedH * 0.62f * pulse);
		int[] alphas = hover ? new int[]{0x2A, 0x3C, 0x55} : new int[]{0x1E, 0x2C, 0x40};
		for (int i = 0; i < 3; i++) {
			disc(ctx, Math.round(cx), Math.round(cy), Math.round(haloBase * (1f - i * 0.22f)), (alphas[i] << 24) | 0xF9C6CB, closedScale);
		}

		// partículas orbitando
		int u = Math.max(1, closedScale);
		for (int i = 0; i < 14; i++) {
			double ang = i * Math.PI * 2 / 14 + t * (i % 2 == 0 ? 0.35 : -0.25);
			float rx = closedW * 0.9f + 6f * (float) Math.sin(t * 1.3f + i);
			float ry = closedH * 0.7f;
			int px = Math.round(cx + (float) Math.cos(ang) * rx);
			int py = Math.round(cy + (float) Math.sin(ang) * ry);
			float tw = (float) Math.sin(t * 3f + i * 1.7f);
			int col = tw > 0.4f ? pal.glow() : tw > -0.3f ? 0xFFF9C6CB : pal.ink()[4];
			sparkle(ctx, px, py, u, col, i % 3 == 0);
		}
		// faíscas subindo
		for (int i = 0; i < 8; i++) {
			float life = (t * 0.35f + i * 0.137f) % 1f;
			int px = Math.round(cx + (PixelNoise.hash(i * 17 + 3, 91) - 0.5f) * closedW * 1.6f);
			int py = Math.round(cy + closedH * 0.6f - life * closedH * 1.4f);
			ctx.fill(px, py, px + u, py + u, life < 0.7f ? 0xFFF9C6CB : pal.ink()[4]);
		}

		// o livro
		ctx.getMatrices().push();
		ctx.getMatrices().translate(cx, cy, 0f);
		ctx.getMatrices().scale(scale, scale, 1f);
		ctx.drawTexture(BOOK, -closedW / 2, -closedH / 2, closedW, closedH, 0f, 0f, BOOK_W, BOOK_H, BOOK_W, BOOK_H);
		ctx.getMatrices().pop();

		if (state == State.CLOSED) {
			float a = 0.55f + 0.45f * (float) Math.sin(t * 2.4f);
			int alpha = Math.max(0x30, Math.round(a * 255));
			MutableText hint = DakuraFonts.heading(I18n.translate("dakura_origins.screen.hint"));
			int hw = textRenderer.getWidth(hint);
			ctx.drawText(textRenderer, hint, (width - hw) / 2, closedY + closedH + 14, (alpha << 24) | (pal.glow() & 0xFFFFFF), false);
		}
	}

	/** Entrada suave da página de consulta. */
	private float pop(long now) {
		float p = Math.min(1f, (now - openedAt) / (float) POP_MS);
		return 0.9f + 0.1f * ease(p);
	}

	private void renderOpening(DrawContext ctx, int mouseX, int mouseY, Race.Palette pal, float t, Race shown, long now) {
		float p = Math.min(1f, (now - stateStart) / (float) OPEN_MS);
		if (p < 0.45f) {
			float q = p / 0.45f;
			renderClosed(ctx, mouseX, mouseY, pal, t, 1f + ease(q) * 0.6f, q * 10f);
			flash(ctx, q * 0.85f);
		} else {
			float q = (p - 0.45f) / 0.55f;
			renderOpen(ctx, mouseX, mouseY, pal, t, shown, -1f, 0.86f + 0.14f * ease(q));
			flash(ctx, (1f - q) * 0.85f);
		}
		if (p >= 1f) {
			state = State.OPEN;
			applyBackdropFocus();
		}
	}

	private void flash(DrawContext ctx, float a) {
		int alpha = Math.max(0, Math.min(255, Math.round(a * 255)));
		ctx.fill(0, 0, width, height, (alpha << 24) | 0xFBE6EA);
	}

	// ---------------------------------------------------------------- aberto

	private void renderOpen(DrawContext ctx, int mouseX, int mouseY, Race.Palette pal, float t, Race race, float flip, float scale) {
		if (titleWhenOpen) drawTitle(ctx, t);

		float squash = flip >= 0f ? Math.max(0.03f, Math.abs((float) Math.cos(flip * Math.PI))) : 1f;
		float cx = pageX + pageW / 2f, cy = pageY + pageH / 2f;
		ctx.getMatrices().push();
		ctx.getMatrices().translate(cx, cy, 0f);
		ctx.getMatrices().scale(scale * squash, scale, 1f);
		ctx.getMatrices().translate(-cx, -cy, 0f);

		paper.render(ctx, pageX, pageY, pageW, pageH);

		PageLayout l = layoutFor(race);
		scroll = Math.max(0, Math.min(scroll, maxScroll(l)));
		int top = contentTop();
		ctx.enableScissor(pageX + 8, top - 6, pageX + pageW - 8, top + viewportH());
		l.render(ctx, pageX + PAD, top - Math.round(scroll), pal);
		ctx.disableScissor();
		drawScrollBar(ctx, l, top);

		MutableText folio = DakuraFonts.body("· " + PageLayout.roman(race.order()) + " ·");
		ctx.drawText(textRenderer, folio, pageX + (pageW - textRenderer.getWidth(folio)) / 2,
				pageY + pageH - PAD - 8, 0xFF8FB89A, false);

		if (state == State.OPEN) backdrop.renderPageEdges(ctx);
		ctx.getMatrices().pop();

		if (state == State.OPEN) {
			if (!viewOnly) drawArrows(ctx, mouseX, mouseY, pal);
			drawButton(ctx, mouseX, mouseY);
		}
	}

	private void drawScrollBar(DrawContext ctx, PageLayout l, int top) {
		int max = maxScroll(l);
		if (max <= 0) return;
		int x = pageX + pageW - 13;
		int trackH = viewportH();
		int barH = Math.max(12, trackH * trackH / l.height());
		int barY = top + Math.round((trackH - barH) * (scroll / max));
		ctx.fill(x, top, x + 1, top + trackH, 0x220E3B2A);
		ctx.fill(x, barY, x + 1, barY + barH, 0xFF8FB89A);
	}

	// ---------------------------------------------------------------- peças

	private void drawTitle(DrawContext ctx, float t) {
		ctx.drawTexture(TITLE, titleX, titleY, titleW, titleH, 0f, 0f, TITLE_TW, TITLE_TH, TITLE_TW, TITLE_TH);
		int bob = Math.round((float) Math.sin(t * 1.2f) * 2f);
		ctx.drawTexture(MERMAID, titleX - MERMAID_W + 8, 4 + bob, MERMAID_W, MERMAID_H,
				0f, 0f, MERMAID_W, MERMAID_H, MERMAID_W, MERMAID_H);
	}

	private void drawArrows(DrawContext ctx, int mouseX, int mouseY, Race.Palette pal) {
		boolean many = races.size() > 1;
		drawArrow(ctx, ARROW_LEFT, leftArrowX, many, inside(mouseX, mouseY, leftArrowX, arrowY, 18, 30), pal, -1);
		drawArrow(ctx, ARROW_RIGHT, rightArrowX, many, inside(mouseX, mouseY, rightArrowX, arrowY, 18, 30), pal, 1);
		if (many) {
			MutableText counter = DakuraFonts.body((index + 1) + " / " + races.size());
			ctx.drawText(textRenderer, counter, rightArrowX + 9 - textRenderer.getWidth(counter) / 2, arrowY + 34, pal.glow(), false);
		}
	}

	private void drawArrow(DrawContext ctx, Identifier tex, int x, boolean enabled, boolean hover, Race.Palette pal, int dir) {
		int c = !enabled ? Race.Palette.mix(pal.sky(), pal.glow(), 0.3f) : hover ? 0xFFF9C6CB : 0xFFD6E7D6;
		int bob = enabled && hover ? (int) ((System.currentTimeMillis() / 150) % 2) * dir : 0;
		PaperPage.tint(ctx, c);
		ctx.drawTexture(tex, x + bob, arrowY, 18, 30, 0f, 0f, 9, 15, 9, 15);
		ctx.setShaderColor(1f, 1f, 1f, 1f);
	}

	private void drawButton(DrawContext ctx, int mouseX, int mouseY) {
		boolean hover = inside(mouseX, mouseY, btnX, btnY, btnW, btnH);
		int border = armed ? 0xFFEC95A3 : hover ? 0xFFA8E0BC : 0xFF4E8F6F;
		ctx.fill(btnX + 1, btnY, btnX + btnW - 1, btnY + btnH, border);
		ctx.fill(btnX, btnY + 1, btnX + btnW, btnY + btnH - 1, border);
		ctx.fill(btnX + 1, btnY + 1, btnX + btnW - 1, btnY + btnH - 1, 0xFF0E3B2A);
		ctx.fill(btnX + 2, btnY + 2, btnX + btnW - 2, btnY + 3, 0x22FFFFFF);

		String label = viewOnly ? I18n.translate("dakura_origins.screen.close")
				: waiting ? "..." : I18n.translate(armed ? "dakura_origins.screen.confirm" : "dakura_origins.screen.choose");
		MutableText txt = DakuraFonts.heading(label);
		int color = armed ? 0xFFF9C6CB : 0xFFEEF4E2;
		ctx.drawText(textRenderer, txt, btnX + (btnW - textRenderer.getWidth(txt)) / 2, btnY + 5, color, false);
	}

	private static void disc(DrawContext ctx, int cx, int cy, int r, int color, int u) {
		u = Math.max(1, u);
		for (int dy = -r; dy <= r; dy += u) {
			int half = (int) Math.sqrt(Math.max(0, r * r - dy * dy));
			half = half / u * u;
			if (half <= 0) continue;
			ctx.fill(cx - half, cy + dy, cx + half, cy + dy + u, color);
		}
	}

	private static void sparkle(DrawContext ctx, int x, int y, int u, int color, boolean big) {
		ctx.fill(x, y, x + u, y + u, color);
		if (big) {
			ctx.fill(x - u, y, x, y + u, color);
			ctx.fill(x + u, y, x + 2 * u, y + u, color);
			ctx.fill(x, y - u, x + u, y, color);
			ctx.fill(x, y + u, x + u, y + 2 * u, color);
		}
	}

	private static boolean inside(double mx, double my, int x, int y, int w, int h) {
		return mx >= x && mx < x + w && my >= y && my < y + h;
	}

	private static float ease(float x) {
		return x < 0.5f ? 2 * x * x : 1 - (float) Math.pow(-2 * x + 2, 2) / 2;
	}

	// ================================================================== interação

	private void openBook() {
		if (state != State.CLOSED) return;
		state = State.OPENING;
		stateStart = System.currentTimeMillis();
		if (client != null) {
			client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f));
			client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0f));
		}
	}

	private void turn(int dir) {
		if (viewOnly || state != State.OPEN || races.size() < 2 || flipStart >= 0 || waiting) return;
		flipFrom = index;
		index = Math.floorMod(index + dir, races.size());
		flipTo = index;
		flipStart = System.currentTimeMillis();
		scroll = 0;
		armed = false;
		if (client != null) client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0f));
	}

	private void confirm() {
		if (viewOnly) {
			if (client != null) client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.0f));
			close();
			return;
		}
		if (state != State.OPEN || waiting || races.isEmpty()) return;
		if (client != null) client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0f));
		if (!armed) {
			armed = true;
			return;
		}
		waiting = true;
		ClientPlayNetworking.send(new ChooseRacePayload(races.get(index).id()));
	}

	/** Servidor aceitou a escolha. */
	public void onAccepted() {
		if (viewOnly) {
			// a raça mudou (ex.: /raca definir) com a consulta aberta: fecha para não mostrar a antiga
			close();
			return;
		}
		if (client != null) {
			client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0f));
			client.setScreen(null);
		}
	}

	/** Servidor recusou (raça inválida etc.). */
	public void onRejected() {
		waiting = false;
		armed = false;
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) return true;
		if (state == State.CLOSED) {
			if (inside(mouseX, mouseY, closedX - 10, closedY - 12, closedW + 20, closedH + 24)) openBook();
			return true;
		}
		if (state != State.OPEN) return true;
		if (inside(mouseX, mouseY, leftArrowX, arrowY, 18, 30)) turn(-1);
		else if (inside(mouseX, mouseY, rightArrowX, arrowY, 18, 30)) turn(1);
		else if (inside(mouseX, mouseY, btnX, btnY, btnW, btnH)) confirm();
		else armed = false;
		return true;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		scroll -= (float) verticalAmount * 14f;
		if (scroll < 0) scroll = 0;
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		// a mesma tecla que abre a página de consulta também fecha
		if (viewOnly && DakuraOriginsClient.RACE_PAGE.matchesKey(keyCode, scanCode)) {
			close();
			return true;
		}
		switch (keyCode) {
			case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_A -> turn(-1);
			case GLFW.GLFW_KEY_RIGHT, GLFW.GLFW_KEY_D -> turn(1);
			case GLFW.GLFW_KEY_UP, GLFW.GLFW_KEY_W -> scroll = Math.max(0, scroll - 14);
			case GLFW.GLFW_KEY_DOWN, GLFW.GLFW_KEY_S -> scroll += 14;
			case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_SPACE -> {
				if (state == State.CLOSED) openBook();
				else confirm();
			}
			default -> {
				return super.keyPressed(keyCode, scanCode, modifiers);
			}
		}
		return true;
	}

	// ================================================================== trava

	@Override
	public boolean shouldCloseOnEsc() {
		return viewOnly;
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void close() {
		if (viewOnly || !DakuraOriginsClient.mustChoose) super.close();
	}

	/** Libera as texturas dinâmicas da galáxia e do papel. */
	@Override
	public void removed() {
		backdrop.close();
		paper.close();
		super.removed();
	}
}
