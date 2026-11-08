package com.dakura.origins.power.ability;

import com.dakura.origins.entity.FeatherEntity;
import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:feather_barrage" — rajada de penas cortantes na direção da mira por alguns segundos.
 * { "shots": 12, "interval": 4, "speed": 2.2, "spread": 3.0 }
 */
public final class FeatherBarrageAbility implements Ability {
	private static final Map<UUID, int[]> ACTIVE = new HashMap<>(); // tiros restantes, espera
	private static final Map<UUID, FeatherBarrageAbility> OWNERS = new HashMap<>();
	private final int shots, interval;
	private final float speed, spread;

	public FeatherBarrageAbility(JsonObject json) {
		this.shots = JsonParams.integer(json, "shots", 12);
		this.interval = Math.max(1, JsonParams.integer(json, "interval", 4));
		this.speed = JsonParams.num(json, "speed", 2.2f);
		this.spread = JsonParams.num(json, "spread", 3f);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(FeatherBarrageAbility::tickAll);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		if (ACTIVE.containsKey(p.getUuid())) return false;
		ACTIVE.put(p.getUuid(), new int[]{shots, 0});
		OWNERS.put(p.getUuid(), this);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:feathers", 0), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PARROT_FLY, SoundCategory.PLAYERS, 1.0f, 0.8f);
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		ACTIVE.remove(p.getUuid());
		OWNERS.remove(p.getUuid());
	}

	private static void tickAll(MinecraftServer server) {
		if (ACTIVE.isEmpty()) return;
		Iterator<Map.Entry<UUID, int[]>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			var en = it.next();
			ServerPlayerEntity p = server.getPlayerManager().getPlayer(en.getKey());
			FeatherBarrageAbility ab = OWNERS.get(en.getKey());
			int[] st = en.getValue();
			if (p == null || !p.isAlive() || ab == null || st[0] <= 0) {
				it.remove();
				OWNERS.remove(en.getKey());
				continue;
			}
			if (--st[1] > 0) continue;
			st[1] = ab.interval;
			st[0]--;
			FeatherEntity f = new FeatherEntity(p.getWorld(), p);
			f.setPosition(p.getX(), p.getEyeY() - 0.15, p.getZ());
			f.setVelocity(p, p.getPitch(), p.getYaw(), 0f, ab.speed, ab.spread);
			p.getWorld().spawnEntity(f);
			p.swingHand(st[0] % 2 == 0 ? net.minecraft.util.Hand.MAIN_HAND : net.minecraft.util.Hand.OFF_HAND, true);
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ARROW_SHOOT, SoundCategory.PLAYERS,
					0.35f, 1.6f + p.getRandom().nextFloat() * 0.3f);
			if (st[0] % 3 == 0) p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PHANTOM_FLAP,
					SoundCategory.PLAYERS, 0.25f, 1.8f);
		}
	}
}
