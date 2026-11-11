package com.dakura.origins.power.ability;

import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:dawn_wings" — Asas da Aurora (Fênix): abre as asas e solta um anel de penas em chamas.
 * O fogo sagrado reconhece amigos: aliados se curam, se limpam e ficam imunes ao fogo; inimigos queimam.
 * Um sol pequeno fica girando sobre ela por alguns segundos, repetindo o pulso mais fraco.
 * { "radius": 9, "heal": 6, "damage": 5, "pulses": 4 }
 */
public final class DawnWingsAbility implements Ability {
	private final double radius;
	private final float heal, damage;
	private final int pulses;

	public DawnWingsAbility(JsonObject json) {
		this.radius = JsonParams.num(json, "radius", 9f);
		this.heal = JsonParams.num(json, "heal", 6f);
		this.damage = JsonParams.num(json, "damage", 5f);
		this.pulses = JsonParams.integer(json, "pulses", 4);
	}

	private void pulse(ServerPlayerEntity p, float mult) {
		Vec3d c = p.getPos();
		for (PlayerEntity o : LegendKit.allies(p, c, radius)) {
			o.heal(heal * mult);
			o.extinguish();
			o.addStatusEffect(new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 600, 0, true, false, true));
			if (mult >= 1f) {
				o.removeStatusEffect(StatusEffects.POISON);
				o.removeStatusEffect(StatusEffects.WITHER);
				o.removeStatusEffect(StatusEffects.WEAKNESS);
				o.removeStatusEffect(StatusEffects.SLOWNESS);
				o.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 1, true, false, true));
			}
			if (o != p) LegendKit.fx(o, "sun_blessed", 0);
		}
		for (LivingEntity e : LegendKit.enemies(p, c, radius)) {
			LegendKit.hurt(p, e, damage * mult, false);
			DragonFire.burn(p.getServerWorld(), e, p, 1.5f * mult, 5);
			if (mult >= 1f) {
				Vec3d push = LegendKit.flat(e.getPos().subtract(c));
				e.addVelocity(push.x * 0.9, 0.35, push.z * 0.9);
				e.velocityModified = true;
			}
		}
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		pulse(p, 1f);
		LegendKit.anim(p, "wings_spread", 26);
		LegendKit.fx(p, "dawn_wings", (int) (radius * 10));
		DragonFire.igniteGround(p.getServerWorld(), p.getPos(), (int) radius, 6);
		com.dakura.origins.sound.ModSounds.play(p, com.dakura.origins.sound.ModSounds.FIRE_ROAR, 1f, 1.2f);
		LegendKit.snd(p, SoundEvents.ENTITY_ENDER_DRAGON_FLAP, 1.2f, 0.9f);
		LegendKit.snd(p, SoundEvents.BLOCK_BEACON_ACTIVATE, 1.0f, 1.5f);
		LegendKit.snd(p, SoundEvents.ITEM_TOTEM_USE, 0.4f, 1.6f);
		LegendKit.snd(p, SoundEvents.ENTITY_BLAZE_SHOOT, 0.9f, 0.7f);
		LegendKit.run(p, "dawn_sun", pulses * 20 + 1, (pl, age) -> {
			if (age > 0 && age % 20 == 0) {
				pulse(pl, 0.35f);
				LegendKit.fx(pl, "dawn_pulse", (int) (radius * 10));
				LegendKit.snd(pl, SoundEvents.BLOCK_FIRE_AMBIENT, 1.0f, 1.4f);
			}
			return true;
		});
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "dawn_sun");
	}
}
