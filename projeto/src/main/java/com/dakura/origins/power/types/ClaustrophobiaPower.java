package com.dakura.origins.power.types;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.World;

/**
 * "dakura_origins:claustrophobia" — precisa de espaço livre acima de si.
 * <pre>
 * { "type": "dakura_origins:claustrophobia", "clearance": 6, "slowness": 0, "exhaustion_per_second": 0.25 }
 * </pre>
 * Sem "clearance" blocos livres acima dos pés: lentidão, fome mais rápida e o corpo
 * treme (reaproveita o tremor vanilla de congelamento no render, sem o congelamento).
 * Checa 1x por segundo, ~30 blocos — custo desprezível.
 */
public final class ClaustrophobiaPower implements RacePower {
	public static final String FLAG = "cramped";
	private static final int INTERVAL = 20;

	private final int clearance;
	private final int slowness;
	private final float exhaustion;

	public ClaustrophobiaPower(JsonObject json) {
		this.clearance = Math.max(1, JsonParams.integer(json, "clearance", 6));
		this.slowness = JsonParams.integer(json, "slowness", 0);
		this.exhaustion = JsonParams.num(json, "exhaustion_per_second", 0.25f);
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.age % INTERVAL != 0) return;
		boolean cramped = !player.isSpectator() && !player.getAbilities().flying && isCramped(player);
		EntityFlags.set(player, FLAG, cramped);
		if (!cramped) return;
		player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, INTERVAL + 10, slowness, true, false, true));
		if (exhaustion > 0) player.addExhaustion(exhaustion);
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		EntityFlags.set(player, FLAG, false);
	}

	/** Algum bloco sólido nas colunas do corpo, dos pés até "clearance" blocos acima? */
	private boolean isCramped(ServerPlayerEntity player) {
		World world = player.getWorld();
		Box box = player.getBoundingBox();
		double inset = 0.05;
		double[][] columns = {
				{player.getX(), player.getZ()},
				{box.minX + inset, box.minZ + inset}, {box.maxX - inset, box.minZ + inset},
				{box.minX + inset, box.maxZ - inset}, {box.maxX - inset, box.maxZ - inset}};
		int baseY = (int) Math.floor(player.getY() + 0.5); // ignora laje/tapete sob os pés
		BlockPos.Mutable pos = new BlockPos.Mutable();
		for (double[] col : columns) {
			int x = (int) Math.floor(col[0]), z = (int) Math.floor(col[1]);
			for (int dy = 0; dy < clearance; dy++) {
				pos.set(x, baseY + dy, z);
				if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) return true;
			}
		}
		return false;
	}
}
