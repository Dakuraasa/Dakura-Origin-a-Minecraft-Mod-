package com.dakura.origins.power.ability;

import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.sound.ModSounds;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "dakura_origins:skyfall_grab" — Queda do Céu (Dragão Marrom, inspirada no R do Sett): agarra o inimigo à frente.
 * 3 s de preparo com as asas abertas (os dois ficam presos; qualquer dano no dragão cancela e solta o alvo),
 * voo vertical levando o alvo e mergulho violento: explosão verde, cratera, fragmentos e fogo verde no chão.
 * { "range": 4.5, "windup": 60, "rise": 16, "damage": 18, "radius": 5, "splash": 8 }
 */
public final class SkyfallGrabAbility implements Ability {
	public static final String FLAG = "skyfall";
	private static final Map<UUID, Boolean> WINDUP = new ConcurrentHashMap<>();
	private final double range, radius;
	private final int windup, rise;
	private final float damage, splash;

	public SkyfallGrabAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 4.5f);
		this.windup = JsonParams.integer(json, "windup", 60);
		this.rise = JsonParams.integer(json, "rise", 16);
		this.damage = JsonParams.num(json, "damage", 18f);
		this.radius = JsonParams.num(json, "radius", 5f);
		this.splash = JsonParams.num(json, "splash", 8f);
	}

	public static void bootstrap() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (entity instanceof ServerPlayerEntity p && WINDUP.containsKey(p.getUuid()) && taken > 0) {
				WINDUP.put(p.getUuid(), false); // a tarefa vê isto no próximo tick e cancela
			}
		});
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		LivingEntity t = LegendKit.aim(p, range, 0.75);
		if (t == null) {
			p.sendMessage(Text.translatable("dakura_origins.message.no_target"), true);
			return false;
		}
		int id = t.getId();
		WINDUP.put(p.getUuid(), true);
		EntityFlags.set(p, FLAG, true);
		LegendKit.anim(p, "grab", windup + rise + 40);
		LegendKit.fx(p, "charge_green", windup);
		ModSounds.play(p, ModSounds.DRAGON_GRAB, 1.2f, 1f);
		Vec3d anchor = p.getPos();
		boolean[] cancelled = {false};
		int[] phase = {0};
		LegendKit.run(p, "skyfall", windup + rise + 80, (pl, age) -> {
			if (!(pl.getServerWorld().getEntityById(id) instanceof LivingEntity tg) || !tg.isAlive()) return false;
			Vec3d fwd = LegendKit.flat(pl.getRotationVec(1f));
			Vec3d hold = pl.getPos().add(fwd.multiply(1.1)).add(0, 0.2, 0);
			if (age < windup) {
				if (Boolean.FALSE.equals(WINDUP.get(pl.getUuid()))) {
					cancelled[0] = true;
					return false;
				}
				pl.setVelocity(0, Math.min(0, pl.getVelocity().y), 0);
				pl.requestTeleport(anchor.x, pl.getY(), anchor.z);
				tg.requestTeleport(hold.x, hold.y, hold.z);
				tg.setVelocity(Vec3d.ZERO);
				tg.velocityModified = true;
				tg.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 5, 9, true, false, false));
				if (age % 20 == 0) ModSounds.play(pl, ModSounds.WING_BEAT, 1f, 0.7f);
				return true;
			}
			WINDUP.remove(pl.getUuid());
			pl.fallDistance = 0;
			if (age < windup + rise) {
				if (phase[0] == 0) {
					phase[0] = 1;
					ModSounds.play(pl, ModSounds.WING_BEAT, 1.4f, 0.6f);
				}
				pl.setVelocity(0, 1.0, 0);
				pl.velocityModified = true;
			} else {
				if (phase[0] == 1) {
					phase[0] = 2;
					LegendKit.fx(pl, "dive_green", 0);
				}
				pl.setVelocity(fwd.x * 0.3, -2.2, fwd.z * 0.3);
				pl.velocityModified = true;
				if (age > windup + rise + 2 && (pl.isOnGround() || pl.isTouchingWater())) return false;
			}
			tg.requestTeleport(hold.x, hold.y - 0.4, hold.z);
			tg.fallDistance = 0;
			return true;
		}, pl -> {
			WINDUP.remove(pl.getUuid());
			EntityFlags.set(pl, FLAG, false);
			if (cancelled[0]) {
				LegendKit.fx(pl, "grab_break", 0);
				pl.sendMessage(Text.translatable("dakura_origins.message.skyfall_cancel"), true);
				return;
			}
			if (phase[0] < 2) return;
			pl.fallDistance = 0;
			Vec3d c = pl.getPos();
			if (pl.getServerWorld().getEntityById(id) instanceof LivingEntity tg && tg.isAlive()) {
				tg.timeUntilRegen = 0;
				tg.damage(pl.getDamageSources().playerAttack(pl), damage);
				if (!tg.isFireImmune()) tg.setOnFireFor(6);
				tg.fallDistance = 0;
			}
			DragonFire.quiet = true;
			try {
				DragonFire.explode(pl.getServerWorld(), c.add(0, 0.5, 0), pl, (float) radius, splash, splash * 0.5f, 6, 100, 4f);
			} finally {
				DragonFire.quiet = false;
			}
			// impacto = o mesmo pisão do dragão (ondas, pedaços do chão e poeira)
			com.dakura.origins.network.ModNetworking.sendAround(new com.dakura.origins.network.AnimPayload(pl.getId(), "fx:stomp", (int) Math.round(radius * 10)), pl);
			LegendKit.fxAt(pl.getServerWorld(), "debris", c, (int) Math.round(radius * 7), (float) radius, 0);
			ModSounds.play(pl, ModSounds.DRAGON_SLAM, 1.6f, 0.9f);
		});
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "skyfall");
	}
}
