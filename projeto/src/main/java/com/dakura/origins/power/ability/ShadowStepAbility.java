package com.dakura.origins.power.ability;

import com.dakura.origins.network.FxPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

/**
 * "dakura_origins:shadow_step" — dissolve-se em sombra e reaparece à frente (até "range" blocos),
 * invisível por um instante. { "range": 10, "invis": 60 }
 */
public final class ShadowStepAbility implements Ability {
	private final double range;
	private final int invis;

	public ShadowStepAbility(JsonObject json) {
		this.range = JsonParams.num(json, "range", 10f);
		this.invis = JsonParams.integer(json, "invis", 60);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		Vec3d look = p.getRotationVec(1f), flat = new Vec3d(look.x, Math.max(-0.2, Math.min(0.3, look.y)), look.z).normalize();
		Vec3d start = p.getPos().add(0, 0.6, 0), end = start.add(flat.multiply(range));
		var hit = p.getWorld().raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, p));
		Vec3d dest = hit.getType() == HitResult.Type.MISS ? end : hit.getPos().subtract(flat.multiply(0.7));
		dest = dest.add(0, -0.6, 0);
		for (int i = 0; i < 12; i++) {
			Box box = p.getDimensions(p.getPose()).getBoxAt(dest);
			if (p.getWorld().isSpaceEmpty(p, box)) break;
			dest = dest.subtract(flat.multiply(0.5)).add(0, 0.25, 0);
		}
		if (!p.getWorld().isSpaceEmpty(p, p.getDimensions(p.getPose()).getBoxAt(dest))) return false;
		Vec3d from = p.getPos();
		ModNetworking.sendNear(p.getServerWorld(), from, 64, new FxPayload("shadow_step", from.x, from.y, from.z,
				(float) dest.x, (float) dest.y, (float) dest.z));
		p.requestTeleport(dest.x, dest.y, dest.z);
		p.fallDistance = 0;
		p.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, invis, 0, false, false, true));
		p.getWorld().playSound(null, from.x, from.y, from.z, SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.PLAYERS, 0.6f, 0.6f);
		p.getWorld().playSound(null, dest.x, dest.y, dest.z, SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 0.8f, 0.7f);
		return true;
	}
}
