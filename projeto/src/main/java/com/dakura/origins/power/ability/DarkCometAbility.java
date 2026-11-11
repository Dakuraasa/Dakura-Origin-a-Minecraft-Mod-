package com.dakura.origins.power.ability;

import com.dakura.origins.entity.GeoProjectileEntity;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.types.MaturityPower;
import com.dakura.origins.sound.ModSounds;
import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * "dakura_origins:dark_comet" — Cometa Negro (Kitsune Corrompida): esfera negra muito veloz. Ao acertar, causa dano,
 * prende o alvo (imobilização real) e o acorrenta à Kitsune. As correntes duram mais que a imobilização: solto, o
 * alvo continua preso por elas, puxado de volta se fugir longe e tomando dano contínuo até elas se partirem.
 * { "speed": 2.6, "range": 40, "damage": 6, "root": 60, "chain": 160, "tick_damage": 1.5, "leash": 9, "maturity_damage": [1,1.3,1.6] }
 */
public final class DarkCometAbility implements Ability {
	private final double speed, range, leash;
	private final float damage, tickDamage;
	private final int root, chain;
	private final float[] maturityDamage;

	public DarkCometAbility(JsonObject json) {
		speed = JsonParams.num(json, "speed", 2.6f);
		range = JsonParams.num(json, "range", 40f);
		leash = JsonParams.num(json, "leash", 9f);
		damage = JsonParams.num(json, "damage", 6f);
		tickDamage = JsonParams.num(json, "tick_damage", 1.5f);
		root = JsonParams.integer(json, "root", 60);
		chain = JsonParams.integer(json, "chain", 160);
		maturityDamage = JsonParams.floats(json, "maturity_damage");
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		ServerWorld w = p.getServerWorld();
		float mult = MaturityPower.multiplier(p, maturityDamage);
		GeoProjectileEntity orb = new GeoProjectileEntity(w, p, GeoProjectileEntity.DARK, 0, 1, 200);
		orb.scripted = true;
		orb.setSize(1.1f);
		orb.setVelocity(p.getRotationVec(1f).multiply(speed));
		w.spawnEntity(orb);
		LegendKit.anim(p, "cast", 8);
		ModSounds.play(p, ModSounds.DARK_COMET, 1f, 1f);
		Vec3d start = orb.getPos();
		LegendKit.run(p, "dark_comet", (int) Math.ceil(range / speed) + 4, (pl, age) -> {
			if (orb.isRemoved()) return false;
			Vec3d pos = orb.getPos(), next = pos.add(orb.getVelocity());
			LivingEntity victim = null;
			for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, orb.getBoundingBox().stretch(orb.getVelocity()).expand(0.5),
					e -> e.isAlive() && e != pl && !e.isSpectator() && LegendKit.enemy(pl, e))) {
				victim = e;
				break;
			}
			if (victim != null) {
				orb.discard();
				bind(pl, victim, mult);
				return false;
			}
			if (w.raycast(new RaycastContext(pos, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, orb))
					.getType() != HitResult.Type.MISS || pos.distanceTo(start) >= range) {
				LegendKit.fxAt(w, "dark_burst", pos, 0.6f, 0, 0);
				SpiritOrbAbility.breakBlocks(w, pl, pos, 1.6f);
				orb.discard();
				return false;
			}
			return true;
		}, pl -> {
			if (!orb.isRemoved()) orb.discard();
		});
		return true;
	}

	private void bind(ServerPlayerEntity p, LivingEntity t, float mult) {
		ServerWorld w = p.getServerWorld();
		LegendKit.stop(p, "dark_chain");
		LegendKit.hurt(p, t, damage * mult, false);
		SpiritOrbAbility.freeze(t, root);
		SpiritOrbAbility.breakBlocks(w, p, t.getPos().add(0, t.getHeight() * 0.5, 0), 1.6f);
		Vec3d mid = t.getPos().add(0, t.getHeight() * 0.5, 0);
		LegendKit.fxAt(w, "dark_burst", mid, 1.4f, 0, 0);
		ModSounds.play(w, mid, ModSounds.CHAIN_BIND, 1.2f, 0.9f);
		String flag = "chain#" + t.getId();
		EntityFlags.set(p, flag, true);
		Vec3d anchor = t.getPos();
		LegendKit.run(p, "dark_chain", chain, (pl, age) -> {
			if (!t.isAlive() || t.isRemoved() || t.getWorld() != pl.getWorld() || t.squaredDistanceTo(pl) > 40 * 40) return false;
			if (age < root) {
				FistBarrageAbility.pin(t, anchor);
				if (t instanceof MobEntity mob) mob.setAiDisabled(true);
			} else {
				if (age == root && t instanceof MobEntity mob) mob.setAiDisabled(false);
				// solto, mas ainda acorrentado: se fugir longe, a corrente puxa de volta
				Vec3d to = pl.getPos().subtract(t.getPos());
				double d = to.length();
				if (d > leash) {
					Vec3d pull = to.normalize().multiply(Math.min(0.6, (d - leash) * 0.15));
					t.addVelocity(pull.x, Math.max(0, pull.y) * 0.3, pull.z);
					t.velocityModified = true;
				}
			}
			if (age % 20 == 10) {
				t.timeUntilRegen = 0;
				LegendKit.hurt(pl, t, tickDamage * mult, false);
				LegendKit.fx(t, "chain_pulse", 0);
			}
			return true;
		}, pl -> {
			EntityFlags.set(pl, flag, false);
			if (t instanceof MobEntity mob) mob.setAiDisabled(false);
			ModSounds.play(pl.getServerWorld(), t.getPos(), ModSounds.CHAIN_BIND, 0.7f, 1.4f);
		});
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "dark_comet");
		LegendKit.stop(p, "dark_chain");
	}
}
