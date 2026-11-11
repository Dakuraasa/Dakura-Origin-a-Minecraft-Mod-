package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:dash" — salto ágil para trás (ou na direção que anda, se "forward": true),
 * com velocidade breve depois. Com "orb": true, no meio do salto dispara para a frente uma esfera de energia
 * arcana (projétil 3D, sem fogo) que explode em luz azul no impacto.
 * { "power": 1.3, "lift": 0.45, "forward": false, "orb": true, "orb_damage": 6, "orb_speed": 1.6 }
 */
public final class ElfDashAbility implements Ability {
	private final double power, lift;
	private final boolean forward, orb;
	private static float orbDamage = 6f;
	private final double orbSpeed;

	public ElfDashAbility(JsonObject json) {
		this.power = JsonParams.num(json, "power", 1.3f);
		this.lift = JsonParams.num(json, "lift", 0.45f);
		this.forward = JsonParams.bool(json, "forward", false);
		this.orb = JsonParams.bool(json, "orb", false);
		orbDamage = JsonParams.num(json, "orb_damage", 6f);
		this.orbSpeed = JsonParams.num(json, "orb_speed", 1.6f);
	}

	public static void bootstrap() {
		com.dakura.origins.entity.GeoProjectileEntity.onEntityHit(com.dakura.origins.entity.GeoProjectileEntity.ARCANE, (proj, hit) -> {
			if (hit instanceof net.minecraft.entity.LivingEntity l && proj.getOwner() instanceof ServerPlayerEntity owner) {
				l.timeUntilRegen = 0;
				l.damage(owner.getDamageSources().indirectMagic(proj, owner), orbDamage);
				Vec3d push = proj.getVelocity().normalize();
				l.addVelocity(push.x * 0.8, 0.3, push.z * 0.8);
				l.velocityModified = true;
			}
		});
		com.dakura.origins.entity.GeoProjectileEntity.onImpact(com.dakura.origins.entity.GeoProjectileEntity.ARCANE, (proj, at) -> {
			var w = (net.minecraft.server.world.ServerWorld) proj.getWorld();
			LegendKit.fxAt(w, "boom_arcane", at, 1.6f, 0, 0);
			w.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_AMETHYST_CLUSTER_BREAK, SoundCategory.PLAYERS, 1.0f, 1.5f);
		});
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		Vec3d look = p.getRotationVec(1f);
		Vec3d flat = new Vec3d(look.x, 0, look.z);
		flat = flat.lengthSquared() < 1e-4 ? Vec3d.ZERO : flat.normalize().multiply(forward ? power : -power);
		p.setVelocity(flat.x, lift, flat.z);
		p.velocityModified = true;
		p.fallDistance = 0;
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 40, 1, true, false, true));
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:dash", 0), p);
		if (orb) {
			// a esfera sai 3 ticks depois, já no ar, na direção da mira
			LegendKit.run(p, "elf_orb", 4, (pl, age) -> {
				if (age < 3) return true;
				com.dakura.origins.entity.GeoProjectileEntity o = new com.dakura.origins.entity.GeoProjectileEntity(pl.getWorld(), pl,
						com.dakura.origins.entity.GeoProjectileEntity.ARCANE, 0.0, 1.0, 50);
				o.setVelocity(pl.getRotationVec(1f).multiply(orbSpeed));
				pl.getWorld().spawnEntity(o);
				LegendKit.anim(pl, "throw", 8);
				LegendKit.snd(pl, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 1.0f, 1.8f);
				LegendKit.snd(pl, SoundEvents.ENTITY_BREEZE_SHOOT, 0.8f, 1.4f);
				return false;
			});
		}
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_BREEZE_JUMP, SoundCategory.PLAYERS, 0.7f, 1.3f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value(), SoundCategory.PLAYERS, 0.6f, 1.4f);
		return true;
	}
}
