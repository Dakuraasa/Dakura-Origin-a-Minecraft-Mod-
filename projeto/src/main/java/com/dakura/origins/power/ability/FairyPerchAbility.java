package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * "dakura_origins:fairy_perch" — a Fada pousa no ombro do jogador mais próximo: ele regenera e ganha o dobro
 * de experiência enquanto ela estiver lá. Toque de novo (ou agache) para sair.
 */
public final class FairyPerchAbility implements Ability {
	public static final String FLAG = "fairy";

	public FairyPerchAbility(JsonObject json) {}

	/** Os dois lados: esta entidade é uma Fada? */
	public static boolean isFairy(Entity e) {
		if (!(e instanceof PlayerEntity)) return false;
		return e.getWorld().isClient ? EntityFlags.clientHas(e, FLAG) : e instanceof ServerPlayerEntity sp && EntityFlags.has(sp, FLAG);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (server.getTicks() % 40 != 0) return;
			for (ServerPlayerEntity host : server.getPlayerManager().getPlayerList()) {
				for (Entity pass : host.getPassengerList()) {
					if (!isFairy(pass)) continue;
					host.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 0, true, false, true));
					host.getServerWorld().spawnParticles(net.minecraft.particle.ParticleTypes.WAX_OFF, host.getX(), host.getBodyY(0.8),
							host.getZ(), 3, 0.3, 0.3, 0.3, 0.02);
				}
			}
		});
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		if (p.getVehicle() instanceof PlayerEntity) {
			p.stopRiding();
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ALLAY_AMBIENT_WITHOUT_ITEM, SoundCategory.PLAYERS, 0.8f, 1.5f);
			return false;
		}
		PlayerEntity host = null;
		double best = 16;
		for (PlayerEntity o : p.getWorld().getEntitiesByClass(PlayerEntity.class, p.getBoundingBox().expand(4),
				x -> x != p && x.isAlive() && !x.isSpectator() && !x.hasPassengers() && !isFairy(x))) {
			double d = o.squaredDistanceTo(p);
			if (d < best) {
				best = d;
				host = o;
			}
		}
		if (host == null) {
			p.sendMessage(Text.translatable("dakura_origins.message.no_shoulder").formatted(Formatting.GRAY), true);
			return false;
		}
		p.getAbilities().flying = false;
		p.sendAbilitiesUpdate();
		if (!p.startRiding(host, true)) return false;
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:perch", host.getId()), p, host);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ALLAY_ITEM_GIVEN, SoundCategory.PLAYERS, 1.0f, 1.4f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.8f, 1.8f);
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		if (p.getVehicle() instanceof PlayerEntity) p.stopRiding();
	}
}
