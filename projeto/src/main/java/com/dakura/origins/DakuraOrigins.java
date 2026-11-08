package com.dakura.origins;

import com.dakura.origins.command.RaceCommand;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.PowerRegistry;
import com.dakura.origins.race.RaceAttachments;
import com.dakura.origins.race.RaceEvents;
import com.dakura.origins.race.RaceManager;
import com.dakura.origins.race.RaceRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ponto de entrada comum (cliente + servidor) do Dakura Origins.
 */
public class DakuraOrigins implements ModInitializer {
	public static final String MOD_ID = "dakura_origins";
	public static final Logger LOGGER = LoggerFactory.getLogger("Dakura Origins");

	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		// ordem importa: tipos de poder/habilidade (e addons) antes de ler os JSONs das raças
		RaceAttachments.init();
		PowerRegistry.bootstrap();
		RaceRegistry.load();
		RaceEvents.register();
		ModNetworking.registerPayloads();
		ModNetworking.registerServerReceivers();

		CommandRegistrationCallback.EVENT.register(RaceCommand::register);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> RaceManager.onJoin(handler.player));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> RaceManager.onLeave(handler.player));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> RaceManager.onRespawn(newPlayer));
		ServerTickEvents.END_SERVER_TICK.register(RaceManager::tick);

		LOGGER.info("Dakura Origins carregado com {} raça(s).", RaceRegistry.all().size());
	}
}
