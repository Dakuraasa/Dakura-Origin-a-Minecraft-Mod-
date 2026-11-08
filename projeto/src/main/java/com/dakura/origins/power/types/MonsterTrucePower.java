package com.dakura.origins.power.types;

import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;

/**
 * "dakura_origins:monster_truce" — monstros hostis não miram o jogador, a menos que ele ataque primeiro
 * (o monstro que levou dano dele revida normalmente). Usado pelo MobEntityMixin.
 * { "type": "dakura_origins:monster_truce" }
 */
public final class MonsterTrucePower implements RacePower {
	public MonsterTrucePower(JsonObject json) {}

	/** true = este monstro não pode mirar este jogador agora. */
	public static boolean blocks(MobEntity mob, LivingEntity target) {
		if (!(mob instanceof Monster) || !(target instanceof PlayerEntity player)) return false;
		if (mob.getAttacker() == player) return false;
		return SpectreSupport.has(player, MonsterTrucePower.class);
	}
}
