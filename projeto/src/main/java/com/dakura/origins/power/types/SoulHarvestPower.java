package com.dakura.origins.power.types;

import com.dakura.origins.entity.SoulOrbEntity;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

/**
 * "dakura_origins:soul_harvest" — Colheita de Almas (Demônio). Quem ele mata deixa almas vermelhas (esferas de
 * experiência tingidas de sangue) proporcionais à força da vítima; ao absorvê-las ele amadurece. Na maturidade máxima
 * ganha Espinhos Infernais: devolve parte do dano efetivamente recebido a quem o atacou.
 * { "points_per_health": 1.0, "player_points": 60, "max_orbs": 10, "thorns": 0.3 }
 */
public final class SoulHarvestPower implements RacePower {
	public static final String FLAG = "soul_harvest";
	private static boolean reflecting;
	private final float pointsPerHealth, thorns;
	private final int playerPoints, maxOrbs;

	public SoulHarvestPower(JsonObject json) {
		this.pointsPerHealth = JsonParams.num(json, "points_per_health", 1.0f);
		this.playerPoints = JsonParams.integer(json, "player_points", 60);
		this.maxOrbs = JsonParams.integer(json, "max_orbs", 10);
		this.thorns = JsonParams.num(json, "thorns", 0.3f);
	}

	private static SoulHarvestPower of(PlayerEntity p) {
		return RaceAccess.getRace(p).flatMap(r -> r.powers().stream().filter(x -> x instanceof SoulHarvestPower)
				.map(x -> (SoulHarvestPower) x).findFirst()).orElse(null);
	}

	public static void bootstrap() {
		ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
			if (!(source.getAttacker() instanceof ServerPlayerEntity demon) || victim == demon || victim instanceof ArmorStandEntity) return;
			SoulHarvestPower s = of(demon);
			if (s == null || !(victim.getWorld() instanceof ServerWorld w)) return;
			boolean player = victim instanceof PlayerEntity;
			int total = player ? s.playerPoints : Math.max(1, Math.round(victim.getMaxHealth() * s.pointsPerHealth));
			// fracos: 1-2 almas; fortes: mais almas (e maiores), sem passar do limite
			int count = player ? 6 : Math.max(1, Math.min(s.maxOrbs, (int) Math.ceil(Math.sqrt(victim.getMaxHealth()) / 1.6)));
			Vec3d at = victim.getPos().add(0, victim.getHeight() * 0.4, 0);
			int left = total;
			for (int i = 0; i < count; i++) {
				int v = i == count - 1 ? left : Math.max(1, total / count);
				left -= v;
				if (v <= 0) break;
				w.spawnEntity(new SoulOrbEntity(w, at, v));
			}
			w.spawnParticles(new DustParticleEffect(new Vector3f(0.45f, 0.0f, 0.05f), 1.4f), at.x, at.y, at.z, 10, 0.3, 0.4, 0.3, 0);
		});

		// Espinhos Infernais: 30% do dano efetivamente sofrido volta para o atacante (nunca reflete reflexo)
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (reflecting || taken <= 0 || !(entity instanceof ServerPlayerEntity demon)) return;
			if (!(source.getAttacker() instanceof LivingEntity attacker) || attacker == demon || !attacker.isAlive()) return;
			SoulHarvestPower s = of(demon);
			if (s == null || s.thorns <= 0 || !MaturityPower.isMax(demon)) return;
			reflecting = true;
			try {
				attacker.damage(demon.getDamageSources().thorns(demon), taken * s.thorns);
			} finally {
				reflecting = false;
			}
		});
	}

	@Override
	public void onApply(ServerPlayerEntity p) {
		EntityFlags.set(p, FLAG, true);
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		EntityFlags.set(p, FLAG, false);
	}
}
