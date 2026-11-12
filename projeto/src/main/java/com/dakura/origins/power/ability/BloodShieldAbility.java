package com.dakura.origins.power.ability;

import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.types.MaturityPower;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * "dakura_origins:blood_shield" — Égide de Sangue (Demônio): abre à frente o selo do pentagrama (mesma estrutura e postura
 * do escudo do Elfo Real) e dele saem DUAS correntes de sangue, cada uma presa a um inimigo diferente a até 5 blocos.
 * Por 5 segundos o escudo bloqueia golpes vindos da frente; os acorrentados levam dano contínuo, queimam e têm a vida
 * drenada para o Demônio (mais forte a cada fase de maturidade). Ao fim, escudo, correntes e vínculos somem juntos.
 * { "duration": 100, "range": 5, "every": 10, "damage": 1.5, "heal": 0.6, "burn": 3, "maturity_damage": [1,1.25,1.5,1.8,2.2] }
 */
public final class BloodShieldAbility implements Ability {
	public static final String FLAG = "demon_shield";
	private static final Set<UUID> ACTIVE = new HashSet<>();
	private static final DustParticleEffect BLOOD = new DustParticleEffect(new Vector3f(0.55f, 0.02f, 0.05f), 1.2f);
	private final int duration, every, burn;
	private final double range;
	private final float damage, heal;
	private final float[] maturity;

	public BloodShieldAbility(JsonObject json) {
		this.duration = JsonParams.integer(json, "duration", 100);
		this.range = JsonParams.num(json, "range", 5f);
		this.every = JsonParams.integer(json, "every", 10);
		this.damage = JsonParams.num(json, "damage", 1.5f);
		this.heal = JsonParams.num(json, "heal", 0.6f);
		this.burn = JsonParams.integer(json, "burn", 3);
		float[] m = {1f, 1.25f, 1.5f, 1.8f, 2.2f};
		if (json.has("maturity_damage")) {
			JsonArray a = json.getAsJsonArray("maturity_damage");
			m = new float[a.size()];
			for (int i = 0; i < m.length; i++) m[i] = a.get(i).getAsFloat();
		}
		this.maturity = m;
	}

	public static void bootstrap() {
		// proteção frontal: golpes de quem está à frente do escudo não passam
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((e, src, amount) -> {
			if (!(e instanceof ServerPlayerEntity p) || !ACTIVE.contains(p.getUuid())) return true;
			Vec3d from = src.getPosition();
			if (from == null && src.getAttacker() != null) from = src.getAttacker().getPos();
			if (from == null) return true;
			Vec3d to = from.subtract(p.getPos()).multiply(1, 0, 1);
			Vec3d look = LegendKit.flat(p.getRotationVec(1f));
			if (to.lengthSquared() < 1e-4 || look.lengthSquared() < 1e-4 || to.normalize().dotProduct(look.normalize()) < 0.35) return true;
			if (src.getSource() != null && src.getSource() != src.getAttacker()) src.getSource().discard();
			p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ITEM_SHIELD_BLOCK, SoundCategory.PLAYERS, 0.8f, 0.7f);
			return false;
		});
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		if (ACTIVE.contains(p.getUuid()) && !LegendKit.running(p, "blood_shield")) ACTIVE.remove(p.getUuid()); // sobra de desconexão
		if (ACTIVE.contains(p.getUuid())) return false;
		ServerWorld w = p.getServerWorld();
		// os dois inimigos mais próximos ao alcance (monstros, criaturas e jogadores), um por corrente
		List<LivingEntity> near = new ArrayList<>(w.getEntitiesByClass(LivingEntity.class, p.getBoundingBox().expand(range),
				x -> x != p && x.isAlive() && !x.isSpectator() && x.squaredDistanceTo(p) <= range * range
						&& !(x instanceof net.minecraft.entity.decoration.ArmorStandEntity)
						&& !(x instanceof net.minecraft.entity.passive.TameableEntity t && p.getUuid().equals(t.getOwnerUuid()))
						&& (x instanceof PlayerEntity || LegendKit.enemy(p, x)) && p.canSee(x)));
		near.sort(java.util.Comparator.comparingDouble(x -> x.squaredDistanceTo(p)));
		List<Integer> bound = new ArrayList<>();
		for (int i = 0; i < Math.min(2, near.size()); i++) bound.add(near.get(i).getId());

		ACTIVE.add(p.getUuid());
		EntityFlags.set(p, FLAG, true);
		Channeling.set(p, true);
		for (int id : bound) EntityFlags.set(p, "bchain#" + id, true);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "reach", duration), p);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1f, 0.6f);
		w.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_CHAIN_PLACE, SoundCategory.PLAYERS, 1f, 0.6f);
		float mult = MaturityPower.multiplier(p, maturity);

		LegendKit.run(p, "blood_shield", duration, (pl, age) -> {
			for (int i = bound.size() - 1; i >= 0; i--) {
				int id = bound.get(i);
				// alvo morto, longe demais, fora do mundo ou desconectado: só aquela corrente se desfaz
				if (!(w.getEntityById(id) instanceof LivingEntity t) || !t.isAlive() || t.isRemoved() || t.getWorld() != pl.getWorld()
						|| t.squaredDistanceTo(pl) > (range + 2) * (range + 2)) {
					EntityFlags.set(pl, "bchain#" + id, false);
					bound.remove(i);
					continue;
				}
				// presa pela corrente: lenta e puxada de volta se tentar passar do alcance
				t.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 6, 2, true, false));
				double d = t.distanceTo(pl);
				if (d > range) {
					Vec3d pull = pl.getPos().subtract(t.getPos()).normalize().multiply(Math.min(0.6, (d - range) * 0.5));
					t.addVelocity(pull.x, 0.05, pull.z);
					t.velocityModified = true;
				}
				if (age % every == 0) {
					float before = t.getHealth();
					LegendKit.hurt(pl, t, damage * mult, false);
					t.setOnFireFor(burn);
					float drained = Math.max(0f, before - t.getHealth());
					if (drained > 0) pl.heal(drained * heal * mult);
					Vec3d at = t.getPos().add(0, t.getHeight() * 0.6, 0);
					w.spawnParticles(BLOOD, at.x, at.y, at.z, 6, 0.25, 0.3, 0.25, 0);
					w.spawnParticles(net.minecraft.particle.ParticleTypes.DAMAGE_INDICATOR, at.x, at.y, at.z, 1, 0.1, 0.1, 0.1, 0.1);
				}
			}
			if (age % 20 == 0) w.playSound(null, pl.getX(), pl.getY(), pl.getZ(), SoundEvents.BLOCK_CHAIN_STEP, SoundCategory.PLAYERS, 0.8f, 0.5f);
			return true;
		}, pl -> {
			for (int id : bound) EntityFlags.set(pl, "bchain#" + id, false);
			bound.clear();
			end(pl);
		});
		return true;
	}

	private static void end(ServerPlayerEntity p) {
		ACTIVE.remove(p.getUuid());
		EntityFlags.set(p, FLAG, false);
		Channeling.set(p, false);
		ModNetworking.sendAround(new AnimPayload(p.getId(), "reach", 0), p);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.6f, 0.6f);
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		LegendKit.stop(p, "blood_shield");
		if (ACTIVE.contains(p.getUuid())) end(p);
	}
}
