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
}
