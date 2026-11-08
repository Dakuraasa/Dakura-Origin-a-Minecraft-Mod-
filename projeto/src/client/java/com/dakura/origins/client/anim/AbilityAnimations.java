package com.dakura.origins.client.anim;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Linhas do tempo das animações de habilidade no cliente, por canal:
 *   "bite" / "bitten"  – Mordida Vampírica (vampiro / vítima)
 *   "breath"           – Sopro Dracônico
 *   "cast"             – arremesso da Bola de Fogo
 * O progresso (0 → 1) é calculado a partir da idade da entidade, então é suave
 * entre ticks e não exige nenhum pacote extra durante a animação.
 */
public final class AbilityAnimations {
	private record Anim(float startAge, int duration) {}

	private static final Map<String, Map<Integer, Anim>> CHANNELS = new HashMap<>();

	private AbilityAnimations() {}

	public static void start(ClientWorld world, int entityId, String channel, int duration) {
		Entity e = world.getEntityById(entityId);
		if (e == null || duration <= 0) return;
		CHANNELS.computeIfAbsent(channel, c -> new HashMap<>()).put(entityId, new Anim(e.age, duration));
	}

	/**
	 * Fim vindo do servidor: se ainda estava no começo (interrompida), corta;
	 * se já estava no fim, deixa a fase de recuperação terminar suavemente.
	 */
	public static void stop(ClientWorld world, int entityId, String channel) {
		Map<Integer, Anim> map = CHANNELS.get(channel);
		if (map == null) return;
		Anim a = map.get(entityId);
		if (a == null) return;
		Entity e = world.getEntityById(entityId);
		if (e == null || (e.age - a.startAge) / (float) a.duration < 0.8f) map.remove(entityId);
	}

	public static void clear() {
		CHANNELS.clear();
	}

	/** @return progresso 0..1, ou -1 se a entidade não está nessa animação. */
	public static float progress(Entity entity, String channel, float animationProgress) {
		Map<Integer, Anim> map = CHANNELS.get(channel);
		if (map == null || map.isEmpty()) return -1f;
		Anim a = map.get(entity.getId());
		if (a == null) return -1f;
		float p = (animationProgress - a.startAge) / a.duration;
		if (p > 1.05f || p < -0.5f) {
			map.remove(entity.getId());
			return -1f;
		}
		return MathHelper.clamp(p, 0f, 1f);
	}

	/** Ids de entidades com o canal ativo (para os efeitos de partícula). */
	public static Set<Integer> active(String channel) {
		Map<Integer, Anim> map = CHANNELS.get(channel);
		return map == null ? Collections.emptySet() : map.keySet();
	}

	// ---- curvas

	public static float smooth(float x) {
		x = MathHelper.clamp(x, 0f, 1f);
		return x * x * (3f - 2f * x);
	}

	/** 0 antes de a, sobe até 1 em b, fica 1, desce até 0 entre c e d. */
	public static float window(float p, float a, float b, float c, float d) {
		if (p <= a || p >= d) return 0f;
		if (p < b) return smooth((p - a) / (b - a));
		if (p > c) return 1f - smooth((p - c) / (d - c));
		return 1f;
	}
}
