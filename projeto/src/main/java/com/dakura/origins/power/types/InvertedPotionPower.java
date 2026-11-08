package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * "dakura_origins:inverted_potions" — cura e efeitos positivos ferem; efeitos negativos dão força.
 * { "type": "dakura_origins:inverted_potions", "damage_per_level": 2.0 }
 * - Cura/Dano instantâneos invertidos como num morto-vivo (LivingEntityMixin).
 * - Efeitos positivos de fora (poções, maçãs...) são removidos e causam dano.
 *   Efeitos "ambientes" (os da própria raça, sinalizador) são ignorados.
 */
public final class InvertedPotionPower implements RacePower {
	private final float damagePerLevel;

	public InvertedPotionPower(JsonObject json) {
		this.damagePerLevel = JsonParams.num(json, "damage_per_level", 2f);
	}

	public static boolean inverts(PlayerEntity player) {
		return SpectreSupport.has(player, InvertedPotionPower.class);
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.age % 10 != 0 || player.isCreative()) return;
		List<RegistryEntry<StatusEffect>> burn = new ArrayList<>();
		float dmg = 0f;
		boolean cursed = false;
		for (StatusEffectInstance e : player.getStatusEffects()) {
			if (e.isAmbient()) continue;
			StatusEffectCategory cat = e.getEffectType().value().getCategory();
			if (cat == StatusEffectCategory.BENEFICIAL) {
				burn.add(e.getEffectType());
				dmg += damagePerLevel * (e.getAmplifier() + 1);
			} else if (cat == StatusEffectCategory.HARMFUL) {
				cursed = true;
			}
		}
		for (RegistryEntry<StatusEffect> e : burn) player.removeStatusEffect(e);
		if (dmg > 0f) player.damage(player.getDamageSources().magic(), dmg);
		if (cursed) {
			StatusEffectInstance s = player.getStatusEffect(StatusEffects.STRENGTH);
			if (s == null || (s.isAmbient() && s.getDuration() < 30)) {
				player.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 60, 0, true, false, true));
			}
		}
	}
}
