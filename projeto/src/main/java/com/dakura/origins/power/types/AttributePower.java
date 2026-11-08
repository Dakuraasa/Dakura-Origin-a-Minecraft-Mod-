package com.dakura.origins.power.types;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.power.ConditionalPower;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;

import java.util.Locale;

/**
 * "dakura_origins:attribute" — modifica um atributo vanilla.
 * <pre>
 * Fixo:        { "attribute": "minecraft:generic.movement_speed", "operation": "add_multiplied_base", "amount": 0.15 }
 * Por estágio: { "attribute": "minecraft:generic.armor", "stage_values": [15, 18, 20] }
 * Contínuo:    { "attribute": "minecraft:generic.scale", "maturity_range": [0.39, 0.94],
 *                "smooth": true, "check_space": true }
 * </pre>
 * operation: add_value | add_multiplied_base | add_multiplied_total
 * - stage_values / maturity_range usam a maturidade da raça ({@link MaturityPower}).
 * - smooth: muda no máximo "smooth_step" por atualização (crescimento gradual).
 * - check_space: só cresce se houver espaço livre para o corpo novo (evita ficar preso em blocos).
 */
public final class AttributePower extends ConditionalPower {
	private static final int SYNC_INTERVAL = 10;

	private final RegistryEntry<EntityAttribute> attribute;
	private final EntityAttributeModifier.Operation operation;
	private final Identifier modifierId;
	private final double amount;
	private final double[] stageValues;
	private final double rangeMin, rangeMax;
	private final boolean continuous;
	private final boolean smooth;
	private final double smoothStep;
	private final boolean checkSpace;

	public AttributePower(JsonObject json) {
		super(json);
		Identifier attrId = Identifier.of(json.get("attribute").getAsString());
		this.attribute = Registries.ATTRIBUTE.getEntry(attrId)
				.orElseThrow(() -> new IllegalArgumentException("Atributo desconhecido: " + attrId));
		this.operation = switch (JsonParams.str(json, "operation", "add_value").toLowerCase(Locale.ROOT)) {
			case "add_multiplied_base", "multiply_base" -> EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE;
			case "add_multiplied_total", "multiply_total" -> EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
			default -> EntityAttributeModifier.Operation.ADD_VALUE;
		};
		this.amount = JsonParams.num(json, "amount", 0f);

		if (json.has("stage_values")) {
			JsonArray a = json.getAsJsonArray("stage_values");
			stageValues = new double[a.size()];
			for (int i = 0; i < a.size(); i++) stageValues[i] = a.get(i).getAsDouble();
		} else {
			stageValues = null;
		}
		this.continuous = json.has("maturity_range");
		if (continuous) {
			JsonArray r = json.getAsJsonArray("maturity_range");
			rangeMin = r.get(0).getAsDouble();
			rangeMax = r.get(1).getAsDouble();
		} else {
			rangeMin = rangeMax = 0;
		}
		this.smooth = JsonParams.bool(json, "smooth", false);
		this.smoothStep = JsonParams.num(json, "smooth_step", 0.02f);
		this.checkSpace = JsonParams.bool(json, "check_space", false);

		String idStr = JsonParams.str(json, "modifier_id", "");
		this.modifierId = idStr.isEmpty()
				? DakuraOrigins.id("power/" + attrId.getPath().replace('.', '_') + "_" + Integer.toHexString(json.toString().hashCode()))
				: Identifier.of(idStr);
	}

	@Override
	public void onApply(ServerPlayerEntity player) {
		sync(player);
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.age % SYNC_INTERVAL == 0) sync(player);
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		EntityAttributeInstance inst = player.getAttributeInstance(attribute);
		if (inst != null) inst.removeModifier(modifierId);
	}

	/** Valor que o modificador deveria ter agora. */
	private double target(ServerPlayerEntity player) {
		if (stageValues != null && stageValues.length > 0) {
			int s = Math.min(stageValues.length - 1, MaturityPower.stage(player));
			return stageValues[s];
		}
		if (continuous) {
			return rangeMin + (rangeMax - rangeMin) * MaturityPower.progress(player);
		}
		return amount;
	}

	private void sync(ServerPlayerEntity player) {
		EntityAttributeInstance inst = player.getAttributeInstance(attribute);
		if (inst == null) return;
		EntityAttributeModifier current = inst.getModifier(modifierId);

		if (!isActive(player)) {
			if (current != null) inst.removeModifier(modifierId);
			return;
		}

		double want = target(player);
		double have = current == null ? (smooth ? 0 : Double.NaN) : current.value();
		if (smooth && !Double.isNaN(have)) {
			double delta = want - have;
			if (Math.abs(delta) > smoothStep) want = have + Math.signum(delta) * smoothStep;
		}
		if (current != null && Math.abs(current.value() - want) < 1e-4) return;

		// crescer só se o corpo novo couber no lugar
		if (checkSpace && (current == null ? want > 0 : want > current.value()) && !fits(player, inst, want)) return;

		if (current != null) inst.removeModifier(modifierId);
		inst.addTemporaryModifier(new EntityAttributeModifier(modifierId, want, operation));
	}

	/** Testa se a caixa de colisão com a nova escala cabe sem encostar em blocos. */
	private boolean fits(ServerPlayerEntity player, EntityAttributeInstance inst, double newAmount) {
		if (attribute.value() != EntityAttributes.GENERIC_SCALE.value()) return true;
		EntityAttributeModifier current = inst.getModifier(modifierId);
		double base = inst.getValue() - (current == null ? 0 : current.value());
		double scale = Math.max(0.1, base + newAmount);
		double half = 0.3 * scale;
		double height = 1.8 * scale;
		Box box = new Box(player.getX() - half, player.getY() + 0.01, player.getZ() - half,
				player.getX() + half, player.getY() + height, player.getZ() + half);
		return player.getWorld().isSpaceEmpty(player, box);
	}
}
