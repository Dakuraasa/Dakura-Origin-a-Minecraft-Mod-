package com.dakura.origins.power.types;

import com.dakura.origins.power.ConditionalPower;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * "dakura_origins:status_effect" — mantém um efeito permanente (sem partículas nem ícone).
 * <pre>
 * { "type": "dakura_origins:status_effect", "effect": "minecraft:night_vision",
 *   "amplifier": 0, "requires_food": true }
 * </pre>
 * O efeito é renovado antes de chegar a 10 s, então a visão noturna nunca pisca.
 */
public final class StatusEffectPower extends ConditionalPower {
	private static final int DURATION = 400;
	private static final int REFRESH_BELOW = 260;

	private final RegistryEntry<StatusEffect> effect;
	private final int amplifier;

	public StatusEffectPower(JsonObject json) {
		super(json);
		Identifier id = Identifier.of(json.get("effect").getAsString());
		this.effect = Registries.STATUS_EFFECT.getEntry(id)
				.orElseThrow(() -> new IllegalArgumentException("Efeito desconhecido: " + id));
		this.amplifier = JsonParams.integer(json, "amplifier", 0);
	}

	@Override
	public void onApply(ServerPlayerEntity player) {
		sync(player);
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.age % 20 == 0) sync(player);
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		if (isOurs(player.getStatusEffect(effect))) player.removeStatusEffect(effect);
	}

	private void sync(ServerPlayerEntity player) {
		StatusEffectInstance current = player.getStatusEffect(effect);
		if (isActive(player)) {
			if (current == null || (isOurs(current) && current.getDuration() < REFRESH_BELOW)) {
				player.addStatusEffect(new StatusEffectInstance(effect, DURATION, amplifier, true, false, false));
			}
		} else if (isOurs(current)) {
			player.removeStatusEffect(effect);
		}
	}

	/** Nosso efeito é "ambiente" e sem partículas — poções normais não são removidas por engano. */
	private boolean isOurs(StatusEffectInstance instance) {
		return instance != null && instance.isAmbient() && !instance.shouldShowParticles()
				&& instance.getAmplifier() == amplifier && instance.getDuration() <= DURATION;
	}
}
