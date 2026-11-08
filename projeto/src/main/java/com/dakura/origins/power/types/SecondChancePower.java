package com.dakura.origins.power.types;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:second_chance" — passiva: ao morrer, fica com 1 coração e regeneração leve
 * (como o Totem da Imortalidade, sem os outros efeitos). Mostra a caveira na tela de quem voltou.
 * { "type": "dakura_origins:second_chance", "cooldown": 6000, "regen_seconds": 5 }
 * Não salva de dano que ignora invulnerabilidade (vazio, /kill), igual ao totem.
 */
public final class SecondChancePower implements RacePower {
	public static final String ANIM = "second_chance";
	private static final Map<UUID, Long> READY_AT = new HashMap<>();

	private final int cooldown;
	private final int regenTicks;

	public SecondChancePower(JsonObject json) {
		this.cooldown = JsonParams.integer(json, "cooldown", 6000);
		this.regenTicks = Math.round(JsonParams.num(json, "regen_seconds", 5f) * 20);
	}

	public static void bootstrap() {
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayerEntity player) || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
			return RaceAccess.getRace(player).flatMap(r -> r.powers().stream()
					.filter(p -> p instanceof SecondChancePower).map(p -> (SecondChancePower) p).findFirst())
					.map(p -> !p.trySave(player)).orElse(true);
		});
	}

	private boolean trySave(ServerPlayerEntity player) {
		long now = player.getServerWorld().getTime();
		Long ready = READY_AT.get(player.getUuid());
		if (ready != null && now < ready) return false;
		READY_AT.put(player.getUuid(), now + cooldown);

		player.setHealth(2f);
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, regenTicks, 0, true, false, true));
		ServerWorld w = player.getServerWorld();
		w.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_TOTEM_USE, SoundCategory.PLAYERS, 0.55f, 0.5f);
		w.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_WITHER_SKELETON_AMBIENT, SoundCategory.PLAYERS, 0.8f, 0.55f);
		w.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PARTICLE_SOUL_ESCAPE.value(), SoundCategory.PLAYERS, 1.2f, 0.6f);
		ModNetworking.sendAround(new AnimPayload(player.getId(), ANIM, 40), player);
		ModNetworking.sendAround(new AnimPayload(player.getId(), "fx:second_chance", 0), player);
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		READY_AT.remove(player.getUuid());
	}
}
