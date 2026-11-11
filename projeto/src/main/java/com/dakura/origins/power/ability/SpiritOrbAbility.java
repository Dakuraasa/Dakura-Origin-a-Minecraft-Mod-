package com.dakura.origins.power.ability;

import com.dakura.origins.entity.GeoProjectileEntity;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.types.MaturityPower;
import com.dakura.origins.sound.ModSounds;
import com.google.gson.JsonObject;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * "dakura_origins:spirit_orb" — Orbe Espiritual (Kitsune): lança uma esfera de mana azul na direção da mira.
 * Se acertar uma criatura, explode com força e volta para a Kitsune, explodindo de novo ao chegar (dano real
 * nas criaturas em volta dela). Se não acertar ninguém, só volta, sem nenhuma das duas explosões.
 * { "range": 14, "speed": 1.1, "damage": 6, "radius": 2.5, "return_damage": 5, "return_radius": 3.5,
 *   "maturity_damage": [1.0, 1.4, 1.8] }
 */
public final class SpiritOrbAbility implements Ability {
	private final double range, speed;
	private final float damage, radius, returnDamage, returnRadius;
	private final float[] maturityDamage;

	public SpiritOrbAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 14f);
		this.speed = JsonParams.num(json, "speed", 1.1f);
		this.damage = JsonParams.num(json, "damage", 6f);
		this.radius = JsonParams.num(json, "radius", 2.5f);
		this.returnDamage = JsonParams.num(json, "return_damage", 5f);
		this.returnRadius = JsonParams.num(json, "return_radius", 3.5f);
		this.maturityDamage = JsonParams.floats(json, "maturity_damage");
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		ServerWorld w = p.getServerWorld();
		float mult = MaturityPower.multiplier(p, maturityDamage);
		GeoProjectileEntity orb = new GeoProjectileEntity(w, p, GeoProjectileEntity.MANA, 0, 1, 600);
		orb.scripted = true;
		orb.setSize(1.3f);
		Vec3d look = p.getRotationVec(1f);
		orb.setVelocity(look.multiply(speed));
		w.spawnEntity(orb);
		LegendKit.anim(p, "cast", 10);
		ModSounds.play(p, ModSounds.MANA_CAST, 1f, 1f);
		Vec3d start = orb.getPos();
		boolean[] back = {false}, hit = {false};
		LegendKit.run(p, "spirit_orb", 240, (pl, age) -> {
			if (orb.isRemoved()) return false;
			Vec3d pos = orb.getPos();
			if (!back[0]) {
				Vec3d next = pos.add(orb.getVelocity());
				LivingEntity victim = null;
				for (LivingEntity e : w.getEntitiesByClass(LivingEntity.class, orb.getBoundingBox().stretch(orb.getVelocity()).expand(0.4),
						e -> e.isAlive() && e != pl && !e.isSpectator())) {
					victim = e;
					break;
				}
				if (victim != null) {
					hit[0] = true;
					back[0] = true;
					blast(pl, w, victim.getPos().add(0, victim.getHeight() * 0.5, 0), radius, damage * mult, victim, 2.2f);
				} else if (w.raycast(new RaycastContext(pos, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, orb))
						.getType() != HitResult.Type.MISS || pos.distanceTo(start) >= range) {
					back[0] = true;
				}
				if (back[0]) orb.setVelocity(orb.getVelocity().multiply(0.2));
				return true;
			}
			// volta acelerando até a Kitsune (atravessa blocos, como um espírito)
			Vec3d home = pl.getPos().add(0, pl.getHeight() * 0.6, 0);
			Vec3d to = home.subtract(pos);
			double d = to.length();
			if (d < 1.1) {
				if (hit[0]) blast(pl, w, home, returnRadius, returnDamage * mult, null, 3.2f);
				orb.discard();
				return false;
			}
			double sp = Math.min(d, speed * 1.25 + age * 0.01);
			orb.setVelocity(orb.getVelocity().multiply(0.35).add(to.normalize().multiply(sp * 0.65)));
			orb.velocityModified = true;
			return true;
		}, pl -> {
			if (!orb.isRemoved()) orb.discard();
		});
		return true;
	}

	/** Explosão de mana com dano real: o alvo atingido (se houver) e as criaturas hostis em volta. Nunca fere a Kitsune. */
	static void blast(ServerPlayerEntity p, ServerWorld w, Vec3d c, float r, float dmg, LivingEntity primary, float fx) {
		if (primary != null) LegendKit.hurt(p, primary, dmg, false);
		for (LivingEntity e : LegendKit.enemies(p, c, r)) {
			if (e == primary) continue;
			LegendKit.hurt(p, e, dmg * 0.75f, false);
			Vec3d push = LegendKit.flat(e.getPos().subtract(c)).multiply(0.4);
			e.addVelocity(push.x, 0.2, push.z);
			e.velocityModified = true;
		}
		LegendKit.fxAt(w, "boom_mana", c, fx, 0, 0);
		ModSounds.play(w, c, ModSounds.MANA_BURST, 1.1f, 0.9f + w.random.nextFloat() * 0.2f);
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "spirit_orb");
	}
}
