package com.dakura.origins.power.types;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.google.gson.JsonObject;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * "dakura_origins:flight" — voo de verdade (pulo duplo para levantar voo, como no criativo), mais lento,
 * gastando um pouco de fome enquanto voa. Sem fome, não decola.
 * <pre>
 * {
 *   "speed": 0.035, "exhaustion": 0.02,
 *   "style": "creative" | "elytra",   // creative = voo do criativo (padrão); elytra = segue o olhar, como a Elytra
 *   "lean":  "never" | "sprint" | "always", // quando o corpo deita no ar (pose de Elytra / de pássaro)
 *   "cruise": 0.55, "fast": 1.0       // só no estilo elytra: velocidade normal e com sprint (blocos/tick)
 * }
 * </pre>
 * Também publica dois estados visuais para o cliente (asas e pose): "fly_air" (voando de fato) e "fly_lean"
 * (voando com o corpo deitado).
 */
public final class FlightPower implements RacePower {
	public static final String FLAG_AIR = "fly_air";
	public static final String FLAG_LEAN = "fly_lean";

	private final float speed, exhaustion, cruise, fast;
	private final boolean elytra;
	private final String lean;

	public FlightPower(JsonObject json) {
		this.speed = JsonParams.num(json, "speed", 0.035f);
		this.exhaustion = JsonParams.num(json, "exhaustion", 0.02f);
		this.elytra = "elytra".equals(JsonParams.str(json, "style", "creative"));
		this.lean = JsonParams.str(json, "lean", "never");
		this.cruise = JsonParams.num(json, "cruise", 0.55f);
		this.fast = JsonParams.num(json, "fast", 1.0f);
	}

	@Override
	public void tick(ServerPlayerEntity p) {
		var a = p.getAbilities();
		boolean airborne = a.flying && !p.isOnGround();
		EntityFlags.set(p, FLAG_AIR, airborne);
		EntityFlags.set(p, FLAG_LEAN, airborne && ("always".equals(lean) || ("sprint".equals(lean) && p.isSprinting())));
		if (p.isCreative() || p.isSpectator()) return;
		boolean can = p.getHungerManager().getFoodLevel() > 0 && !EntityFlags.has(p, StatuePower.FLAG)
				&& !EntityFlags.has(p, "meteor");
		boolean changed = false;
		if (a.allowFlying != can) {
			a.allowFlying = can;
			if (!can) a.flying = false;
			changed = true;
		}
		if (Math.abs(a.getFlySpeed() - speed) > 1e-4) {
			a.setFlySpeed(speed);
			changed = true;
		}
		if (changed) p.sendAbilitiesUpdate();
		if (a.flying) {
			p.addExhaustion(exhaustion);
			p.fallDistance = 0;
			if (elytra) elytraGlide(p);
			if (p.age % 14 == 0) p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(),
					net.minecraft.sound.SoundEvents.ENTITY_PHANTOM_FLAP, net.minecraft.sound.SoundCategory.PLAYERS, 0.25f, 0.55f);
		}
	}

	/**
	 * Voo estilo Elytra: a velocidade acompanha a direção do olhar (suavizada) em vez de ficar parada no ar.
	 * Sprint = voo rápido; agachar = freia (pairar). O voo do criativo continua por baixo, só que com
	 * "speed" muito baixo no JSON para os controles vanilla não brigarem com isto.
	 */
	private void elytraGlide(ServerPlayerEntity p) {
		Vec3d look = p.getRotationVec(1f);
		boolean brake = p.isSneaking();
		double target = brake ? 0.03 : (p.isSprinting() ? fast : cruise);
		Vec3d v = p.getVelocity();
		Vec3d want = look.multiply(target);
		double k = brake ? 0.25 : 0.12;
		p.setVelocity(v.add(want.subtract(v).multiply(k)));
		p.velocityModified = true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		EntityFlags.set(p, FLAG_AIR, false);
		EntityFlags.set(p, FLAG_LEAN, false);
		if (p.isCreative() || p.isSpectator()) return;
		var a = p.getAbilities();
		a.allowFlying = false;
		a.flying = false;
		a.setFlySpeed(0.05f);
		p.sendAbilitiesUpdate();
	}
}
