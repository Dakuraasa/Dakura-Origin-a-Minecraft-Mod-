package com.dakura.origins.client.render;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.entity.FeatherEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.ProjectileEntityRenderer;
import net.minecraft.util.Identifier;

/** Pena cortante da Vastaya (mesmo formato de textura da flecha). */
public class FeatherRenderer extends ProjectileEntityRenderer<FeatherEntity> {
	private static final Identifier TEX = DakuraOrigins.id("textures/entity/feather.png");

	public FeatherRenderer(EntityRendererFactory.Context ctx) {
		super(ctx);
	}

	@Override
	public Identifier getTexture(FeatherEntity entity) {
		return TEX;
	}
}
