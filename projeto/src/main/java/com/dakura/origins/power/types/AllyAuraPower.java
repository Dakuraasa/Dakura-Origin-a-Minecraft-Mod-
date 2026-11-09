package com.dakura.origins.power.types;

import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * "dakura_origins:ally_aura" — jogadores por perto (não o próprio) recebem efeitos suaves.
 * { "radius": 8, "effects": [{ "effect": "minecraft:regeneration", "amplifier": 0 }], "color": "#FFD45E" }
 */
public final class AllyAuraPower implements RacePower {
	private final double radius;
	private final List<RegistryEntry<StatusEffect>> effects = new ArrayList<>();
	private final List<Integer> amps = new ArrayList<>();
	private final DustParticleEffect dust;

	public AllyAuraPower(JsonObject json) {
		this.radius = JsonParams.num(json, "radius", 8f);
		if (json.has("effects")) for (JsonElement e : json.getAsJsonArray("effects")) {
			JsonObject o = e.getAsJsonObject();
			Registries.STATUS_EFFECT.getEntry(Identifier.of(JsonParams.str(o, "effect", "minecraft:regeneration"))).ifPresent(en -> {
				effects.add(en);
				amps.add(JsonParams.integer(o, "amplifier", 0));
			});
		}
		int c = Integer.parseInt(JsonParams.str(json, "color", "#FFD45E").replace("#", ""), 16);
		this.dust = new DustParticleEffect(new Vector3f(((c >> 16) & 255) / 255f, ((c >> 8) & 255) / 255f, (c & 255) / 255f), 0.8f);
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		if (p.age % 40 != 0 || p.isSpectator()) return;
		for (PlayerEntity o : p.getWorld().getEntitiesByClass(PlayerEntity.class, p.getBoundingBox().expand(radius),
				x -> x != p && x.isAlive() && !x.isSpectator())) {
			for (int i = 0; i < effects.size(); i++) o.addStatusEffect(new StatusEffectInstance(effects.get(i), 60, amps.get(i), true, false, true));
			p.getServerWorld().spawnParticles(dust, o.getX(), o.getBodyY(0.6), o.getZ(), 3, 0.3, 0.4, 0.3, 0);
		}
	}
}
