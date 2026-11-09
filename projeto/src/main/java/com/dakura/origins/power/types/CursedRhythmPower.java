package com.dakura.origins.power.types;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "dakura_origins:cursed_rhythm" — Fios do Teatro (passiva da Boneca): a cada N golpes seguidos (sem parar
 * por mais de 3 s), o próximo libera um corte de energia amaldiçoada com dano extra. { "hits": 5, "bonus": 4 }
 */
public final class CursedRhythmPower implements RacePower {
	private static final Map<UUID, long[]> COMBO = new ConcurrentHashMap<>(); // {golpes, último tick}
	private static boolean reentry;
	private final int hits;
	private final float bonus;

	public CursedRhythmPower(JsonObject json) {
		this.hits = JsonParams.integer(json, "hits", 5);
		this.bonus = JsonParams.num(json, "bonus", 4f);
	}

	public static void bootstrap() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, base, taken, blocked) -> {
			if (reentry || blocked || !(source.getAttacker() instanceof ServerPlayerEntity p) || source.getSource() != p) return;
			CursedRhythmPower pw = RaceAccess.getRace(p).flatMap(r -> r.powers().stream().filter(x -> x instanceof CursedRhythmPower)
					.map(x -> (CursedRhythmPower) x).findFirst()).orElse(null);
			if (pw == null) return;
			long now = p.getWorld().getTime();
			long[] c = COMBO.computeIfAbsent(p.getUuid(), k -> new long[]{0, now});
			if (now - c[1] > 60) c[0] = 0;
			c[1] = now;
			if (++c[0] < pw.hits) {
				ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:cursed_tick", (int) c[0]), p);
				return;
			}
			c[0] = 0;
			reentry = true;
			try {
				target.timeUntilRegen = 0;
				target.damage(p.getDamageSources().indirectMagic(p, p), pw.bonus);
			} finally {
				reentry = false;
			}
			ModNetworking.sendAround(new AnimPayload(target.getId(), "fx:cursed_burst", p.getId()), p, target);
			p.getWorld().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.ENTITY_WARDEN_ATTACK_IMPACT,
					SoundCategory.PLAYERS, 0.8f, 1.6f);
			p.getWorld().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK,
					SoundCategory.PLAYERS, 0.9f, 0.6f);
		});
	}

	public static boolean has(PlayerEntity p) {
		return RaceAccess.getRace(p).map(r -> r.powers().stream().anyMatch(x -> x instanceof CursedRhythmPower)).orElse(false);
	}
}
