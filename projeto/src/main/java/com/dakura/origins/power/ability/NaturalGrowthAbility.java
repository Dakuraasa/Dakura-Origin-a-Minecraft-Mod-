package com.dakura.origins.power.ability;

import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.sound.ModSounds;
import com.google.gson.JsonObject;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.GrassBlock;
import net.minecraft.block.MossBlock;
import net.minecraft.block.NyliumBlock;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:natural_growth" — Florescer (Dríade): uma onda de energia natural num raio de 20 blocos faz crescer
 * na hora tudo o que pode crescer: mudas viram árvores (se houver espaço e solo), plantações amadurecem, plantas
 * crescem. Usa as próprias regras do jogo (o mesmo crescimento da farinha de osso, repetido até o fim), então nada
 * cresce onde não poderia. Grama, musgo e nylium ficam de fora (só espalhariam mato). Respeita proteção de área.
 * { "radius": 20, "height": 8, "max_steps": 12 }
 */
public final class NaturalGrowthAbility implements Ability {
	private final int radius, height, maxSteps;

	public NaturalGrowthAbility(JsonObject json) {
		radius = JsonParams.integer(json, "radius", 20);
		height = JsonParams.integer(json, "height", 8);
		maxSteps = JsonParams.integer(json, "max_steps", 12);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		ServerWorld w = p.getServerWorld();
		BlockPos c = p.getBlockPos();
		int grown = 0;
		BlockPos.Mutable m = new BlockPos.Mutable();
		for (int dx = -radius; dx <= radius; dx++)
			for (int dz = -radius; dz <= radius; dz++) {
				if (dx * dx + dz * dz > radius * radius) continue;
				for (int dy = -height; dy <= height; dy++) {
					m.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
					BlockState st = w.getBlockState(m);
					if (!(st.getBlock() instanceof Fertilizable f) || st.getBlock() instanceof GrassBlock
							|| st.getBlock() instanceof MossBlock || st.getBlock() instanceof NyliumBlock) continue;
					BlockPos pos = m.toImmutable();
					if (!p.canModifyAt(w, pos)) continue;
					boolean any = false;
					for (int k = 0; k < maxSteps; k++) {
						BlockState cur = w.getBlockState(pos);
						if (!(cur.getBlock() instanceof Fertilizable ff) || !ff.isFertilizable(w, pos, cur)) break;
						ff.grow(w, w.random, pos, cur);
						any = true;
					}
					if (any) {
						grown++;
						if (grown % 2 == 0) {
							w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0);
							w.spawnParticles(ParticleTypes.CHERRY_LEAVES, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 2, 0.5, 0.3, 0.5, 0);
						}
					}
				}
			}
		// onda de energia natural saindo da Dríade
		Vec3d o = p.getPos();
		for (int ring = 1; ring <= 4; ring++) {
			double r = radius * ring / 4.0;
			int n = 24 * ring;
			for (int i = 0; i < n; i++) {
				double a = i * Math.PI * 2 / n;
				w.spawnParticles(i % 3 == 0 ? ParticleTypes.SPORE_BLOSSOM_AIR : ParticleTypes.COMPOSTER, o.x + Math.cos(a) * r, o.y + 0.4,
						o.z + Math.sin(a) * r, 1, 0.1, 0.2, 0.1, 0);
			}
		}
		w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, o.x, o.y + 1, o.z, 60, 1.5, 1, 1.5, 0.1);
		w.spawnParticles(ParticleTypes.TOTEM_OF_UNDYING, o.x, o.y + 1, o.z, 40, 0.6, 0.8, 0.6, 0.4);
		LegendKit.fxAt(w, "bloom_wave", o, radius, 0, 0);
		LegendKit.anim(p, "cast", 14);
		ModSounds.play(p, ModSounds.FLOWER_BLOOM, 1.4f, 0.8f);
		return true;
	}
}
