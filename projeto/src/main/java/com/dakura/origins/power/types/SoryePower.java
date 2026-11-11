package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.power.ability.LegendKit;
import com.dakura.origins.race.RaceAccess;
import com.dakura.origins.sound.ModSounds;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.mob.AbstractPiglinEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * "dakura_origins:sorye" — Sorye, a sorte da Lilac. Tudo usa as próprias tabelas de loot do jogo (nada de item
 * impossível para aquele baú ou criatura); a sorte só aumenta chances e quantidades dentro delas.
 * <ul>
 * <li>Baú com loot ainda não gerado: gera com a sorte dela e, com chance, rola a mesma tabela de novo nos espaços vazios.</li>
 * <li>Piglins mortos por ela: chance de rolar a tabela de drops deles mais uma vez.</li>
 * <li>Aldeões: desconto (VillagerEntityMixin, marcador "lucky_discount").</li>
 * <li>Rerrolagem: bater num baú de loot já aberto gira os dados e gera o conteúdo de novo (uma vez por baú, cooldown próprio).</li>
 * </ul>
 * { "chest_chance": 0.35, "piglin_chance": 0.4, "reroll_cooldown": 6000, "reroll_luck": 3 }
 */
public final class SoryePower implements RacePower {
	/** Tabela original de cada baú de loot visto (dimensão + posição). Some com o servidor reiniciando. */
	private static final Map<String, RegistryKey<LootTable>> ORIGIN = new HashMap<>();
	private static final Set<String> REROLLED = new HashSet<>();
	private static final Map<UUID, Long> READY_AT = new HashMap<>();
	private final float chestChance, piglinChance, rerollLuck;
	private final int rerollCooldown;

	public SoryePower(JsonObject json) {
		this.chestChance = JsonParams.num(json, "chest_chance", 0.35f);
		this.piglinChance = JsonParams.num(json, "piglin_chance", 0.4f);
		this.rerollCooldown = JsonParams.integer(json, "reroll_cooldown", 6000);
		this.rerollLuck = JsonParams.num(json, "reroll_luck", 3f);
	}

	private static SoryePower of(PlayerEntity p) {
		return RaceAccess.getRace(p).flatMap(r -> r.powers().stream().filter(x -> x instanceof SoryePower)
				.map(x -> (SoryePower) x).findFirst()).orElse(null);
	}

	private static String key(ServerWorld w, BlockPos pos) {
		return w.getRegistryKey().getValue() + "@" + pos.asLong();
	}

	private static LootContextParameterSet chestParams(ServerWorld w, BlockPos pos, ServerPlayerEntity p, float bonusLuck) {
		return new LootContextParameterSet.Builder(w).add(LootContextParameters.ORIGIN, Vec3d.ofCenter(pos))
				.add(LootContextParameters.THIS_ENTITY, p).luck(p.getLuck() + bonusLuck).build(LootContextTypes.CHEST);
	}

	public static void bootstrap() {
		UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
			if (!(world instanceof ServerWorld w) || !(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
			BlockPos pos = hit.getBlockPos();
			if (!(w.getBlockEntity(pos) instanceof LootableContainerBlockEntity lc) || lc.getLootTable() == null) return ActionResult.PASS;
			RegistryKey<LootTable> table = lc.getLootTable();
			ORIGIN.put(key(w, pos), table);
			SoryePower s = of(sp);
			if (s == null) return ActionResult.PASS;
			// gera agora (com a sorte dela) para poder somar a rolagem extra; o vanilla não gera de novo
			lc.generateLoot(sp);
			if (w.random.nextFloat() < s.chestChance) {
				w.getServer().getReloadableRegistries().getLootTable(table).supplyInventory(lc, chestParams(w, pos, sp, 1f), w.random.nextLong());
				LegendKit.fxAt(w, "lucky_chest", Vec3d.ofCenter(pos), 0, 0, 0);
				ModSounds.play(w, Vec3d.ofCenter(pos), ModSounds.DICE_ROLL, 0.5f, 1.4f);
			}
			return ActionResult.PASS;
		});

		AttackBlockCallback.EVENT.register((player, world, hand, pos, dir) -> {
			if (!(world instanceof ServerWorld w) || !(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
			SoryePower s = of(sp);
			BlockEntity be = w.getBlockEntity(pos);
			if (s == null || !(be instanceof LootableContainerBlockEntity lc)) return ActionResult.PASS;
			String k = key(w, pos);
			RegistryKey<LootTable> table = ORIGIN.get(k);
			if (table == null || lc.getLootTable() != null) return ActionResult.PASS;
			if (REROLLED.contains(k)) {
				sp.sendMessage(Text.translatable("dakura_origins.message.reroll_used").formatted(Formatting.GRAY), true);
				return ActionResult.SUCCESS;
			}
			long now = w.getTime();
			long ready = READY_AT.getOrDefault(sp.getUuid(), 0L);
			if (now < ready) {
				sp.sendMessage(Text.translatable("dakura_origins.message.reroll_wait", (ready - now) / 20).formatted(Formatting.GOLD), true);
				return ActionResult.SUCCESS;
			}
			if (LegendKit.running(sp, "chest_reroll")) return ActionResult.SUCCESS;
			REROLLED.add(k);
			READY_AT.put(sp.getUuid(), now + s.rerollCooldown);
			Vec3d c = Vec3d.ofCenter(pos);
			LegendKit.fx(sp, "dice_hud", 40);
			LegendKit.fxAt(w, "chest_reroll", c, 40, 0, 0);
			ModSounds.play(w, c, ModSounds.DICE_ROLL, 1.0f, 1.0f);
			LegendKit.run(sp, "chest_reroll", 40, (pl, age) -> {
				if (age == 20) ModSounds.play(w, c, ModSounds.DICE_ROLL, 0.9f, 1.15f);
				return true;
			}, pl -> {
				if (!(w.getBlockEntity(pos) instanceof LootableContainerBlockEntity box)) return;
				// conteúdo antigo some e a mesma tabela é sorteada de novo, com sorte extra
				box.clear();
				w.getServer().getReloadableRegistries().getLootTable(table).supplyInventory(box, chestParams(w, pos, pl, s.rerollLuck), w.random.nextLong());
				box.markDirty();
				LegendKit.fxAt(w, "lucky_chest", c, 1, 0, 0);
				ModSounds.play(w, c, ModSounds.JACKPOT, 0.9f, 1.2f);
			});
			return ActionResult.SUCCESS;
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
			if (!(victim instanceof AbstractPiglinEntity) || !(source.getAttacker() instanceof ServerPlayerEntity sp)) return;
			SoryePower s = of(sp);
			if (s == null || sp.getRandom().nextFloat() >= s.piglinChance) return;
			ServerWorld w = sp.getServerWorld();
			LootContextParameterSet params = new LootContextParameterSet.Builder(w).add(LootContextParameters.THIS_ENTITY, victim)
					.add(LootContextParameters.ORIGIN, victim.getPos()).add(LootContextParameters.DAMAGE_SOURCE, source)
					.addOptional(LootContextParameters.ATTACKING_ENTITY, sp).addOptional(LootContextParameters.DIRECT_ATTACKING_ENTITY, source.getSource())
					.add(LootContextParameters.LAST_DAMAGE_PLAYER, sp).luck(sp.getLuck()).build(LootContextTypes.ENTITY);
			w.getServer().getReloadableRegistries().getLootTable(victim.getLootTable()).generateLoot(params, victim::dropStack);
			LegendKit.fxAt(w, "lucky_chest", victim.getPos().add(0, 0.6, 0), 0, 0, 0);
		});
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		Long ready = READY_AT.get(p.getUuid());
		if (ready != null && p.getServerWorld().getTime() == ready) {
			p.sendMessage(Text.translatable("dakura_origins.message.reroll_ready").formatted(Formatting.GOLD), true);
			ModSounds.play(p, ModSounds.DICE_ROLL, 0.6f, 1.6f);
		}
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "chest_reroll");
	}
}
