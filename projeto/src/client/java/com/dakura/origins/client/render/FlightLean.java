package com.dakura.origins.client.render;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.types.FlightPower;
import net.minecraft.entity.Entity;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Quanto o corpo está deitado no voo (0 = em pé, 1 = deitado como numa Elytra), suavizado no tempo.
 * Quem decide QUANDO deitar é o servidor (flag "fly_lean" do poder de voo: Fênix sempre, Serafim só no sprint);
 * aqui só se transforma o liga/desliga em transição. Usado pela rotação do corpo (PlayerEntityRendererMixin)
 * e pela pose dos membros (BipedEntityModelMixin), sempre com o mesmo valor no mesmo quadro.
 */
public final class FlightLean {
	private static final Map<Entity, float[]> STATE = new WeakHashMap<>();

	private FlightLean() {}

	public static float amount(Entity e, float t) {
		boolean on = EntityFlags.clientHas(e, FlightPower.FLAG_LEAN);
		if (!on && !STATE.containsKey(e)) return 0f;
		float[] s = STATE.computeIfAbsent(e, k -> new float[]{0f, Float.NaN});
		float dt = Float.isNaN(s[1]) ? 100f : t - s[1];
		if (dt < 0f || dt > 5f) dt = 100f;
		s[1] = t;
		float k = 1f - (float) Math.exp(-dt * 0.16f);
		s[0] += ((on ? 1f : 0f) - s[0]) * k;
		if (!on && s[0] < 0.005f) {
			STATE.remove(e);
			return 0f;
		}
		return s[0];
	}
}
