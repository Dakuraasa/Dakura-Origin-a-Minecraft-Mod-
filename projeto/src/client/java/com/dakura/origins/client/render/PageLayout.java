package com.dakura.origins.client.render;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.race.Race;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Monta a página única de uma raça no estilo "capítulo de livro":
 *   ícone no canto superior esquerdo com o texto contornando, numeral romano grande,
 *   nome e epíteto à direita, capitular, divisor de jade, lista ✦ e habilidades.
 * Tudo vem do JSON da raça (traduzido pelo idioma do jogo); textos grandes ganham rolagem.
 *
 * Padrão dos ícones: a arte original vai inteira para uma caixa fixa (ICON_BOX), centralizada
 * e com a proporção preservada. Arte pequena cresce em escala inteira (pixels nítidos);
 * arte grande é reduzida só na hora de desenhar (a textura continua com todos os pixels).
 */
public final class PageLayout {
	public static final Identifier DIVIDER = DakuraOrigins.id("textures/gui/divider.png");
	public static final Identifier STAR = DakuraOrigins.id("textures/gui/star.png");
	private static final int DIVIDER_W = 112, DIVIDER_H = 9, STAR_SIZE = 9;
	private static final int LINE_H = 12;
	private static final int TEXT_INDENT = 13;
	/** Caixa padrão de todos os ícones de raça (em pixels de interface). */
	public static final int ICON_BOX_W = 156, ICON_BOX_H = 164; // olhos maiores, sempre limitados à largura da folha

	/**
	 * Dados extras da página de consulta (só na tela "sua raça").
	 * @param pageKey tecla que abre a página
	 * @param status  linha de estado (ex.: maturidade), ou null
	 */
	public record Details(Text pageKey, @Nullable String status) {}

	public enum Role { TEXT, ACCENT, MUTED, PRO, CON, NEUTRAL, JADE }

	private sealed interface Op permits TextOp, ImageOp, StarOp, IconOp {}
	private record TextOp(Text text, int x, int y, Role role, float scale) implements Op {}
	private record ImageOp(Identifier texture, int x, int y, int w, int h, int texW, int texH) implements Op {}
	private record StarOp(int x, int y, Role role) implements Op {}

	/** Marcador em pixel art das listas: maçã (vantagem), bigorna (vantagem vanilla), poção (desvantagem). */
	private record IconOp(Identifier tex, int x, int y, int w, int h, int texW, int texH) implements Op {}

	public static final Identifier ICON_VANILLA = DakuraOrigins.id("textures/gui/icon_vanilla.png");

	private final List<Op> ops = new ArrayList<>();
	private final TextRenderer tr;
	private final int innerW;
	private int iconW, iconH;
	private int height;

	public final String raceId;
	/** 0 = página única (tudo); 1 = página direita (retrato, nome e descrição); 2 = página esquerda (vantagens, fraquezas, habilidades). */
	public final int part;

	private PageLayout(TextRenderer tr, int innerW, String raceId, int part) {
		this.tr = tr;
		this.innerW = innerW;
		this.raceId = raceId;
		this.part = part;
	}

	public int innerWidth() {
		return innerW;
	}

	public int height() {
		return height;
	}

	public static PageLayout build(Race race, int innerW, TextRenderer tr, List<Text> abilityKeys) {
		return build(race, innerW, tr, abilityKeys, null);
	}

	public static PageLayout build(Race race, int innerW, TextRenderer tr, List<Text> abilityKeys, @Nullable Details details) {
		return build(race, innerW, tr, abilityKeys, details, 0);
	}

	public static PageLayout build(Race race, int innerW, TextRenderer tr, List<Text> abilityKeys, @Nullable Details details, int part) {
		PageLayout l = new PageLayout(tr, innerW, race.id(), part);
		if (part == 1) l.composePortrait(race);
		else l.compose(race, abilityKeys, details);
		return l;
	}

	/** Página direita do livro aberto: numeral, retrato centralizado, nome, epíteto e descrição. */
	private void composePortrait(Race race) {
		iconW = 0;
		iconH = -100; // o texto não contorna o retrato: ocupa a largura toda
		MutableText num = DakuraFonts.heading(roman(race.order()));
		ops.add(new TextOp(num, 0, -2, Role.MUTED, 1.6f));

		int boxW = Math.min(ICON_BOX_W + 16, innerW), boxH = ICON_BOX_H + 8;
		float fit = Math.min(boxW / (float) race.iconWidth(), boxH / (float) race.iconHeight());
		if (fit >= 1f) fit = (float) Math.floor(fit);
		int drawW = Math.max(1, Math.round(race.iconWidth() * fit));
		int drawH = Math.max(1, Math.round(race.iconHeight() * fit));
		ops.add(new ImageOp(race.icon(), (innerW - drawW) / 2, 4, drawW, drawH, race.iconWidth(), race.iconHeight()));
		int y = drawH + 10;

		MutableText name = DakuraFonts.heading(race.nameText().getString());
		float nameScale = 1.5f;
		ops.add(new TextOp(name, (innerW - Math.round(tr.getWidth(name) * nameScale)) / 2, y, Role.ACCENT, nameScale));
		y += 16;
		String epithet = race.epithetText().getString();
		if (!epithet.isBlank()) {
			MutableText ep = DakuraFonts.bodyItalic(epithet);
			ops.add(new TextOp(ep, (innerW - tr.getWidth(ep)) / 2, y, Role.MUTED, 1f));
			y += LINE_H + 2;
		}
		y += 8;

		boolean first = true;
		for (String paragraph : race.descriptionText().getString().split("\\n")) {
			if (paragraph.isBlank()) continue;
			y = flow(paragraph.trim(), y, Role.TEXT, first, 0) + 4;
			first = false;
		}
		height = y + 2;
	}

	private void compose(Race race, List<Text> abilityKeys, @Nullable Details details) {
		int y;
		if (part == 2) {
			iconW = 0;
			iconH = -100;
			y = 2;
		} else {
			y = composeHeader(race);
		}

		if (details != null && details.status() != null) {
			MutableText st = DakuraFonts.bodyItalic(details.status());
			ops.add(new TextOp(st, (innerW - tr.getWidth(st)) / 2, y, Role.MUTED, 1f));
			y += LINE_H + 4;
		}

		if (details == null && part != 2) {
			// página de escolha: lista única, a cor da ✦ diz se é vantagem ou fraqueza
			for (Race.TraitKind k : Race.TraitKind.values())
				for (Race.Trait trait : race.traits()) if (trait.kind() == k) y = trait(trait, y);
		} else {
			// livro aberto e página de consulta: agrupada
			y = traitGroup(race, Race.TraitKind.VANTAGEM, "page.advantages", y);
			y = traitGroup(race, Race.TraitKind.VANILLA, "page.vanilla", y);
			y = traitGroup(race, Race.TraitKind.DESVANTAGEM, "page.weaknesses", y);
			y = traitGroup(race, Race.TraitKind.NEUTRO, "page.traits", y);
		}
		composeAbilities(race, abilityKeys, details, y);
	}

	private int composeHeader(Race race) {
		// ícone: encaixado na caixa padrão, centralizado nela
		// nunca passa de ~45% da largura da página (não invade o nome nem o texto ao lado)
		int boxW = Math.min(innerW >= 180 ? ICON_BOX_W : ICON_BOX_W / 2, Math.round(innerW * 0.45f));
		int boxH = innerW >= 180 ? ICON_BOX_H : ICON_BOX_H / 2;
		float fit = Math.min(boxW / (float) race.iconWidth(), boxH / (float) race.iconHeight());
		if (fit >= 1f) fit = (float) Math.floor(fit);
		int drawW = Math.max(1, Math.round(race.iconWidth() * fit));
		int drawH = Math.max(1, Math.round(race.iconHeight() * fit));
		iconW = boxW;
		iconH = drawH;
		ops.add(new ImageOp(race.icon(), (boxW - drawW) / 2, 0, drawW, drawH, race.iconWidth(), race.iconHeight()));

		// numeral romano grande + nome + epíteto, alinhados à direita
		int headerMinX = iconW + 6;
		MutableText num = DakuraFonts.heading(roman(race.order()));
		float numScale = 3f;
		ops.add(new TextOp(num, innerW - Math.round(tr.getWidth(num) * numScale), -4, Role.TEXT, numScale));

		MutableText name = DakuraFonts.heading(race.nameText().getString());
		float nameScale = 1.4f;
		int nameW = Math.round(tr.getWidth(name) * nameScale);
		ops.add(new TextOp(name, Math.max(headerMinX, innerW - nameW), 28, Role.ACCENT, nameScale));
		int y = 44;

		String epithet = race.epithetText().getString();
		if (!epithet.isBlank()) {
			MutableText ep = DakuraFonts.bodyItalic(epithet);
			int epW = tr.getWidth(ep);
			ops.add(new TextOp(ep, Math.max(headerMinX, innerW - epW), 43, Role.MUTED, 1f));
			y = 57;
		}

		boolean first = true;
		for (String paragraph : race.descriptionText().getString().split("\\n")) {
			if (paragraph.isBlank()) continue;
			y = flow(paragraph.trim(), y, Role.TEXT, first, 0) + 4;
			first = false;
		}

		return Math.max(y + 2, iconH + 4) + 10;
	}

	private void composeAbilities(Race race, List<Text> abilityKeys, @Nullable Details details, int y) {
		if (!race.abilities().isEmpty()) {
			y = heading(tr("page.abilities"), y);
			for (int i = 0; i < race.abilities().size(); i++) {
				Race.AbilityInfo ability = race.abilities().get(i);
				int cs = colStart(y);
				ops.add(new StarOp(cs, y + 1, Role.JADE));
				MutableText title = DakuraFonts.heading(ability.nameText().getString());
				int x = cs + TEXT_INDENT;
				ops.add(new TextOp(title, x, y, Role.JADE, 1f));
				x += tr.getWidth(title) + 5;
				if (i < abilityKeys.size()) {
					MutableText key = DakuraFonts.body("[").append(abilityKeys.get(i)).append(DakuraFonts.body("]"));
					ops.add(new TextOp(key, x, y, Role.MUTED, 1f));
					x += tr.getWidth(key) + 5;
				}
				if (ability.cooldownTicks() > 0) {
					MutableText cd = DakuraFonts.bodyItalic("· " + tr("page.cooldown", seconds(ability.cooldownTicks())));
					// se não couber na linha do título, vai para a linha de baixo
					if (x + tr.getWidth(cd) > innerW) {
						y += LINE_H;
						x = colStart(y) + TEXT_INDENT;
					}
					ops.add(new TextOp(cd, x, y, Role.MUTED, 1f));
				}
				y += LINE_H;
				String desc = ability.descriptionText().getString();
				if (!desc.isBlank()) y = flow(desc, y, Role.TEXT, false, TEXT_INDENT);
				y += 7;
			}
		} else if (details != null) {
			y = heading(tr("page.abilities"), y);
			y = flow(tr("page.no_abilities"), y, Role.MUTED, false, 0) + 4;
		}

		if (details != null) {
			y = heading(tr("page.controls"), y);
			for (int i = 0; i < race.abilities().size() && i < abilityKeys.size(); i++) {
				y = control(abilityKeys.get(i), race.abilities().get(i).nameText().getString(), y);
			}
			y = control(details.pageKey(), tr("page.control.page"), y);
			y = control(Text.literal(tr("page.control.change_cmd")), tr("page.control.change"), y);
			y += 2;
			y = flow(tr("page.control.rebind"), y, Role.MUTED, false, 0);
		}
		height = y + 2;
	}

	private int trait(Race.Trait trait, int y) {
		switch (trait.kind()) {
			case VANTAGEM -> ops.add(new StarOp(colStart(y), y + 1, Role.PRO));
			case VANILLA -> ops.add(new IconOp(ICON_VANILLA, colStart(y) - 2, y + 1, 12, 7, 28, 16));
			case DESVANTAGEM -> ops.add(new StarOp(colStart(y), y + 1, Role.CON));
			case NEUTRO -> ops.add(new StarOp(colStart(y), y + 1, Role.NEUTRAL));
		}
		return flow(trait.asText().getString(), y, Role.TEXT, false, TEXT_INDENT) + 5;
	}

	private int traitGroup(Race race, Race.TraitKind kind, String titleKey, int y) {
		boolean any = false;
		for (Race.Trait t : race.traits()) {
			if (t.kind() != kind) continue;
			if (!any) {
				MutableText h = DakuraFonts.heading(tr(titleKey));
				ops.add(new TextOp(h, colStart(y), y, kind == Race.TraitKind.DESVANTAGEM ? Role.CON
						: kind == Race.TraitKind.VANTAGEM || kind == Race.TraitKind.VANILLA ? Role.PRO : Role.MUTED, 1.1f));
				y += LINE_H + 5;
				any = true;
			}
			y = trait(t, y);
		}
		return any ? y + 8 : y;
	}

	/** Título centralizado de seção ("Habilidades Ativas", "Controles"). */
	private int heading(String text, int y) {
		y += 6;
		MutableText heading = DakuraFonts.heading(text);
		ops.add(new TextOp(heading, (innerW - Math.round(tr.getWidth(heading) * 1.25f)) / 2, y, Role.ACCENT, 1.25f));
		return y + LINE_H + 7;
	}

	/** Linha de controle: [tecla]  ação. */
	private int control(Text key, String action, int y) {
		int cs = colStart(y);
		ops.add(new StarOp(cs, y + 1, Role.JADE));
		MutableText k = DakuraFonts.body("[").append(key.copy()).append(DakuraFonts.body("]"));
		ops.add(new TextOp(k, cs + TEXT_INDENT, y, Role.JADE, 1f));
		int x = cs + TEXT_INDENT + tr.getWidth(k) + 6;
		return flowFrom(action, x, y) + 2;
	}

	/** Texto simples que começa numa coluna x e quebra linha voltando para a margem do item. */
	private int flowFrom(String text, int startX, int y) {
		int x = startX;
		int space = tr.getWidth(DakuraFonts.body(" "));
		for (String word : text.trim().split("\\s+")) {
			MutableText w = DakuraFonts.body(word);
			int ww = tr.getWidth(w);
			if (x > startX && x + ww > innerW) {
				y += LINE_H;
				x = colStart(y) + TEXT_INDENT;
			}
			ops.add(new TextOp(w, x, y, Role.TEXT, 1f));
			x += ww + space;
		}
		return y + LINE_H;
	}

	private static String tr(String key, Object... args) {
		return I18n.translate("dakura_origins." + key, args);
	}

	/** 100 → "5s", 30 → "1.5s" (no idioma atual). */
	public static String seconds(int ticks) {
		float s = ticks / 20f;
		String n = s == Math.floor(s) ? String.valueOf((int) s) : String.format(java.util.Locale.ROOT, "%.1f", s);
		return tr("page.seconds", n);
	}

	/** Coluna onde o texto começa: ao lado do ícone enquanto estiver na altura dele. */
	private int colStart(int y) {
		return y < iconH + 2 ? iconW + 6 : 0;
	}

	/** Texto justificado contornando o ícone; dropCap = letra grande ocupando 2 linhas. */
	private int flow(String text, int y, Role role, boolean dropCap, int indent) {
		List<String> words = new ArrayList<>(Arrays.asList(text.trim().split("\\s+")));
		if (words.isEmpty()) return y;
		int space = tr.getWidth(DakuraFonts.body(" "));

		MutableText cap = null;
		int capW = 0;
		if (dropCap) {
			String firstWord = words.get(0);
			int cpLen = Character.charCount(firstWord.codePointAt(0));
			cap = DakuraFonts.heading(firstWord.substring(0, cpLen));
			words.set(0, firstWord.substring(cpLen));
			capW = Math.round(tr.getWidth(cap) * 2.2f) + 3;
		}

		int line = 0;
		while (!words.isEmpty()) {
			int left = colStart(y) + indent + (dropCap && line < 2 ? capW : 0);
			int avail = innerW - left;
			List<String> lineWords = new ArrayList<>();
			int total = 0;
			while (!words.isEmpty()) {
				int w = tr.getWidth(DakuraFonts.body(words.get(0)));
				int need = total + (lineWords.isEmpty() ? 0 : space) + w;
				if (!lineWords.isEmpty() && need > avail) break;
				lineWords.add(words.remove(0));
				total = need;
			}
			int[] widths = lineWords.stream().mapToInt(s -> tr.getWidth(DakuraFonts.body(s))).toArray();
			int sum = Arrays.stream(widths).sum();
			float gap = space;
			if (!words.isEmpty() && lineWords.size() > 1) {
				float g = (avail - sum) / (float) (lineWords.size() - 1);
				if (g <= space * 3) gap = g;
			}
			float x = left;
			for (int i = 0; i < lineWords.size(); i++) {
				if (!lineWords.get(i).isEmpty()) {
					ops.add(new TextOp(DakuraFonts.body(lineWords.get(i)), Math.round(x), y, role, 1f));
				}
				x += widths[i] + gap;
			}
			if (dropCap && line == 0) {
				ops.add(new TextOp(cap, colStart(y) + indent, y - 1, Role.ACCENT, 2.2f));
			}
			y += LINE_H;
			line++;
		}
		if (dropCap && line < 2) y += LINE_H * (2 - line);
		return y;
	}

	public void render(DrawContext ctx, int originX, int originY, Race.Palette pal) {
		for (Op op : ops) {
			if (op instanceof TextOp t) {
				int color = color(t.role, pal);
				if (t.scale == 1f) {
					ctx.drawText(tr, t.text, originX + t.x, originY + t.y, color, false);
				} else {
					ctx.getMatrices().push();
					ctx.getMatrices().translate(originX + t.x, originY + t.y, 0f);
					ctx.getMatrices().scale(t.scale, t.scale, 1f);
					ctx.drawText(tr, t.text, 0, 0, color, false);
					ctx.getMatrices().pop();
				}
			} else if (op instanceof ImageOp i) {
				ctx.drawTexture(i.texture, originX + i.x, originY + i.y, i.w, i.h, 0f, 0f, i.texW, i.texH, i.texW, i.texH);
			} else if (op instanceof IconOp ic) {
				ctx.drawTexture(ic.tex, originX + ic.x, originY + ic.y, ic.w, ic.h, 0f, 0f, ic.texW, ic.texH, ic.texW, ic.texH);
			} else if (op instanceof StarOp s) {
				PaperPage.tint(ctx, color(s.role, pal));
				ctx.drawTexture(STAR, originX + s.x, originY + s.y, STAR_SIZE, STAR_SIZE, 0f, 0f, STAR_SIZE, STAR_SIZE, STAR_SIZE, STAR_SIZE);
				ctx.setShaderColor(1f, 1f, 1f, 1f);
			}
		}
	}

	public static int color(Role role, Race.Palette pal) {
		return switch (role) {
			case TEXT -> pal.text();
			case ACCENT -> pal.accent();
			case MUTED, NEUTRAL -> pal.muted();
			// página escura (texto claro): verde e vermelho mais vivos para ler sobre o preto
			case PRO, JADE -> light(pal.text()) ? 0xFF7FE0AE : 0xFF2E6B4F;
			case CON -> light(pal.text()) ? 0xFFFF8FA5 : 0xFFA8445A;
		};
	}

	private static boolean light(int c) {
		return ((c >> 16 & 0xFF) + (c >> 8 & 0xFF) + (c & 0xFF)) > 600;
	}

	public static String roman(int n) {
		if (n <= 0) return String.valueOf(n);
		int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
		String[] symbols = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < values.length; i++) {
			while (n >= values[i]) {
				sb.append(symbols[i]);
				n -= values[i];
			}
		}
		return sb.toString();
	}
}
