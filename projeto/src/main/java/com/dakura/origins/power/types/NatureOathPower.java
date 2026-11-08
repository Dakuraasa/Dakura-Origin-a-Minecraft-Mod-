package com.dakura.origins.power.types;

import com.dakura.origins.power.RacePower;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.FlowerBlock;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * "dakura_origins:nature_oath" — Dríade: quebrar um tronco de árvore mata na hora (traição),
 * e ao morrer ela espalha flores variadas pelo chão em volta.
 * { "type": "dakura_origins:nature_oath" }
 */
public final class NatureOathPower implements RacePower {
	public NatureOathPower(JsonObject json) {}

	private static boolean has(ServerPlayerEntity p) {
		return SpectreSupport.has(p, NatureOathPower.class);
	}

	public static void bootstrap() {
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
			if (!(player instanceof ServerPlayerEntity sp) || sp.isCreative() || !state.isIn(BlockTags.LOGS) || !has(sp)) return;
			sp.sendMessage(Text.translatable("dakura_origins.message.dryad_betrayal").formatted(Formatting.DARK_GREEN), false);
			world.playSound(null, pos, SoundEvents.BLOCK_AZALEA_LEAVES_BREAK, SoundCategory.PLAYERS, 1f, 0.5f);
			sp.damage(sp.getDamageSources().magic(), Float.MAX_VALUE);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (entity instanceof ServerPlayerEntity sp && has(sp)) scatterFlowers(sp.getServerWorld(), sp.getBlockPos());
		});
	}

	private static void scatterFlowers(ServerWorld w, BlockPos center) {
		List<Block> flowers = new ArrayList<>();
		for (Block b : Registries.BLOCK) if (b instanceof FlowerBlock && b != Blocks.WITHER_ROSE) flowers.add(b);
		if (flowers.isEmpty()) return;
		for (int i = 0; i < 28; i++) {
			BlockPos p = center.add(w.random.nextInt(9) - 4, w.random.nextInt(5) - 2, w.random.nextInt(9) - 4);
			BlockState f = flowers.get(w.random.nextInt(flowers.size())).getDefaultState();
			if (w.isAir(p) && f.canPlaceAt(w, p)) w.setBlockState(p, f);
		}
		w.playSound(null, center, SoundEvents.BLOCK_FLOWERING_AZALEA_PLACE, SoundCategory.PLAYERS, 1f, 0.8f);
	}
}
