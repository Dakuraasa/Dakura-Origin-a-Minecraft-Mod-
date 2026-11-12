package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.power.ability.LegendKit;
import com.dakura.origins.power.ability.NightFx;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:moon_mark" — Marca do Eclipse (Elfo da Lua): à noite, a cada N golpes no mesmo alvo um clarão lunar
 * explode nele (dano extra mágico e cegueira breve).
 * { "hits": 3, "damage": 5 }
 */
public final class MoonMarkPower implements RacePower {
	private static final Map<UUID, int[]> COUNT = new HashMap<>(); // {id do alvo, golpes}
	private final int hits;
	private final float damage;

	public MoonMarkPower(JsonObject json) {
		this.hits = JsonParams.integer(json, "hits", 3);
		this.damage = JsonParams.num(json, "damage", 5f);
	}

	public static void bootstrap() {
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (world.isClient || !(player instanceof ServerPlayerEntity sp) || !(entity instanceof LivingEntity t) || !world.isNight()) return ActionResult.PASS;
			MoonMarkPower m = RaceAccess.getRace(sp).flatMap(r -> r.powers().stream().filter(x -> x instanceof MoonMarkPower)
					.map(x -> (MoonMarkPower) x).findFirst()).orElse(null);
			if (m == null) return ActionResult.PASS;
			int[] c = COUNT.computeIfAbsent(sp.getUuid(), u -> new int[2]);
			if (c[0] != t.getId()) {
				c[0] = t.getId();
				c[1] = 0;
			}
			if (++c[1] >= m.hits) {
				c[1] = 0;
				LegendKit.hurt(sp, t, m.damage, false);
				t.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 40, 0));
				Vec3d at = t.getPos().add(0, t.getHeight() * 0.6, 0);
				NightFx.burst(sp.getServerWorld(), at, 6, 0.35);
				sp.getServerWorld().spawnParticles(net.minecraft.particle.ParticleTypes.END_ROD, at.x, at.y, at.z, 12, 0.3, 0.4, 0.3, 0.08);
				LegendKit.sndAt(sp.getServerWorld(), at, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, 1f, 0.6f);
			}
			return ActionResult.PASS;
		});
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		COUNT.remove(p.getUuid());
	}
}
