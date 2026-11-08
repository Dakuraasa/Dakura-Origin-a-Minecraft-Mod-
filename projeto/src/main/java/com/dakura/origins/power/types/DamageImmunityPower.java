package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * "dakura_origins:damage_immunity" — anula um tipo de dano (por tag de tipo de dano).
 * <pre>
 * { "type": "dakura_origins:damage_immunity", "damage_tag": "minecraft:is_fire" }
 * </pre>
 */
public final class DamageImmunityPower implements RacePower {
	private final TagKey<DamageType> tag;
	/** opcional: um tipo exato (ex.: "minecraft:sweet_berry_bush") em vez de uma tag */
	private final String type;

	public DamageImmunityPower(JsonObject json) {
		this.type = JsonParams.str(json, "damage_type", "");
		this.tag = TagKey.of(RegistryKeys.DAMAGE_TYPE, Identifier.of(JsonParams.str(json, "damage_tag", "minecraft:is_fire")));
	}

	@Override
	public boolean allowDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		if (!type.isEmpty()) return !source.getTypeRegistryEntry().getKey()
				.map(k -> k.getValue().toString().equals(type)).orElse(false);
		return !source.isIn(tag);
	}
}
