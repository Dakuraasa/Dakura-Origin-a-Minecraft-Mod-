package com.dakura.origins.power.ability;

import com.dakura.origins.entity.ModEntities;
import com.dakura.origins.entity.UndeadMinionEntity;
import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.FxPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.types.MaturityPower;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;

/**
 * "dakura_origins:summon_undead" — ergue zumbis de cota de malha que atacam quem o dono atacar
 * (ou quem atacar o dono). O equipamento melhora com a maturidade (4 níveis).
 * { "count": 5, "duration": 600 }
 */
public final class SummonUndeadAbility implements Ability {
	private final int count, duration;

	public SummonUndeadAbility(JsonObject json) {
		this.count = JsonParams.integer(json, "count", 5);
		this.duration = JsonParams.integer(json, "duration", 600);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		ServerWorld w = p.getServerWorld();
		for (UndeadMinionEntity old : w.getEntitiesByClass(UndeadMinionEntity.class, p.getBoundingBox().expand(64),
				m -> m.ownerUuid().map(u -> u.equals(p.getUuid())).orElse(false))) old.vanish();
		int stage = MaturityPower.stage(p);
		for (int i = 0; i < count; i++) {
			double a = Math.toRadians(p.getYaw()) + Math.PI / 2 + (i - (count - 1) / 2.0) * 0.7;
			double r = 2.2;
			double x = p.getX() + Math.cos(a) * r, z = p.getZ() + Math.sin(a) * r;
			BlockPos bp = BlockPos.ofFloored(x, p.getY(), z);
			double y = p.getY();
			for (int dy = 2; dy >= -3; dy--) {
				BlockPos c = bp.up(dy);
				if (w.getBlockState(c.down()).isSolidBlock(w, c.down()) && w.isAir(c) && w.isAir(c.up())) {
					y = c.getY();
					break;
				}
			}
			UndeadMinionEntity m = ModEntities.UNDEAD_MINION.create(w);
			if (m == null) continue;
			m.refreshPositionAndAngles(x, y, z, p.getYaw(), 0);
			m.setup(p, stage, duration + i * 3);
			w.spawnEntity(m);
			ModNetworking.sendNear(w, m.getPos(), 64, new FxPayload("raise_dead", x, y, z, stage, 0, 0));
		}
		ModNetworking.sendAround(new AnimPayload(p.getId(), "cast", 14), p);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.PLAYERS, 0.8f, 0.6f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_ROOTED_DIRT_BREAK, SoundCategory.PLAYERS, 1.0f, 0.5f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_SOUL_SOIL_BREAK, SoundCategory.PLAYERS, 1.0f, 0.6f);
		return true;
	}
}
