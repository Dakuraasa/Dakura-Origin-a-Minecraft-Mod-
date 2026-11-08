package com.dakura.origins.power.types;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * "dakura_origins:damage_cap" — golpes muito fortes (acima de "above") perdem "reduce" de dano.
 * { "above": 10, "reduce": 6 }  (5 e 3 corações)
 */
public final class DamageCapPower implements RacePower {
	private static boolean reentry;
	private final float above, reduce;

	public DamageCapPower(JsonObject json) {
		this.above = JsonParams.num(json, "above", 10f);
		this.reduce = JsonParams.num(json, "reduce", 6f);
	}

	@Override
	public boolean allowDamage(ServerPlayerEntity p, DamageSource source, float amount) {
		if (reentry || amount <= above || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
		reentry = true;
		try {
			p.damage(source, amount - reduce);
		} finally {
			reentry = false;
		}
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:stone_guard", 0), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 0.8f, 0.6f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_DEEPSLATE_BREAK, SoundCategory.PLAYERS, 0.6f, 0.7f);
		return false;
	}
}
