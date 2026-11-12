package com.dakura.origins.race;

import com.dakura.origins.network.CooldownPayload;
import com.dakura.origins.network.OpenSelectionPayload;
import com.dakura.origins.network.SelectionResultPayload;
import com.dakura.origins.power.RacePower;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Regras de "quem é qual raça" (lado do servidor): aplicar/remover poderes,
 * repassar ticks e disparar habilidades com tempo de recarga.
 */
public final class RaceManager {
	private static final Map<UUID, long[]> COOLDOWNS = new HashMap<>();
	/** Jogadores com "/do no cooldown" ligado. */
	public static final java.util.Set<UUID> NO_COOLDOWN = java.util.concurrent.ConcurrentHashMap.newKeySet();
	private static long serverTicks = 0;

	private RaceManager() {}

	// ------------------------------------------------------------ consulta

	public static Optional<Race> getRace(ServerPlayerEntity player) {
		return RaceAccess.getRace(player);
	}

	public static boolean hasRace(ServerPlayerEntity player) {
		return getRace(player).isPresent();
	}

	// ------------------------------------------------------------ eventos

	public static void onJoin(ServerPlayerEntity player) {
		Optional<Race> race = getRace(player);
		sendCooldown(player, CooldownPayload.reset());
		if (race.isPresent()) {
			applyPowers(player, race.get());
			if (ServerPlayNetworking.canSend(player, SelectionResultPayload.ID)) {
				ServerPlayNetworking.send(player, new SelectionResultPayload(true, race.get().id()));
			}
		} else {
			RaceAccess.IN_NETHER.remove(player.getUuid());
		if (player.getAttached(RaceAttachments.RACE) != null) player.removeAttached(RaceAttachments.RACE);
			requestSelection(player, "");
		}
	}

	public static void onLeave(ServerPlayerEntity player) {
		getRace(player).ifPresent(r -> removeAbilities(player, r));
		COOLDOWNS.remove(player.getUuid());
	}

	public static void onRespawn(ServerPlayerEntity player) {
		getRace(player).ifPresent(r -> removeAbilities(player, r));
		// a transformação acaba ao morrer: reaplica a raça verdadeira
		getRace(player).ifPresent(r -> applyPowers(player, r));
	}

	public static void tick(MinecraftServer server) {
		serverTicks++;
		for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			if (!player.isAlive()) continue;
			netherCheck(player);
			Optional<Race> race = getRace(player);
			if (race.isEmpty()) continue;
			for (RacePower power : race.get().powers()) power.tick(player);
			if (serverTicks % 40 == 0 && NO_COOLDOWN.contains(player.getUuid())) resetPassives(player, race.get());
		}
	}

	/** Dríade, Morto-Vivo e Espectro: no Nether perdem vantagens e habilidades; tudo volta ao sair (maturidade intacta). */
	private static void netherCheck(ServerPlayerEntity player) {
		if (RaceAccess.BORROWED.containsKey(player.getUuid())) return;
		String id = player.getAttached(RaceAttachments.RACE);
		Optional<Race> weak = RaceRegistry.netherForm(id);
		boolean should = weak.isPresent() && player.getWorld().getRegistryKey() == net.minecraft.world.World.NETHER;
		boolean now = RaceAccess.IN_NETHER.contains(player.getUuid());
		if (should == now) return;
		Optional<Race> full = RaceRegistry.get(id);
		if (should) {
			full.ifPresent(r -> {
				removeAbilities(player, r);
				for (RacePower power : r.powers()) if (!weak.get().powers().contains(power)) power.onRemove(player);
			});
			RaceAccess.IN_NETHER.add(player.getUuid());
			player.sendMessage(Text.translatable("dakura_origins.message.nether_weak").formatted(Formatting.DARK_RED), true);
		} else {
			RaceAccess.IN_NETHER.remove(player.getUuid());
			Optional<Race> w = RaceRegistry.netherForm(id);
			full.ifPresent(r -> {
				for (RacePower power : r.powers()) if (w.isEmpty() || !w.get().powers().contains(power)) power.onApply(player);
			});
			player.sendMessage(Text.translatable("dakura_origins.message.nether_restored").formatted(Formatting.GREEN), true);
		}
	}

	// ------------------------------------------------------------ escolha

	public static void requestSelection(ServerPlayerEntity player, String preselect) {
		if (ServerPlayNetworking.canSend(player, OpenSelectionPayload.ID)) {
			ServerPlayNetworking.send(player, new OpenSelectionPayload(preselect));
		} else {
			// o cliente sem o mod não tem as traduções: o texto reserva aparece em português
			player.sendMessage(Text.translatableWithFallback("dakura_origins.message.install_mod",
					"[Datura Origins] Instale o mod para escolher sua raça.").formatted(Formatting.RED), false);
		}
	}

	public static boolean choose(ServerPlayerEntity player, String raceId) {
		if (hasRace(player)) return false;
		Optional<Race> race = RaceRegistry.get(raceId);
		if (race.isEmpty()) return false;
		setRace(player, race.get());
		return true;
	}

	public static void setRace(ServerPlayerEntity player, Race race) {
		clearRace(player);
		player.setAttached(RaceAttachments.RACE, race.id());
		applyPowers(player, race);
		if (ServerPlayNetworking.canSend(player, SelectionResultPayload.ID)) {
			ServerPlayNetworking.send(player, new SelectionResultPayload(true, race.id()));
		}
		player.sendMessage(Text.translatable("dakura_origins.message.awakened",
				colored(race.nameText(), race.palette().ink()[5])).formatted(Formatting.GRAY), false);
	}

	public static void clearRace(ServerPlayerEntity player) {
		com.dakura.origins.power.ability.MimicAbility.revert(player);
		getRace(player).ifPresent(r -> {
			removeAbilities(player, r);
			for (RacePower power : r.powers()) power.onRemove(player);
		});
		RaceAccess.IN_NETHER.remove(player.getUuid());
		if (player.getAttached(RaceAttachments.RACE) != null) player.removeAttached(RaceAttachments.RACE);
		COOLDOWNS.remove(player.getUuid());
		sendCooldown(player, CooldownPayload.reset());
	}

	public static void restartSelection(ServerPlayerEntity player) {
		String previous = player.getAttached(RaceAttachments.RACE);
		clearRace(player);
		requestSelection(player, previous == null ? "" : previous);
	}

	private static void applyPowers(ServerPlayerEntity player, Race race) {
		for (RacePower power : race.powers()) power.onApply(player);
	}

	private static void removeAbilities(ServerPlayerEntity player, Race race) {
		for (Race.AbilityInfo info : race.abilities()) {
			if (info.ability() != null) info.ability().onRemove(player);
		}
	}

	// ------------------------------------------------------------ habilidades

	/** raw = slot | modo << 8 | ticks segurados << 12 (modo 0 = apertou, 1 = começou a segurar, 2 = soltou). */
	public static void useAbility(ServerPlayerEntity player, int raw) {
		int slot = raw & 0xFF, mode = (raw >> 8) & 0xF, held = raw >>> 12;
		if (mode == 3 || mode == 4 || mode == 5) {
			var r = RaceAccess.getRace(player);
			if (r.isEmpty() || slot >= r.get().abilities().size() || r.get().abilities().get(slot).ability() == null) return;
			if (mode == 3) r.get().abilities().get(slot).ability().scroll(player, held - 8);
			else if (mode == 4) r.get().abilities().get(slot).ability().attackKey(player);
			else r.get().abilities().get(slot).ability().choose(player, held - 8);
			return;
		}
		if (!player.isAlive() || (player.isSpectator() && !com.dakura.origins.power.ability.PhaseAbility.isPhased(player))) return;
		Optional<Race> race = getRace(player);
		if (race.isEmpty() || slot < 0 || slot >= race.get().abilities().size()) return;
		if (com.dakura.origins.power.Control.isLocked(player)) {
			player.sendMessage(Text.translatable("dakura_origins.message.locked").formatted(Formatting.GRAY), true);
			return;
		}
		Race.AbilityInfo info = race.get().abilities().get(slot);

		long[] ready = COOLDOWNS.computeIfAbsent(player.getUuid(), u -> new long[2]);
		if (NO_COOLDOWN.contains(player.getUuid())) ready[slot < ready.length ? slot : 0] = 0;
		if (slot < ready.length && serverTicks < ready[slot]) {
			long secs = (ready[slot] - serverTicks + 19) / 20;
			if (mode != 2) player.sendMessage(Text.translatable("dakura_origins.message.cooldown", info.nameText(), secs)
					.formatted(Formatting.GRAY), true);
			return;
		}
		if (info.ability() == null) {
			player.sendMessage(Text.translatable("dakura_origins.message.not_implemented", info.nameText())
					.formatted(Formatting.GRAY), true);
			return;
		}
		boolean used;
		if (mode == 1) {
			info.ability().holdStart(player);
			used = false;
		} else if (mode == 2) used = info.ability().release(player, held);
		else used = info.ability().activate(player);
		if (used) {
			Race rc = RaceAccess.getRace(player).orElse(null);
			int glow = rc == null ? 0xFFFFFF : rc.palette().glow() & 0xFFFFFF;
			com.dakura.origins.network.ModNetworking.sendAround(
					new com.dakura.origins.network.AnimPayload(player.getId(), "fx:flourish", glow), player);
		}
		if (used && slot < ready.length && !NO_COOLDOWN.contains(player.getUuid())) {
			ready[slot] = serverTicks + info.cooldownTicks();
			if (info.cooldownTicks() > 0) {
				sendCooldown(player, new CooldownPayload(slot, info.cooldownTicks(), info.cooldownTicks()));
			}
		}
	}

	/** /do reset cooldown: habilidades e passivas prontas na hora (não mexe em maturidade, atributos nem raça). */
	public static void resetCooldowns(ServerPlayerEntity player) {
		COOLDOWNS.remove(player.getUuid());
		sendCooldown(player, CooldownPayload.reset());
		getRace(player).ifPresent(r -> resetPassives(player, r));
	}

	/** Passivas guardam a recarga no próprio poder e a limpam no onRemove: reaplica só as que têm recarga. */
	private static void resetPassives(ServerPlayerEntity player, Race race) {
		for (RacePower pw : race.powers()) {
			if (pw instanceof com.dakura.origins.power.types.SecondChancePower || pw instanceof com.dakura.origins.power.types.RebirthPower
					|| pw instanceof com.dakura.origins.power.types.DollLeapPower || pw instanceof com.dakura.origins.power.types.DevourPower
					|| pw instanceof com.dakura.origins.power.types.FungalEmergencyPower
					|| (pw instanceof com.dakura.origins.power.types.GoldenWardPower && !com.dakura.origins.power.EntityFlags.has(player, com.dakura.origins.power.types.GoldenWardPower.FLAG))) {
				pw.onRemove(player);
				pw.onApply(player);
			}
		}
		if (race.powers().stream().anyMatch(p -> p instanceof com.dakura.origins.power.types.GoldenWardPower))
			com.dakura.origins.power.EntityFlags.set(player, com.dakura.origins.power.types.GoldenWardPower.FLAG, true);
	}

	/** /do full maturity: aplica na hora os valores do estágio final (escala e atributos sem crescimento gradual). */
	public static void snapAttributes(ServerPlayerEntity player) {
		getRace(player).ifPresent(r -> {
			for (RacePower pw : r.powers()) if (pw instanceof com.dakura.origins.power.types.AttributePower ap) ap.snap(player);
		});
	}

	/** Avisa a HUD do jogador (indicadores de recarga). */
	private static void sendCooldown(ServerPlayerEntity player, CooldownPayload payload) {
		if (ServerPlayNetworking.canSend(player, CooldownPayload.ID)) ServerPlayNetworking.send(player, payload);
	}

	private static MutableText colored(MutableText text, int rgb) {
		return text.styled(s -> s.withColor(TextColor.fromRgb(rgb & 0xFFFFFF)).withBold(true));
	}
}
