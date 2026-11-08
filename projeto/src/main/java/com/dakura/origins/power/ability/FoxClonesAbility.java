package com.dakura.origins.power.ability;

import com.dakura.origins.entity.HuliCloneEntity;
import com.dakura.origins.entity.ModEntities;
import com.dakura.origins.network.FxPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * "dakura_origins:fox_clones" — invoca ilusões de si mesma (com arma e armadura copiadas).
 * { "count": 3, "duration": 140, "snow": false }
 */
public final class FoxClonesAbility implements Ability {
	private final int count, duration;
	private final boolean snow;

	public FoxClonesAbility(JsonObject json) {
		this.count = JsonParams.integer(json, "count", 3);
		this.duration = JsonParams.integer(json, "duration", 140);
		this.snow = JsonParams.bool(json, "snow", false);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		ServerWorld w = p.getServerWorld();
		for (int i = 0; i < count; i++) {
			double a = Math.toRadians(p.getYaw()) + Math.PI + (i - (count - 1) / 2.0) * 1.1;
			double x = p.getX() - Math.sin(a) * 1.6, z = p.getZ() + Math.cos(a) * 1.6;
			HuliCloneEntity c = ModEntities.HULI_CLONE.create(w);
			if (c == null) continue;
			c.refreshPositionAndAngles(x, p.getY(), z, p.getYaw(), 0);
			c.setup(p, snow, duration);
			w.spawnEntity(c);
			ModNetworking.sendNear(w, c.getPos(), 64, new FxPayload("clone_poof", x, p.getY(), z, snow ? 1 : 0, 0, 0));
		}
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ILLUSIONER_PREPARE_MIRROR, SoundCategory.PLAYERS, 0.6f, 1.3f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), snow ? SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME : SoundEvents.ITEM_FIRECHARGE_USE,
				SoundCategory.PLAYERS, 0.6f, snow ? 1.5f : 1.2f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_FOX_SCREECH, SoundCategory.PLAYERS, 0.3f, 1.4f);
		return true;
	}
}
