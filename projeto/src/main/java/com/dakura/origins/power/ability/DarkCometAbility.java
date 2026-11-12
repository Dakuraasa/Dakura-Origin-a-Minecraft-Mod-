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
 * "dakura_origins:dark_comet" — Prisão de Correntes (Kitsune Corrompida): acorrenta na hora os inimigos mais próximos num
 * raio de 10 blocos (3, 6 ou 9 conforme a maturidade). Cada um leva dano, fica imobilizado por "root" ticks e depois
 * segue acorrentado: se passar de "leash" blocos é puxado de volta, tomando dano contínuo até as correntes se partirem.
 * { "radius": 10, "damage": 6, "root": 60, "chain": 160, "tick_damage": 1.5, "leash": 10, "maturity_damage": [1,1.3,1.6] }
 */
public final class DarkCometAbility implements Ability {
	private final double radius, leash;
	private final float damage, tickDamage;
	private final int root, chain;
	private final float[] maturityDamage;

	public DarkCometAbility(JsonObject json) {
		radius = JsonParams.num(json, "radius", 10f);
		leash = JsonParams.num(json, "leash", 10f);
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
		int max = ManaOrbsAbility.count(p);
		java.util.List<LivingEntity> targets = new java.util.ArrayList<>(LegendKit.enemies(p, p.getPos(), radius));
		targets.removeIf(e -> e.getPos().squaredDistanceTo(p.getPos()) > radius * radius || !p.canSee(e));
		targets.sort(java.util.Comparator.comparingDouble(e -> e.squaredDistanceTo(p)));
		if (targets.isEmpty()) {
			p.sendMessage(net.minecraft.text.Text.translatable("dakura_origins.message.no_target").formatted(net.minecraft.util.Formatting.GRAY), true);
			return false;
		}
		LegendKit.anim(p, "cast", 8);
		ModSounds.play(p, ModSounds.DARK_COMET, 1f, 1f);
		LegendKit.fxAt(w, "dark_burst", p.getPos().add(0, 1, 0), 0.8f, 0, 0);
		for (int i = 0; i < Math.min(max, targets.size()); i++) bind(p, targets.get(i), mult);
		return true;
	}

	private void bind(ServerPlayerEntity p, LivingEntity t, float mult) {
		ServerWorld w = p.getServerWorld();
		String key = "dark_chain#" + t.getId();
		LegendKit.stop(p, key);
		LegendKit.hurt(p, t, damage * mult, false);
		SpiritOrbAbility.freeze(t, root);
		Vec3d mid = t.getPos().add(0, t.getHeight() * 0.5, 0);
		LegendKit.fxAt(w, "dark_burst", mid, 1.4f, 0, 0);
		ModSounds.play(w, mid, ModSounds.CHAIN_BIND, 1.2f, 0.9f);
		String flag = "chain#" + t.getId();
		EntityFlags.set(p, flag, true);
		Vec3d anchor = t.getPos();
		LegendKit.run(p, key, chain, (pl, age) -> {
			if (!t.isAlive() || t.isRemoved() || t.getWorld() != pl.getWorld() || t.squaredDistanceTo(pl) > 40 * 40) return false;
			if (age < root) {
				FistBarrageAbility.pin(t, anchor);
				com.dakura.origins.power.Control.lock(t, 2, anchor);
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
		LegendKit.stopPrefix(p, "dark_chain#");
	}
}
