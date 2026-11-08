package com.dakura.origins.power;

import com.dakura.origins.network.EntityFlagPayload;
import com.dakura.origins.network.ModNetworking;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Estados visuais simples de jogadores, sincronizados só quando mudam
 * (ex.: "cramped" = tremendo por falta de espaço). Reutilizável por qualquer poder.
 */
public final class EntityFlags {
	private static final Map<UUID, Set<String>> SERVER = new ConcurrentHashMap<>();
	/** Cliente: id de entidade → flags. */
	public static final Map<Integer, Set<String>> CLIENT = new ConcurrentHashMap<>();

	private EntityFlags() {}

	public static void bootstrap() {
		EntityTrackingEvents.START_TRACKING.register((tracked, viewer) -> {
			Set<String> flags = SERVER.get(tracked.getUuid());
			if (flags == null || !ServerPlayNetworking.canSend(viewer, EntityFlagPayload.ID)) return;
			for (String f : flags) ServerPlayNetworking.send(viewer, new EntityFlagPayload(tracked.getId(), f, true));
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> SERVER.remove(handler.player.getUuid()));
	}

	/** Liga/desliga um estado; só envia pacote se mudou. */
	public static void set(ServerPlayerEntity player, String flag, boolean value) {
		Set<String> flags = SERVER.computeIfAbsent(player.getUuid(), u -> ConcurrentHashMap.newKeySet());
		boolean changed = value ? flags.add(flag) : flags.remove(flag);
		if (changed) ModNetworking.sendAround(new EntityFlagPayload(player.getId(), flag, value), player);
	}

	public static boolean has(ServerPlayerEntity player, String flag) {
		Set<String> flags = SERVER.get(player.getUuid());
		return flags != null && flags.contains(flag);
	}

	/** Consulta no cliente. */
	public static boolean clientHas(Entity entity, String flag) {
		if (CLIENT.isEmpty()) return false;
		Set<String> flags = CLIENT.get(entity.getId());
		return flags != null && flags.contains(flag);
	}

	public static void clientSet(int entityId, String flag, boolean value) {
		if (value) CLIENT.computeIfAbsent(entityId, i -> ConcurrentHashMap.newKeySet()).add(flag);
		else {
			Set<String> flags = CLIENT.get(entityId);
			if (flags != null) {
				flags.remove(flag);
				if (flags.isEmpty()) CLIENT.remove(entityId);
			}
		}
	}
}
