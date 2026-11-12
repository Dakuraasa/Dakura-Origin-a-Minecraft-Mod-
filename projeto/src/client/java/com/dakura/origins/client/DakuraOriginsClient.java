package com.dakura.origins.client;

import com.dakura.origins.client.anim.AbilityAnimations;
import com.dakura.origins.client.anim.BatDisguise;
import com.dakura.origins.client.anim.ClientEffects;
import com.dakura.origins.client.hud.AbilityHud;
import com.dakura.origins.client.screen.RaceSelectionScreen;
import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.BatStatePayload;
import com.dakura.origins.network.BiteAnimPayload;
import com.dakura.origins.network.CooldownPayload;
import com.dakura.origins.network.EntityFlagPayload;
import com.dakura.origins.network.FireballPayload;
import com.dakura.origins.network.MaturityPayload;
import com.dakura.origins.network.OpenSelectionPayload;
import com.dakura.origins.network.SelectionResultPayload;
import com.dakura.origins.network.UseAbilityPayload;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.ability.BatFormState;
import com.dakura.origins.power.types.MaturityPower;
import com.dakura.origins.race.RaceAccess;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

public class DakuraOriginsClient implements ClientModInitializer {
	/** true enquanto o servidor exige que o jogador escolha uma raça. */
	public static boolean mustChoose = false;
	public static String preselect = "";
	public static String currentRace = "";

	/**
	 * Teclas das habilidades: keybinds nativos do Minecraft, trocáveis em
	 * Opções → Controles → Datura Origins. Z e B são só o padrão sugerido.
	 */
	public static KeyBinding ABILITY_1;
	public static KeyBinding ABILITY_2;
	/** Abre (e fecha) a página de consulta da própria raça. */
	public static KeyBinding RACE_PAGE;

	@Override
	public void onInitializeClient() {
		com.dakura.origins.client.config.DakuraConfig.load();
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
				com.dakura.origins.entity.ModEntities.HULI_CLONE, com.dakura.origins.client.render.HuliCloneRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
				com.dakura.origins.entity.ModEntities.UNDEAD_MINION, net.minecraft.client.render.entity.ZombieEntityRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
				com.dakura.origins.entity.ModEntities.FEATHER, com.dakura.origins.client.render.FeatherRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
				com.dakura.origins.entity.ModEntities.GEO_PROJECTILE, com.dakura.origins.client.render.GeoProjectileRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
				com.dakura.origins.entity.ModEntities.SOUL_ORB, com.dakura.origins.client.render.SoulOrbRenderer::new);
		com.dakura.origins.client.fx.FxWorld.init();
		com.dakura.origins.client.fx.DarkFx.init();
		com.dakura.origins.client.fx.ElfFx.init();
		// opção "Aparências de raça" na tela de Personalização da skin (fica salva em config/dakura_origins.json)
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!(screen instanceof net.minecraft.client.gui.screen.option.SkinOptionsScreen)) return;
			net.fabricmc.fabric.api.client.screen.v1.Screens.getButtons(screen).add(
					net.minecraft.client.gui.widget.ButtonWidget.builder(cosmeticsLabel(), b -> {
						com.dakura.origins.client.config.DakuraConfig.raceCosmetics = !com.dakura.origins.client.config.DakuraConfig.raceCosmetics;
						com.dakura.origins.client.config.DakuraConfig.save();
						b.setMessage(cosmeticsLabel());
					}).dimensions(6, 6, 170, 20)
							.tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(net.minecraft.text.Text.translatable("dakura_origins.option.race_cosmetics.tooltip")))
							.build());
		});

		ABILITY_1 = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.dakura_origins.habilidade_1", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, "category.dakura_origins"));
		ABILITY_2 = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.dakura_origins.habilidade_2", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_B, "category.dakura_origins"));
		RACE_PAGE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.dakura_origins.race_page", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_O, "category.dakura_origins"));

		// HUD: teclas das habilidades + livro da página da raça (canto inferior esquerdo)
		HudRenderCallback.EVENT.register(AbilityHud::render);
		HudRenderCallback.EVENT.register(com.dakura.origins.client.hud.CasinoHud::render);
		HudRenderCallback.EVENT.register(com.dakura.origins.client.hud.DiceHud::render);
		net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents.AFTER_ENTITIES.register(com.dakura.origins.client.anim.FloatingText::render);
		net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT.register(
				(type, renderer, helper, context) -> {
					if (renderer instanceof net.minecraft.client.render.entity.PlayerEntityRenderer pr) {
						helper.register(new com.dakura.origins.client.render.FlowerCrownFeature(pr));
						helper.register(new com.dakura.origins.client.render.FoxEarsFeature(pr));
						helper.register(new com.dakura.origins.client.render.RaceFeatureRenderer(pr));
						helper.register(new com.dakura.origins.client.render.LegendFeatureRenderer(pr));
						helper.register(new com.dakura.origins.client.render.GeoPartsRenderer(pr));
					}
				});
		HudRenderCallback.EVENT.register(com.dakura.origins.client.hud.SecondChanceOverlay::render);
		// com o chat aberto o mouse fica livre: o livro da HUD vira um botão clicável
		ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
			if (!(screen instanceof ChatScreen)) return;
			ScreenMouseEvents.allowMouseClick(screen).register((scr, mx, my, button) -> {
				if (button == 0 && AbilityHud.infoHit(mx, my)) {
					client.send(() -> openRacePage(client));
					return false;
				}
				return true;
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(OpenSelectionPayload.ID, (payload, context) -> {
			mustChoose = true;
			preselect = payload.preselect();
			currentRace = "";
			RaceAccess.clientRaceId = "";
			MinecraftClient client = context.client();
			if (client.currentScreen == null || client.currentScreen instanceof ChatScreen
					|| client.currentScreen instanceof RaceSelectionScreen s && s.isViewOnly()) {
				client.setScreen(new RaceSelectionScreen(preselect));
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(SelectionResultPayload.ID, (payload, context) -> {
			MinecraftClient client = context.client();
			if (payload.success()) {
				mustChoose = false;
				currentRace = payload.raceId();
				RaceAccess.clientRaceId = payload.raceId();
				RaceAccess.clientPlayerId = client.player != null ? client.player.getUuid() : null;
				if (client.currentScreen instanceof RaceSelectionScreen screen) screen.onAccepted();
			} else if (client.currentScreen instanceof RaceSelectionScreen screen) {
				screen.onRejected();
			}
		});

		// Mordida: um pacote, dois canais (vampiro e vítima)
		ClientPlayNetworking.registerGlobalReceiver(BiteAnimPayload.ID, (payload, context) -> {
			if (context.client().world == null) return;
			if (payload.durationTicks() > 0) {
				AbilityAnimations.start(context.client().world, payload.vampireId(), "bite", payload.durationTicks());
				AbilityAnimations.start(context.client().world, payload.victimId(), "bitten", payload.durationTicks());
			} else {
				AbilityAnimations.stop(context.client().world, payload.vampireId(), "bite");
				AbilityAnimations.stop(context.client().world, payload.victimId(), "bitten");
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(AnimPayload.ID, (payload, context) -> {
			if (context.client().world == null) return;
			if (payload.channel().startsWith("fx:")) {
				ClientEffects.fx(payload.channel().substring(3), payload.entityId(), payload.durationTicks());
				return;
			}
			if (payload.channel().equals("second_chance")) {
				if (context.client().player != null && payload.entityId() == context.client().player.getId()) {
					com.dakura.origins.client.hud.SecondChanceOverlay.trigger();
				}
				return;
			}
			if (payload.durationTicks() > 0) {
				AbilityAnimations.start(context.client().world, payload.entityId(), payload.channel(), payload.durationTicks());
			} else {
				AbilityAnimations.stop(context.client().world, payload.entityId(), payload.channel());
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(FireballPayload.ID, (payload, context) ->
				ClientEffects.fireball(payload.id(), new Vec3d(payload.x(), payload.y(), payload.z()),
						new Vec3d(payload.vx(), payload.vy(), payload.vz()), payload.state()));

		ClientPlayNetworking.registerGlobalReceiver(EntityFlagPayload.ID, (payload, context) ->
		{
			EntityFlags.clientSet(payload.entityId(), payload.flag(), payload.value());
			if (payload.flag().equals(com.dakura.origins.power.ability.FoxFormAbility.FLAG) && context.client().world != null) {
				Entity e = context.client().world.getEntityById(payload.entityId());
				if (e != null) e.calculateDimensions();
				if (!payload.value()) com.dakura.origins.client.anim.FoxDisguise.forget(payload.entityId());
			}
			if (payload.flag().startsWith(com.dakura.origins.power.ability.MorphAbility.PREFIX) && context.client().world != null) {
				Entity e = context.client().world.getEntityById(payload.entityId());
				if (e != null) e.calculateDimensions();
				if (!payload.value()) com.dakura.origins.client.anim.MorphDisguise.forget(payload.entityId());
			}
		});

		ClientPlayNetworking.registerGlobalReceiver(MaturityPayload.ID, (payload, context) ->
				MaturityPower.clientPoints = payload.points());

		ClientPlayNetworking.registerGlobalReceiver(com.dakura.origins.network.FxPayload.ID, (p, context) ->
				ClientEffects.fxAt(p.name(), new Vec3d(p.x(), p.y(), p.z()), p.a(), p.b(), p.c()));

		ClientPlayNetworking.registerGlobalReceiver(CooldownPayload.ID, (payload, context) ->
				AbilityHud.setCooldown(payload.slot(), payload.totalTicks(), payload.remainingTicks()));

		ClientPlayNetworking.registerGlobalReceiver(BatStatePayload.ID, (payload, context) -> {
			if (payload.bat()) BatFormState.CLIENT.add(payload.entityId());
			else {
				BatFormState.CLIENT.remove(payload.entityId());
				BatDisguise.forget(payload.entityId());
			}
			if (context.client().world != null) {
				Entity e = context.client().world.getEntityById(payload.entityId());
				if (e != null) e.calculateDimensions();
			}
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			com.dakura.origins.client.anim.FlightClient.tick(client);
			if (mustChoose && client.player != null && client.currentScreen == null) {
				client.setScreen(new RaceSelectionScreen(preselect));
			}
			if (client.player != null && !currentRace.isEmpty()) RaceAccess.clientPlayerId = client.player.getUuid();
			handleKey(client, ABILITY_1, 0);
			handleKey(client, ABILITY_2, 1);
			while (RACE_PAGE.wasPressed()) openRacePage(client);
			ClientEffects.tick(client);
			if (client.world != null) {
				com.dakura.origins.client.anim.FoxDisguise.tick(client.world);
				com.dakura.origins.client.anim.MorphDisguise.tick(client.world);
			}
		});

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			mustChoose = false;
			preselect = "";
			currentRace = "";
			RaceAccess.clientRaceId = "";
			RaceAccess.clientPlayerId = null;
			MaturityPower.clientPoints = 0;
			BatFormState.CLIENT.clear();
			EntityFlags.CLIENT.clear();
			BatDisguise.clear();
			AbilityAnimations.clear();
			ClientEffects.clear();
			ClientEffects.KEEN_UNTIL = 0;
			com.dakura.origins.client.anim.FoxDisguise.clear();
			com.dakura.origins.client.anim.MorphDisguise.clear();
			HELD[0] = HELD[1] = -1;
			AbilityHud.clear();
			com.dakura.origins.client.hud.SecondChanceOverlay.clear();
		});
	}

	private static net.minecraft.text.Text cosmeticsLabel() {
		return net.minecraft.text.Text.translatable("dakura_origins.option.race_cosmetics",
				net.minecraft.screen.ScreenTexts.onOrOff(com.dakura.origins.client.config.DakuraConfig.raceCosmetics));
	}

	/** Página de consulta da raça atual (só no jogo, sem outra tela aberta por cima). */
	public static void openRacePage(MinecraftClient client) {
		if (mustChoose || currentRace.isEmpty() || client.player == null) return;
		if (client.currentScreen != null && !(client.currentScreen instanceof ChatScreen)) return;
		RaceSelectionScreen page = RaceSelectionScreen.info(currentRace);
		if (page != null) client.setScreen(page);
	}

	private static void sendAbility(int slot) {
		if (!mustChoose && ClientPlayNetworking.canSend(UseAbilityPayload.ID)) {
			ClientPlayNetworking.send(new UseAbilityPayload(slot));
		}
	}

	/** Envia um modo especial (3 = rodinha, 4 = ataque) para a habilidade deste tipo na raça do jogador. */
	public static boolean sendSpecial(Class<?> abilityType, int mode, int value) {
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null) return false;
		var race = RaceAccess.getRace(client.player).orElse(null);
		if (race == null) return false;
		for (int i = 0; i < race.abilities().size(); i++) {
			if (abilityType.isInstance(race.abilities().get(i).ability())) {
				sendAbility(i | mode << 8 | (value + 8) << 12);
				return true;
			}
		}
		return false;
	}

	/** Ticks que cada tecla está segurada (-1 = solta), para habilidades de carregar/segurar. */
	private static final int[] HELD = {-1, -1};

	public static int held(int slot) {
		return HELD[slot];
	}

	private static boolean chargeable(MinecraftClient client, int slot) {
		if (client.player == null) return false;
		return RaceAccess.getRace(client.player).map(r -> slot < r.abilities().size() && r.abilities().get(slot).ability() != null
				&& r.abilities().get(slot).ability().chargeable()).orElse(false);
	}

	private static void handleKey(MinecraftClient client, KeyBinding bind, int slot) {
		if (!chargeable(client, slot)) {
			HELD[slot] = -1;
			while (bind.wasPressed()) sendAbility(slot);
			return;
		}
		while (bind.wasPressed()) { }
		boolean down = bind.isPressed() && client.currentScreen == null && !mustChoose;
		if (down) {
			if (HELD[slot] < 0) {
				HELD[slot] = 0;
				sendAbility(slot | 1 << 8);
			} else {
				HELD[slot]++;
				if (HELD[slot] <= 30 && HELD[slot] % 6 == 0 && client.player != null) client.player.playSound(
						net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.12f, 0.6f + HELD[slot] / 40f);
			}
		} else if (HELD[slot] >= 0) {
			sendAbility(slot | 2 << 8 | Math.min(4095, HELD[slot]) << 12);
			HELD[slot] = -1;
		}
	}
}
