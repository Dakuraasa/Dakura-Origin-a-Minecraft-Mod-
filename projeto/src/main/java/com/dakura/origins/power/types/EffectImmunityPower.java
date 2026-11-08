package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * "dakura_origins:effect_immunity" — certos efeitos nunca pegam (LivingEntityMixin).
 * { "effects": ["minecraft:poison", "minecraft:wither"] }
 */
public final class EffectImmunityPower implements RacePower {
	private final List<RegistryEntry<StatusEffect>> effects = new ArrayList<>();
	/** "all": true = nenhum efeito de fora pega (poções, maçãs...). Os da própria raça são "ambientes" e passam. */
	private final boolean all;

	public EffectImmunityPower(JsonObject json) {
		this.all = JsonParams.bool(json, "all", false);
		for (String s : JsonParams.strings(json, "effects")) {
			Registries.STATUS_EFFECT.getEntry(Identifier.of(s)).ifPresent(effects::add);
		}
	}

	public static boolean blocks(PlayerEntity player, StatusEffectInstance effect) {
		return com.dakura.origins.race.RaceAccess.getRace(player).map(r -> r.powers().stream()
				.anyMatch(p -> p instanceof EffectImmunityPower e
						&& (e.all ? !effect.isAmbient() : e.effects.contains(effect.getEffectType())))).orElse(false);
	}
}
