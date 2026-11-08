package com.dakura.origins.network;

import com.dakura.origins.race.RaceManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.LinkedHashSet;
import java.util.Set;

public final class ModNetworking {
	private ModNetworking() {}

	public static void registerPayloads() {
		PayloadTypeRegistry.playS2C().register(OpenSelectionPayload.ID, OpenSelectionPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SelectionResultPayload.ID, SelectionResultPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(BiteAnimPayload.ID, BiteAnimPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(BatStatePayload.ID, BatStatePayload.CODEC);
		PayloadTypeRegistry.playS2C().register(AnimPayload.ID, AnimPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(FireballPayload.ID, FireballPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(EntityFlagPayload.ID, EntityFlagPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(MaturityPayload.ID, MaturityPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(CooldownPayload.ID, CooldownPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(FxPayload.ID, FxPayload.CODEC);
		PayloadTypeRegistry.playC2S().register(ChooseRacePayload.ID, ChooseRacePayload.CODEC);
		PayloadTypeRegistry.playC2S().register(UseAbilityPayload.ID, UseAbilityPayload.CODEC);
	}

	public static void registerServerReceivers() {
		ServerPlayNetworking.registerGlobalReceiver(ChooseRacePayload.ID, (payload, context) -> {
			ServerPlayerEntity player = context.player();
			if (RaceManager.hasRace(player)) {
				String current = RaceManager.getRace(player).get().id();
				ServerPlayNetworking.send(player, new SelectionResultPayload(true, current));
				return;
			}
			if (!RaceManager.choose(player, payload.raceId())) {
				ServerPlayNetworking.send(player, new SelectionResultPayload(false, ""));
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(UseAbilityPayload.ID,
				(payload, context) -> RaceManager.useAbility(context.player(), payload.slot()));
	}

	/** Envia para todos os jogadores num raio ao redor de um ponto (efeitos que se movem pelo mundo). */
	public static void sendNear(net.minecraft.server.world.ServerWorld world, net.minecraft.util.math.Vec3d pos,
								double radius, CustomPayload payload) {
		for (ServerPlayerEntity p : PlayerLookup.around(world, pos, radius)) {
			if (ServerPlayNetworking.canSend(p, payload.getId())) ServerPlayNetworking.send(p, payload);
		}
	}

	/** Envia para todos que enxergam as entidades (e para elas mesmas, se forem jogadores). */
	public static void sendAround(CustomPayload payload, Entity... entities) {
		Set<ServerPlayerEntity> targets = new LinkedHashSet<>();
		for (Entity e : entities) {
			if (e == null) continue;
			targets.addAll(PlayerLookup.tracking(e));
			if (e instanceof ServerPlayerEntity sp) targets.add(sp);
		}
		for (ServerPlayerEntity p : targets) {
			if (ServerPlayNetworking.canSend(p, payload.getId())) ServerPlayNetworking.send(p, payload);
		}
	}
}
