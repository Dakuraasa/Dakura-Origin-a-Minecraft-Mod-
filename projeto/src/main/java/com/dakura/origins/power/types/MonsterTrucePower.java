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
	private final boolean undeadOnly;
	/** Fada: se ela atacar qualquer monstro, a trégua acaba para todos por um tempo. */
	private final int breakTicks;
	private static final java.util.Map<java.util.UUID, Long> BROKEN = new java.util.concurrent.ConcurrentHashMap<>();

	public MonsterTrucePower(JsonObject json) {
		this.undeadOnly = com.dakura.origins.power.JsonParams.bool(json, "undead_only", false);
		this.breakTicks = com.dakura.origins.power.JsonParams.integer(json, "break_on_attack", 0);
	}

	public static void bootstrap() {
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> {
			if (victim instanceof Monster && source.getAttacker() instanceof PlayerEntity pl)
				BROKEN.put(pl.getUuid(), pl.getWorld().getTime());
			return true;
		});
	}

	/** true = este monstro não pode mirar este jogador agora. */
	public static boolean blocks(MobEntity mob, LivingEntity target) {
		if (!(mob instanceof Monster) || !(target instanceof PlayerEntity player)) return false;
		if (mob.getAttacker() == player) return false;
		Long broke = BROKEN.get(player.getUuid());
		long now = player.getWorld().getTime();
		return com.dakura.origins.race.RaceAccess.getRace(player).map(r -> r.powers().stream().anyMatch(p ->
				p instanceof MonsterTrucePower t && !(t.breakTicks > 0 && broke != null && now - broke < t.breakTicks) && (!t.undeadOnly
						|| mob.getType().isIn(net.minecraft.registry.tag.EntityTypeTags.UNDEAD)))).orElse(false);
	}
}
