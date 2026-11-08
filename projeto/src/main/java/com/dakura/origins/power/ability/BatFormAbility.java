package com.dakura.origins.power.ability;

import com.dakura.origins.network.BatStatePayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.EntityTrackingEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerAbilities;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;

import java.util.ArrayList;
import java.util.UUID;

/**
 * "dakura_origins:bat_form" — vira o morcego vanilla (modelo, tamanho e animação originais)
 * e voa livremente, sem limite de tempo. Apertar de novo volta à forma humanoide.
 * <pre>
 * { "type": "dakura_origins:bat_form", "fly_speed": 0.06 }
 * </pre>
 * Em forma de morcego não dá para atacar, usar itens nem interagir com blocos.
 */
public final class BatFormAbility implements Ability {
	private static final float DEFAULT_FLY_SPEED = 0.05f;
	private final float flySpeed;

	public BatFormAbility(JsonObject json) {
		this.flySpeed = JsonParams.num(json, "fly_speed", 0.06f);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(BatFormAbility::tickAll);

		// novos observadores precisam saber que este jogador é um morcego
		EntityTrackingEvents.START_TRACKING.register((tracked, viewer) -> {
			if (tracked instanceof PlayerEntity p && BatFormState.SERVER.contains(p.getUuid())
					&& ServerPlayNetworking.canSend(viewer, BatStatePayload.ID)) {
				ServerPlayNetworking.send(viewer, new BatStatePayload(p.getId(), true));
			}
		});

		// morreu = voltou a ser humanoide (o corpo novo não é morcego)
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (BatFormState.SERVER.remove(newPlayer.getUuid())) {
				ModNetworking.sendAround(new BatStatePayload(newPlayer.getId(), false), newPlayer);
			}
		});

		// morcegos não seguram itens nem atacam (cliente e servidor)
		AttackEntityCallback.EVENT.register((p, w, h, e, hit) -> BatFormState.isBat(p) ? ActionResult.FAIL : ActionResult.PASS);
		AttackBlockCallback.EVENT.register((p, w, h, pos, dir) -> BatFormState.isBat(p) ? ActionResult.FAIL : ActionResult.PASS);
		UseBlockCallback.EVENT.register((p, w, h, hit) -> BatFormState.isBat(p) ? ActionResult.FAIL : ActionResult.PASS);
		UseEntityCallback.EVENT.register((p, w, h, e, hit) -> BatFormState.isBat(p) ? ActionResult.FAIL : ActionResult.PASS);
		UseItemCallback.EVENT.register((p, w, h) -> BatFormState.isBat(p)
				? TypedActionResult.fail(p.getStackInHand(h)) : TypedActionResult.pass(p.getStackInHand(h)));
	}

	@Override
	public boolean activate(ServerPlayerEntity player) {
		if (BatFormState.SERVER.contains(player.getUuid())) {
			return revert(player, true);
		}
		if (VampireBiteAbility.isBiting(player)) return false;
		transform(player);
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		if (BatFormState.SERVER.contains(player.getUuid())) revert(player, false);
	}

	// ================================================================== transformação

	private void transform(ServerPlayerEntity player) {
		BatFormState.SERVER.add(player.getUuid());
		PlayerAbilities abilities = player.getAbilities();
		abilities.allowFlying = true;
		abilities.flying = true;
		abilities.setFlySpeed(flySpeed);
		player.sendAbilitiesUpdate();
		player.calculateDimensions();
		ModNetworking.sendAround(new BatStatePayload(player.getId(), true), player);
		poof(player, true);
	}

	/** @return false se não houver espaço para voltar a ficar de pé. */
	private static boolean revert(ServerPlayerEntity player, boolean checkSpace) {
		if (checkSpace) {
			Box standing = new Box(player.getX() - 0.3, player.getY(), player.getZ() - 0.3,
					player.getX() + 0.3, player.getY() + 1.8, player.getZ() + 0.3);
			if (!player.getWorld().isSpaceEmpty(player, standing)) {
				player.sendMessage(Text.translatable("dakura_origins.message.bat_no_room").formatted(Formatting.GRAY), true);
				return false;
			}
		}
		BatFormState.SERVER.remove(player.getUuid());
		PlayerAbilities abilities = player.getAbilities();
		boolean keepsFlight = player.isCreative() || player.isSpectator();
		abilities.allowFlying = keepsFlight;
		if (!keepsFlight) abilities.flying = false;
		abilities.setFlySpeed(DEFAULT_FLY_SPEED);
		player.sendAbilitiesUpdate();
		player.calculateDimensions();
		if (!player.isOnGround() && !keepsFlight) {
			player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 50, 0, false, false, true));
		}
		ModNetworking.sendAround(new BatStatePayload(player.getId(), false), player);
		poof(player, false);
		return true;
	}

	private static void poof(ServerPlayerEntity player, boolean toBat) {
		if (!(player.getWorld() instanceof ServerWorld sw)) return;
		double y = player.getY() + (toBat ? 0.9 : 0.4);
		sw.spawnParticles(ParticleTypes.LARGE_SMOKE, player.getX(), y, player.getZ(), 8, 0.3, 0.45, 0.3, 0.02);
		ModNetworking.sendAround(new com.dakura.origins.network.AnimPayload(player.getId(), "fx:bat", toBat ? 1 : 0), player);
		sw.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE,
				SoundCategory.PLAYERS, 0.5f, toBat ? 0.8f : 0.6f);
		sw.playSound(null, player.getX(), player.getY(), player.getZ(),
				toBat ? SoundEvents.ENTITY_BAT_TAKEOFF : SoundEvents.ENTITY_BAT_HURT,
				SoundCategory.PLAYERS, 0.7f, toBat ? 1.0f : 0.6f);
	}

	// ================================================================== tick

	private static void tickAll(MinecraftServer server) {
		if (BatFormState.SERVER.isEmpty()) return;
		for (UUID id : new ArrayList<>(BatFormState.SERVER)) {
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(id);
			if (player == null) {
				BatFormState.SERVER.remove(id);
				continue;
			}
			// voo livre e sem limite: mantém o voo ligado mesmo se o jogador tocar o chão
			PlayerAbilities abilities = player.getAbilities();
			if (!abilities.allowFlying || !abilities.flying) {
				abilities.allowFlying = true;
				abilities.flying = true;
				player.sendAbilitiesUpdate();
			}
			player.fallDistance = 0;
		}
	}
}
