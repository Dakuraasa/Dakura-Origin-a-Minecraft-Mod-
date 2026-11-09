package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.power.ability.LegendKit;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "dakura_origins:rabbit_frenzy" — Vastaya Coelho: cada soco seguido acumula frenesi (pressa e velocidade).
 * No acúmulo máximo, o próximo golpe é um Chute Lunar: dano extra e arremesso. { "max": 5, "finisher": 6 }
 */
public final class RabbitFrenzyPower implements RacePower {
	private static final Map<UUID, long[]> STACK = new ConcurrentHashMap<>(); // {acúmulo, último tick}
	private static boolean reentry;
	private final int max;
	private final float finisher;

	public RabbitFrenzyPower(JsonObject json) {
		this.max = JsonParams.integer(json, "max", 5);
		this.finisher = JsonParams.num(json, "finisher", 6f);
	}

	public static void bootstrap() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((target, source, base, taken, blocked) -> {
			if (reentry || blocked || !(source.getAttacker() instanceof ServerPlayerEntity p) || source.getSource() != p) return;
			RabbitFrenzyPower pw = RaceAccess.getRace(p).flatMap(r -> r.powers().stream().filter(x -> x instanceof RabbitFrenzyPower)
					.map(x -> (RabbitFrenzyPower) x).findFirst()).orElse(null);
			if (pw == null) return;
			long now = p.getWorld().getTime();
			long[] s = STACK.computeIfAbsent(p.getUuid(), k -> new long[]{0, now});
			if (now - s[1] > 50) s[0] = 0;
			s[1] = now;
			if (s[0] >= pw.max) {
				s[0] = 0;
				reentry = true;
				try {
					LegendKit.hurt(p, target, pw.finisher, true);
				} finally {
					reentry = false;
				}
				Vec3d dir = LegendKit.flat(target.getPos().subtract(p.getPos()));
				target.addVelocity(dir.x * 1.1, 0.75, dir.z * 1.1);
				target.velocityModified = true;
				LegendKit.fx(target, "moon_kick", p.getId());
				LegendKit.anim(p, "kick_high", 10);
				LegendKit.snd(target, SoundEvents.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.0f, 0.7f);
				LegendKit.snd(target, SoundEvents.ENTITY_IRON_GOLEM_ATTACK, 0.8f, 1.3f);
				LegendKit.snd(target, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, 0.8f, 1.5f);
				return;
			}
			s[0]++;
			int lvl = (int) s[0];
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE, 60, Math.min(2, lvl / 2), true, false, true));
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, Math.min(2, lvl / 2), true, false, true));
			LegendKit.fx(target, "frenzy_hit", lvl);
			LegendKit.snd(target, SoundEvents.ENTITY_PLAYER_ATTACK_CRIT, 0.5f, 1.2f + lvl * 0.1f);
		});
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		STACK.remove(p.getUuid());
	}
}
