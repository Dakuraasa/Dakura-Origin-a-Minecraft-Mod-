package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

/**
 * "dakura_origins:greed" — sorte de garimpeira: minérios têm chance de render o dobro e dar mais experiência;
 * monstros derrotados às vezes deixam pepitas de ouro (Toque de Midas). { "ore_chance": 0.25, "midas_chance": 0.3 }
 */
public final class GreedPower implements RacePower {
	private static final TagKey<Block> ORES = TagKey.of(RegistryKeys.BLOCK, Identifier.of("c", "ores"));
	private final float oreChance, midasChance;

	public GreedPower(JsonObject json) {
		this.oreChance = JsonParams.num(json, "ore_chance", 0.25f);
		this.midasChance = JsonParams.num(json, "midas_chance", 0.3f);
	}

	private static GreedPower of(PlayerEntity p) {
		return RaceAccess.getRace(p).flatMap(r -> r.powers().stream().filter(x -> x instanceof GreedPower)
				.map(x -> (GreedPower) x).findFirst()).orElse(null);
	}

	public static void bootstrap() {
		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, be) -> {
			if (!(world instanceof ServerWorld w) || !(player instanceof ServerPlayerEntity sp) || !state.isIn(ORES)) return;
			GreedPower g = of(sp);
			if (g == null || w.random.nextFloat() >= g.oreChance) return;
			for (ItemStack drop : Block.getDroppedStacks(state, w, pos, be, sp, sp.getMainHandStack())) {
				Block.dropStack(w, pos, drop);
			}
			sp.addExperience(2 + w.random.nextInt(4));
			w.spawnParticles(ParticleTypes.WAX_ON, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.35, 0.35, 0.35, 0.1);
			w.playSound(null, pos, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.6f, 1.6f);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
			if (!(victim instanceof Monster) || !(source.getAttacker() instanceof ServerPlayerEntity sp)) return;
			GreedPower g = of(sp);
			if (g == null || sp.getRandom().nextFloat() >= g.midasChance) return;
			ServerWorld w = sp.getServerWorld();
			w.spawnEntity(new ItemEntity(w, victim.getX(), victim.getBodyY(0.5), victim.getZ(), new ItemStack(Items.GOLD_NUGGET, 1 + w.random.nextInt(3))));
			w.spawnParticles(ParticleTypes.WAX_ON, victim.getX(), victim.getBodyY(0.6), victim.getZ(), 10, 0.3, 0.4, 0.3, 0.1);
		});
	}
}
