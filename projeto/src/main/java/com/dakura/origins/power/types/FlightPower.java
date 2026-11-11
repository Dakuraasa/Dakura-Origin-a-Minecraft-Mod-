package com.dakura.origins.power.types;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.EntityElytraEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;

import java.util.Locale;

/**
 * "dakura_origins:flight" — voo dinâmico das raças aladas: postura de planagem do élitro (corpo deitado no ar),
 * sem élitro e sem foguete. Pular no ar abre as asas; segurando "para frente" o bater de asas dá impulso contínuo
 * (FlightClient, no cliente, que é quem manda no movimento do jogador). Agachar fecha. Gasta um pouco de fome.
 * O impulso e a velocidade máxima saem de "speed": { "speed": 0.035, "exhaustion": 0.02 }
 */
public final class FlightPower implements RacePower {
	private final float speed, exhaustion;
	private final String flag;

	public FlightPower(JsonObject json) {
		this.speed = JsonParams.num(json, "speed", 0.035f);
		this.exhaustion = JsonParams.num(json, "exhaustion", 0.02f);
		// impulso por tick e velocidade máxima (blocos/tick), enviados ao cliente pela flag
		this.flag = String.format(Locale.ROOT, "flier#%.4f#%.3f", speed * 1.6f, speed * 45f);
	}

	public static boolean has(PlayerEntity p) {
		return RaceAccess.getRace(p).map(r -> r.powers().stream().anyMatch(x -> x instanceof FlightPower)).orElse(false);
	}

	public static void bootstrap() {
		// planagem sem élitro (servidor e cliente); estátua/meteoro e fome zero não voam
		EntityElytraEvents.CUSTOM.register((entity, tickElytra) -> entity instanceof PlayerEntity p && !p.isSneaking() && has(p)
				&& p.getHungerManager().getFoodLevel() > 0
				&& !(p instanceof ServerPlayerEntity sp && (EntityFlags.has(sp, StatuePower.FLAG) || EntityFlags.has(sp, "meteor"))));
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		if (p.isCreative() || p.isSpectator()) return;
		if (!EntityFlags.has(p, flag)) EntityFlags.set(p, flag, true);
		// quem tinha o voo antigo (criativo) perde ao entrar nesta versão
		var a = p.getAbilities();
		if (a.allowFlying) {
			a.allowFlying = false;
			a.flying = false;
			a.setFlySpeed(0.05f);
			p.sendAbilitiesUpdate();
		}
		if (p.isFallFlying()) {
			if (p.isSneaking()) p.stopFallFlying();
			p.addExhaustion(exhaustion);
			if (p.age % 14 == 0) p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(),
					SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 0.3f, 0.55f);
		}
		if (p.isFallFlying() || p.isOnGround()) p.fallDistance = 0;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		EntityFlags.set(p, flag, false);
		if (p.isFallFlying()) p.stopFallFlying();
	}
}
