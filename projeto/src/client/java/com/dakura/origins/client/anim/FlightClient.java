package com.dakura.origins.client.anim;

import com.dakura.origins.client.config.DakuraConfig;
import com.dakura.origins.power.EntityFlags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.joml.Vector3f;

import java.util.Set;

/**
 * Voo dinâmico no cliente (o movimento do jogador é do cliente): planando como élitro, segurar "para frente" bate as
 * asas e empurra na direção do olhar, até a velocidade máxima da raça. Correndo, mais força. Sem foguete.
 * Também desenha o rastro de cometa do Dragão e da Fênix em voo (para todos os jogadores visíveis).
 */
public final class FlightClient {
	private static final DustParticleEffect GREEN = new DustParticleEffect(new Vector3f(0.3f, 1f, 0.55f), 1.3f);
	private static final DustParticleEffect GOLD = new DustParticleEffect(new Vector3f(1f, 0.75f, 0.2f), 1.4f);

	private FlightClient() {}

	/** {impulso, velocidade máxima} da flag "flier#a#b", ou null. */
	private static float[] params(PlayerEntity p) {
		Set<String> fl = EntityFlags.CLIENT.get(p.getId());
		if (fl == null) return null;
		for (String f : fl) {
			if (!f.startsWith("flier#")) continue;
			String[] a = f.split("#");
			try {
				return new float[]{Float.parseFloat(a[1]), Float.parseFloat(a[2]), a.length > 3 && a[3].equals("hover") ? 1f : 0f};
			} catch (Exception ex) {
				return null;
			}
		}
		return null;
	}

	/** Free flight: forward pushes along the look direction, jump climbs, no input hovers in place. */
	private static void hover(MinecraftClient mc, ClientPlayerEntity me, float[] fp) {
		float thrust = fp[0] * (me.isSprinting() || mc.options.sprintKey.isPressed() ? 1.5f : 1f);
		Vec3d v = me.getVelocity();
		boolean forward = me.input.movementForward > 0, back = me.input.movementForward < 0;
		if (forward) v = v.add(me.getRotationVector().multiply(thrust));
		else if (back) v = v.multiply(0.8, 1, 0.8);
		else v = v.multiply(0.86, 1, 0.86);
		if (me.input.jumping) v = new Vec3d(v.x, Math.min(v.y + thrust * 1.4, fp[1] * 0.45), v.z);
		else if (!forward) v = new Vec3d(v.x, v.y * 0.5 + 0.035, v.z);
		double len = v.length();
		if (len > fp[1]) v = v.multiply(fp[1] / len);
		me.setVelocity(v);
	}

	public static void tick(MinecraftClient mc) {
		if (mc.world == null) return;
		ClientPlayerEntity me = mc.player;
		if (me != null && me.isFallFlying() && !me.isSneaking()) {
			float[] fp = params(me);
			if (fp == null && (DakuraConfig.cosmetic(me, "phoenix_trail")
					|| DakuraConfig.cosmetic(me, "dragon_red") || DakuraConfig.cosmetic(me, "dragon_brown")))
				fp = new float[]{0.06f, 1.8f, 0f};
			if (fp != null && fp[2] > 0) {
				hover(mc, me, fp);
			} else if (fp != null && (me.input.movementForward > 0 || me.input.jumping)) {
				float thrust = fp[0] * (me.isSprinting() || mc.options.sprintKey.isPressed() ? 1.5f : 1f);
				Vec3d look = me.getRotationVector();
				Vec3d v = me.getVelocity().add(look.multiply(thrust));
				if (me.input.jumping) v = v.add(0, thrust * 0.6, 0);
				double len = v.length();
				if (len > fp[1]) v = v.multiply(fp[1] / len);
				me.setVelocity(v);
			}
		}
		// rastro de cometa
		Random r = mc.world.random;
		for (PlayerEntity p : mc.world.getPlayers()) {
			if (!p.isFallFlying() || p.isInvisible()) continue;
			boolean red = DakuraConfig.cosmetic(p, "dragon_red"), brown = DakuraConfig.cosmetic(p, "dragon_brown"),
					phoenix = DakuraConfig.cosmetic(p, "phoenix_trail");
			if (!red && !brown && !phoenix) continue;
			Vec3d c = p.getPos().add(0, 0.4, 0), back = p.getVelocity().multiply(-1.2);
			double sp = p.getVelocity().length();
			int n = 2 + (int) (sp * 4);
			for (int i = 0; i < n; i++) {
				double k = r.nextDouble();
				Vec3d at = c.add(back.multiply(k)).add(r.nextGaussian() * 0.25, r.nextGaussian() * 0.25, r.nextGaussian() * 0.25);
				ParticleEffect fx = brown ? (i % 3 == 0 ? GREEN : ParticleTypes.SOUL_FIRE_FLAME)
						: phoenix ? (i % 3 == 0 ? ParticleTypes.END_ROD : i % 3 == 1 ? GOLD : ParticleTypes.FLAME)
						: (i % 3 == 0 ? ParticleTypes.SMALL_FLAME : ParticleTypes.FLAME);
				mc.world.addParticle(fx, at.x, at.y, at.z, back.x * 0.05, back.y * 0.05 + 0.01, back.z * 0.05);
			}
			if (r.nextInt(3) == 0) mc.world.addParticle(brown ? ParticleTypes.GLOW : ParticleTypes.LAVA, c.x, c.y, c.z, 0, 0, 0);
			if (sp > 1.0 && r.nextInt(2) == 0) mc.world.addParticle(ParticleTypes.ELECTRIC_SPARK, c.x, c.y + 0.3, c.z, 0, 0, 0);
		}
	}
}
