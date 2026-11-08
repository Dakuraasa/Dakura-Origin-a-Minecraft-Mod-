package com.dakura.origins.client.render;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.types.FlowerCrownPower;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.RotationAxis;

/** Tiara de flores em volta da cabeça da Dríade (segue a cabeça, visível para todos). */
public class FlowerCrownFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	private static final ItemStack[] FLOWERS = {
			new ItemStack(Items.POPPY), new ItemStack(Items.OXEYE_DAISY), new ItemStack(Items.CORNFLOWER),
			new ItemStack(Items.DANDELION), new ItemStack(Items.PINK_TULIP), new ItemStack(Items.AZURE_BLUET),
			new ItemStack(Items.ALLIUM), new ItemStack(Items.BLUE_ORCHID)};

	public FlowerCrownFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) {
		super(ctx);
	}

	@Override
	public void render(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float limbAngle,
					   float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
		if (e.isInvisible() || !com.dakura.origins.client.config.DakuraConfig.cosmetic(e, FlowerCrownPower.FLAG)) return;
		var items = MinecraftClient.getInstance().getItemRenderer();
		m.push();
		getContextModel().getHead().rotate(m);
		for (int i = 0; i < FLOWERS.length; i++) {
			double a = i * Math.PI * 2 / FLOWERS.length;
			m.push();
			m.translate(Math.sin(a) * 0.3, -0.5 + (i % 2) * 0.03, Math.cos(a) * 0.3);
			m.multiply(RotationAxis.POSITIVE_Y.rotation((float) a));
			m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180));
			m.scale(0.3f, 0.3f, 0.3f);
			items.renderItem(FLOWERS[i], ModelTransformationMode.FIXED, light, OverlayTexture.DEFAULT_UV, m, vcp, e.getWorld(), i);
			m.pop();
		}
		m.pop();
	}
}
