package com.dakura.origins.power;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** Leitura de parâmetros opcionais dos JSONs de poderes e habilidades. */
public final class JsonParams {
	private JsonParams() {}

	public static String str(JsonObject o, String key, String fallback) {
		return o.has(key) ? o.get(key).getAsString() : fallback;
	}

	public static float num(JsonObject o, String key, float fallback) {
		return o.has(key) ? o.get(key).getAsFloat() : fallback;
	}

	public static int integer(JsonObject o, String key, int fallback) {
		return o.has(key) ? o.get(key).getAsInt() : fallback;
	}

	public static boolean bool(JsonObject o, String key, boolean fallback) {
		return o.has(key) ? o.get(key).getAsBoolean() : fallback;
	}

	/** Lista de números (ex.: um valor por estágio de maturidade). Vazia se ausente. */
	public static float[] floats(JsonObject o, String key) {
		if (!o.has(key)) return new float[0];
		var arr = o.getAsJsonArray(key);
		float[] out = new float[arr.size()];
		for (int i = 0; i < out.length; i++) out[i] = arr.get(i).getAsFloat();
		return out;
	}

	/**
	 * Texto de mensagem vindo do JSON: aceita uma chave de tradução
	 * ("dakura_origins.message.x") ou um texto pronto (usado como está se não houver tradução).
	 */
	public static net.minecraft.text.MutableText message(JsonObject o, String key, String fallbackKey) {
		String v = str(o, key, fallbackKey);
		return net.minecraft.text.Text.translatableWithFallback(v, v);
	}

	public static List<String> strings(JsonObject o, String key) {
		List<String> out = new ArrayList<>();
		if (o.has(key)) for (JsonElement e : o.getAsJsonArray(key)) out.add(e.getAsString());
		return out;
	}
}
