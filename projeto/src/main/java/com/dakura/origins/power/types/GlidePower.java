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

/**
 * "dakura_origins:glide" — a asa natural funciona como um élitro de verdade: mesma física, mesma postura
 * (corpo deitado no ar) e o mesmo controle. Abre apertando pular no ar, ou sozinha ao cair de "min_fall"
 * blocos; agachar fecha. Não gasta durabilidade (não é item). { "min_fall": 3.5 }
 */
public final class GlidePower implements RacePower {
	public static final String FLAG = "gliding";
	private final float minFall;

	public GlidePower(JsonObject json) {
		this.minFall = JsonParams.num(json, "min_fall", 3.5f);
	}

	public static boolean has(PlayerEntity p) {
		return RaceAccess.getRace(p).map(r -> r.powers().stream().anyMatch(x -> x instanceof GlidePower)).orElse(false);
	}

	public static void bootstrap() {
		// voo de élitro sem élitro: vale para o servidor e para o cliente (postura e física vanilla)
		EntityElytraEvents.CUSTOM.register((entity, tickElytra) -> entity instanceof PlayerEntity p && !p.isSneaking() && has(p));
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		boolean air = !p.isOnGround() && !p.isTouchingWater() && !p.getAbilities().flying && !p.hasVehicle();
		if (!p.isFallFlying() && air && !p.isSneaking() && p.fallDistance >= minFall && p.getVelocity().y < 0) {
			p.startFallFlying();
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PARROT_FLY, SoundCategory.PLAYERS, 1.0f, 0.7f);
		}
		if (p.isFallFlying() && p.isSneaking()) p.stopFallFlying();
		boolean gliding = p.isFallFlying();
		if (EntityFlags.has(p, FLAG) != gliding) EntityFlags.set(p, FLAG, gliding);
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		EntityFlags.set(p, FLAG, false);
		if (p.isFallFlying()) p.stopFallFlying();
	}
}
