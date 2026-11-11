package com.dakura.origins.client.render;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.client.config.DakuraConfig;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Orelhas de raposa na cabeça ("fox_ears" ruivas e brancas; "fox_ears_snow" brancas e cinza, mais longas e felpudas).
 * Animação: em pé quando há alguém por perto, caídas quando ela está tranquila, balançando ao andar,
 * com uma "tremidinha" de vez em quando. Somem com a opção de aparências desligada.
 */
public class FoxEarsFeature extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	private static final Identifier FIRE_TEX = DakuraOrigins.id("textures/entity/fox_ears.png");
	private static final Identifier SNOW_TEX = DakuraOrigins.id("textures/entity/fox_ears_snow.png");
	private static final Identifier DARK_TEX = DakuraOrigins.id("textures/entity/fox_ears_dark.png");
	private static final ModelPart FIRE = build(false), SNOW = build(true);
	private static final Map<Integer, float[]> STATE = new HashMap<>(); // perk, último check, havia alguém perto

	public FoxEarsFeature(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) {
		super(ctx);
	}

	private static ModelPart build(boolean snow) {
		ModelData md = new ModelData();
		ModelPartData root = md.getRoot();
		ModelPartBuilder ear = snow
				? ModelPartBuilder.create().uv(0, 0).cuboid(-2f, -4f, -0.5f, 4, 4, 1).uv(0, 5).cuboid(-1.5f, -7f, -0.5f, 3, 3, 1)
				.uv(0, 9).cuboid(-1f, -9f, -0.5f, 2, 2, 1).uv(12, 0).cuboid(-2.6f, -3f, -0.3f, 1, 2, 1)
				: ModelPartBuilder.create().uv(0, 0).cuboid(-1.5f, -3f, -0.5f, 3, 3, 1).uv(0, 4).cuboid(-1f, -5f, -0.5f, 2, 2, 1)
				.uv(0, 7).cuboid(-0.5f, -7f, -0.5f, 1, 2, 1);
		root.addChild("left", ear, ModelTransform.pivot(2.6f, -7.6f, -0.5f));
		root.addChild("right", ear, ModelTransform.pivot(-2.6f, -7.6f, -0.5f));
		return TexturedModelData.of(md, 32, 32).createModel();
	}

	public static void forget(int id) {
		STATE.remove(id);
	}

	@Override
	public void render(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float limbAngle,
					   float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
		if (e.isInvisible()) return;
		boolean dark = DakuraConfig.cosmetic(e, "fox_ears_dark");
		boolean snow = dark || DakuraConfig.cosmetic(e, "fox_ears_snow");
		if (!snow && !DakuraConfig.cosmetic(e, "fox_ears")) return;

		float[] st = STATE.computeIfAbsent(e.getId(), i -> new float[]{0.5f, -100f, 0f});
		if (e.age - st[1] >= 10 || e.age < st[1]) {
			st[1] = e.age;
			st[2] = e.getWorld().getOtherEntities(e, e.getBoundingBox().expand(5), x -> x instanceof LivingEntity).isEmpty() ? 0f : 1f;
		}
		boolean moving = limbDistance > 0.12f;
		float target = st[2] > 0 ? 1f : (moving ? 0.6f : 0.12f);
		st[0] += (target - st[0]) * 0.06f;
		float perk = st[0];

		float droop = (1f - perk) * 0.6f;
		float sway = (float) Math.sin(animationProgress * 0.12f) * 0.035f
				+ limbDistance * (float) Math.sin(limbAngle * 0.6662f) * 0.14f;
		float flick = ((int) animationProgress % 113) < 3 ? 0.22f * MathHelper.sin(animationProgress * 2.5f) : 0f;

		ModelPart model = snow ? SNOW : FIRE;
		ModelPart left = model.getChild("left"), right = model.getChild("right");
		left.pitch = droop + sway;
		right.pitch = droop - sway * 0.8f;
		left.roll = 0.16f + droop * 0.45f + flick;
		right.roll = -0.16f - droop * 0.45f;
		left.yaw = -0.1f;
		right.yaw = 0.1f;

		m.push();
		getContextModel().getHead().rotate(m);
		if (!snow) {
			// Raposa de Fogo: orelhas um pouco maiores, crescendo a partir do topo da cabeça
			m.translate(0, -8f / 16f, 0);
			m.scale(1.18f, 1.18f, 1.18f);
			m.translate(0, 8f / 16f, 0);
		}
		model.render(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(dark ? DARK_TEX : snow ? SNOW_TEX : FIRE_TEX)), light, OverlayTexture.DEFAULT_UV);
		m.pop();
	}
}
