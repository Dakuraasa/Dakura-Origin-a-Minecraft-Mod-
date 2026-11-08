package com.dakura.origins.power.ability;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * "dakura_origins:morph" — Metamorfose: guarda só a ÚLTIMA criatura que matou.
 * Toque: vira ela (ou volta ao normal). Segurar a tecla: libera a forma guardada.
 * Criaturas voadoras voam; aquáticas respiram na água. Itens não podem ser usados na forma.
 */
public final class MorphAbility implements Ability {
	public static final String PREFIX = "morph:";
	public static final AttachmentType<String> SLOT = AttachmentRegistry.<String>create(
			DakuraOrigins.id("morph_slot"), b -> b.persistent(Codec.STRING).copyOnDeath());
	private static final Map<UUID, String> MORPHED = new ConcurrentHashMap<>();
	private static final Set<String> BANNED = Set.of("minecraft:ender_dragon", "minecraft:wither", "minecraft:giant",
			"minecraft:warden", "minecraft:elder_guardian", "minecraft:ghast", "minecraft:armor_stand",
			"dakura_origins:huli_clone", "dakura_origins:undead_minion");
	private static final Set<String> FLYERS = Set.of("minecraft:bat", "minecraft:parrot", "minecraft:bee", "minecraft:allay",
			"minecraft:phantom", "minecraft:vex", "minecraft:blaze");
	private static final int HOLD = 16;

	public MorphAbility(JsonObject json) {}

	public static void bootstrap() {
		ServerLivingEntityEvents.AFTER_DEATH.register((victim, source) -> {
			if (!(source.getAttacker() instanceof ServerPlayerEntity killer) || victim instanceof PlayerEntity || !has(killer)) return;
			String id = Registries.ENTITY_TYPE.getId(victim.getType()).toString();
			if (BANNED.contains(id)) return;
			if (id.equals(killer.getAttached(SLOT))) return;
			killer.setAttached(SLOT, id);
			killer.sendMessage(Text.translatable("dakura_origins.message.morph_stored", victim.getType().getName())
					.formatted(Formatting.DARK_RED), true);
			killer.getWorld().playSound(null, victim.getX(), victim.getY(), victim.getZ(), SoundEvents.BLOCK_SCULK_CATALYST_BLOOM,
					SoundCategory.PLAYERS, 0.6f, 1.4f);
			ModNetworking.sendAround(new AnimPayload(victim.getId(), "fx:soul_take", killer.getId()), killer);
		});
		ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> {
			if (MORPHED.containsKey(newP.getUuid())) set(newP, null);
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (MORPHED.isEmpty() || server.getTicks() % 20 != 0) return;
			for (UUID u : MORPHED.keySet()) {
				ServerPlayerEntity p = server.getPlayerManager().getPlayer(u);
				if (p == null) {
					MORPHED.remove(u);
					continue;
				}
				EntityType<?> t = morphType(p);
				if (t != null && aquatic(t)) p.addStatusEffect(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 40, 0, true, false, false));
			}
		});
		UseItemCallback.EVENT.register((p, w, h) -> morphType(p) != null
				? TypedActionResult.fail(p.getStackInHand(h)) : TypedActionResult.pass(p.getStackInHand(h)));
		UseBlockCallback.EVENT.register((p, w, h, hit) -> morphType(p) != null ? ActionResult.FAIL : ActionResult.PASS);
	}

	private static boolean has(PlayerEntity p) {
		return RaceAccess.getRace(p).map(r -> r.abilities().stream().anyMatch(a -> a.ability() instanceof MorphAbility)).orElse(false);
	}

	private static boolean aquatic(EntityType<?> t) {
		SpawnGroup g = t.getSpawnGroup();
		return g == SpawnGroup.WATER_CREATURE || g == SpawnGroup.WATER_AMBIENT || g == SpawnGroup.UNDERGROUND_WATER_CREATURE
				|| g == SpawnGroup.AXOLOTLS;
	}

	/** Forma atual (cliente e servidor), ou null. */
	public static EntityType<?> morphType(PlayerEntity p) {
		String id = null;
		if (p.getWorld().isClient) {
			Set<String> f = EntityFlags.CLIENT.get(p.getId());
			if (f != null) for (String s : f) if (s.startsWith(PREFIX)) id = s.substring(PREFIX.length());
		} else id = MORPHED.get(p.getUuid());
		if (id == null) return null;
		Identifier ident = Identifier.tryParse(id);
		return ident == null ? null : Registries.ENTITY_TYPE.getOrEmpty(ident).orElse(null);
	}

	/** Tamanho do jogador transformado (usado pelo PlayerEntityMixin). */
	public static EntityDimensions dimensions(PlayerEntity p) {
		EntityType<?> t = morphType(p);
		return t == null ? null : t.getDimensions();
	}

	@Override
	public boolean chargeable() {
		return true;
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		return release(p, 0);
	}

	@Override
	public boolean release(ServerPlayerEntity p, int held) {
		if (held >= HOLD) {
			if (MORPHED.containsKey(p.getUuid())) set(p, null);
			if (p.getAttached(SLOT) == null) return false;
			p.removeAttached(SLOT);
			p.sendMessage(Text.translatable("dakura_origins.message.morph_freed").formatted(Formatting.GRAY), true);
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_SOUL_SAND_BREAK, SoundCategory.PLAYERS, 0.8f, 0.8f);
			ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:morph", 2), p);
			return false;
		}
		if (MORPHED.containsKey(p.getUuid())) {
			Box standing = new Box(p.getX() - 0.3, p.getY(), p.getZ() - 0.3, p.getX() + 0.3, p.getY() + p.getScale() * 1.8, p.getZ() + 0.3);
			if (!p.getWorld().isSpaceEmpty(p, standing)) {
				p.sendMessage(Text.translatable("dakura_origins.message.bat_no_room").formatted(Formatting.GRAY), true);
				return false;
			}
			set(p, null);
			return true;
		}
		String id = p.getAttached(SLOT);
		Identifier ident = id == null ? null : Identifier.tryParse(id);
		EntityType<?> t = ident == null ? null : Registries.ENTITY_TYPE.getOrEmpty(ident).orElse(null);
		if (t == null) {
			p.sendMessage(Text.translatable("dakura_origins.message.morph_empty").formatted(Formatting.GRAY), true);
			return false;
		}
		EntityDimensions d = t.getDimensions();
		Box box = new Box(p.getX() - d.width() / 2, p.getY(), p.getZ() - d.width() / 2, p.getX() + d.width() / 2, p.getY() + d.height(), p.getZ() + d.width() / 2);
		if (!p.getWorld().isSpaceEmpty(p, box)) {
			p.sendMessage(Text.translatable("dakura_origins.message.bat_no_room").formatted(Formatting.GRAY), true);
			return false;
		}
		set(p, id);
		return true;
	}

	private static void set(ServerPlayerEntity p, String id) {
		String old = MORPHED.remove(p.getUuid());
		if (old != null) EntityFlags.set(p, PREFIX + old, false);
		boolean fly = id != null && FLYERS.contains(id);
		if (id != null) {
			MORPHED.put(p.getUuid(), id);
			EntityFlags.set(p, PREFIX + id, true);
		}
		if (!p.isCreative() && !p.isSpectator()) {
			p.getAbilities().allowFlying = fly;
			if (!fly) p.getAbilities().flying = false;
			p.sendAbilitiesUpdate();
		}
		p.calculateDimensions();
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:morph", id != null ? 1 : 0), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 0.7f, 0.7f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_SCULK_SPREAD, SoundCategory.PLAYERS, 0.8f, 0.6f);
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		if (MORPHED.containsKey(p.getUuid())) set(p, null);
		p.removeAttached(SLOT);
	}
}
