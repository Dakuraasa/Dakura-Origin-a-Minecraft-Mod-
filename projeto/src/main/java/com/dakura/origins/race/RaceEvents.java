package com.dakura.origins.race;

import com.dakura.origins.power.RacePower;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.CakeBlock;
import net.minecraft.block.CandleCakeBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * Liga os eventos do jogo aos ganchos de {@link RacePower}, para todas as raças.
 * Cada evento só olha os poderes da raça do jogador envolvido.
 */
public final class RaceEvents {
	private RaceEvents() {}

	public static void register() {
		// usar item / comer
		UseItemCallback.EVENT.register((player, world, hand) -> {
			ItemStack stack = player.getStackInHand(hand);
			Optional<Race> race = raceOf(player);
			if (race.isEmpty()) return TypedActionResult.pass(stack);
			boolean food = stack.get(DataComponentTypes.FOOD) != null;
			for (RacePower power : race.get().powers()) {
				Text t = power.useRejection(player, stack);
				if (t == null && food) t = power.consumeRejection(player, stack);
				if (t != null) {
					notify(player, world, t);
					return TypedActionResult.fail(stack);
				}
			}
			return TypedActionResult.pass(stack);
		});

		// bolo
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			BlockState state = world.getBlockState(hit.getBlockPos());
			if (!(state.getBlock() instanceof CakeBlock) && !(state.getBlock() instanceof CandleCakeBlock)) {
				return ActionResult.PASS;
			}
			if (player.isSneaking() && !player.getStackInHand(hand).isEmpty()) return ActionResult.PASS;
			Optional<Race> race = raceOf(player);
			if (race.isEmpty()) return ActionResult.PASS;
			ItemStack cake = new ItemStack(Items.CAKE);
			for (RacePower power : race.get().powers()) {
				Text t = power.consumeRejection(player, cake);
				if (t != null) {
					notify(player, world, t);
					return ActionResult.FAIL;
				}
			}
			return ActionResult.PASS;
		});

		// abates
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
			if (!(source.getAttacker() instanceof ServerPlayerEntity killer) || killer == entity) return;
			Optional<Race> race = RaceManager.getRace(killer);
			if (race.isEmpty()) return;
			for (RacePower power : race.get().powers()) power.onKill(killer, entity);
		});

		// imunidades
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayerEntity player)) return true;
			Optional<Race> race = RaceManager.getRace(player);
			if (race.isEmpty()) return true;
			for (RacePower power : race.get().powers()) {
				if (!power.allowDamage(player, source, amount)) return false;
			}
			return true;
		});
	}

	private static Optional<Race> raceOf(PlayerEntity player) {
		if (player.isCreative()) return Optional.empty();
		return RaceAccess.getRace(player);
	}

	private static void notify(PlayerEntity player, World world, Text message) {
		if (!world.isClient) player.sendMessage(message, true);
	}
}
