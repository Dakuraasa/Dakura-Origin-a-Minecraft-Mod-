package com.dakura.origins.power.types;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * "dakura_origins:conditional_attribute" — bônus de atributo só numa condição.
 * { "attribute": "minecraft:generic.attack_damage", "amount": 2.0, "when": "forest" | "night" }
 */
public final class ConditionalAttributePower implements RacePower {
	private static final net.minecraft.registry.tag.TagKey<net.minecraft.world.biome.Biome> DESERT =
			net.minecraft.registry.tag.TagKey.of(net.minecraft.registry.RegistryKeys.BIOME, Identifier.of("c", "is_desert"));
	private final RegistryEntry<EntityAttribute> attribute;
	private final double amount;
	private final String when;
	private final Identifier id;
	private final EntityAttributeModifier.Operation op;

	public ConditionalAttributePower(JsonObject json) {
		Identifier attr = Identifier.of(JsonParams.str(json, "attribute", "minecraft:generic.attack_damage"));
		this.attribute = Registries.ATTRIBUTE.getEntry(attr).orElseThrow();
		this.amount = JsonParams.num(json, "amount", 1f);
		this.when = JsonParams.str(json, "when", "forest");
		this.id = DakuraOrigins.id("cond/" + when + "_" + attr.getPath().replace('.', '_'));
		this.op = switch (JsonParams.str(json, "operation", "add_value")) {
			case "add_multiplied_base" -> EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE;
			case "add_multiplied_total" -> EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
			default -> EntityAttributeModifier.Operation.ADD_VALUE;
		};
	}

	private boolean active(ServerPlayerEntity p) {
		return switch (when) {
			case "night" -> !p.getWorld().isDay() && p.getWorld().getDimension().hasSkyLight();
			case "nether" -> p.getWorld().getRegistryKey() == net.minecraft.world.World.NETHER;
			case "desert" -> p.getWorld().getBiome(p.getBlockPos()).isIn(DESERT) || p.getWorld().getBiome(p.getBlockPos()).isIn(BiomeTags.IS_BADLANDS);
			default -> p.getWorld().getBiome(p.getBlockPos()).isIn(BiomeTags.IS_FOREST);
		};
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.age % 20 != 0) return;
		EntityAttributeInstance inst = player.getAttributeInstance(attribute);
		if (inst == null) return;
		boolean on = active(player), has = inst.hasModifier(id);
		if (on && !has) inst.addTemporaryModifier(new EntityAttributeModifier(id, amount, op));
		else if (!on && has) inst.removeModifier(id);
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		EntityAttributeInstance inst = player.getAttributeInstance(attribute);
		if (inst != null) inst.removeModifier(id);
	}
}
