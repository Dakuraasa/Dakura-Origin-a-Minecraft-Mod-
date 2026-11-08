package com.dakura.origins.power.types;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * "dakura_origins:forest_stealth" — só em florestas: passos silenciosos, sem nome sobre a cabeça,
 * fora dos radares (Olhar de Caçador, Sentidos Aguçados) e os monstros não o miram, a menos que ataque primeiro.
 */
public final class ForestStealthPower implements RacePower {
	public static final String FLAG = "camouflaged";

	public ForestStealthPower(JsonObject json) {}

	@Override
	public void tick(ServerPlayerEntity p) {
		if (p.age % 10 != 0) return;
		boolean in = p.getWorld().getBiome(p.getBlockPos()).isIn(BiomeTags.IS_FOREST) && !p.isSpectator();
		if (in == EntityFlags.has(p, FLAG)) return;
		EntityFlags.set(p, FLAG, in);
		EntityFlags.set(p, StealthPower.SILENT_FLAG, in);
		if (in) {
			for (MobEntity m : p.getServerWorld().getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(24),
					m -> m.getTarget() == p && m.getAttacker() != p)) m.setTarget(null);
		}
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		EntityFlags.set(p, FLAG, false);
		EntityFlags.set(p, StealthPower.SILENT_FLAG, false);
	}

	/** Servidor: este monstro não pode mirar este jogador camuflado. */
	public static boolean blocks(MobEntity mob, PlayerEntity p) {
		return p instanceof ServerPlayerEntity sp && EntityFlags.has(sp, FLAG) && mob.getAttacker() != p;
	}

	/** Cliente: camuflado agora? */
	public static boolean camouflaged(net.minecraft.entity.Entity e) {
		return EntityFlags.clientHas(e, FLAG);
	}
}
