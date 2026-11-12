package com.dakura.origins.client.render;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.client.fx.FxGlow;
import com.dakura.origins.client.geo.GeoModel;
import com.dakura.origins.entity.GeoProjectileEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

/**
 * Desenha o projétil 3D girando: o caldeirão venenoso (gira em arco, com veneno roxo pingando)
 * e o bloco de magma verde (gira rápido, com aura e rastro verdes desenhados como malha).
 */
public class GeoProjectileRenderer extends EntityRenderer<GeoProjectileEntity> {
	private static final Identifier CAULDRON = DakuraOrigins.id("textures/entity/geo/toxic_cauldron.png");
	private static final Identifier MAGMA = DakuraOrigins.id("textures/entity/geo/magma_core.png");

	public GeoProjectileRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
	}

	@Override
	public void render(GeoProjectileEntity e, float yaw, float tickDelta, MatrixStack m, VertexConsumerProvider vcp, int light) {
		float t = e.age + tickDelta;
		if (e.kind() == GeoProjectileEntity.MANA || e.kind() == GeoProjectileEntity.DARK || e.kind() == GeoProjectileEntity.VIOLET) nativeTrail(e);
		if (e.kind() == GeoProjectileEntity.SWORD) {
			m.push();
			m.translate(0, 0.25, 0);
			com.dakura.origins.client.fx.ElfFx.sword(m, vcp, e.getVelocity(), t, false);
			m.pop();
			if (e.getWorld().random.nextInt(2) == 0) e.getWorld().addParticle(e.getWorld().random.nextBoolean()
					? net.minecraft.particle.ParticleTypes.WITCH : net.minecraft.particle.ParticleTypes.SQUID_INK, e.getX(), e.getY() + 0.25, e.getZ(), 0, 0, 0);
			super.render(e, yaw, tickDelta, m, vcp, light);
			return;
		}
		if (e.kind() == GeoProjectileEntity.MOON) {
			com.dakura.origins.client.fx.ElfFx.moon(m, vcp, t, e.size());
			super.render(e, yaw, tickDelta, m, vcp, light);
			return;
		}
		if (e.kind() == GeoProjectileEntity.VIOLET) {
			FxGlow.manaOrb(m, vcp, t, e.getVelocity(), e.size(), true);
			super.render(e, yaw, tickDelta, m, vcp, light);
			return;
		}
		if (e.kind() == GeoProjectileEntity.MANA) {
			FxGlow.manaOrb(m, vcp, t, e.getVelocity(), e.size());
			super.render(e, yaw, tickDelta, m, vcp, light);
			return;
		}
		if (e.kind() == GeoProjectileEntity.DARK) {
			com.dakura.origins.client.fx.DarkFx.comet(m, vcp, t, e.getVelocity(), e.size());
			super.render(e, yaw, tickDelta, m, vcp, light);
			return;
		}
		if (e.kind() == GeoProjectileEntity.ARCANE) {
			FxGlow.arcaneOrb(m, vcp, t, e.getVelocity());
			super.render(e, yaw, tickDelta, m, vcp, light);
			return;
		}
		boolean magma = e.kind() == GeoProjectileEntity.MAGMA;
		GeoModel model = GeoModel.get(magma ? "magma_core" : "toxic_cauldron");
		m.push();
		m.translate(0, magma ? 0.3 : 0.15, 0);
		m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(t * (magma ? 23 : 9)));
		m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(t * (magma ? 31 : 14)));
		float s = (magma ? 0.9f : 0.55f) * e.size();
		m.scale(s, -s, -s);
		if (!magma) m.translate(0, -1.0, 0);
		model.render(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(getTexture(e))), magma ? 0xF000F0 : light);
		m.pop();
		if (magma) FxGlow.magmaAura(m, vcp, t, e.getVelocity());
		else FxGlow.poisonDrip(m, vcp, t);
		super.render(e, yaw, tickDelta, m, vcp, light);
	}

	@Override
	public Identifier getTexture(GeoProjectileEntity e) {
		return e.kind() == GeoProjectileEntity.MAGMA ? MAGMA : CAULDRON;
	}

	/** Rastro nativo do Minecraft atrás das esferas da Kitsune (mana azul) e da Corrompida (cometa negro). */
	private static void nativeTrail(GeoProjectileEntity e) {
		var w = e.getWorld();
		var r = w.random;
		boolean dark = e.kind() == GeoProjectileEntity.DARK;
		if (e.kind() == GeoProjectileEntity.VIOLET) {
			var r0 = w.random;
			for (int i = 0; i < 4; i++) w.addParticle(i % 2 == 0 ? net.minecraft.particle.ParticleTypes.WITCH : net.minecraft.particle.ParticleTypes.GLOW,
					e.getX() + r0.nextGaussian() * 0.18, e.getY() + 0.3 + r0.nextGaussian() * 0.18, e.getZ() + r0.nextGaussian() * 0.18,
					-e.getVelocity().x * 0.05, r0.nextGaussian() * 0.03, -e.getVelocity().z * 0.05);
			if (r0.nextInt(2) == 0) w.addParticle(net.minecraft.particle.ParticleTypes.REVERSE_PORTAL, e.getX(), e.getY() + 0.3, e.getZ(), 0, 0, 0);
			return;
		}
		for (int i = 0; i < 4; i++) {
			double x = e.getX() + r.nextGaussian() * 0.18, y = e.getY() + 0.3 + r.nextGaussian() * 0.18, z = e.getZ() + r.nextGaussian() * 0.18;
			double vx = -e.getVelocity().x * 0.05 + r.nextGaussian() * 0.03, vy = r.nextGaussian() * 0.03, vz = -e.getVelocity().z * 0.05 + r.nextGaussian() * 0.03;
			net.minecraft.particle.ParticleEffect fx = dark
					? switch (i) { case 0 -> net.minecraft.particle.ParticleTypes.REVERSE_PORTAL; case 1 -> net.minecraft.particle.ParticleTypes.SOUL_FIRE_FLAME;
						case 2 -> net.minecraft.particle.ParticleTypes.WITCH; default -> net.minecraft.particle.ParticleTypes.SQUID_INK; }
					: switch (i) { case 0 -> net.minecraft.particle.ParticleTypes.END_ROD; case 1 -> net.minecraft.particle.ParticleTypes.GLOW;
						case 2 -> net.minecraft.particle.ParticleTypes.SNOWFLAKE; default -> net.minecraft.particle.ParticleTypes.ELECTRIC_SPARK; };
			w.addParticle(fx, x, y, z, vx, vy, vz);
		}
		if (r.nextInt(3) == 0) w.addParticle(dark ? net.minecraft.particle.ParticleTypes.DRAGON_BREATH : net.minecraft.particle.ParticleTypes.FIREWORK,
				e.getX(), e.getY() + 0.3, e.getZ(), 0, 0, 0);
	}
}
