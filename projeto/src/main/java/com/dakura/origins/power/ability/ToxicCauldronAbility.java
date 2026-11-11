package com.dakura.origins.power.ability;

import com.dakura.origins.block.ModBlocks;
import com.dakura.origins.entity.GeoProjectileEntity;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.sound.ModSounds;
import com.google.gson.JsonObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:toxic_cauldron" — Caldeirão Venenoso (Dama Cogumelo): arremessa um caldeirão de ferro em arco.
 * Onde cai, ele estoura e fica plantado no chão por 10 s, vazando um veneno roxo que se espalha até 10 blocos:
 * dano contínuo, cegueira e lentidão. { "speed": 1.15, "radius": 10, "duration": 200, "damage": 2, "impact": 5 }
 */
public final class ToxicCauldronAbility implements Ability {
	private static float radius = 10f, damage = 2f, impact = 5f;
	private static int duration = 200;
	private final double speed;

	public ToxicCauldronAbility(JsonObject json) {
		this.speed = JsonParams.num(json, "speed", 1.15f);
		radius = JsonParams.num(json, "radius", 10f);
		duration = JsonParams.integer(json, "duration", 200);
		damage = JsonParams.num(json, "damage", 2f);
		impact = JsonParams.num(json, "impact", 5f);
	}

	public static void bootstrap() {
		GeoProjectileEntity.onEntityHit(GeoProjectileEntity.CAULDRON, (proj, hit) -> {
			if (hit instanceof LivingEntity l && proj.getWorld() instanceof ServerWorld w) {
				l.timeUntilRegen = 0;
				l.damage(AreaZones.poison(w, proj.getOwner()), impact);
			}
		});
		GeoProjectileEntity.onImpact(GeoProjectileEntity.CAULDRON, (proj, at) -> {
			ServerWorld w = (ServerWorld) proj.getWorld();
			Entity owner = proj.getOwner();
			BlockPos bp = BlockPos.ofFloored(at);
			for (int i = 0; i < 6 && w.getBlockState(bp.down()).isAir(); i++) bp = bp.down();
			if (!w.getBlockState(bp).isAir()) bp = bp.up();
			BlockPos placed = null;
			if (w.getBlockState(bp).isAir() || w.getBlockState(bp).isReplaceable()) {
				w.setBlockState(bp, ModBlocks.TOXIC_CAULDRON.getDefaultState());
				placed = bp;
			}
			Vec3d c = Vec3d.ofBottomCenter(bp);
			AreaZones.create(w, c, radius, 40, duration, AreaZones.Kind.POISON, owner, damage, placed);
			LegendKit.fxAt(w, "boom_poison", c.add(0, 0.6, 0), 3.5f, 0, 0);
			ModSounds.play(w, c, ModSounds.CAULDRON_SHATTER, 1.2f, 1f);
			ModSounds.play(w, c, ModSounds.POISON_BURST, 0.8f, 1.2f);
		});
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		GeoProjectileEntity c = new GeoProjectileEntity(p.getWorld(), p, GeoProjectileEntity.CAULDRON, 0.045, 0.99, 120);
		Vec3d look = p.getRotationVec(1f);
		c.setVelocity(look.x * speed, look.y * speed + 0.25, look.z * speed);
		p.getWorld().spawnEntity(c);
		LegendKit.anim(p, "throw", 12);
		ModSounds.play(p, ModSounds.CAULDRON_THROW, 1f, 1f);
		return true;
	}
}
