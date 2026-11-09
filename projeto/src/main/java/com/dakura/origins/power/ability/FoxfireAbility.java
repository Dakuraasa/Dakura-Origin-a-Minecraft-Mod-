package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.FxPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:foxfire" — Kitsunebi: nove chamas-fantasma azuis giram em volta da Kitsune. A cada segundo,
 * uma delas voa até o inimigo mais próximo (dano + marca brilhante) e, enquanto houver chamas, os aliados
 * por perto se curam. { "wisps": 9, "duration": 240, "range": 10, "damage": 3.5, "heal": 1 }
 */
public final class FoxfireAbility implements Ability {
	public static final String FLAG_PREFIX = "foxfire:";
	private static final Map<UUID, int[]> ACTIVE = new HashMap<>(); // {chamas, idade}
	private static final Map<UUID, FoxfireAbility> OWNERS = new HashMap<>();
	private final int wisps, duration;
	private final double range;
	private final float damage, heal;

	public FoxfireAbility(JsonObject json) {
		this.wisps = JsonParams.integer(json, "wisps", 9);
		this.duration = JsonParams.integer(json, "duration", 240);
		this.range = JsonParams.num(json, "range", 10f);
		this.damage = JsonParams.num(json, "damage", 3.5f);
		this.heal = JsonParams.num(json, "heal", 1f);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(FoxfireAbility::tickAll);
	}

	private static void setCount(ServerPlayerEntity p, int old, int now) {
		if (old > 0) EntityFlags.set(p, FLAG_PREFIX + old, false);
		if (now > 0) EntityFlags.set(p, FLAG_PREFIX + now, true);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		int[] cur = ACTIVE.get(p.getUuid());
		if (cur != null) setCount(p, cur[0], 0);
		ACTIVE.put(p.getUuid(), new int[]{wisps, 0});
		OWNERS.put(p.getUuid(), this);
		setCount(p, 0, wisps);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fan", 18), p);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:foxfire_summon", 0), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_FIRECHARGE_USE, SoundCategory.PLAYERS, 0.6f, 1.6f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.2f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_FOX_AMBIENT, SoundCategory.PLAYERS, 0.7f, 1.4f);
		return true;
	}

	private static void tickAll(MinecraftServer server) {
		if (ACTIVE.isEmpty()) return;
		Iterator<Map.Entry<UUID, int[]>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			var en = it.next();
			ServerPlayerEntity p = server.getPlayerManager().getPlayer(en.getKey());
			FoxfireAbility ab = OWNERS.get(en.getKey());
			int[] st = en.getValue();
			if (p == null || ab == null || !p.isAlive() || st[0] <= 0 || ++st[1] > ab.duration) {
				it.remove();
				OWNERS.remove(en.getKey());
				if (p != null) setCount(p, st[0], 0);
				continue;
			}
			if (st[1] % 20 == 0) {
				// cura dos aliados enquanto as chamas existirem
				for (PlayerEntity o : p.getWorld().getEntitiesByClass(PlayerEntity.class, p.getBoundingBox().expand(6), x -> x.isAlive())) {
					if (o.getHealth() < o.getMaxHealth()) o.heal(ab.heal);
				}
				// uma chama voa até o monstro mais próximo
				LivingEntity best = null;
				double bd = ab.range * ab.range;
				for (LivingEntity e : p.getWorld().getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(ab.range),
						x -> x.isAlive() && x != p && (x instanceof Monster || x instanceof net.minecraft.entity.mob.MobEntity m && m.getTarget() == p))) {
					double d = e.squaredDistanceTo(p);
					if (d < bd && p.canSee(e)) {
						bd = d;
						best = e;
					}
				}
				if (best != null) {
					int old = st[0];
					st[0]--;
					setCount(p, old, st[0]);
					best.timeUntilRegen = 0;
					best.damage(p.getDamageSources().indirectMagic(p, p), ab.damage);
					best.addStatusEffect(new StatusEffectInstance(StatusEffects.GLOWING, 100, 0));
					best.setOnFireFor(2f);
					Vec3d from = p.getPos().add(0, p.getHeight() * 0.8, 0), to = best.getPos().add(0, best.getHeight() / 2, 0);
					ModNetworking.sendNear(p.getServerWorld(), from, 64, new FxPayload("foxfire_shot", from.x, from.y, from.z,
							(float) to.x, (float) to.y, (float) to.z));
					p.getWorld().playSound(null, to.x, to.y, to.z, SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 0.4f, 1.8f);
					p.getWorld().playSound(null, to.x, to.y, to.z, SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.PLAYERS, 0.7f, 1.5f);
				}
			}
		}
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		int[] st = ACTIVE.remove(p.getUuid());
		OWNERS.remove(p.getUuid());
		if (st != null) setCount(p, st[0], 0);
	}
}
