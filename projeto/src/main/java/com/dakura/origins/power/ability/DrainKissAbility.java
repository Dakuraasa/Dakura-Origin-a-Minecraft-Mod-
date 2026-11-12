package com.dakura.origins.power.ability;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:drain_kiss" — Beijo Drenante (Succubus): a mecânica da mordida da vampira virada beijo, só em aldeões e
 * jogadores. O alvo fica totalmente preso, corações sobem até ela, e ela drena:
 * vida (que ela absorve), armadura (o alvo fica sem proteção por um tempo) e experiência. Ao fim, o alvo fica encantado.
 * { "range": 4.5, "duration": 50, "pulses": 5, "damage": 2.0, "armor": 2.0, "armor_ticks": 600 }
 */
public final class DrainKissAbility implements Ability {
	private static final Map<UUID, int[]> ACTIVE = new HashMap<>(); // idade
	private static final Map<UUID, Object[]> DATA = new HashMap<>(); // {habilidade, alvo}
	private static final List<Object[]> ARMOR_RESTORE = new ArrayList<>(); // {alvo, id, tick}
	private static long ticks;
	private final double range;
	private final int duration, pulses, armorTicks;
	private final float damage, armor;

	public DrainKissAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 4.5f);
		this.duration = JsonParams.integer(json, "duration", 50);
		this.pulses = JsonParams.integer(json, "pulses", 5);
		this.damage = JsonParams.num(json, "damage", 2f);
		this.armor = JsonParams.num(json, "armor", 2f);
		this.armorTicks = JsonParams.integer(json, "armor_ticks", 600);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(DrainKissAbility::tickAll);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		if (ACTIVE.containsKey(p.getUuid())) return false;
		// só aldeões e jogadores: ela se alimenta de prazer e energia vital
		LivingEntity t = TargetUtil.aimed(p, range, e -> e instanceof PlayerEntity || e instanceof net.minecraft.entity.passive.MerchantEntity);
		if (t == null) {
			p.sendMessage(Text.translatable("dakura_origins.message.no_target").formatted(Formatting.GRAY), true);
			return false;
		}
		ACTIVE.put(p.getUuid(), new int[]{0});
		DATA.put(p.getUuid(), new Object[]{this, t});
		com.dakura.origins.power.Control.lock(t, 3);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "reach", duration), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ALLAY_ITEM_TAKEN, SoundCategory.PLAYERS, 1.0f, 0.6f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_AMETHYST_BLOCK_RESONATE, SoundCategory.PLAYERS, 0.8f, 0.7f);
		return true;
	}

	private static void tickAll(MinecraftServer server) {
		ticks++;
		ARMOR_RESTORE.removeIf(o -> {
			if (ticks < (long) o[2]) return false;
			LivingEntity t = (LivingEntity) o[0];
			var inst = t.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
			if (inst != null) inst.removeModifier((Identifier) o[1]);
			return true;
		});
		if (ACTIVE.isEmpty()) return;
		Iterator<Map.Entry<UUID, int[]>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			var en = it.next();
			ServerPlayerEntity p = server.getPlayerManager().getPlayer(en.getKey());
			Object[] d = DATA.get(en.getKey());
			if (p == null || d == null) {
				it.remove();
				DATA.remove(en.getKey());
				continue;
			}
			DrainKissAbility ab = (DrainKissAbility) d[0];
			LivingEntity t = (LivingEntity) d[1];
			int age = ++en.getValue()[0];
			boolean end = age > ab.duration || !p.isAlive() || !t.isAlive() || t.getWorld() != p.getWorld()
					|| t.squaredDistanceTo(p) > (ab.range + 2) * (ab.range + 2);
			if (end) {
				com.dakura.origins.power.Control.release(t);
				it.remove();
				DATA.remove(en.getKey());
				ModNetworking.sendAround(new AnimPayload(p.getId(), "reach", 0), p);
				if (t.isAlive() && t.getWorld() == p.getWorld()) CharmAbility.charmDirect(p, t, 160);
				continue;
			}
			com.dakura.origins.power.Control.lock(t, 3);
			kissFx(p, t);
			int every = Math.max(1, ab.duration / ab.pulses);
			if (age % every != 0) continue;
			// vida
			float before = t.getHealth();
			t.damage(p.getDamageSources().indirectMagic(p, p), ab.damage);
			float drained = Math.max(0.5f, before - t.getHealth());
			p.heal(drained);
			// prazer e energia vital alimentam: enche a fome como a mordida da vampira
			p.getHungerManager().add(Math.max(1, Math.round(drained)), drained * 0.4f);
			// armadura
			var inst = t.getAttributeInstance(EntityAttributes.GENERIC_ARMOR);
			if (inst != null) {
				Identifier id = DakuraOrigins.id("drained_armor_" + age);
				inst.removeModifier(id);
				inst.addTemporaryModifier(new EntityAttributeModifier(id, -ab.armor, EntityAttributeModifier.Operation.ADD_VALUE));
				ARMOR_RESTORE.add(new Object[]{t, id, ticks + ab.armorTicks});
			}
			// experiência
			if (t instanceof PlayerEntity tp && tp.experienceLevel > 0) {
				tp.addExperienceLevels(-1);
				p.addExperienceLevels(1);
			} else p.addExperience(3);
			p.getWorld().playSound(null, t.getX(), t.getY(), t.getZ(), SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 0.5f, 0.6f);
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_GENERIC_DRINK, SoundCategory.PLAYERS, 0.4f, 1.4f);
		}
	}

	/** Corações subindo do alvo até os lábios dela. */
	private static void kissFx(ServerPlayerEntity p, LivingEntity t) {
		if (p.age % 3 != 0) return;
		var w = p.getServerWorld();
		net.minecraft.util.math.Vec3d a = t.getEyePos().add(0, -0.1, 0), b = p.getEyePos().add(0, -0.1, 0);
		double u = (p.age % 15) / 15.0;
		net.minecraft.util.math.Vec3d at = a.lerp(b, u).add(0, Math.sin(u * Math.PI) * 0.4, 0);
		w.spawnParticles(net.minecraft.particle.ParticleTypes.HEART, at.x, at.y, at.z, 1, 0.05, 0.05, 0.05, 0);
		if (p.age % 9 == 0) w.spawnParticles(net.minecraft.particle.ParticleTypes.HEART, t.getX(), t.getBodyY(0.8), t.getZ(), 2, 0.3, 0.3, 0.3, 0);
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		ACTIVE.remove(p.getUuid());
		DATA.remove(p.getUuid());
	}
}
