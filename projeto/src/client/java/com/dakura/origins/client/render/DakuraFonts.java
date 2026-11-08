package com.dakura.origins.client.render;

import com.dakura.origins.DakuraOrigins;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Fontes elegantes do livro (OFL, empacotadas no mod):
 *  - BODY:    EB Garamond (texto corrido)
 *  - HEADING: Cormorant Garamond Bold (nomes, capitular, botões)
 */
public final class DakuraFonts {
	public static final Identifier BODY = DakuraOrigins.id("body");
	public static final Identifier HEADING = DakuraOrigins.id("heading");

	private DakuraFonts() {}

	public static MutableText body(String s) {
		return Text.literal(s).styled(st -> st.withFont(BODY));
	}

	public static MutableText bodyItalic(String s) {
		return Text.literal(s).styled(st -> st.withFont(BODY).withItalic(true));
	}

	public static MutableText heading(String s) {
		return Text.literal(s).styled(st -> st.withFont(HEADING));
	}
}
