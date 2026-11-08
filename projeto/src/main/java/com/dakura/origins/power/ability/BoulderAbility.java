package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.FxPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtFloat;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * "dakura_origins:boulder" — arranca um bloco grande de pedra do chão, ergue sobre a cabeça e arremessa:
 * explode no impacto (quebra blocos só com a regra mobGriefing ligada), com estilhaços menores em volta.
 * { "windup": 14, "speed": 1.25, "power": 2.2, "damage": 6, "size": 1.4 }
 */
public final class BoulderAbility implements Ability {
	private static final List<Rock> ROCKS = new ArrayList<>();
	private final int windup;
	private final double speed;
	private final float power, damage, size;

	private static final class Rock {
		final BoulderAbility ab;
		final ServerPlayerEntity owner;
		final DisplayEntity.BlockDisplayEntity display;
		final BlockState state;
		Vec3d pos, vel = Vec3d.ZERO;
		final Vec3d start;
		int age, after = -1;
		Vec3d hitAt;

		Rock(BoulderAbility ab, ServerPlayerEntity owner, DisplayEntity.BlockDisplayEntity d, BlockState s, Vec3d start) {
			this.ab = ab;
			this.owner = owner;
			this.display = d;
			this.state = s;
			this.start = start;
			this.pos = start;
		}
	}

	public BoulderAbility(JsonObject json) {
		this.windup = JsonParams.integer(json, "windup", 14);
		this.speed = JsonParams.num(json, "speed", 1.25f);
		this.power = JsonParams.num(json, "power", 2.2f);
		this.damage = JsonParams.num(json, "damage", 6f);
		this.size = JsonParams.num(json, "size", 1.4f);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(BoulderAbility::tickAll);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		ServerWorld w = p.getServerWorld();
		BlockPos below = BlockPos.ofFloored(p.getX(), p.getY() - 0.2, p.getZ());
		BlockState ground = w.getBlockState(below);
		if (!p.isOnGround() || ground.isAir()) {
			p.sendMessage(Text.translatable("dakura_origins.message.need_ground").formatted(Formatting.GRAY), true);
			return false;
		}
		BlockState rock = ground.isIn(BlockTags.BASE_STONE_OVERWORLD) || ground.isIn(BlockTags.BASE_STONE_NETHER)
				|| ground.isOf(Blocks.COBBLESTONE) || ground.isOf(Blocks.MOSSY_COBBLESTONE) ? ground : Blocks.COBBLESTONE.getDefaultState();
		Vec3d look = p.getRotationVec(1f);
		Vec3d flat = new Vec3d(look.x, 0, look.z);
		flat = flat.lengthSquared() < 1e-4 ? Vec3d.ZERO : flat.normalize();
		Vec3d start = p.getPos().add(flat.multiply(1.3)).add(0, -0.6, 0);

		DisplayEntity.BlockDisplayEntity d = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, w);
		NbtCompound n = new NbtCompound();
		n.put("block_state", NbtHelper.fromBlockState(rock));
		NbtCompound tr = new NbtCompound();
		tr.put("left_rotation", floats(0, 0, 0, 1));
		tr.put("right_rotation", floats(0, 0, 0, 1));
		tr.put("translation", floats(-size / 2, -size / 2, -size / 2));
		tr.put("scale", floats(size, size, size));
		n.put("transformation", tr);
		n.putInt("teleport_duration", 2);
		d.readNbt(n);
		d.refreshPositionAndAngles(start.x, start.y, start.z, p.getYaw(), 0);
		w.spawnEntity(d);
		ROCKS.add(new Rock(this, p, d, rock, start));

		w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, rock), start.x, start.y + 0.6, start.z, 24, 0.5, 0.2, 0.5, 0.15);
		w.playSound(null, start.x, start.y, start.z, SoundEvents.BLOCK_STONE_BREAK, SoundCategory.PLAYERS, 1.0f, 0.55f);
		w.playSound(null, start.x, start.y, start.z, SoundEvents.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.PLAYERS, 0.8f, 0.6f);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "lift", windup + 8), p);
		return true;
	}

	private static NbtList floats(float... v) {
		NbtList l = new NbtList();
		for (float f : v) l.add(NbtFloat.of(f));
		return l;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		ROCKS.removeIf(r -> {
			if (r.owner == p && r.after < 0) {
				r.display.discard();
				return true;
			}
			return false;
		});
	}

	private static void tickAll(MinecraftServer server) {
		if (ROCKS.isEmpty()) return;
		Iterator<Rock> it = ROCKS.iterator();
		List<Rock> done = new ArrayList<>();
		while (it.hasNext()) {
			Rock r = it.next();
			if (r.after >= 0) {
				if (++r.after > 8) it.remove();
				else if (r.after == 3 || r.after == 7) r.ab.shard(r);
				continue;
			}
			if (r.display.isRemoved() || r.owner.isRemoved()) {
				r.display.discard();
				it.remove();
				continue;
			}
			r.age++;
			ServerWorld w = (ServerWorld) r.display.getWorld();
			if (r.age <= r.ab.windup) {
				if (!r.owner.isAlive()) {
					r.display.discard();
					it.remove();
					continue;
				}
				float t = r.age / (float) r.ab.windup;
				float s = t * t * (3 - 2 * t);
				Vec3d top = r.owner.getPos().add(0, r.owner.getHeight() + 0.5, 0);
				r.pos = r.start.lerp(top, s);
				r.display.refreshPositionAndAngles(r.pos.x, r.pos.y, r.pos.z, r.owner.getYaw(), (float) Math.sin(r.age * 0.4) * 6);
				if (r.age == r.ab.windup) {
					Vec3d look = r.owner.getRotationVec(1f);
					r.vel = look.multiply(r.ab.speed).add(0, 0.18, 0);
					w.playSound(null, r.pos.x, r.pos.y, r.pos.z, SoundEvents.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.PLAYERS, 1.0f, 0.6f);
					w.playSound(null, r.pos.x, r.pos.y, r.pos.z, SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.7f, 0.5f);
				}
				continue;
			}
			Vec3d next = r.pos.add(r.vel);
			var hit = w.raycast(new RaycastContext(r.pos, next, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, r.display));
			Vec3d at = null;
			if (hit.getType() != HitResult.Type.MISS) at = hit.getPos();
			if (at == null) {
				Box box = new Box(next, next).expand(r.ab.size * 0.6);
				for (Entity e : w.getOtherEntities(r.display, box, x -> x instanceof LivingEntity l && l.isAlive() && x != r.owner && !x.isSpectator())) {
					at = next;
					break;
				}
			}
			if (at == null && r.age > r.ab.windup + 80) at = next;
			if (at != null) {
				r.hitAt = at;
				r.ab.impact(r);
				r.after = 0;
				continue;
			}
			r.pos = next;
			r.vel = r.vel.multiply(0.99).add(0, -0.045, 0);
			r.display.refreshPositionAndAngles(next.x, next.y, next.z, r.display.getYaw() + 13f, r.display.getPitch() + 17f);
			if (r.age % 2 == 0) w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, r.state), next.x, next.y, next.z, 2, 0.3, 0.3, 0.3, 0.02);
		}
	}

	private void impact(Rock r) {
		ServerWorld w = (ServerWorld) r.display.getWorld();
		Vec3d at = r.hitAt;
		r.display.discard();
		for (Entity e : w.getOtherEntities(null, new Box(at, at).expand(1.8), x -> x instanceof LivingEntity l && l.isAlive() && x != r.owner)) {
			e.damage(r.owner.getDamageSources().playerAttack(r.owner), damage);
		}
		w.createExplosion(r.owner, at.x, at.y, at.z, power, World.ExplosionSourceType.MOB);
		w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, r.state), at.x, at.y + 0.3, at.z, 50, 0.8, 0.5, 0.8, 0.25);
		ModNetworking.sendNear(w, at, 64, new FxPayload("boulder", at.x, at.y, at.z, 0, 0, 0));
		w.playSound(null, at.x, at.y, at.z, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 0.35f, 0.5f);
	}

	private void shard(Rock r) {
		ServerWorld w = (ServerWorld) r.display.getWorld();
		Vec3d at = r.hitAt.add((w.random.nextDouble() - 0.5) * 3, 0.2, (w.random.nextDouble() - 0.5) * 3);
		w.createExplosion(r.owner, at.x, at.y, at.z, power * 0.4f, World.ExplosionSourceType.NONE);
		w.spawnParticles(new BlockStateParticleEffect(ParticleTypes.BLOCK, r.state), at.x, at.y, at.z, 14, 0.4, 0.3, 0.4, 0.2);
	}
}
