package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

/**
 * "dakura_origins:keen_sight" — por alguns segundos, só quem usou vê o contorno de todas as
 * criaturas por perto, até atrás de paredes (sentidos de caçador, sem magia).
 * { "duration": 200, "radius": 40 }
 */
public final class KeenSightAbility implements Ability {
	private final int duration, radius;

	public KeenSightAbility(JsonObject json) {
		this.duration = JsonParams.integer(json, "duration", 200);
		this.radius = JsonParams.integer(json, "radius", 40);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		// param = duração em ticks × 1000 + raio (o cliente só aplica se for o próprio jogador)
		net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p, new AnimPayload(p.getId(), "fx:keen", duration * 1000 + radius));
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.3f, 0.5f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.5f, 1.8f);
		return true;
	}
}
