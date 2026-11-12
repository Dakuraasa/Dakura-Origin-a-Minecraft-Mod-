package com.dakura.origins.command;

import com.dakura.origins.race.Race;
import com.dakura.origins.race.RaceManager;
import com.dakura.origins.race.RaceRegistry;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Optional;

/**
 * Dois nomes, mesmo comando (sistema de comandos vanilla, com autocompletar):
 * <pre>
 * /raca                          /race                     → mostra sua raça
 * /raca trocar                   /race change              → tira sua raça e reabre a seleção
 * /raca trocar &lt;jogador&gt;         /race change &lt;player&gt;     → (OP) faz outro jogador escolher de novo
 * /raca definir &lt;jogador&gt; &lt;id&gt;   /race set &lt;player&gt; &lt;id&gt;   → (OP) define a raça direto
 * /raca lista                    /race list                → lista as raças instaladas
 * </pre>
 * As respostas usam chaves de tradução: cada jogador lê no próprio idioma.
 */
public final class RaceCommand {
	private RaceCommand() {}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
								CommandRegistryAccess registryAccess,
								CommandManager.RegistrationEnvironment environment) {
		// prefixo oficial: /do (Dakura Origins). Os antigos /raca e /race não existem mais.
		dispatcher.register(CommandManager.literal("do")
				.then(build("race", "change", "set", "list", "player", "race"))
				.then(CommandManager.literal("full").requires(src -> src.hasPermissionLevel(2)).then(CommandManager.literal("maturity")
						.executes(ctx -> fullMaturity(ctx.getSource(), ctx.getSource().getPlayerOrThrow()))))
				.then(CommandManager.literal("reset").requires(src -> src.hasPermissionLevel(2)).then(CommandManager.literal("cooldown")
						.executes(ctx -> resetCooldown(ctx.getSource(), ctx.getSource().getPlayerOrThrow()))))
				.then(CommandManager.literal("no").requires(src -> src.hasPermissionLevel(2)).then(CommandManager.literal("cooldown")
						.executes(ctx -> noCooldown(ctx.getSource(), ctx.getSource().getPlayerOrThrow())))));
	}

	private static int fullMaturity(ServerCommandSource source, ServerPlayerEntity p) {
		if (!com.dakura.origins.power.types.MaturityPower.maxOut(p)) {
			source.sendError(Text.translatable("dakura_origins.command.no_maturity"));
			return 0;
		}
		RaceManager.snapAttributes(p);
		p.calculateDimensions();
		source.sendFeedback(() -> Text.translatable("dakura_origins.command.full_maturity").formatted(Formatting.GOLD), false);
		return 1;
	}

	private static int resetCooldown(ServerCommandSource source, ServerPlayerEntity p) {
		RaceManager.resetCooldowns(p);
		source.sendFeedback(() -> Text.translatable("dakura_origins.command.reset_cooldown").formatted(Formatting.AQUA), false);
		return 1;
	}

	private static int noCooldown(ServerCommandSource source, ServerPlayerEntity p) {
		boolean on = RaceManager.NO_COOLDOWN.add(p.getUuid());
		if (!on) RaceManager.NO_COOLDOWN.remove(p.getUuid());
		else RaceManager.resetCooldowns(p);
		source.sendFeedback(() -> Text.translatable(on ? "dakura_origins.command.no_cooldown_on" : "dakura_origins.command.no_cooldown_off")
				.formatted(on ? Formatting.GREEN : Formatting.GRAY), false);
		return 1;
	}

	private static LiteralArgumentBuilder<ServerCommandSource> build(String root, String change, String set, String list,
																	  String playerArg, String raceArg) {
		return CommandManager.literal(root)
				.executes(ctx -> show(ctx.getSource(), ctx.getSource().getPlayerOrThrow()))
				.then(CommandManager.literal(change)
						.executes(ctx -> change(ctx.getSource(), ctx.getSource().getPlayerOrThrow()))
						.then(CommandManager.argument(playerArg, EntityArgumentType.player())
								.requires(src -> src.hasPermissionLevel(2))
								.executes(ctx -> change(ctx.getSource(), EntityArgumentType.getPlayer(ctx, playerArg)))))
				.then(CommandManager.literal(set)
						.requires(src -> src.hasPermissionLevel(2))
						.then(CommandManager.argument(playerArg, EntityArgumentType.player())
								.then(CommandManager.argument(raceArg, StringArgumentType.word())
										.suggests((ctx, builder) -> CommandSource.suggestMatching(RaceRegistry.ids(), builder))
										.executes(ctx -> define(ctx.getSource(),
												EntityArgumentType.getPlayer(ctx, playerArg),
												StringArgumentType.getString(ctx, raceArg))))))
				.then(CommandManager.literal(list)
						.executes(ctx -> list(ctx.getSource())));
	}

	private static int show(ServerCommandSource source, ServerPlayerEntity player) {
		Optional<Race> race = RaceManager.getRace(player);
		if (race.isPresent()) {
			source.sendFeedback(() -> Text.translatable("dakura_origins.command.current",
							race.get().nameText().formatted(Formatting.GOLD)).formatted(Formatting.GRAY)
					.append(Text.translatable("dakura_origins.command.change_hint").formatted(Formatting.DARK_GRAY)), false);
		} else {
			source.sendFeedback(() -> Text.translatable("dakura_origins.command.none").formatted(Formatting.GRAY), false);
		}
		return 1;
	}

	private static int change(ServerCommandSource source, ServerPlayerEntity target) {
		RaceManager.restartSelection(target);
		if (source.getPlayer() != target) {
			source.sendFeedback(() -> Text.translatable("dakura_origins.command.other_choosing", target.getDisplayName()), true);
		}
		return 1;
	}

	private static int define(ServerCommandSource source, ServerPlayerEntity target, String id) {
		Optional<Race> race = RaceRegistry.get(id);
		if (race.isEmpty()) {
			source.sendError(Text.translatable("dakura_origins.command.unknown", id));
			return 0;
		}
		RaceManager.setRace(target, race.get());
		source.sendFeedback(() -> Text.translatable("dakura_origins.command.defined",
				target.getDisplayName(), race.get().nameText()), true);
		return 1;
	}

	private static int list(ServerCommandSource source) {
		MutableText names = Text.empty();
		boolean first = true;
		for (Race r : RaceRegistry.all()) {
			if (!first) names.append(Text.literal(", ").formatted(Formatting.DARK_GRAY));
			names.append(r.nameText().formatted(Formatting.GOLD))
					.append(Text.literal(" (" + r.id() + ")").formatted(Formatting.DARK_GRAY));
			first = false;
		}
		source.sendFeedback(() -> Text.translatable("dakura_origins.command.list", names), false);
		return RaceRegistry.all().size();
	}
}
