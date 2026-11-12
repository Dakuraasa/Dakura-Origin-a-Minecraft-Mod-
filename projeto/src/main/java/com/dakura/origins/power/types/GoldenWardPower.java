package com.dakura.origins.power.types;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.power.ability.LegendKit;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:golden_ward" — Alto Elfo: a cada "interval" ticks ganha uma aura dourada (contorno brilhante)
 * que anula por completo o próximo ataque. Golpe corpo a corpo: o atacante (e quem estiver perto) é arremessado
 * para longe. Projétil ou habilidade lançada: ela se teletransporta antes do impacto para um ponto aleatório
 * (nunca dentro do chão); se surgir no ar, cai devagar. { "interval": 1200, "push": 1.6, "min": 6, "max": 12 }
 */
public final class GoldenWardPower implements RacePower {
	public static final String FLAG = "gold_ward";
	private static final Map<UUID, Long> NEXT = new HashMap<>();
	private static boolean reentry;
	private final int interval;
	private final double push, min, max;

	public GoldenWardPower(JsonObject json) {
		this.interval = JsonParams.integer(json, "interval", 1200);
		this.push = JsonParams.num(json, "push", 1.6f);
		this.min = JsonParams.num(json, "min", 6f);
		this.max = JsonParams.num(json, "max", 12f);
	}

	public static void bootstrap() {
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((e, src, amount) -> {
			if (reentry || !(e instanceof ServerPlayerEntity p) || !EntityFlags.has(p, FLAG)) return true;
			if (src.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY) || src.getAttacker() == p) return true;
			GoldenWardPower pw = RaceAccess.getRace(p).flatMap(r -> r.powers().stream().filter(x -> x instanceof GoldenWardPower)
					.map(x -> (GoldenWardPower) x).findFirst()).orElse(null);
			if (pw == null) return true;
			boolean ranged = isRanged(src);
			if (!ranged && !(src.getAttacker() instanceof LivingEntity)) return true; // queda, fome etc. não gastam a proteção
			EntityFlags.set(p, FLAG, false);
			NEXT.put(p.getUuid(), p.getServerWorld().getTime() + pw.interval);
			reentry = true;
			try {
				if (ranged) pw.blink(p, src);
				else pw.repel(p, (LivingEntity) src.getAttacker());
			} finally {
				reentry = false;
			}
			return false;
		});
	}

	private static boolean isRanged(DamageSource src) {
		Entity direct = src.getSource();
		return src.isIn(DamageTypeTags.IS_PROJECTILE) || direct instanceof ProjectileEntity
				|| (direct != null && src.getAttacker() != null && direct != src.getAttacker())
				|| src.isIn(DamageTypeTags.IS_EXPLOSION);
	}

	/** Explosão de energia reaproveitável (Égide Dourada, ressurreição da Kitsune): fere e empurra quem estiver perto. */
	public static void burst(ServerPlayerEntity p, double radius, double push, float damage) {
		Vec3d c = p.getPos();
		for (LivingEntity o : p.getWorld().getEntitiesByClass(LivingEntity.class, new Box(c, c).expand(radius), x -> x != p && x.isAlive())) {
			if (damage > 0 && LegendKit.enemy(p, o)) {
				o.timeUntilRegen = 0;
				o.damage(p.getDamageSources().playerAttack(p), damage);
			}
			Vec3d d = LegendKit.flat(o.getPos().subtract(c));
			o.addVelocity(d.x * push, 0.5, d.z * push);
			o.velocityModified = true;
		}
		LegendKit.fx(p, "ward_melee", 0);
		LegendKit.snd(p, SoundEvents.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.6f);
		LegendKit.snd(p, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.0f);
	}

	/** Golpe físico: a aura explode em luz e arremessa o atacante e quem estiver por perto. */
	private void repel(ServerPlayerEntity p, LivingEntity attacker) {
		Vec3d c = p.getPos();
		for (LivingEntity o : p.getWorld().getEntitiesByClass(LivingEntity.class, new Box(c, c).expand(3.5), x -> x != p && x.isAlive())) {
			double k = o == attacker ? 1.0 : 0.55;
			Vec3d d = LegendKit.flat(o.getPos().subtract(c));
			o.addVelocity(d.x * push * k, 0.45 * k + 0.15, d.z * push * k);
			o.velocityModified = true;
		}
		LegendKit.fx(p, "ward_melee", 0);
		LegendKit.snd(p, SoundEvents.BLOCK_BEACON_DEACTIVATE, 1.0f, 1.6f);
		LegendKit.snd(p, SoundEvents.ITEM_SHIELD_BLOCK, 1.0f, 1.3f);
		LegendKit.snd(p, SoundEvents.BLOCK_AMETHYST_BLOCK_BREAK, 1.0f, 1.0f);
	}

	/** Projétil: some em luz e reaparece longe, sobre o chão ou no ar com queda lenta. */
	private void blink(ServerPlayerEntity p, DamageSource src) {
		if (src.getSource() instanceof ProjectileEntity proj) proj.discard();
		escape(p, min, max);
	}

	/** O salto/teleporte defensivo da Égide: reaparece longe, no chão ou no ar caindo devagar. Sem nenhum ataque. */
	public static void escape(ServerPlayerEntity p, double min, double max) {
		ServerWorld w = p.getServerWorld();
		Vec3d from = p.getPos();
		LegendKit.fxAt(w, "boom_gold", from.add(0, 1, 0), 1.6f, 0, 0);
		Vec3d to = null;
		boolean air = false;
		for (int i = 0; i < 12 && to == null; i++) {
			double a = w.random.nextDouble() * Math.PI * 2, d = min + w.random.nextDouble() * (max - min);
			int x = (int) Math.floor(from.x + Math.cos(a) * d), z = (int) Math.floor(from.z + Math.sin(a) * d);
			int top = w.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
			if (top <= from.y + 5 && top > w.getBottomY() && w.isSpaceEmpty(p, p.getBoundingBox().offset(x + 0.5 - p.getX(), top - p.getY(), z + 0.5 - p.getZ()))) {
				to = new Vec3d(x + 0.5, top, z + 0.5);
			}
		}
		if (to == null) {
			// sem chão seguro por perto: sobe no ar (nunca para dentro do terreno) e plana devagar
			for (int up = 4; up <= 10 && to == null; up++) {
				Vec3d c = from.add(0, up, 0);
				if (w.isSpaceEmpty(p, p.getBoundingBox().offset(0, up, 0)) && !w.getBlockState(BlockPos.ofFloored(c)).isOpaque()) to = c;
			}
			air = true;
		}
		if (to == null) return;
		p.requestTeleport(to.x, to.y, to.z);
		p.fallDistance = 0;
		LegendKit.fx(p, "ward_blink", 0);
		LegendKit.snd(p, SoundEvents.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.6f);
		LegendKit.snd(p, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.4f);
		if (air || to.y > from.y + 2) {
			// queda lenta e longa, feita por código (sem poção)
			LegendKit.run(p, "ward_float", 120, (pl, age) -> {
				if (pl.isOnGround() || pl.isTouchingWater()) return false;
				Vec3d v = pl.getVelocity();
				if (v.y < -0.08) {
					pl.setVelocity(v.x, -0.08, v.z);
					pl.velocityModified = true;
				}
				pl.fallDistance = 0;
				return true;
			});
		}
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		if (EntityFlags.has(p, FLAG)) return;
		long now = p.getServerWorld().getTime();
		long next = NEXT.computeIfAbsent(p.getUuid(), k -> now + 100);
		if (now >= next) {
			EntityFlags.set(p, FLAG, true);
			LegendKit.snd(p, SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, 0.6f, 1.6f);
		}
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		NEXT.remove(p.getUuid());
		EntityFlags.set(p, FLAG, false);
	}
}
