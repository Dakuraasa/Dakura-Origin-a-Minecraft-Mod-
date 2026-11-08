package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.types.PhotosynthesisPower;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.RaycastContext;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:solar_beam" — só de dia e sob o céu aberto: junta luz do sol (braços para o alto,
 * quase parada) e solta um raio que queima quem estiver na frente e quebra blocos não muito duros.
 * { "charge": 30, "fire": 24, "range": 24, "damage": 3, "damage_interval": 4, "break_interval": 3, "max_hardness": 3 }
 * Quebrar blocos respeita a regra mobGriefing e a proteção de spawn.
 */
public final class SolarBeamAbility implements Ability {
	public static final String CHANNEL = "solar";
	private static final Map<UUID, Session> SESSIONS = new HashMap<>();
	private static final net.minecraft.util.Identifier STANCE = com.dakura.origins.DakuraOrigins.id("solar_stance");

	private record Session(SolarBeamAbility a, ServerPlayerEntity p, int[] age) {}

	private final int charge, fire, damageInterval, breakInterval;
	private final double range;
	private final float damage, maxHardness;

	public SolarBeamAbility(JsonObject json) {
		this.charge = JsonParams.integer(json, "charge", 30);
		this.fire = JsonParams.integer(json, "fire", 24);
		this.range = JsonParams.num(json, "range", 24f);
		this.damage = JsonParams.num(json, "damage", 3f);
		this.damageInterval = Math.max(1, JsonParams.integer(json, "damage_interval", 4));
		this.breakInterval = Math.max(1, JsonParams.integer(json, "break_interval", 3));
		this.maxHardness = JsonParams.num(json, "max_hardness", 3f);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(SolarBeamAbility::tickAll);
	}

	@Override
	public boolean activate(ServerPlayerEntity player) {
		if (SESSIONS.containsKey(player.getUuid())) return false;
		if (!PhotosynthesisPower.inSun(player)) {
			player.sendMessage(Text.translatable("dakura_origins.message.solar_no_sun").formatted(Formatting.GOLD), true);
			return false;
		}
		SESSIONS.put(player.getUuid(), new Session(this, player, new int[1]));
		stance(player, true);
		ModNetworking.sendAround(new AnimPayload(player.getId(), CHANNEL, charge + fire), player);
		player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE,
				SoundCategory.PLAYERS, 0.8f, 1.4f);
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		Session s = SESSIONS.remove(player.getUuid());
		if (s != null) stop(s);
	}

	private static void stance(ServerPlayerEntity p, boolean on) {
		var inst = p.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		if (inst == null) return;
		inst.removeModifier(STANCE);
		if (on) inst.addTemporaryModifier(new EntityAttributeModifier(STANCE, -0.75, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
	}

	private static void stop(Session s) {
		stance(s.p, false);
		ModNetworking.sendAround(new AnimPayload(s.p.getId(), CHANNEL, 0), s.p);
		s.p.getWorld().playSound(null, s.p.getX(), s.p.getY(), s.p.getZ(), SoundEvents.BLOCK_BEACON_DEACTIVATE,
				SoundCategory.PLAYERS, 0.6f, 1.5f);
	}

	private static void tickAll(MinecraftServer server) {
		if (SESSIONS.isEmpty()) return;
		Iterator<Session> it = SESSIONS.values().iterator();
		while (it.hasNext()) {
			Session s = it.next();
			int age = ++s.age[0];
			if (s.p.isRemoved() || !s.p.isAlive() || age > s.a.charge + s.a.fire) {
				it.remove();
				stop(s);
				continue;
			}
			if (age == s.a.charge) {
				s.p.getWorld().playSound(null, s.p.getX(), s.p.getEyeY(), s.p.getZ(), SoundEvents.BLOCK_BEACON_POWER_SELECT,
						SoundCategory.PLAYERS, 1f, 1.6f);
			}
			if (age >= s.a.charge) s.a.beam(s, age - s.a.charge);
		}
	}

	private void beam(Session s, int t) {
		ServerPlayerEntity p = s.p;
		ServerWorld w = p.getServerWorld();
		Vec3d eye = p.getEyePos(), look = p.getRotationVec(1f), end = eye.add(look.multiply(range));
		BlockHitResult hit = w.raycast(new RaycastContext(eye, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
		if (hit.getType() == HitResult.Type.BLOCK) end = hit.getPos();
		if (t % damageInterval == 0) {
			Box box = new Box(eye, end).expand(1.0);
			for (Entity e : w.getOtherEntities(p, box)) {
				if (!(e instanceof LivingEntity le) || !le.isAlive()) continue;
				if (e.getBoundingBox().expand(0.4).raycast(eye, end).isEmpty()) continue;
				le.damage(p.getDamageSources().indirectMagic(p, p), damage);
				le.setOnFireFor(3f);
			}
		}
		if (t % breakInterval == 0 && hit.getType() == HitResult.Type.BLOCK
				&& w.getGameRules().getBoolean(GameRules.DO_MOB_GRIEFING)) {
			BlockPos bp = hit.getBlockPos();
			BlockState st = w.getBlockState(bp);
			float h = st.getHardness(w, bp);
			if (h >= 0 && h <= maxHardness && p.canModifyBlocks() && w.canPlayerModifyAt(p, bp)) {
				w.breakBlock(bp, true, p);
			}
		}
		if (t % 6 == 0) {
			w.playSound(null, end.x, end.y, end.z, SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.6f, 1.6f);
		}
	}
}
