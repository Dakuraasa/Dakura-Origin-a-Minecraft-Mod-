package com.dakura.origins.power.ability;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.types.MaturityPower;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.AbstractFireBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:breath" — postura de ataque e uma rajada contínua de fogo.
 * <pre>
 * { "type": "dakura_origins:breath", "duration": 70, "windup": 8, "range": 9, "cone": 22,
 *   "damage": 2.0, "damage_interval": 5, "fire_seconds": 4,
 *   "break_interval": 4, "break_per_interval": 2, "slow": 0.7 }
 * </pre>
 * - Durante a rajada o dragão anda devagar (≈ agachado).
 * - Fere tudo no cone à frente (com linha de visão) e incendeia.
 * - maturity_damage: multiplicador do dano por estágio de maturidade (ex.: [1.0, 1.4, 1.8]).
 * - Queima blocos frágeis da tag "dakura_origins:breath_breakable" (madeira, terra, folhas...).
 * Os efeitos visuais são gerados no cliente a partir do AnimPayload "breath" (sem spam de rede).
 */
public final class BreathAbility implements Ability {
	public static final TagKey<Block> BREAKABLE = TagKey.of(RegistryKeys.BLOCK, DakuraOrigins.id("breath_breakable"));
	private static final Identifier SLOW_ID = DakuraOrigins.id("breath_stance");

	private final int duration, windup;
	private final double range;
	private final double coneCos;
	private final float damage;
	private final int damageInterval;
	private final int fireTicks;
	private final int breakInterval, breakPerInterval;
	private final double slow;
	private final float[] maturityDamage;

	public BreathAbility(JsonObject json) {
		this.duration = JsonParams.integer(json, "duration", 70);
		this.windup = JsonParams.integer(json, "windup", 8);
		this.range = JsonParams.num(json, "range", 9f);
		this.coneCos = Math.cos(Math.toRadians(JsonParams.num(json, "cone", 22f)));
		this.damage = JsonParams.num(json, "damage", 2f);
		this.damageInterval = Math.max(1, JsonParams.integer(json, "damage_interval", 5));
		this.fireTicks = Math.round(JsonParams.num(json, "fire_seconds", 4f) * 20);
		this.breakInterval = Math.max(1, JsonParams.integer(json, "break_interval", 4));
		this.breakPerInterval = JsonParams.integer(json, "break_per_interval", 2);
		this.slow = JsonParams.num(json, "slow", 0.7f);
		this.maturityDamage = JsonParams.floats(json, "maturity_damage");
	}

	private static final class Session {
		final BreathAbility config;
		final ServerPlayerEntity owner;
		int age;

		Session(BreathAbility config, ServerPlayerEntity owner) {
			this.config = config;
			this.owner = owner;
		}
	}

	private static final Map<UUID, Session> SESSIONS = new HashMap<>();

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(BreathAbility::tickAll);
	}

	public static boolean isBreathing(PlayerEntity player) {
		return SESSIONS.containsKey(player.getUuid());
	}

	@Override
	public boolean activate(ServerPlayerEntity player) {
		if (isBreathing(player) || BatFormState.isBat(player)) return false;
		SESSIONS.put(player.getUuid(), new Session(this, player));
		setStance(player, true);
		ModNetworking.sendAround(new AnimPayload(player.getId(), "breath", duration), player);
		player.getWorld().playSound(null, player.getX(), player.getEyeY(), player.getZ(),
				SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.9f, 1.2f);
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		Session s = SESSIONS.remove(player.getUuid());
		if (s != null) finish(s);
		else setStance(player, false);
	}

	// ================================================================== tick

	private static void tickAll(MinecraftServer server) {
		if (SESSIONS.isEmpty()) return;
		Iterator<Session> it = SESSIONS.values().iterator();
		while (it.hasNext()) {
			Session s = it.next();
			if (!s.config.step(s)) {
				it.remove();
				s.config.finish(s);
			}
		}
	}

	private boolean step(Session s) {
		ServerPlayerEntity owner = s.owner;
		if (owner.isRemoved() || !owner.isAlive()) return false;
		s.age++;
		if (s.age >= duration) return false;
		if (s.age < windup) return true;

		int t = s.age - windup;
		if (t % 7 == 0) {
			owner.getWorld().playSound(null, owner.getX(), owner.getEyeY(), owner.getZ(),
					SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.7f, 0.7f + owner.getRandom().nextFloat() * 0.2f);
		}
		Vec3d look = owner.getRotationVec(1f);
		Vec3d mouth = owner.getEyePos().add(look.multiply(0.35 * owner.getScale()));
		if (t % damageInterval == 0) burnCone(owner, mouth, look);
		if (t % breakInterval == 0 && breakPerInterval > 0) scorchBlocks(owner, mouth, look);
		return true;
	}

	private void burnCone(ServerPlayerEntity owner, Vec3d mouth, Vec3d look) {
		float hit = damage * MaturityPower.multiplier(owner, maturityDamage);
		List<Entity> list = owner.getWorld().getOtherEntities(owner, owner.getBoundingBox().expand(range + 1),
				e -> e instanceof LivingEntity && e.isAlive() && !e.isSpectator());
		for (Entity e : list) {
			LivingEntity target = (LivingEntity) e;
			Vec3d to = target.getBoundingBox().getCenter().subtract(mouth);
			double dist = to.length();
			if (dist > range + target.getWidth() * 0.5) continue;
			if (dist > 0.5 && to.multiply(1.0 / dist).dotProduct(look) < coneCos) continue;
			if (target instanceof PlayerEntity p && (p.isCreative() || !owner.shouldDamagePlayer(p))) continue;
			if (!owner.canSee(target)) continue;
			target.timeUntilRegen = 0;
			target.damage(owner.getDamageSources().playerAttack(owner), hit);
			target.setFireTicks(Math.max(target.getFireTicks(), fireTicks));
		}
	}

	/** Raios dentro do cone; o primeiro bloco frágil de cada raio vira cinza (e às vezes fogo). */
	private void scorchBlocks(ServerPlayerEntity owner, Vec3d mouth, Vec3d look) {
		if (!owner.canModifyBlocks() || !(owner.getWorld() instanceof ServerWorld world)) return;
		for (int ray = 0; ray < breakPerInterval; ray++) {
			Vec3d dir = ray == 0 ? look : jitter(look, owner);
			for (double d = 1.0; d <= range; d += 0.5) {
				BlockPos pos = BlockPos.ofFloored(mouth.add(dir.multiply(d)));
				BlockState state = world.getBlockState(pos);
				if (state.isAir()) continue;
				if (!state.getFluidState().isEmpty()) break;
				if (state.isIn(BREAKABLE) && world.canPlayerModifyAt(owner, pos)) {
					world.breakBlock(pos, false, owner);
					if (world.random.nextFloat() < 0.3f && AbstractFireBlock.canPlaceAt(world, pos, Direction.UP)) {
						world.setBlockState(pos, AbstractFireBlock.getState(world, pos));
					}
				}
				break;
			}
		}
	}

	private Vec3d jitter(Vec3d look, ServerPlayerEntity owner) {
		double spread = Math.sqrt(1 - coneCos * coneCos);
		Vec3d j = new Vec3d(owner.getRandom().nextGaussian(), owner.getRandom().nextGaussian(), owner.getRandom().nextGaussian())
				.multiply(spread * 0.5);
		return look.add(j).normalize();
	}

	private void finish(Session s) {
		setStance(s.owner, false);
		ModNetworking.sendAround(new AnimPayload(s.owner.getId(), "breath", 0), s.owner);
	}

	/** Postura de ataque: anda como se estivesse agachado. */
	private void setStance(ServerPlayerEntity player, boolean on) {
		EntityAttributeInstance speed = player.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
		if (speed == null) return;
		if (on && !speed.hasModifier(SLOW_ID)) {
			speed.addTemporaryModifier(new EntityAttributeModifier(SLOW_ID, -slow,
					EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		} else if (!on) {
			speed.removeModifier(SLOW_ID);
		}
	}
}
