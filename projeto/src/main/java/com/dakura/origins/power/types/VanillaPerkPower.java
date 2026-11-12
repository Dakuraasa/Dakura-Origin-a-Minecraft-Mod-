package com.dakura.origins.power.types;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CocoaBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.StemBlock;
import net.minecraft.block.SweetBerryBushBlock;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.input.SingleStackRecipeInput;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.stat.Stats;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:vanilla_perk" — Vantagens Vanilla (mexem nas mecânicas normais do Minecraft, só para a própria jogadora).
 * { "mode": "fortune" | "autosmelt" | "brew_double" | "crop_growth" | "no_trample" | "needs_roof" | "butcher" | "scare_animals" }
 */
public final class VanillaPerkPower implements RacePower {
	private static final Map<UUID, Vec3d> STILL = new HashMap<>();
	/** Último jogador a mexer em cada suporte de poções (dimensão@posição). */
	private static final Map<String, UUID> BREWER = new HashMap<>();
	private final String mode;

	public VanillaPerkPower(JsonObject json) {
		this.mode = JsonParams.str(json, "mode", "");
	}

	public static boolean has(PlayerEntity p, String mode) {
		if (p == null || p.getWorld().isClient) return false;
		return RaceAccess.getRace(p).map(r -> r.powers().stream().anyMatch(x -> x instanceof VanillaPerkPower v && v.mode.equals(mode))).orElse(false);
	}

	private static boolean isOre(BlockState s) {
		String path = Registries.BLOCK.getId(s.getBlock()).getPath();
		return path.endsWith("_ore") || path.equals("ancient_debris");
	}

	private static RegistryEntry<Enchantment> ench(ServerWorld w, RegistryKey<Enchantment> key) {
		return w.getRegistryManager().get(RegistryKeys.ENCHANTMENT).getEntry(key).orElse(null);
	}

	private static String key(net.minecraft.world.World w, BlockPos pos) {
		return w.getRegistryKey().getValue() + "@" + pos.asLong();
	}

	/** O suporte foi abastecido por uma Dama Cogumelo que ainda está por perto? */
	public static boolean brewedByAlchemist(net.minecraft.world.World w, BlockPos pos) {
		UUID id = BREWER.get(key(w, pos));
		if (id == null || w.getServer() == null) return false;
		ServerPlayerEntity p = w.getServer().getPlayerManager().getPlayer(id);
		return p != null && p.getWorld() == w && p.squaredDistanceTo(Vec3d.ofCenter(pos)) < 24 * 24 && has(p, "brew_double");
	}

	public static void bootstrap() {
		// quem abriu o suporte por último é quem "preparou" as poções
		net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!world.isClient && world.getBlockState(hit.getBlockPos()).getBlock() instanceof net.minecraft.block.BrewingStandBlock)
				BREWER.put(key(world, hit.getBlockPos()), player.getUuid());
			return ActionResult.PASS;
		});

		// Orc: animais abatidos por ele rendem a carne de novo
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
			if (!(victim instanceof net.minecraft.entity.passive.AnimalEntity) || !(source.getAttacker() instanceof ServerPlayerEntity sp) || !has(sp, "butcher")) return;
			ServerWorld w = sp.getServerWorld();
			var params = new net.minecraft.loot.context.LootContextParameterSet.Builder(w)
					.add(net.minecraft.loot.context.LootContextParameters.THIS_ENTITY, victim)
					.add(net.minecraft.loot.context.LootContextParameters.ORIGIN, victim.getPos())
					.add(net.minecraft.loot.context.LootContextParameters.DAMAGE_SOURCE, source)
					.addOptional(net.minecraft.loot.context.LootContextParameters.ATTACKING_ENTITY, sp)
					.add(net.minecraft.loot.context.LootContextParameters.LAST_DAMAGE_PLAYER, sp)
					.build(net.minecraft.loot.context.LootContextTypes.ENTITY);
			w.getServer().getReloadableRegistries().getLootTable(victim.getLootTable()).generateLoot(params, victim::dropStack);
		});

		// Elfo da Lua: o Warden não o fere
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) ->
				!(victim instanceof PlayerEntity p && source.getAttacker() instanceof net.minecraft.entity.mob.WardenEntity && MarkerPower.has(p, "warden_friend")));

		// Fortuna natural (Lilac) e minério já fundido (Fênix): só nos blocos que ela mesma quebra
		PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, be) -> {
			if (!(world instanceof ServerWorld sw) || !(player instanceof ServerPlayerEntity sp) || sp.isCreative() || !isOre(state)) return true;
			boolean fortune = has(sp, "fortune"), smelt = has(sp, "autosmelt");
			if (!fortune && !smelt) return true;
			ItemStack tool = sp.getMainHandStack();
			RegistryEntry<Enchantment> silk = ench(sw, Enchantments.SILK_TOUCH), fort = ench(sw, Enchantments.FORTUNE);
			if (silk != null && EnchantmentHelper.getLevel(silk, tool) > 0) return true;
			if (!sp.canHarvest(state)) return true;
			ItemStack used = tool;
			if (fortune && fort != null && EnchantmentHelper.getLevel(fort, tool) < 2) {
				used = tool.copy();
				used.addEnchantment(fort, 2);
			}
			List<ItemStack> drops = new ArrayList<>(Block.getDroppedStacks(state, sw, pos, be, sp, used));
			if (smelt) {
				for (int i = 0; i < drops.size(); i++) {
					ItemStack d = drops.get(i);
					var r = sw.getRecipeManager().getFirstMatch(RecipeType.SMELTING, new SingleStackRecipeInput(d), sw);
					if (r.isPresent()) {
						ItemStack out = r.get().value().getResult(sw.getRegistryManager()).copy();
						out.setCount(out.getCount() * d.getCount());
						drops.set(i, out);
					}
				}
				sw.spawnParticles(ParticleTypes.FLAME, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, 0.3, 0.3, 0.3, 0.02);
			}
			ItemStack before = tool.copy();
			sw.breakBlock(pos, false, sp);
			for (ItemStack d : drops) Block.dropStack(sw, pos, d);
			state.onStacksDropped(sw, pos, before, true);
			tool.postMine(sw, state, pos, sp);
			sp.incrementStat(Stats.MINED.getOrCreateStat(state.getBlock()));
			sp.addExhaustion(0.005f);
			return false;
		});

		// Dragões e Fênix: só dormem com teto a até ~6 blocos
		EntitySleepEvents.ALLOW_SLEEPING.register((player, pos) -> {
			if (!(player instanceof ServerPlayerEntity sp) || !has(sp, "needs_roof")) return null;
			for (int dy = 1; dy <= 6; dy++) {
				BlockPos up = pos.up(dy);
				if (!sp.getWorld().getBlockState(up).getCollisionShape(sp.getWorld(), up).isEmpty()) return null;
			}
			sp.sendMessage(Text.translatable("dakura_origins.message.needs_roof").formatted(Formatting.GOLD), true);
			return PlayerEntity.SleepFailureReason.OTHER_PROBLEM;
		});
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		switch (mode) {
			case "crop_growth" -> cropGrowth(p);
			case "scare_animals" -> scareAnimals(p);
			default -> {
			}
		}
	}

	/** Dríade parada perto das plantações: elas recebem crescimento extra (respeita luz, solo e água do próprio bloco). */
	private void cropGrowth(ServerPlayerEntity p) {
		if (p.age % 20 != 0) return;
		Vec3d last = STILL.put(p.getUuid(), p.getPos());
		if (last == null || last.squaredDistanceTo(p.getPos()) > 0.01) return;
		ServerWorld w = p.getServerWorld();
		BlockPos c = p.getBlockPos();
		for (int i = 0; i < 24; i++) {
			BlockPos b = c.add(w.random.nextInt(11) - 5, w.random.nextInt(5) - 2, w.random.nextInt(11) - 5);
			BlockState s = w.getBlockState(b);
			Block blk = s.getBlock();
			if (blk instanceof CropBlock || blk instanceof StemBlock || blk instanceof CocoaBlock || blk instanceof SweetBerryBushBlock) {
				s.randomTick(w, b, w.random);
				if (w.random.nextInt(3) == 0)
					w.spawnParticles(ParticleTypes.HAPPY_VILLAGER, b.getX() + 0.5, b.getY() + 0.6, b.getZ() + 0.5, 2, 0.25, 0.2, 0.25, 0);
			}
		}
	}

	/** Elfo Noturno: assassino conhecido, os animais fogem dele. */
	private void scareAnimals(ServerPlayerEntity p) {
		if (p.age % 10 != 0 || p.isSpectator()) return;
		for (net.minecraft.entity.passive.AnimalEntity a : p.getServerWorld().getEntitiesByClass(net.minecraft.entity.passive.AnimalEntity.class,
				p.getBoundingBox().expand(9), x -> !(x instanceof net.minecraft.entity.passive.TameableEntity t && t.isTamed()) && !x.hasPassengers())) {
			Vec3d away = a.getPos().subtract(p.getPos()).multiply(1, 0, 1);
			if (away.lengthSquared() < 1e-3) away = new Vec3d(1, 0, 0);
			Vec3d to = a.getPos().add(away.normalize().multiply(8));
			a.getNavigation().startMovingTo(to.x, to.y, to.z, 1.6);
		}
	}

	@Override
	public void onApply(ServerPlayerEntity p) {
		if (mode.equals("needs_roof")) EntityFlags.set(p, "needs_roof", true);
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		STILL.remove(p.getUuid());
		if (mode.equals("needs_roof")) EntityFlags.set(p, "needs_roof", false);
	}
}
