package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * "dakura_origins:damage_multiplier" — multiplica o dano recebido de um tipo.
 * { "damage_tag": "minecraft:is_fire", "multiplier": 2.0 }
 */
public final class DamageMultiplierPower implements RacePower {
	private static boolean reentry;
	private final TagKey<net.minecraft.entity.damage.DamageType> tag;
	private final float multiplier;

	public DamageMultiplierPower(JsonObject json) {
		this.tag = TagKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(JsonParams.str(json, "damage_tag", "minecraft:is_fire")));
		this.multiplier = JsonParams.num(json, "multiplier", 2f);
	}

	@Override
	public boolean allowDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		if (reentry || !source.isIn(tag)) return true;
		reentry = true;
		try {
			player.damage(source, amount * multiplier);
		} finally {
			reentry = false;
		}
		return false;
	}
}
