package com.dakura.origins.power.ability;

import com.dakura.origins.entity.GeoProjectileEntity;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.types.MaturityPower;
import com.dakura.origins.sound.ModSounds;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:magma_volley" — Rajada de Magma (Dragão Marrom): dispara blocos de magma verde que giram no ar.
 * A maturidade aumenta a QUANTIDADE (1, 2 e 3 disparos em sequência), não a força de cada um.
 * Cada impacto: explosão verde, fogo real, fragmentos de terreno voando e uma pequena área de fogo verde.
 * { "speed": 1.5, "interval": 6, "radius": 3.5, "blast": 5, "fire": 3 }
 */
public final class MagmaVolleyAbility implements Ability {
	private static float radius = 3.5f, blast = 5f, fire = 3f;
	private final double speed;
	private final int interval;

	public MagmaVolleyAbility(JsonObject json) {
		this.speed = JsonParams.num(json, "speed", 1.5f);
		this.interval = JsonParams.integer(json, "interval", 6);
		radius = JsonParams.num(json, "radius", 3.5f);
		blast = JsonParams.num(json, "blast", 5f);
		fire = JsonParams.num(json, "fire", 3f);
	}

	public static void bootstrap() {
		GeoProjectileEntity.onImpact(GeoProjectileEntity.MAGMA, (proj, at) ->
				DragonFire.explode((ServerWorld) proj.getWorld(), at, proj.getOwner(), radius, blast, fire, 5, 60, 2.5f));
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		int shots = 1 + Math.max(0, MaturityPower.stage(p));
		LegendKit.run(p, "magma_volley", shots * interval + 1, (pl, age) -> {
			if (age % interval == 0 && age / interval < shots) {
				GeoProjectileEntity m = new GeoProjectileEntity(pl.getWorld(), pl, GeoProjectileEntity.MAGMA, 0.0, 1.0, 60);
				Vec3d look = pl.getRotationVec(1f);
				double spread = (age / interval) * 0.04;
				m.setVelocity(look.x * speed + (pl.getRandom().nextDouble() - 0.5) * spread, look.y * speed,
						look.z * speed + (pl.getRandom().nextDouble() - 0.5) * spread);
				pl.getWorld().spawnEntity(m);
				LegendKit.anim(pl, "throw", 8);
				ModSounds.play(pl, ModSounds.MAGMA_LAUNCH, 1f, 0.9f + age / interval * 0.12f);
			}
			return true;
		});
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "magma_volley");
	}
}
