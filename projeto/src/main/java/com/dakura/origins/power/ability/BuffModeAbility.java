package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:buff_mode" — um modo temporário com efeitos (corrida da felina, caçada do cão...).
 * { "duration": 160, "flag": "prowl", "fall_immune": true, "sound": "cat",
 *   "effects": [{ "effect": "minecraft:speed", "amplifier": 1 }] }
 */
public final class BuffModeAbility implements Ability {
	private static final Map<UUID, Object[]> ACTIVE = new HashMap<>(); // {flag, fim, fallImmune, som}
	private static long ticks;
	private final int duration;
	private final String flag, sound;
	private final boolean fallImmune;
	private final List<RegistryEntry<StatusEffect>> effects = new ArrayList<>();
	private final List<Integer> amps = new ArrayList<>();

	public BuffModeAbility(JsonObject json) {
		this.duration = JsonParams.integer(json, "duration", 160);
		this.flag = JsonParams.str(json, "flag", "buff");
		this.sound = JsonParams.str(json, "sound", "");
		this.fallImmune = JsonParams.bool(json, "fall_immune", false);
		if (json.has("effects")) for (JsonElement e : json.getAsJsonArray("effects")) {
			JsonObject o = e.getAsJsonObject();
			Registries.STATUS_EFFECT.getEntry(Identifier.of(JsonParams.str(o, "effect", "minecraft:speed"))).ifPresent(en -> {
				effects.add(en);
				amps.add(JsonParams.integer(o, "amplifier", 0));
			});
		}
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ticks++;
			if (ACTIVE.isEmpty()) return;
			ACTIVE.entrySet().removeIf(en -> {
				ServerPlayerEntity p = server.getPlayerManager().getPlayer(en.getKey());
				Object[] st = en.getValue();
				if (p == null) return true;
				if (ticks >= (long) st[1] || !p.isAlive()) {
					EntityFlags.set(p, (String) st[0], false);
					return true;
				}
				if ("wolf".equals(st[3]) && ticks % 60 == 0) p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(),
						SoundEvents.ENTITY_WOLF_GROWL, SoundCategory.PLAYERS, 0.6f, 0.85f);
				return false;
			});
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((e, src, amount) -> {
			Object[] st = ACTIVE.get(e.getUuid());
			return st == null || !(boolean) st[2] || !src.isIn(DamageTypeTags.IS_FALL);
		});
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		for (int i = 0; i < effects.size(); i++) {
			p.addStatusEffect(new StatusEffectInstance(effects.get(i), duration, amps.get(i), false, false, true));
		}
		ACTIVE.put(p.getUuid(), new Object[]{flag, ticks + duration, fallImmune, sound});
		EntityFlags.set(p, flag, true);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:" + flag, 0), p);
		SoundEvent s = switch (sound) {
			case "wolf" -> SoundEvents.ENTITY_WOLF_GROWL;
			case "cat" -> SoundEvents.ENTITY_CAT_HISS;
			default -> SoundEvents.ENTITY_PARROT_FLY;
		};
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundCategory.PLAYERS, 0.8f, "wolf".equals(sound) ? 0.8f : 1.2f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_BREEZE_JUMP, SoundCategory.PLAYERS, 0.3f, 1.4f);
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		Object[] st = ACTIVE.remove(p.getUuid());
		if (st != null) EntityFlags.set(p, (String) st[0], false);
	}
}
