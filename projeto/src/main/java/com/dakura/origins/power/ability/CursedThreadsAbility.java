package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * "dakura_origins:cursed_threads" — Fios da Marionete: fios de energia amaldiçoada saem dos dedos da Boneca,
 * prendem até 3 inimigos à frente, puxam todos para perto e os deixam presos por um instante.
 * { "range": 12, "targets": 3, "damage": 3, "bind": 40 }
 */
public final class CursedThreadsAbility implements Ability {
	private final double range;
	private final int targets, bind;
	private final float damage;

	public CursedThreadsAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 12f);
		this.targets = JsonParams.integer(json, "targets", 3);
		this.bind = JsonParams.integer(json, "bind", 40);
		this.damage = JsonParams.num(json, "damage", 3f);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		Vec3d eye = p.getEyePos(), look = p.getRotationVec(1f);
		List<LivingEntity> found = new ArrayList<>();
		for (Entity e : p.getWorld().getOtherEntities(p, p.getBoundingBox().expand(range))) {
			if (!(e instanceof LivingEntity le) || !le.isAlive() || e.isSpectator()) continue;
			Vec3d to = e.getPos().add(0, e.getHeight() / 2, 0).subtract(eye);
			if (to.length() > range || to.normalize().dotProduct(look) < 0.82 || !p.canSee(e)) continue;
			found.add(le);
		}
		if (found.isEmpty()) {
			p.sendMessage(Text.translatable("dakura_origins.message.no_target").formatted(Formatting.GRAY), true);
			return false;
		}
		found.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(p)));
		int ids = 0, n = 0;
		for (LivingEntity t : found) {
			if (n >= targets) break;
			t.damage(p.getDamageSources().indirectMagic(p, p), damage);
			Vec3d pull = p.getPos().subtract(t.getPos());
			double d = pull.length();
			if (d > 2) {
				pull = pull.normalize().multiply(Math.min(1.6, 0.35 + d * 0.12));
				t.setVelocity(pull.x, 0.25 + Math.min(0.3, d * 0.02), pull.z);
				t.velocityModified = true;
			}
			t.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, bind, 4));
			t.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, bind, 0));
			ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:threads", t.getId()), p, t);
			n++;
		}
		ModNetworking.sendAround(new AnimPayload(p.getId(), "puppet", 14), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_TRIPWIRE_CLICK_ON, SoundCategory.PLAYERS, 1.0f, 0.6f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_WARDEN_SONIC_CHARGE, SoundCategory.PLAYERS, 0.3f, 2.0f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 0.9f, 1.4f);
		return true;
	}
}
