package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * "dakura_origins:sleep_altitude" — só consegue dormir no alto (montanhas). { "min_y": 100 }
 * Funciona só no Overworld; nas outras dimensões vale a regra vanilla.
 */
public final class SleepAltitudePower implements RacePower {
	private final int minY;
	private final String messageKey;

	public SleepAltitudePower(JsonObject json) {
		this.minY = JsonParams.integer(json, "min_y", 100);
		this.messageKey = JsonParams.str(json, "message", "dakura_origins.message.sleep_high");
	}

	public static void bootstrap() {
		EntitySleepEvents.ALLOW_SLEEPING.register((player, pos) -> {
			if (player.getWorld().isClient) return null;
			SleepAltitudePower s = RaceAccess.getRace(player).flatMap(r -> r.powers().stream()
					.filter(x -> x instanceof SleepAltitudePower).map(x -> (SleepAltitudePower) x).findFirst()).orElse(null);
			if (s == null || pos.getY() >= s.minY) return null;
			player.sendMessage(Text.translatable(s.messageKey, s.minY).formatted(Formatting.GRAY), true);
			return PlayerEntity.SleepFailureReason.OTHER_PROBLEM;
		});
	}
}
