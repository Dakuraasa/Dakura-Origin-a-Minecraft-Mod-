package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:golden_fist" — Punho de Ouro: o punho se cobre de ouro num soco pesado. Com uma chance pequena,
 * acerta o "Jackpot" (inspirado no Black Flash): dano triplo, "JACKPOT" dourado no ar e festa de cassino.
 * { "range": 4, "damage": 7, "jackpot_chance": 0.08, "jackpot_mult": 3 }
 */
public final class GoldenFistAbility implements Ability {
	private final double range;
	private final float damage, chance, mult;

	public GoldenFistAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 4f);
		this.damage = JsonParams.num(json, "damage", 7f);
		this.chance = JsonParams.num(json, "jackpot_chance", 0.08f);
		this.mult = JsonParams.num(json, "jackpot_mult", 3f);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		LivingEntity t = TargetUtil.aimed(p, range, e -> true);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "punch", 10), p);
		if (t == null) {
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_WEAK, SoundCategory.PLAYERS, 0.8f, 0.8f);
			p.sendMessage(Text.translatable("dakura_origins.message.no_target").formatted(Formatting.GRAY), true);
			return false;
		}
		boolean jackpot = p.getRandom().nextFloat() < chance;
		t.timeUntilRegen = 0;
		t.damage(p.getDamageSources().playerAttack(p), damage * (jackpot ? mult : 1f));
		Vec3d dir = t.getPos().subtract(p.getPos()).multiply(1, 0, 1);
		dir = dir.lengthSquared() < 1e-4 ? Vec3d.ZERO : dir.normalize();
		double k = jackpot ? 2.2 : 1.1;
		t.setVelocity(dir.x * k, 0.35 + (jackpot ? 0.3 : 0), dir.z * k);
		t.velocityModified = true;
		ModNetworking.sendAround(new AnimPayload(t.getId(), "fx:gold_fist", jackpot ? 1 : 0), p, t);
		p.getWorld().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_STRONG, SoundCategory.PLAYERS, 1.0f, 0.8f);
		p.getWorld().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.BLOCK_CHAIN_BREAK, SoundCategory.PLAYERS, 0.8f, 1.4f);
		if (jackpot) {
			ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:jackpot", 0), p);
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.2f);
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.PLAYERS, 1.0f, 1.6f);
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 0.8f, 1.2f);
		}
		return true;
	}
}
