package com.dakura.origins.power.types;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.power.ability.LegendKit;
import com.dakura.origins.race.RaceAccess;
import com.dakura.origins.sound.ModSounds;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.mob.ElderGuardianEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.mob.WardenEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * "dakura_origins:devour" — Devorar (Kitsune Corrompida, inspirado no Banquete do Cho'Gath). Agachada, golpear uma
 * criatura enfraquecida (vida ≤ max(6, 25%)) a devora na hora. A maturidade ganha depende da ameaça: animal quase nada,
 * monstro comum pouco, chefões muito. Intervalo de 5 min; cada jogador só rende maturidade uma vez para quem o devorou.
 * { "cooldown": 6000, "threshold": 0.25, "min_health": 6 }
 */
public final class DevourPower implements RacePower {
	private static final Map<UUID, Long> READY_AT = new HashMap<>();
	private static final Map<UUID, Set<UUID>> EATEN_PLAYERS = new HashMap<>();
	private final int cooldown;
	private final float threshold, minHealth;

	public DevourPower(JsonObject json) {
		cooldown = JsonParams.integer(json, "cooldown", 6000);
		threshold = JsonParams.num(json, "threshold", 0.25f);
		minHealth = JsonParams.num(json, "min_health", 6f);
	}

	private static DevourPower of(PlayerEntity p) {
		return RaceAccess.getRace(p).flatMap(r -> r.powers().stream().filter(x -> x instanceof DevourPower)
				.map(x -> (DevourPower) x).findFirst()).orElse(null);
	}

	/** Maturidade pela ameaça da criatura. Vaca ≈ 3, zumbi ≈ 30, jogador ≈ 60, Wither ≈ 3600. */
	static int worth(LivingEntity e) {
		float hp = e.getMaxHealth();
		if (e instanceof WitherEntity || e instanceof EnderDragonEntity || e instanceof WardenEntity || e instanceof ElderGuardianEntity)
			return Math.round(hp * 12);
		if (e instanceof PlayerEntity) return Math.round(hp * 3);
		if (e instanceof Monster) return Math.round(hp * 1.5f);
		return Math.max(1, Math.round(hp * 0.3f));
	}

	public static void bootstrap() {
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
			if (world.isClient || !(player instanceof ServerPlayerEntity sp) || !sp.isSneaking() || !(entity instanceof LivingEntity t)) return ActionResult.PASS;
			DevourPower d = of(sp);
			if (d == null || !t.isAlive() || !LegendKit.enemy(sp, t)) return ActionResult.PASS;
			long now = sp.getServerWorld().getTime();
			long ready = READY_AT.getOrDefault(sp.getUuid(), 0L);
			if (now < ready) {
				sp.sendMessage(Text.translatable("dakura_origins.message.devour_wait", (ready - now) / 20).formatted(Formatting.DARK_PURPLE), true);
				return ActionResult.PASS;
			}
			if (t.getHealth() > Math.max(d.minHealth, t.getMaxHealth() * d.threshold)) {
				sp.sendMessage(Text.translatable("dakura_origins.message.devour_strong").formatted(Formatting.GRAY), true);
				return ActionResult.PASS;
			}
			int gain = worth(t);
			boolean isPlayer = t instanceof PlayerEntity;
			if (isPlayer && !EATEN_PLAYERS.computeIfAbsent(sp.getUuid(), u -> new HashSet<>()).add(t.getUuid())) gain = 0;
			LegendKit.anim(sp, "fan", 20);
			LegendKit.fx(t, "devour_bite", 0);
			ModSounds.play(sp, ModSounds.DEVOUR, 1.2f, 0.9f);
			t.timeUntilRegen = 0;
			t.damage(sp.getDamageSources().playerAttack(sp), Math.max(t.getHealth() * 4f, 40f));
			if (t.isAlive()) return ActionResult.SUCCESS; // totem ou imunidade: não conta
			READY_AT.put(sp.getUuid(), now + d.cooldown);
			MaturityPower.grant(sp, gain);
			sp.heal(Math.min(6f, t.getMaxHealth() * 0.2f));
			// fogo roxo de absorção (só visual, curto)
			EntityFlags.set(sp, "purple_fire", true);
			LegendKit.fx(sp, "devour_flame", 40);
			LegendKit.run(sp, "devour_glow", 40, (pl, age) -> true, pl -> EntityFlags.set(pl, "purple_fire", false));
			sp.sendMessage(Text.translatable("dakura_origins.message.devour_gain", gain).formatted(Formatting.LIGHT_PURPLE), true);
			return ActionResult.SUCCESS;
		});
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		Long ready = READY_AT.get(p.getUuid());
		if (ready != null && p.getServerWorld().getTime() == ready) {
			p.sendMessage(Text.translatable("dakura_origins.message.devour_ready").formatted(Formatting.DARK_PURPLE), true);
			ModSounds.play(p, ModSounds.DEVOUR, 0.4f, 1.5f);
		}
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "devour_glow");
		EntityFlags.set(p, "purple_fire", false);
	}
}
