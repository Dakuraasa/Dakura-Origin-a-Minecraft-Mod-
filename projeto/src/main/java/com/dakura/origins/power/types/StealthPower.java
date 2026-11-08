package com.dakura.origins.power.types;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "dakura_origins:stealth" — passos sem som; parada por alguns segundos, fica invisível e
 * nenhum monstro a detecta (mas ainda pode ser atingida). Mexer-se ou atacar revela.
 * { "still_ticks": 60 }
 */
public final class StealthPower implements RacePower {
	public static final String SILENT_FLAG = "silent_steps";
	private static final Map<UUID, Integer> STILL = new ConcurrentHashMap<>();
	private static final Map<UUID, Vec3d> LAST = new ConcurrentHashMap<>();
	private final int stillTicks;
	private static int hideAfter = 60;

	public StealthPower(JsonObject json) {
		this.stillTicks = JsonParams.integer(json, "still_ticks", 60);
		hideAfter = stillTicks;
	}

	/** Servidor: está escondida agora? */
	public static boolean hidden(PlayerEntity p) {
		return STILL.getOrDefault(p.getUuid(), 0) >= hideAfter;
	}

	/** Atacar ou ser notada revela. */
	public static void reveal(PlayerEntity p) {
		if (STILL.containsKey(p.getUuid())) STILL.put(p.getUuid(), 0);
	}

	@Override
	public void onApply(ServerPlayerEntity player) {
		EntityFlags.set(player, SILENT_FLAG, true);
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		EntityFlags.set(player, SILENT_FLAG, false);
		STILL.remove(player.getUuid());
		LAST.remove(player.getUuid());
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		Vec3d pos = p.getPos(), last = LAST.put(p.getUuid(), pos);
		boolean moved = last == null || last.squaredDistanceTo(pos) > 0.0004 || p.handSwinging;
		int before = STILL.getOrDefault(p.getUuid(), 0);
		int now = moved ? 0 : before + 1;
		STILL.put(p.getUuid(), now);
		if (now == stillTicks) {
			// sumiu: quem estava mirando nela perde o alvo
			for (MobEntity m : p.getServerWorld().getEntitiesByClass(MobEntity.class, p.getBoundingBox().expand(32),
					m -> m.getTarget() == p)) m.setTarget(null);
			p.getServerWorld().spawnParticles(ParticleTypes.WHITE_SMOKE, p.getX(), p.getBodyY(0.5), p.getZ(), 6, 0.25, 0.4, 0.25, 0.01);
		}
		if (now >= stillTicks && p.age % 5 == 0) {
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 12, 0, true, false, false));
		} else if (now == 0 && before >= stillTicks) {
			StatusEffectInstance inv = p.getStatusEffect(StatusEffects.INVISIBILITY);
			if (inv != null && inv.isAmbient() && inv.getDuration() <= 12) p.removeStatusEffect(StatusEffects.INVISIBILITY);
		}
	}

	/** Os dois lados: este jogador anda sem fazer barulho? */
	public static boolean silent(PlayerEntity p) {
		return p.getWorld().isClient ? EntityFlags.clientHas(p, SILENT_FLAG)
				: p instanceof ServerPlayerEntity sp && EntityFlags.has(sp, SILENT_FLAG);
	}
}
