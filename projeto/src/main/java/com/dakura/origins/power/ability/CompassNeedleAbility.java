package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:compass_needle" — Técnica de Sangue Demoníaco · Destruição: Agulha da Bússola (à la Akaza).
 * O Oni ergue o joelho e pisa: um pentagrama vermelho brilhante se abre no chão e o acompanha. Por "duration"
 * ticks: força, velocidade, resistência, regeneração absurda, sede de sangue (desvio ampliado) e a posição de
 * todos os inimigos e jogadores em volta (menos os protegidos). { "duration": 300, "radius": 48 }
 */
public final class CompassNeedleAbility implements Ability {
	public static final String FLAG = "bloodlust";
	private static final Map<UUID, long[]> ACTIVE = new HashMap<>();
	private static long ticks;
	private final int duration, radius;

	public CompassNeedleAbility(JsonObject json) {
		this.duration = JsonParams.integer(json, "duration", 300);
		this.radius = JsonParams.integer(json, "radius", 48);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ticks++;
			if (ACTIVE.isEmpty()) return;
			ACTIVE.entrySet().removeIf(en -> {
				ServerPlayerEntity p = server.getPlayerManager().getPlayer(en.getKey());
				if (p == null) return true;
				if (ticks >= en.getValue()[0] || !p.isAlive()) {
					EntityFlags.set(p, FLAG, false);
					return true;
				}
				if (ticks % 40 == 0) p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_HEARTBEAT,
						SoundCategory.PLAYERS, 0.8f, 1.2f);
				return false;
			});
		});
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		ACTIVE.put(p.getUuid(), new long[]{ticks + duration + 14});
		EntityFlags.set(p, FLAG, true);
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, duration, 2, false, false, true));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, duration, 1, false, false, true));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, duration, 1, false, false, true));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, duration, 3, false, false, true));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, duration, 1, false, false, true));
		ModNetworking.sendAround(new AnimPayload(p.getId(), "akaza_stomp", 18), p);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:compass", duration), p);
		ServerPlayNetworking.send(p, new AnimPayload(p.getId(), "fx:keen", duration * 1000 + radius));
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.PLAYERS, 0.5f, 1.6f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.0f, 0.6f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_ROAR, SoundCategory.PLAYERS, 0.35f, 1.5f);
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		if (ACTIVE.remove(p.getUuid()) != null) EntityFlags.set(p, FLAG, false);
	}
}
