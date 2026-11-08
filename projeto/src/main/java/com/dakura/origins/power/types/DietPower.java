package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * "dakura_origins:restricted_diet" — a raça não consegue comer alimentos comuns.
 * <pre>
 * { "type": "dakura_origins:restricted_diet", "allowed": ["minecraft:honey_bottle", "#c:foods/raw_meat"],
 *   "message": "Seu corpo rejeita isso." }
 * </pre>
 * "allowed" aceita ids de item e tags (com #). Lista vazia = nenhum alimento comum.
 */
public final class DietPower implements RacePower {
	private final List<Identifier> allowedItems = new ArrayList<>();
	private final List<TagKey<Item>> allowedTags = new ArrayList<>();
	/** "forbidden": se presente, só esses são recusados e o resto é liberado. */
	private final List<Identifier> forbiddenItems = new ArrayList<>();
	private final List<TagKey<Item>> forbiddenTags = new ArrayList<>();
	private final Text message;

	public DietPower(JsonObject json) {
		for (String s : JsonParams.strings(json, "allowed")) {
			if (s.startsWith("#")) allowedTags.add(TagKey.of(RegistryKeys.ITEM, Identifier.of(s.substring(1))));
			else allowedItems.add(Identifier.of(s));
		}
		for (String s : JsonParams.strings(json, "forbidden")) {
			if (s.startsWith("#")) forbiddenTags.add(TagKey.of(RegistryKeys.ITEM, Identifier.of(s.substring(1))));
			else forbiddenItems.add(Identifier.of(s));
		}
		this.message = JsonParams.message(json, "message", "dakura_origins.message.diet_default")
				.formatted(Formatting.DARK_RED);
	}

	@Override
	public Text consumeRejection(PlayerEntity player, ItemStack stack) {
		if (!forbiddenItems.isEmpty() || !forbiddenTags.isEmpty()) {
			boolean bad = forbiddenItems.contains(Registries.ITEM.getId(stack.getItem()));
			for (TagKey<Item> tag : forbiddenTags) bad |= stack.isIn(tag);
			return bad ? message : null;
		}
		if (allowedItems.contains(Registries.ITEM.getId(stack.getItem()))) return null;
		for (TagKey<Item> tag : allowedTags) if (stack.isIn(tag)) return null;
		return message;
	}
}
