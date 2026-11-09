package com.dakura.origins.power.ability;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:mimic" — Metamorfose da Succubus: copia a aparência de um jogador na mira e rouba fisicamente
 * os atributos dele (vida, armadura vestida, força, velocidade, tamanho...), mas não as habilidades.
 * Toque de novo para desfazer. { "range": 24, "duration": 1200 }
 */
public final class MimicAbility implements Ability {
	public static final String PREFIX = "mimic:";
	private static final List<RegistryEntry<EntityAttribute>> ATTRS = List.of(EntityAttributes.GENERIC_MAX_HEALTH,
			EntityAttributes.GENERIC_ARMOR, EntityAttributes.GENERIC_ARMOR_TOUGHNESS, EntityAttributes.GENERIC_ATTACK_DAMAGE,
			EntityAttributes.GENERIC_ATTACK_SPEED, EntityAttributes.GENERIC_MOVEMENT_SPEED, EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,
			EntityAttributes.GENERIC_SCALE);
	private static final Map<UUID, Object[]> ACTIVE = new HashMap<>(); // {alvo uuid, fim}
	private static long ticks;
	private final double range;
	private final int duration;

	public MimicAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 24f);
		this.duration = JsonParams.integer(json, "duration", 1200);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ticks++;
			if (ACTIVE.isEmpty()) return;
			ACTIVE.entrySet().removeIf(en -> {
				ServerPlayerEntity p = server.getPlayerManager().getPlayer(en.getKey());
				if (p == null) return true;
				if (ticks >= (long) en.getValue()[1] || !p.isAlive()) {
					clear(p, (UUID) en.getValue()[0]);
					return true;
				}
				return false;
			});
		});
	}

	private static Identifier id(RegistryEntry<EntityAttribute> a) {
		return DakuraOrigins.id("mimic_" + a.getKey().map(k -> k.getValue().getPath().replace('.', '_')).orElse("x"));
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		Object[] cur = ACTIVE.remove(p.getUuid());
		if (cur != null) {
			clear(p, (UUID) cur[0]);
			return false;
		}
		var t = TargetUtil.aimed(p, range, e -> e instanceof PlayerEntity);
		if (!(t instanceof PlayerEntity target)) {
			p.sendMessage(Text.translatable("dakura_origins.message.no_target").formatted(Formatting.GRAY), true);
			return false;
		}
		for (var a : ATTRS) {
			var mine = p.getAttributeInstance(a);
			if (mine == null || target.getAttributeInstance(a) == null) continue;
			mine.removeModifier(id(a));
			double delta = target.getAttributeValue(a) - mine.getValue();
			if (Math.abs(delta) > 1e-4) mine.addTemporaryModifier(new EntityAttributeModifier(id(a), delta, EntityAttributeModifier.Operation.ADD_VALUE));
		}
		p.setHealth(Math.min(p.getHealth(), p.getMaxHealth()));
		ACTIVE.put(p.getUuid(), new Object[]{target.getUuid(), ticks + duration});
		EntityFlags.set(p, PREFIX + target.getUuid(), true);
		p.calculateDimensions();
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:morph", 1), p);
		p.sendMessage(Text.translatable("dakura_origins.message.mimic", target.getName()).formatted(Formatting.LIGHT_PURPLE), true);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 0.9f, 1.2f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 0.8f);
		return true;
	}

	private static void clear(ServerPlayerEntity p, UUID target) {
		for (var a : ATTRS) {
			var inst = p.getAttributeInstance(a);
			if (inst != null) inst.removeModifier(id(a));
		}
		p.setHealth(Math.min(p.getHealth(), p.getMaxHealth()));
		EntityFlags.set(p, PREFIX + target, false);
		p.calculateDimensions();
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:morph", 0), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 0.7f, 0.8f);
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		Object[] cur = ACTIVE.remove(p.getUuid());
		if (cur != null) clear(p, (UUID) cur[0]);
	}
}
