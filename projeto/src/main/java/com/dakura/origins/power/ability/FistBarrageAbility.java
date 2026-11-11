package com.dakura.origins.power.ability;

import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.sound.ModSounds;
import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:fist_barrage" — Fúria da Lebre (Vastaya Coelha), inspirada no estilo de luta de monge: avança no alvo,
 * o primeiro soco o prende (ele não anda, não pula, não ataca e não foge até o fim), segue uma barragem de socos
 * rapidíssimos (cada um com estouro de luz, onda de choque e faíscas no ponto do impacto) e fecha com um soco final
 * devastador que arremessa o alvo para longe. Só então ele recupera o controle.
 * { "range": 10, "punches": 10, "damage": 2.0, "finisher": 9, "knockback": 2.6 }
 */
public final class FistBarrageAbility implements Ability {
	public static final String FLAG = "barrage";
	private static final int APPROACH = 5, GAP = 2;
	private final double range;
	private final int punches;
	private final float damage, finisher, knockback;

	public FistBarrageAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 10f);
		this.punches = JsonParams.integer(json, "punches", 10);
		this.damage = JsonParams.num(json, "damage", 2.0f);
		this.finisher = JsonParams.num(json, "finisher", 9f);
		this.knockback = JsonParams.num(json, "knockback", 2.6f);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		LivingEntity t = LegendKit.aim(p, range, 0.85);
		if (t == null) {
			p.sendMessage(Text.translatable("dakura_origins.message.no_target"), true);
			return false;
		}
		int id = t.getId();
		int hitStart = APPROACH + 1, finalAt = hitStart + punches * GAP + 3, total = finalAt + 2;
		boolean[] grabbed = {false};
		boolean[] aiWasOff = {false};
		Vec3d[] anchor = {null};
		EntityFlags.set(p, FLAG, true);
		LegendKit.anim(p, "punch_rush", total);
		LegendKit.snd(p, ModSounds.WING_BEAT, 0.8f, 1.5f);
		LegendKit.run(p, "fist_barrage", total, (pl, age) -> {
			if (!(pl.getServerWorld().getEntityById(id) instanceof LivingEntity tg) || !tg.isAlive()) return false;
			pl.fallDistance = 0;
			Vec3d dir = LegendKit.flat(tg.getPos().subtract(pl.getPos()));
			if (age < APPROACH) {
				// avanço rápido até ficar cara a cara com o alvo
				Vec3d goal = tg.getPos().subtract(dir.multiply(1.15)).subtract(pl.getPos());
				pl.setVelocity(goal.multiply(0.5).add(0, 0.08, 0));
				pl.velocityModified = true;
				return true;
			}
			if (!grabbed[0]) {
				// primeiro soco: prende o alvo no lugar até o fim da sequência
				grabbed[0] = true;
				anchor[0] = tg.getPos();
				if (tg instanceof MobEntity mob) {
					aiWasOff[0] = mob.isAiDisabled();
					mob.setAiDisabled(true);
				}
			}
			// a coelha fica colada na frente do alvo durante a barragem
			Vec3d hold = anchor[0].subtract(dir.multiply(1.1)).subtract(pl.getPos());
			pl.setVelocity(hold.multiply(0.5));
			pl.velocityModified = true;
			pin(tg, anchor[0]);
			Vec3d contact = anchor[0].add(0, tg.getHeight() * 0.55, 0).subtract(dir.multiply(tg.getWidth() * 0.55));
			int k = (age - hitStart) / GAP;
			if (age >= hitStart && (age - hitStart) % GAP == 0 && k < punches) {
				LegendKit.hurt(pl, tg, damage, true);
				pin(tg, anchor[0]);
				LegendKit.fxAt(pl.getServerWorld(), "punch_hit", contact, k, (float) dir.x, (float) dir.z);
				ModSounds.play(tg, ModSounds.PUNCH_HIT, 0.8f, 0.9f + (k % 4) * 0.08f + pl.getRandom().nextFloat() * 0.1f);
			}
			if (age == finalAt) {
				LegendKit.hurt(pl, tg, finisher, true);
				release(tg, aiWasOff[0]);
				grabbed[0] = false;
				tg.setVelocity(dir.x * knockback, 0.55, dir.z * knockback);
				tg.velocityModified = true;
				LegendKit.anim(pl, "punch_final", 12);
				LegendKit.fxAt(pl.getServerWorld(), "punch_final", contact, 0, (float) dir.x, (float) dir.z);
				ModSounds.play(tg, ModSounds.PUNCH_FINAL, 1.3f, 1.0f);
				return false;
			}
			return true;
		}, pl -> {
			EntityFlags.set(pl, FLAG, false);
			if (grabbed[0] && pl.getServerWorld().getEntityById(id) instanceof LivingEntity tg) release(tg, aiWasOff[0]);
		});
		return true;
	}

	/** Prende o alvo: sem movimento, sem pulo, sem ataque. Jogadores são teleportados de volta ao ponto. */
	static void pin(LivingEntity tg, Vec3d at) {
		tg.setVelocity(Vec3d.ZERO);
		tg.velocityModified = true;
		if (tg instanceof ServerPlayerEntity sp) {
			if (sp.getPos().squaredDistanceTo(at) > 0.01) sp.networkHandler.requestTeleport(at.x, at.y, at.z, sp.getYaw(), sp.getPitch());
		} else tg.setPosition(at.x, at.y, at.z);
		tg.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 6, 254, true, false, false));
		tg.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 6, 254, true, false, false));
		tg.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 6, 4, true, false, false));
		if (tg instanceof MobEntity mob) {
			mob.setTarget(null);
			mob.getNavigation().stop();
		}
	}

	private static void release(LivingEntity tg, boolean aiWasOff) {
		if (tg instanceof MobEntity mob && !aiWasOff) mob.setAiDisabled(false);
		tg.removeStatusEffect(StatusEffects.SLOWNESS);
		tg.removeStatusEffect(StatusEffects.WEAKNESS);
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "fist_barrage");
	}
}
