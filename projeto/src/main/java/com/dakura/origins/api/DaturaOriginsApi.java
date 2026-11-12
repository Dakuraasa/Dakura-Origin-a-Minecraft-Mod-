package com.dakura.origins.api;

import com.dakura.origins.power.PowerRegistry;
import com.dakura.origins.race.Race;
import com.dakura.origins.race.RaceAccess;
import com.dakura.origins.race.RaceManager;
import com.dakura.origins.race.RaceRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Optional;

/**
 * Stable entry point for add-on mods.
 * <p>
 * Races are data: every installed mod can ship {@code resources/dakura_races/<id>.json} and Datura Origins picks it up
 * at startup. Custom power and ability types are registered from a {@link com.dakura.origins.power.DakuraOriginsPlugin}
 * declared under the {@code "dakura_origins"} entrypoint, before any race file is read.
 */
public final class DaturaOriginsApi {
	public static final String MOD_ID = "dakura_origins";
	public static final String ENTRYPOINT = "dakura_origins";

	private DaturaOriginsApi() {}

	/** Registers a passive power type usable as {@code "type": "<id>"} inside a race's {@code powers} list. */
	public static void registerPowerType(Identifier id, PowerRegistry.PowerFactory factory) {
		PowerRegistry.registerPowerType(id, factory);
	}

	/** Registers an active ability type usable as {@code "type": "<id>"} inside a race's {@code abilities} list. */
	public static void registerAbilityType(Identifier id, PowerRegistry.AbilityFactory factory) {
		PowerRegistry.registerAbilityType(id, factory);
	}

	/** Every loaded race, from Datura Origins and from add-ons, in book order. */
	public static List<Race> races() {
		return RaceRegistry.all();
	}

	public static Optional<Race> race(String id) {
		return RaceRegistry.get(id);
	}

	/** The race a player currently has (works on both sides). */
	public static Optional<Race> raceOf(PlayerEntity player) {
		return RaceAccess.getRace(player);
	}

	/** Changes a player's race on the server, exactly like picking it in the book. Returns false for unknown ids. */
	public static boolean setRace(ServerPlayerEntity player, String raceId) {
		return RaceManager.choose(player, raceId);
	}
}
