package com.dakura.origins.client.render;

/**
 * Ruído barato (value noise + fbm) usado para animar a tinta em volta da página.
 */
public final class PixelNoise {
	private PixelNoise() {}

	public static float hash(int x, int y) {
		int h = x * 374761393 + y * 668265263;
		h = (h ^ (h >>> 13)) * 1274126177;
		h ^= h >>> 16;
		return (h & 0xFFFFFF) / 16777215f;
	}

	public static float noise(float x, float y) {
		int xi = (int) Math.floor(x);
		int yi = (int) Math.floor(y);
		float xf = x - xi, yf = y - yi;
		float u = xf * xf * (3 - 2 * xf);
		float v = yf * yf * (3 - 2 * yf);
		float a = hash(xi, yi), b = hash(xi + 1, yi), c = hash(xi, yi + 1), d = hash(xi + 1, yi + 1);
		return a + (b - a) * u + (c - a) * v + (a - b - c + d) * u * v;
	}

	public static float fbm(float x, float y) {
		float v = 0, amp = 0.5f;
		for (int i = 0; i < 3; i++) {
			v += amp * noise(x, y);
			x *= 2.03f;
			y *= 2.03f;
			amp *= 0.5f;
		}
		return v / 0.875f;
	}
}
