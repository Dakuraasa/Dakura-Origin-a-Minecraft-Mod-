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
	private static final Identifier BOOK = DakuraOrigins.id("textures/gui/book.png");
	private static final Identifier ARROW_LEFT = DakuraOrigins.id("textures/gui/arrow_left.png");
	private static final Identifier ARROW_RIGHT = DakuraOrigins.id("textures/gui/arrow_right.png");
	private static final int TITLE_TW = 720, TITLE_TH = 240;
	private static final int BOOK_W = 43, BOOK_H = 43;
	private static final long OPEN_MS = 750, FLIP_MS = 640;
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

	private final java.util.Map<String, PageLayout> layouts = new java.util.HashMap<>();
	/** +1 = folha vira da direita para a esquerda (avançar); -1 = da esquerda para a direita (voltar). */
	private int flipDir = 1;
	/** Sumário aberto / variante mostrada (ex.: Huli Jing das Neves) — e como estavam antes do flip. */
	private boolean toc, fromToc, variantFlip;
	/** 0 = forma base; 1, 2... = variantes (e.g. Snow Huli Jing). */
	private int variant, fromVariant;
	private float ribbonHover, variantHover;
	private static final int TOC_LINE = 15;
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
		this.races = RaceRegistry.pages();
		String base = RaceRegistry.baseOf(preselect);
		for (int i = 0; i < races.size(); i++) {
			if (races.get(i).id().equals(base)) index = i;
		}
		for (int i = 0; i < races.size(); i++) {
			if (!races.get(i).id().equals(base)) continue;
			List<Race> forms = RaceRegistry.forms(races.get(i));
			for (int k = 0; k < forms.size(); k++) if (forms.get(k).id().equals(preselect)) variant = k;
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
		// título (só o texto elegante, centralizado)
		titleW = Math.min(Math.round(width * 0.5f), 220);
		titleH = Math.round(titleW * TITLE_TH / (float) TITLE_TW);
		titleX = (width - titleW) / 2;
		titleY = 6;
		int titleBottom = 6;

		// livro fechado
		closedScale = Math.max(1, Math.min(3, (height - titleBottom - 46) / BOOK_H));
		closedW = BOOK_W * closedScale;
		closedH = BOOK_H * closedScale;
		closedX = (width - closedW) / 2;
		closedY = titleBottom + Math.max(6, (height - titleBottom - 30 - closedH) / 2);

		// página aberta (o título só aparece junto se houver espaço)
		titleWhenOpen = false;
		pageY = titleWhenOpen ? titleBottom + 4 : 10;
		pageH = height - pageY - 30;
		// livro aberto: duas folhas lado a lado (pageX/pageW = as duas juntas)
		int leaf = Math.max(180, Math.min(250, Math.round(pageH * 0.86f)));
		pageW = Math.min(leaf * 2, width - 76) & ~1;
		pageX = (width - pageW) / 2;

		btnW = viewOnly ? 110 : 150;
		btnH = 18;
		btnX = (width - btnW) / 2;
		btnY = pageY + pageH + 7;

		arrowY = pageY + pageH / 2 - 15;
		leftArrowX = pageX - 30;
		rightArrowX = pageX + pageW + 12;

		applyBackdropFocus();
		layouts.clear();
		scroll = 0;
	}

	private void applyBackdropFocus() {
		if (state == State.CLOSED) backdrop.layout(width, height, closedX, closedY, closedW, closedH);
		else backdrop.layout(width, height, pageX, pageY, pageW, pageH);
	}

	private int leafW() { return pageW / 2; }
	private int contentW() { return leafW() - PAD * 2; }
	private int contentTop() { return pageY + PAD + 2; }
	private int viewportH() { return pageH - PAD * 2 - 12; }

	/** part 1 = folha direita (retrato e descrição), part 2 = folha esquerda (vantagens, fraquezas, habilidades). */
	private PageLayout layoutFor(Race race, int part) {
		int innerW = contentW();
		String key = race.id() + "#" + part;
		PageLayout l = layouts.get(key);
		if (l == null || l.innerWidth() != innerW) {
			List<Text> keys = List.of(
					DakuraOriginsClient.ABILITY_1.getBoundKeyLocalizedText(),
					DakuraOriginsClient.ABILITY_2.getBoundKeyLocalizedText());
			l = PageLayout.build(race, innerW, textRenderer, keys, viewOnly ? details() : null, part);
			layouts.put(key, l);
		}
		return l;
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
		boolean before = flipping && flip < 0.5f;
		Race shown = before ? view(flipFrom, fromVariant) : current();
		boolean shownToc = before ? fromToc : toc;
		Race.Palette pal = flipping
				? view(flipFrom, fromVariant).palette().lerp(current().palette(), ease(flip))
				: current().palette();

		backdrop.render(ctx, pal, t, flipping);

		switch (state) {
			case CLOSED -> renderClosed(ctx, mouseX, mouseY, pal, t, 1f, 0f);
			case OPENING -> renderOpening(ctx, mouseX, mouseY, pal, t, shown, now);
			case OPEN -> renderOpen(ctx, mouseX, mouseY, pal, t, shown, shownToc, flipping ? flip : -1f, viewOnly ? pop(now) : 1f);
		}
		drawRunes(ctx);
		Text sig = Text.literal("Created by: Daturaasa").formatted(net.minecraft.util.Formatting.BOLD);
		float ss = 0.75f;
		ctx.getMatrices().push();
		ctx.getMatrices().translate(width - 4 - textRenderer.getWidth(sig) * ss, height - 4 - 8 * ss, 400);
		ctx.getMatrices().scale(ss, ss, 1f);
		ctx.drawText(textRenderer, sig, 0, 0, 0xCCFFFFFF, true);
		ctx.getMatrices().pop();
	}

	/** Raça da página i, na forma base ou numa variante. */
	private Race view(int i, int var) {
		Race base = races.get(i);
		if (var <= 0 || viewOnly) return base;
		List<Race> forms = RaceRegistry.forms(base);
		return forms.get(Math.min(var, forms.size() - 1));
	}

	private int formCount(int i) {
		return viewOnly ? 1 : RaceRegistry.forms(races.get(i)).size();
	}

	private Race current() {
		return view(index, variant);
	}

	// ---------------------------------------------------------------- fechado

	private void renderClosed(DrawContext ctx, int mouseX, int mouseY, Race.Palette pal, float t, float scale, float lift) {

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
			renderOpen(ctx, mouseX, mouseY, pal, t, shown, false, -1f, 0.86f + 0.14f * ease(q));
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

	private void renderOpen(DrawContext ctx, int mouseX, int mouseY, Race.Palette pal, float t, Race race, boolean showToc,
							float flip, float scale) {
		if (titleWhenOpen) drawTitle(ctx, t);

		boolean flipping = flip >= 0f;
		boolean interactive = !flipping && state == State.OPEN;
		Race oldRace = flipping ? view(flipFrom, fromVariant) : race, newRace = flipping ? current() : race;
		boolean oldToc = flipping ? fromToc : showToc, newToc = flipping ? toc : showToc;
		Race.Palette oldPal = oldRace.palette(), newPal = newRace.palette();
		int spine = pageX + leafW();
		float cx = pageX + pageW / 2f, cy = pageY + pageH / 2f;

		ctx.getMatrices().push();
		ctx.getMatrices().translate(cx, cy, 0f);
		ctx.getMatrices().scale(scale, scale, 1f);
		ctx.getMatrices().translate(-cx, -cy, 0f);

		// sombra do livro sobre a mesa
		ctx.fill(pageX + 3, pageY + 4, pageX + pageW + 3, pageY + pageH + 4, 0x40000000);

		if (!flipping) {
			if (!showToc) scroll = Math.min(scroll, Math.max(maxScroll(layoutFor(race, 1)), maxScroll(layoutFor(race, 2))));
			drawLeaf(ctx, 0, race, showToc, mouseX, mouseY, pal, interactive);
			drawLeaf(ctx, 1, race, showToc, mouseX, mouseY, pal, interactive);
		} else {
			float e = ease(flip);
			boolean fwd = flipDir > 0;
			// folhas paradas: a de baixo já mostra o destino, a outra ainda mostra a origem
			drawLeaf(ctx, 0, fwd ? oldRace : newRace, fwd ? oldToc : newToc, mouseX, mouseY, fwd ? oldPal : newPal, false);
			drawLeaf(ctx, 1, fwd ? newRace : oldRace, fwd ? newToc : oldToc, mouseX, mouseY, fwd ? newPal : oldPal, false);
			// sombra da folha que está no ar sobre as páginas paradas
			int shade = Math.round((float) Math.sin(e * Math.PI) * 70);
			if (shade > 0) {
				int w = Math.round(leafW() * (float) Math.sin(e * Math.PI) * 0.5f);
				for (int i = 0; i < 6; i++) {
					int a = shade * (6 - i) / 6;
					int x0 = spine - w + i * w / 6, x1 = spine + w - i * w / 6;
					ctx.fill(x0, pageY, x1, pageY + pageH, (a / 3 << 24));
				}
			}
			// a folha virando: gira em torno da lombada (encolhe até o meio e abre do outro lado)
			float sx = (float) Math.abs(Math.cos(e * Math.PI));
			boolean firstHalf = e < 0.5f;
			int side = fwd ? (firstHalf ? 1 : 0) : (firstHalf ? 0 : 1);
			Race lr = firstHalf ? oldRace : newRace;
			boolean lt = firstHalf ? oldToc : newToc;
			Race.Palette lp = firstHalf ? oldPal : newPal;
			ctx.getMatrices().push();
			ctx.getMatrices().translate(spine, cy, 0f);
			ctx.getMatrices().scale(Math.max(0.02f, sx), 1f + 0.03f * (1f - sx), 1f);
			ctx.getMatrices().translate(-spine, -cy, 0f);
			drawLeaf(ctx, side, lr, lt, mouseX, mouseY, lp, false);
			// a folha escurece quando fica de lado
			int dark = Math.round((1f - sx) * 120);
			int lx = pageX + side * leafW();
			ctx.fill(lx, pageY, lx + leafW(), pageY + pageH, dark << 24);
			ctx.getMatrices().pop();
			// troca de variante: um brilho suave da cor da forma nova
			if (variantFlip) {
				int alpha = Math.round(0.22f * (float) Math.sin(e * Math.PI) * 255);
				ctx.fill(pageX, pageY, pageX + pageW, pageY + pageH, (alpha << 24) | (newPal.glow() & 0xFFFFFF));
			}
		}

		if (state == State.OPEN) backdrop.renderPageEdges(ctx);
		if (!viewOnly) drawRibbons(ctx, mouseX, mouseY, t, newRace, newToc, interactive);
		boolean tagHover = false;
		ctx.getMatrices().pop();
		if (tagHover) ctx.drawTooltip(textRenderer, Text.translatable("dakura_origins.tag." + RaceRegistry.tagOf(newRace.id())), mouseX, mouseY);

		if (state == State.OPEN) {
			if (!viewOnly) drawArrows(ctx, mouseX, mouseY, pal);
			if (!newToc) drawButton(ctx, mouseX, mouseY);
		}
	}

	/** Uma folha do livro aberto. side 0 = esquerda (vantagens, fraquezas, habilidades / sumário), 1 = direita (retrato e descrição). */
	private void drawLeaf(DrawContext ctx, int side, Race race, boolean showToc, int mouseX, int mouseY, Race.Palette pal, boolean interactive) {
		int lw = leafW(), lx = pageX + side * lw;
		if (race != null && "serafim".equals(race.id())) ctx.fill(lx, pageY, lx + lw, pageY + pageH, 0xFF000000);
		else paper.render(ctx, lx, pageY, lw, pageH);
		// dobra perto da lombada: sombra em degradê
		for (int i = 0; i < 10; i++) {
			int a = (10 - i) * 7;
			int x = side == 0 ? lx + lw - 1 - i : lx + i;
			ctx.fill(x, pageY, x + 1, pageY + pageH, a << 24);
		}
		// borda externa levemente mais escura (espessura das folhas)
		int ex = side == 0 ? lx : lx + lw - 2;
		ctx.fill(ex, pageY + 1, ex + 2, pageY + pageH - 1, 0x1A3A2A10);

		int top = contentTop();
		if (showToc) {
			if (side == 0) renderToc(ctx, mouseX, mouseY, pal, interactive);
			else renderCover(ctx, lx, pal);
			return;
		}
		PageLayout l = layoutFor(race, side == 0 ? 2 : 1);
		int max = maxScroll(l);
		int off = Math.round(Math.min(scroll, max));
		ctx.enableScissor(lx + 8, top - 6, lx + lw - 8, top + viewportH());
		l.render(ctx, lx + PAD, top - off, pal);
		ctx.disableScissor();
		drawScrollBar(ctx, l, top, lx + lw - 13, off);

		MutableText folio = DakuraFonts.body(side == 1 ? "· " + PageLayout.roman(race.order()) + " ·" : "✦");
		ctx.drawText(textRenderer, folio, lx + (lw - textRenderer.getWidth(folio)) / 2, pageY + pageH - PAD - 8, 0xFF8FB89A, false);
	}

	/** Folha direita do sumário: o emblema do livro e o nome do mod. */
	private void renderCover(DrawContext ctx, int lx, Race.Palette pal) {
		int lw = leafW();
		int size = BOOK_W * Math.max(1, Math.min(2, (lw - PAD * 4) / BOOK_W));
		int y = pageY + (pageH - size) / 2 - 16;
		ctx.drawTexture(BOOK, lx + (lw - size) / 2, y, size, size, 0f, 0f, 43, 43, 43, 43);
	}

	// ---------------------------------------------------------------- sumário

	private record TocEntry(int index, int variant, Race race) {}

	private List<TocEntry> tocEntries() {
		List<TocEntry> list = new java.util.ArrayList<>();
		for (int i = 0; i < races.size(); i++) {
			List<Race> forms = RaceRegistry.forms(races.get(i));
			for (int k = 0; k < forms.size(); k++) list.add(new TocEntry(i, k, forms.get(k)));
		}
		return list;
	}

	private int tocTop() {
		return contentTop() + 34;
	}

	/** Sumário: numeral, nome e linha pontilhada até o número da página. Tudo clicável. */
	private void renderToc(DrawContext ctx, int mouseX, int mouseY, Race.Palette pal, boolean interactive) {
		MutableText title = DakuraFonts.heading(I18n.translate("dakura_origins.screen.contents"));
		float ts = 1.5f;
		int tw = Math.round(textRenderer.getWidth(title) * ts);
		ctx.getMatrices().push();
		ctx.getMatrices().translate(pageX + (leafW() - tw) / 2f, contentTop() + 2, 0f);
		ctx.getMatrices().scale(ts, ts, 1f);
		ctx.drawText(textRenderer, title, 0, 0, pal.accent() | 0xFF000000, false);
		ctx.getMatrices().pop();
		int divY = contentTop() + 20;
		ctx.fill(pageX + PAD + 30, divY, pageX + leafW() - PAD - 30, divY + 1, 0x558FB89A);
		ctx.fill(pageX + leafW() / 2 - 2, divY - 1, pageX + leafW() / 2 + 2, divY + 2, 0xFF8FB89A);

		List<TocEntry> entries = tocEntries();
		scroll = Math.max(0, Math.min(scroll, tocMaxScroll(entries.size())));
		int y = tocTop() - Math.round(scroll);
		int left = pageX + PAD + 6, right = pageX + leafW() - PAD - 6;
		int bottom = contentTop() + viewportH();
		ctx.enableScissor(pageX + 8, tocTop() - 3, pageX + leafW() - 8, bottom);
		for (TocEntry e : entries) {
			boolean hover = interactive && mouseY >= tocTop() - 3 && mouseY < bottom
					&& inside(mouseX, mouseY, left - 4, y - 2, right - left + 8, TOC_LINE - 1);
			int ink = hover ? (pal.accent() | 0xFF000000) : (pal.text() | 0xFF000000);
			int x = left;
			if (e.variant() > 0) {
				x += 14;
				ctx.drawText(textRenderer, DakuraFonts.body("↳"), left + 3, y, 0xFF8FB89A, false);
			} else {
				MutableText num = DakuraFonts.heading(PageLayout.roman(e.race().order()));
				ctx.drawText(textRenderer, num, x, y, 0xFF8FB89A, false);
				x += 26;
			}
			MutableText name = e.variant() > 0 ? DakuraFonts.bodyItalic(e.race().nameText().getString())
					: DakuraFonts.heading(e.race().nameText().getString());
			if (hover) ctx.fill(x - 3, y + 3, x - 1, y + 5, ink);
			ctx.drawText(textRenderer, name, x + (hover ? 2 : 0), y, ink, false);
			MutableText page = DakuraFonts.body(String.valueOf(e.race().order()));
			int pw = textRenderer.getWidth(page);
			ctx.drawText(textRenderer, page, right - pw, y, 0xFF8FB89A, false);
			// pontilhado
			int dotsFrom = x + textRenderer.getWidth(name) + 6, dotsTo = right - pw - 5;
			for (int dx = dotsFrom; dx < dotsTo; dx += 4) ctx.fill(dx, y + 7, dx + 1, y + 8, 0x668FB89A);
			y += TOC_LINE;
		}
		ctx.disableScissor();
		int max = tocMaxScroll(entries.size());
		if (max > 0) {
			int x = pageX + leafW() - 13, top = tocTop() - 3, trackH = bottom - top;
			int barH = Math.max(12, trackH * trackH / (trackH + max));
			int barY = top + Math.round((trackH - barH) * (scroll / max));
			ctx.fill(x, top, x + 1, bottom, 0x220E3B2A);
			ctx.fill(x, barY, x + 1, barY + barH, 0xFF8FB89A);
		}
	}

	private int tocMaxScroll(int entries) {
		return Math.max(0, entries * TOC_LINE + 6 - (contentTop() + viewportH() - tocTop()));
	}

	private TocEntry tocHit(double mx, double my) {
		int left = pageX + PAD + 6, right = pageX + leafW() - PAD - 6;
		if (my < tocTop() - 3 || my >= contentTop() + viewportH()) return null;
		int y = tocTop() - Math.round(scroll);
		for (TocEntry e : tocEntries()) {
			if (inside(mx, my, left - 4, y - 2, right - left + 8, TOC_LINE - 1)) return e;
			y += TOC_LINE;
		}
		return null;
	}

	// ---------------------------------------------------------------- marcadores (fitas)

	private int ribbonX() { return pageX + pageW - 40; }
	private int variantRibbonX() { return pageX + pageW - 64; }
	private int ribbonY() { return pageY - 7; }

	private boolean hasVariantRibbon(Race shown, boolean showToc) {
		return !showToc && formCount(index) > 1;
	}

	private void drawRibbons(DrawContext ctx, int mouseX, int mouseY, float t, Race shown, boolean showToc, boolean interactive) {
		boolean hoverMenu = interactive && inside(mouseX, mouseY, ribbonX() - 2, ribbonY(), 20, 52);
		ribbonHover += ((hoverMenu ? 1f : 0f) - ribbonHover) * 0.25f;
		// fita do sumário: veludo vinho, ponta em "V" cortado e estrela dourada
		int sway = Math.round((float) Math.sin(t * 1.6f) * 1.2f);
		int mlen = 40 + sway + Math.round(ribbonHover * 6);
		ribbon(ctx, ribbonX(), ribbonY(), 16, mlen, 0xFF7A1630, 0xFF4E0C1F, 0xFFB23A55, false, t);
		emblem(ctx, ribbonX() + 3, ribbonY() + mlen - 19, () -> emblemStar(ctx, 0, 0, 0xFFF2C14E));

		if (hasVariantRibbon(shown, showToc)) {
			boolean hoverVar = interactive && inside(mouseX, mouseY, variantRibbonX() - 2, ribbonY(), 18, 46);
			variantHover += ((hoverVar ? 1f : 0f) - variantHover) * 0.25f;
			int len = 34 + Math.round((float) Math.sin(t * 1.6f + 1.3f) * 1.2f) + Math.round(variantHover * 6);
			// a fita mostra a PRÓXIMA forma (cor e emblema dela)
			Race next = view(index, (variant + 1) % formCount(index));
			int ex = variantRibbonX() + 2, ey = ribbonY() + len - 14;
			switch (next.id()) {
				case "huli_jing" -> {
					ribbon(ctx, variantRibbonX(), ribbonY(), 14, len, 0xFFD2561E, 0xFF8A2A0C, 0xFFF59A4A, true, t);
					emblem(ctx, ex, ey, () -> emblemFlame(ctx, 0, 0));
				}
				case "huli_jing_neve" -> {
					ribbon(ctx, variantRibbonX(), ribbonY(), 14, len, 0xFFB8D8EE, 0xFF6F93B0, 0xFFF2FAFF, true, t);
					emblem(ctx, ex, ey, () -> emblemSnow(ctx, 0, 0));
				}
				default -> {
					int[] ink = next.palette().ink();
					ribbon(ctx, variantRibbonX(), ribbonY(), 14, len, 0xFF000000 | ink[3], 0xFF000000 | ink[1], 0xFF000000 | ink[4], true, t);
					if (next.id().endsWith("_felina") || next.id().endsWith("_cao")) emblem(ctx, ex, ey, () -> emblemPaw(ctx, 0, 0, 0xFF000000 | ink[5]));
					else emblem(ctx, ex, ey, () -> emblemFeather(ctx, 0, 0, 0xFF000000 | ink[5]));
				}
			}
		}
	}

	/** Emblema desenhado em 1,5× (mais legível na fita larga). */
	private static void emblem(DrawContext ctx, int x, int y, Runnable draw) {
		ctx.getMatrices().push();
		ctx.getMatrices().translate(x, y, 0);
		ctx.getMatrices().scale(1.5f, 1.5f, 1f);
		draw.run();
		ctx.getMatrices().pop();
	}

	/** Fita de marcador em pixel art. pointed = ponta em flecha (variante); senão corte em "V". */
	private static void ribbon(DrawContext ctx, int x, int y, int w, int len, int base, int dark, int light, boolean pointed, float t) {
		// sombra suave sobre a página, para a fita se destacar
		ctx.fill(x + 2, y + 7, x + w + 2, y + len + 3, 0x38000000);
		ctx.fill(x + w, y + 7, x + w + 1, y + len, 0x22000000);
		// a parte presa atrás da página aparece mais escura
		ctx.fill(x, y, x + w, y + 7, dark);
		float c = (w - 1) / 2f;
		int notch = 5;
		int sheen = Math.floorMod(Math.round(t * 6), w + 6) - 3;
		for (int i = 0; i < w; i++) {
			float k = 1f - Math.abs(i - c) / c;
			int bottom = pointed ? y + len + Math.round(notch * k) : y + len - Math.round(notch * k);
			int col = (i == 0 || i == w - 1) ? dark : (i == 1 ? light : base);
			if (i == sheen) col = light;
			ctx.fill(x + i, y + 7, x + i + 1, bottom, col);
			ctx.fill(x + i, bottom - 1, x + i + 1, bottom, dark);
		}
		// costura
		for (int yy = y + 9; yy < y + len - 6; yy += 3) ctx.fill(x + 2, yy, x + 3, yy + 1, 0x55FFFFFF);
	}

	private static void emblemStar(DrawContext ctx, int x, int y, int c) {
		ctx.fill(x + 2, y, x + 3, y + 5, c);
		ctx.fill(x, y + 2, x + 5, y + 3, c);
		ctx.fill(x + 1, y + 1, x + 4, y + 4, c);
		ctx.fill(x + 2, y + 2, x + 3, y + 3, 0xFFFFF4C8);
	}

	private static void emblemPaw(DrawContext ctx, int x, int y, int c) {
		ctx.fill(x + 1, y + 4, x + 6, y + 7, c);
		ctx.fill(x + 2, y + 3, x + 5, y + 4, c);
		ctx.fill(x, y + 1, x + 1, y + 3, c);
		ctx.fill(x + 2, y, x + 3, y + 2, c);
		ctx.fill(x + 4, y, x + 5, y + 2, c);
		ctx.fill(x + 6, y + 1, x + 7, y + 3, c);
	}

	private static void emblemFeather(DrawContext ctx, int x, int y, int c) {
		for (int i = 0; i < 7; i++) ctx.fill(x + 6 - i, y + i, x + 7 - i, y + i + 1, 0xFF4A2A20);
		ctx.fill(x + 3, y, x + 6, y + 1, c);
		ctx.fill(x + 2, y + 1, x + 6, y + 2, c);
		ctx.fill(x + 1, y + 2, x + 5, y + 3, c);
		ctx.fill(x + 1, y + 3, x + 4, y + 4, c);
		ctx.fill(x + 1, y + 4, x + 3, y + 5, c);
	}

	/** Selo do perfil da raça no topo da página (item vanilla) — devolve true se o mouse está em cima. */
	private boolean drawTag(DrawContext ctx, int mouseX, int mouseY, Race race, Race.Palette pal, boolean interactive) {
		net.minecraft.item.Item item = switch (RaceRegistry.tagOf(race.id())) {
			case "combat" -> net.minecraft.item.Items.IRON_SWORD;
			case "exploration" -> net.minecraft.item.Items.COMPASS;
			case "casual" -> net.minecraft.item.Items.EMERALD;
			case "support" -> net.minecraft.item.Items.GOLD_INGOT;
			case "mixed" -> net.minecraft.item.Items.ARROW;
			default -> null;
		};
		if (item == null) return false;
		int x = pageX + 14, y = pageY - 8, s = 22;
		int rim = 0xFF000000 | Race.Palette.mix(pal.accent(), 0x000000, 0.25f);
		int face = 0xFFF4EAD3, shade = 0xFFE2D3B2;
		// plaquinha octogonal (cantos cortados) com borda na cor da raça
		ctx.fill(x + 2, y, x + s - 2, y + s, rim);
		ctx.fill(x, y + 2, x + s, y + s - 2, rim);
		ctx.fill(x + 1, y + 1, x + s - 1, y + s - 1, rim);
		ctx.fill(x + 2, y + 1, x + s - 2, y + s - 1, face);
		ctx.fill(x + 1, y + 2, x + s - 1, y + s - 2, face);
		ctx.fill(x + 2, y + s - 3, x + s - 2, y + s - 1, shade);
		ctx.fill(x + 3, y + 2, x + s - 3, y + 3, 0x66FFFFFF);
		boolean hover = interactive && inside(mouseX, mouseY, x, y, s, s);
		ctx.getMatrices().push();
		ctx.getMatrices().translate(0, hover ? -1 : 0, 0);
		ctx.drawItem(new net.minecraft.item.ItemStack(item), x + 3, y + 3);
		ctx.getMatrices().pop();
		return hover;
	}

	private static void emblemSnow(DrawContext ctx, int x, int y) {
		int c = 0xFF4F7FA3;
		ctx.fill(x + 3, y, x + 4, y + 7, c);
		ctx.fill(x, y + 3, x + 7, y + 4, c);
		ctx.fill(x + 1, y + 1, x + 2, y + 2, c);
		ctx.fill(x + 5, y + 1, x + 6, y + 2, c);
		ctx.fill(x + 1, y + 5, x + 2, y + 6, c);
		ctx.fill(x + 5, y + 5, x + 6, y + 6, c);
	}

	private static void emblemFlame(DrawContext ctx, int x, int y) {
		ctx.fill(x + 3, y, x + 4, y + 2, 0xFFFFE08A);
		ctx.fill(x + 2, y + 2, x + 5, y + 4, 0xFFFFC14E);
		ctx.fill(x + 1, y + 4, x + 6, y + 7, 0xFFFFE9B0);
		ctx.fill(x + 3, y + 4, x + 4, y + 6, 0xFFFFC14E);
	}

	private void drawScrollBar(DrawContext ctx, PageLayout l, int top, int x, int off) {
		int max = maxScroll(l);
		if (max <= 0) return;
		int trackH = viewportH();
		int barH = Math.max(12, trackH * trackH / l.height());
		int barY = top + Math.round((trackH - barH) * (off / (float) max));
		ctx.fill(x, top, x + 1, top + trackH, 0x220E3B2A);
		ctx.fill(x, barY, x + 1, barY + barH, 0xFF8FB89A);
	}

	// ---------------------------------------------------------------- peças

	private void drawTitle(DrawContext ctx, float t) {
		ctx.drawTexture(TITLE, titleX, titleY, titleW, titleH, 0f, 0f, TITLE_TW, TITLE_TH, TITLE_TW, TITLE_TH);
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
		if (viewOnly || races.size() < 2) return;
		flipTo(Math.floorMod(index + dir, races.size()), false, 0, false);
	}

	/** Vira a página para outro estado (raça, sumário ou variante). */
	private void flipTo(int newIndex, boolean newToc, int newVariant, boolean variantKind) {
		if (viewOnly || state != State.OPEN || flipStart >= 0 || waiting) return;
		if (newToc) flipDir = -1;
		else if (toc || variantKind) flipDir = 1;
		else {
			int n = races.size();
			flipDir = newIndex == Math.floorMod(index + 1, n) ? 1 : newIndex == Math.floorMod(index - 1, n) ? -1 : (newIndex > index ? 1 : -1);
		}
		flipFrom = index;
		fromToc = toc;
		fromVariant = variant;
		index = newIndex;
		toc = newToc;
		variant = newVariant;
		flipTo = index;
		variantFlip = variantKind;
		flipStart = System.currentTimeMillis();
		scroll = 0;
		armed = false;
		if (client == null) return;
		enchantFx();
		if (variantKind) {
			client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, 1.35f));
			boolean fire = view(newIndex, newVariant).id().equals("huli_jing");
			client.getSoundManager().play(PositionedSoundInstance.master(
					fire ? SoundEvents.ITEM_FIRECHARGE_USE : SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, fire ? 1.4f : 1.4f + 0.15f * newVariant, 0.35f));
		} else {
			client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ITEM_BOOK_PAGE_TURN, newToc ? 0.85f : 1.0f));
		}
	}

	// ----- efeito de mesa de encantamento ao virar a página
	private long runeStart = -1;
	private final long[] runeSeed = new long[18];

	private void enchantFx() {
		runeStart = System.currentTimeMillis();
		for (int i = 0; i < runeSeed.length; i++) runeSeed[i] = (long) (Math.random() * Long.MAX_VALUE);
		client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.0f, 0.9f));
		var p = client.player;
		if (p != null && client.world != null) {
			for (int i = 0; i < 40; i++) {
				double a = Math.random() * Math.PI * 2, r = 1 + Math.random() * 1.5;
				client.world.addParticle(net.minecraft.particle.ParticleTypes.ENCHANT, p.getX(), p.getY() + 1.6, p.getZ(),
						Math.cos(a) * r, Math.random() * 1.2 - 0.3, Math.sin(a) * r);
			}
		}
	}

	/** Runas do alfabeto galáctico (a fonte da mesa de encantamento) voando até o centro do livro, só durante a virada. */
	private void drawRunes(DrawContext ctx) {
		if (runeStart < 0) return;
		float k = (System.currentTimeMillis() - runeStart) / 900f;
		if (k >= 1f) {
			runeStart = -1;
			return;
		}
		int cx = width / 2, cy = height / 2;
		for (int i = 0; i < runeSeed.length; i++) {
			java.util.Random r = new java.util.Random(runeSeed[i]);
			double a = r.nextDouble() * Math.PI * 2, d = (0.35 + r.nextDouble() * 0.25) * Math.min(width, height);
			float t = Math.min(1f, k * (1.1f + r.nextFloat() * 0.5f));
			int x = (int) (cx + Math.cos(a) * d * (1 - t)), y = (int) (cy + Math.sin(a) * d * (1 - t) - Math.sin(t * Math.PI) * 20);
			int alpha = (int) (255 * Math.sin(t * Math.PI)) & 0xFF;
			if (alpha < 8) continue;
			String ch = String.valueOf((char) ('a' + r.nextInt(26)));
			Text rune = Text.literal(ch).setStyle(net.minecraft.text.Style.EMPTY.withFont(Identifier.ofVanilla("alt")));
			ctx.getMatrices().push();
			ctx.getMatrices().translate(0, 0, 400);
			ctx.drawText(textRenderer, rune, x, y, (alpha << 24) | 0xC8B8FF, false);
			ctx.getMatrices().pop();
		}
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
		ClientPlayNetworking.send(new ChooseRacePayload(current().id()));
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
		if (!viewOnly && flipStart < 0) {
			if (inside(mouseX, mouseY, ribbonX() - 2, ribbonY(), 20, 52)) {
				flipTo(index, !toc, variant, false);
				return true;
			}
			if (!toc && formCount(index) > 1
					&& inside(mouseX, mouseY, variantRibbonX() - 2, ribbonY(), 18, 46)) {
				flipTo(index, false, (variant + 1) % formCount(index), true);
				return true;
			}
			if (toc) {
				TocEntry e = tocHit(mouseX, mouseY);
				if (e != null) flipTo(e.index(), false, e.variant(), false);
				return true;
			}
		}
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
				else if (!toc) confirm();
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
