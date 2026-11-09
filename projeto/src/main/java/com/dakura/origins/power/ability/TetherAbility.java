package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:tether" — Laço Áureo: um feixe de luz dourada prende o alvo na mira, preso dentro de um
 * campo de luz. Por alguns segundos o Alto Elfo o arrasta com o olhar; a rodinha do mouse aproxima ou afasta,
 * e ao fim o alvo é arremessado com o último movimento. { "range": 20, "duration": 60 }
 */
public final class TetherAbility implements Ability {
	private static final Map<UUID, Session> ACTIVE = new HashMap<>();
	private final double range;
	private final int duration;

	private static final class Session {
		final ServerPlayerEntity p;
		final LivingEntity t;
		double dist;
		int age;
		final int duration;
		Vec3d last = Vec3d.ZERO;

		Session(ServerPlayerEntity p, LivingEntity t, double dist, int duration) {
			this.p = p;
			this.t = t;
			this.dist = dist;
			this.duration = duration;
		}
	}

	public TetherAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 20f);
		this.duration = JsonParams.integer(json, "duration", 60);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(TetherAbility::tickAll);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		if (ACTIVE.containsKey(p.getUuid())) return false;
		LivingEntity t = TargetUtil.aimed(p, range, e -> !TargetUtil.boss(e));
		if (t == null) {
			p.sendMessage(Text.translatable("dakura_origins.message.no_target").formatted(Formatting.GRAY), true);
			return false;
		}
		double d = MathHelper.clamp(p.getEyePos().distanceTo(t.getPos().add(0, t.getHeight() / 2, 0)), 3, 12);
		ACTIVE.put(p.getUuid(), new Session(p, t, d, duration));
		Channeling.set(p, true);
		if (t instanceof MobEntity m) m.setTarget(null);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:tether", t.getId()), p, t);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "reach", duration), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.8f, 1.6f);
		p.getWorld().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 1.0f, 1.3f);
		return true;
	}

	@Override
	public void scroll(ServerPlayerEntity p, int delta) {
		Session s = ACTIVE.get(p.getUuid());
		if (s != null) s.dist = MathHelper.clamp(s.dist + Math.signum(delta) * 0.9, 2, 14);
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		Session s = ACTIVE.remove(p.getUuid());
		if (s != null) Channeling.set(p, false);
	}

	private static void tickAll(MinecraftServer server) {
		if (ACTIVE.isEmpty()) return;
		Iterator<Session> it = ACTIVE.values().iterator();
		while (it.hasNext()) {
			Session s = it.next();
			s.age++;
			boolean end = s.age >= s.duration || !s.p.isAlive() || s.p.isRemoved() || !s.t.isAlive() || s.t.isRemoved()
					|| s.t.getWorld() != s.p.getWorld();
			if (end) {
				it.remove();
				Channeling.set(s.p, false);
				ModNetworking.sendAround(new AnimPayload(s.p.getId(), "reach", 0), s.p);
				if (s.t.isAlive()) {
					s.t.setVelocity(s.last.multiply(1.7).add(0, 0.15, 0));
					s.t.velocityModified = true;
					s.p.getWorld().playSound(null, s.t.getX(), s.t.getY(), s.t.getZ(), SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
							SoundCategory.PLAYERS, 0.8f, 1.4f);
				}
				continue;
			}
			Vec3d hold = s.p.getEyePos().add(s.p.getRotationVec(1f).multiply(s.dist)).add(0, -s.t.getHeight() / 2, 0);
			Vec3d delta = hold.subtract(s.t.getPos());
			Vec3d vel = delta.multiply(0.45);
			if (vel.length() > 1.6) vel = vel.normalize().multiply(1.6);
			s.last = vel;
			s.t.setVelocity(vel);
			s.t.velocityModified = true;
			s.t.fallDistance = 0;
			if (s.age % 10 == 0) s.p.getWorld().playSound(null, s.t.getX(), s.t.getY(), s.t.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME,
					SoundCategory.PLAYERS, 0.4f, 1.8f);
		}
	}
}
