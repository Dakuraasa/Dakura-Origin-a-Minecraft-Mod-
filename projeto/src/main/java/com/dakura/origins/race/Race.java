package com.dakura.origins.race;

import com.dakura.origins.power.Ability;
import com.dakura.origins.power.RacePower;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Uma raça, lida de um arquivo JSON em resources/dakura_races/ (de qualquer mod).
 * Texto e cores alimentam a página do livro; "powers" e "abilities" viram lógica de jogo.
 *
 * Tradução: todo texto da raça tem uma chave automática no arquivo de idioma
 * ("dakura_origins.race.&lt;id&gt;.name", ".epithet", ".description", ".trait.N",
 * ".ability.N.name", ".ability.N.description"). O texto do JSON é só o reserva,
 * usado quando o idioma atual não tem a chave (útil para addons).
 */
public record Race(
		String id,
		int order,
		String name,
		String epithet,
		Identifier icon,
		int iconWidth,
		int iconHeight,
		Palette palette,
		String description,
		List<Trait> traits,
		List<RacePower> powers,
		List<AbilityInfo> abilities
) {
	public static String key(String raceId, String suffix) {
		return "dakura_origins.race." + raceId + "." + suffix;
	}

	public MutableText nameText() {
		return Text.translatableWithFallback(key(id, "name"), name);
	}

	public MutableText epithetText() {
		return Text.translatableWithFallback(key(id, "epithet"), epithet);
	}

	public MutableText descriptionText() {
		return Text.translatableWithFallback(key(id, "description"), description);
	}

	public enum TraitKind { VANTAGEM, DESVANTAGEM, NEUTRO }

	/** Uma linha da lista com ✦ (só texto, para a página). */
	public record Trait(TraitKind kind, String text, String key) {
		public MutableText asText() {
			return Text.translatableWithFallback(key, text);
		}
	}

	/** Uma habilidade ativa: texto da página + a lógica criada pelo tipo do JSON. */
	public record AbilityInfo(String type, String name, String description, int cooldownTicks, @Nullable Ability ability,
							  String key) {
		public MutableText nameText() {
			return Text.translatableWithFallback(key + ".name", name);
		}

		public MutableText descriptionText() {
			return Text.translatableWithFallback(key + ".description", description);
		}
	}

	/** Cores da raça. ink = 6 tons da nebulosa (escuro → claro). */
	public record Palette(int[] ink, int sky, int glow, int text, int accent, int muted) {
		public Palette lerp(Palette other, float t) {
			int[] mixed = new int[6];
			for (int i = 0; i < 6; i++) mixed[i] = mix(ink[i], other.ink[i], t);
			return new Palette(mixed, mix(sky, other.sky, t), mix(glow, other.glow, t),
					mix(text, other.text, t), mix(accent, other.accent, t), mix(muted, other.muted, t));
		}

		public static int mix(int a, int b, float t) {
			if (t <= 0f) return a;
			if (t >= 1f) return b;
			int r = (int) (((a >> 16) & 0xFF) + (((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t);
			int g = (int) (((a >> 8) & 0xFF) + (((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t);
			int bl = (int) ((a & 0xFF) + ((b & 0xFF) - (a & 0xFF)) * t);
			return 0xFF000000 | (r << 16) | (g << 8) | bl;
		}
	}
}
