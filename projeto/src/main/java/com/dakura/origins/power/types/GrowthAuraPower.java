package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.PlantBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

/**
 * "dakura_origins:growth_aura" — plantações (plantas fertilizáveis) por perto crescem sozinhas, como farinha de osso.
 * { "radius": 6, "interval": 10, "attempts": 8, "chance": 0.3 }  Grama e musgo ficam de fora.
 */
public final class GrowthAuraPower implements RacePower {
	private final int radius, interval, attempts;
	private final float chance;

	public GrowthAuraPower(JsonObject json) {
		this.radius = JsonParams.integer(json, "radius", 6);
		this.interval = Math.max(1, JsonParams.integer(json, "interval", 10));
		this.attempts = JsonParams.integer(json, "attempts", 8);
		this.chance = JsonParams.num(json, "chance", 0.3f);
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.age % interval != 0 || player.isSpectator()) return;
		ServerWorld w = player.getServerWorld();
		Random r = w.random;
		BlockPos c = player.getBlockPos();
		for (int i = 0; i < attempts; i++) {
			BlockPos p = c.add(r.nextInt(radius * 2 + 1) - radius, r.nextInt(5) - 2, r.nextInt(radius * 2 + 1) - radius);
			BlockState s = w.getBlockState(p);
			if (!(s.getBlock() instanceof PlantBlock) || !(s.getBlock() instanceof Fertilizable f)) continue;
			if (!f.isFertilizable(w, p, s) || r.nextFloat() > chance) continue;
			f.grow(w, r, p, s);
			w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 0.6, p.getZ() + 0.5, 3, 0.25, 0.2, 0.25, 0);
		}
	}
}
