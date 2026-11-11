package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:mirror_shield" — Égide Espelhada: estende a mão e abre um selo dourado à frente.
 * Por alguns segundos todo golpe de um inimigo é absorvido e devolvido a ele como num espelho. { "duration": 60, "reflect": 1.0 }
 */
public final class MirrorShieldAbility implements Ability {
	public static final String FLAG = "mirror_shield";
	private static final Map<UUID, long[]> ACTIVE = new HashMap<>(); // fim
	private static final Map<UUID, Float> REFLECT = new HashMap<>();
	private static long ticks;
	private static boolean reentry;
	private final int duration;
	private final float reflect;

	public MirrorShieldAbility(JsonObject json) {
		this.duration = JsonParams.integer(json, "duration", 60);
		this.reflect = JsonParams.num(json, "reflect", 1.0f);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ticks++;
			if (ACTIVE.isEmpty()) return;
			ACTIVE.entrySet().removeIf(en -> {
				if (ticks < en.getValue()[0]) return false;
				ServerPlayerEntity p = server.getPlayerManager().getPlayer(en.getKey());
				if (p != null) stop(p);
				return true;
			});
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((e, src, amount) -> {
			if (reentry || !(e instanceof ServerPlayerEntity p) || !ACTIVE.containsKey(p.getUuid())) return true;
			if (!(src.getAttacker() instanceof LivingEntity attacker) || attacker == p) return true;
			reentry = true;
			try {
				attacker.damage(p.getDamageSources().indirectMagic(p, p), amount * REFLECT.getOrDefault(p.getUuid(), 1f));
			} finally {
				reentry = false;
			}
			if (src.getSource() != null && src.getSource() != attacker) src.getSource().discard();
			ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:mirror_hit", attacker.getId()), p, attacker);
			LegendKit.fx(p, "shield_flash", 0);
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 1.0f, 1.8f);
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 0.7f, 1.5f);
			return false;
		});
	}

	private static void stop(ServerPlayerEntity p) {
		EntityFlags.set(p, FLAG, false);
		Channeling.set(p, false);
		REFLECT.remove(p.getUuid());
		ModNetworking.sendAround(new AnimPayload(p.getId(), "reach", 0), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.6f, 1.6f);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		if (ACTIVE.containsKey(p.getUuid())) return false;
		ACTIVE.put(p.getUuid(), new long[]{ticks + duration});
		REFLECT.put(p.getUuid(), reflect);
		EntityFlags.set(p, FLAG, true);
		Channeling.set(p, true);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "reach", duration), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.PLAYERS, 0.9f, 1.7f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.PLAYERS, 1.0f, 1.2f);
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		if (ACTIVE.remove(p.getUuid()) != null) stop(p);
	}
}
