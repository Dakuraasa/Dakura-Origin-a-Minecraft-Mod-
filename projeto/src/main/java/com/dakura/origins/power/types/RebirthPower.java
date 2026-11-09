package com.dakura.origins.power.types;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.power.ability.LegendKit;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:rebirth" — Fênix: ao morrer, vira um ovo de chamas e renasce das cinzas com a vida cheia,
 * explodindo em fogo à sua volta. { "cooldown": 18000, "radius": 6, "damage": 8 }
 */
public final class RebirthPower implements RacePower {
	public static final String FLAG = "reborn";
	private static final Map<UUID, Long> READY_AT = new HashMap<>();
	private final int cooldown;
	private final double radius;
	private final float damage;

	public RebirthPower(JsonObject json) {
		this.cooldown = JsonParams.integer(json, "cooldown", 18000);
		this.radius = JsonParams.num(json, "radius", 6f);
		this.damage = JsonParams.num(json, "damage", 8f);
	}

	public static void bootstrap() {
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayerEntity p) || source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) return true;
			return RaceAccess.getRace(p).flatMap(r -> r.powers().stream().filter(x -> x instanceof RebirthPower)
					.map(x -> (RebirthPower) x).findFirst()).map(x -> !x.reborn(p)).orElse(true);
		});
	}

	private boolean reborn(ServerPlayerEntity p) {
		long now = p.getServerWorld().getTime();
		Long ready = READY_AT.get(p.getUuid());
		if (ready != null && now < ready) return false;
		READY_AT.put(p.getUuid(), now + cooldown);
		p.setHealth(p.getMaxHealth());
		p.clearStatusEffects();
		p.extinguish();
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.RESISTANCE, 60, 4, true, false, true));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 1200, 0, true, false, true));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 1, true, false, true));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH, 300, 1, true, false, true));
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 300, 1, true, false, true));
		Vec3d c = p.getPos();
		for (LivingEntity e : LegendKit.enemies(p, c, radius)) {
			LegendKit.hurt(p, e, damage, false);
			e.setOnFireFor(6f);
			Vec3d push = LegendKit.flat(e.getPos().subtract(c)).multiply(1.2);
			e.addVelocity(push.x, 0.6, push.z);
			e.velocityModified = true;
		}
		EntityFlags.set(p, FLAG, true);
		LegendKit.run(p, "rebirth", 100, (pl, age) -> true);
		LegendKit.anim(p, "rebirth", 40);
		LegendKit.fx(p, "rebirth", (int) (radius * 10));
		LegendKit.snd(p, SoundEvents.ITEM_TOTEM_USE, 0.8f, 0.7f);
		LegendKit.snd(p, SoundEvents.ENTITY_BLAZE_SHOOT, 1.2f, 0.5f);
		LegendKit.snd(p, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.8f);
		LegendKit.snd(p, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0f, 1.4f);
		return true;
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		if (p.age % 10 == 0 && EntityFlags.has(p, FLAG) && !LegendKit.running(p, "rebirth")) EntityFlags.set(p, FLAG, false);
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		READY_AT.remove(p.getUuid());
		EntityFlags.set(p, FLAG, false);
	}
}
