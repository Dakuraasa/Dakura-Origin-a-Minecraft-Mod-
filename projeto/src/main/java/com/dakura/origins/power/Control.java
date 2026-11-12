package com.dakura.origins.power;

import com.dakura.origins.DakuraOrigins;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Imobilização total: quem está preso não anda, não pula, não ataca, não usa itens nem habilidades.
 * Cada habilidade só chama {@link #lock} enquanto segura o alvo (renovando a cada tick, se quiser).
 * Com âncora, o próprio Control mantém o alvo parado no lugar.
 */
public final class Control {
	private static final Identifier NO_JUMP = DakuraOrigins.id("control_no_jump");
	private static final Map<UUID, Hold> LOCKED = new HashMap<>();
	private static long ticks;

	private record Hold(LivingEntity entity, long until, Vec3d anchor, boolean puppet) {}

	private Control() {}

	/** Prende por {@code duration} ticks; {@code anchor} != null mantém o alvo naquele ponto. */
	public static void lock(LivingEntity e, int duration, Vec3d anchor) {
		if (e == null || e.getWorld().isClient) return;
		Hold old = LOCKED.get(e.getUuid());
		long until = Math.max(ticks + duration, old == null ? 0 : old.until);
		LOCKED.put(e.getUuid(), new Hold(e, until, anchor != null ? anchor : old != null ? old.anchor : null, false));
		var jump = e.getAttributeInstance(EntityAttributes.GENERIC_JUMP_STRENGTH);
		if (jump != null && !jump.hasModifier(NO_JUMP))
			jump.addTemporaryModifier(new EntityAttributeModifier(NO_JUMP, -1.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		freeze(e, anchor);
	}

	/**
	 * Puppet hold: same input lock, but the holder moves the entity by updating {@code anchor} every tick, and the
	 * entity keeps full strength so it can strike for its controller.
	 */
	public static void puppet(LivingEntity e, int duration, Vec3d anchor) {
		if (e == null || e.getWorld().isClient) return;
		LOCKED.put(e.getUuid(), new Hold(e, ticks + duration, anchor, true));
		var jump = e.getAttributeInstance(EntityAttributes.GENERIC_JUMP_STRENGTH);
		if (jump != null && !jump.hasModifier(NO_JUMP))
			jump.addTemporaryModifier(new EntityAttributeModifier(NO_JUMP, -1.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
		move(e, anchor);
	}

	private static void move(LivingEntity e, Vec3d anchor) {
		e.setVelocity(Vec3d.ZERO);
		e.velocityModified = true;
		e.fallDistance = 0;
		if (anchor != null && e.getPos().squaredDistanceTo(anchor) > 0.0004) {
			if (e instanceof ServerPlayerEntity sp) sp.networkHandler.requestTeleport(anchor.x, anchor.y, anchor.z, sp.getYaw(), sp.getPitch());
			else e.setPosition(anchor.x, anchor.y, anchor.z);
		}
	}

	public static void lock(LivingEntity e, int duration) {
		lock(e, duration, null);
	}

	public static boolean isLocked(Entity e) {
		Hold h = e == null ? null : LOCKED.get(e.getUuid());
		return h != null && h.until > ticks;
	}

	public static void release(LivingEntity e) {
		if (LOCKED.remove(e.getUuid()) != null) unjump(e);
	}

	private static void unjump(LivingEntity e) {
		var jump = e.getAttributeInstance(EntityAttributes.GENERIC_JUMP_STRENGTH);
		if (jump != null) jump.removeModifier(NO_JUMP);
	}

	private static void freeze(LivingEntity e, Vec3d anchor) {
		e.setVelocity(Vec3d.ZERO);
		e.velocityModified = true;
		e.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 4, 254, true, false, false));
		e.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 4, 254, true, false, false));
		if (e instanceof MobEntity mob) {
			mob.setTarget(null);
			mob.setAttacking(false);
			mob.getNavigation().stop();
		}
		if (anchor != null && e.getPos().squaredDistanceTo(anchor) > 0.01) {
			if (e instanceof ServerPlayerEntity sp) sp.networkHandler.requestTeleport(anchor.x, anchor.y, anchor.z, sp.getYaw(), sp.getPitch());
			else e.setPosition(anchor.x, anchor.y, anchor.z);
		}
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(Control::tick);
		AttackEntityCallback.EVENT.register((p, w, h, e, hit) -> !w.isClient && isLocked(p) ? ActionResult.FAIL : ActionResult.PASS);
		UseEntityCallback.EVENT.register((p, w, h, e, hit) -> !w.isClient && isLocked(p) ? ActionResult.FAIL : ActionResult.PASS);
		UseBlockCallback.EVENT.register((p, w, h, hit) -> !w.isClient && isLocked(p) ? ActionResult.FAIL : ActionResult.PASS);
		UseItemCallback.EVENT.register((p, w, h) -> !w.isClient && isLocked(p) ? TypedActionResult.fail(p.getStackInHand(h)) : TypedActionResult.pass(p.getStackInHand(h)));
		// presos não ferem ninguém (golpes de monstros, flechas soltas no instante)
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((victim, source, amount) -> !(source.getAttacker() != null && source.getAttacker() == source.getSource()
				&& isLocked(source.getAttacker()) && !LOCKED.get(source.getAttacker().getUuid()).puppet()));
	}

	private static void tick(MinecraftServer server) {
		ticks++;
		if (LOCKED.isEmpty()) return;
		Iterator<Hold> it = LOCKED.values().iterator();
		while (it.hasNext()) {
			Hold h = it.next();
			LivingEntity e = h.entity;
			if (h.until <= ticks || !e.isAlive() || e.isRemoved()) {
				it.remove();
				unjump(e);
				continue;
			}
			if (h.puppet) move(e, h.anchor);
			else freeze(e, h.anchor);
			if (e instanceof PlayerEntity p) p.stopUsingItem();
		}
	}
}
