package com.dakura.origins.client.render;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.client.config.DakuraConfig;
import com.dakura.origins.power.EntityFlags;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.ModelPartBuilder;
import net.minecraft.client.model.ModelPartData;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Small race features drawn from one atlas (race_features.png): the Minotaur's horns and the Doll's wind-up key.
 * Hidden when "Race appearance" is turned off.
 */
public class RaceFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	private static final Identifier TEX = DakuraOrigins.id("textures/entity/race_features.png");
	// regiões do atlas (16×16 cada)
	private static final int[] IVORY = {0, 0}, TAN = {16, 0}, TIP = {32, 0}, BLACK = {48, 0}, BLACK_HI = {0, 16},
			STONE = {16, 16}, STONE_DK = {32, 16}, STONE_HI = {48, 16}, CAT = {0, 32}, PINK = {16, 32}, WOLF = {32, 32},
			WOLF_IN = {48, 32}, RED = {0, 48}, CREAM = {16, 48}, RED_DK = {32, 48},
			SUC = {64, 0}, SUC_HI = {80, 0}, SUC_TIP = {96, 0}, TUSK = {112, 0},
			WING = {64, 16}, WING_SH = {80, 16}, WING_TIP = {96, 16}, WING_BONE = {112, 16},
			FAIRY = {64, 32}, FAIRY_VEIN = {80, 32}, FAIRY_GLOW = {96, 32}, BRASS = {112, 32},
			FUR = {64, 48}, FUR_SH = {80, 48}, HALO = {96, 48}, ONI = {112, 48};
	private static final ModelPart BULL = bull(), DOLL_KEY = dollKey();

	public RaceFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) {
		super(ctx);
	}

	private static ModelPartBuilder box(int[] uv, float x, float y, float z, float w, float h, float d) {
		return ModelPartBuilder.create().uv(uv[0], uv[1]).cuboid(x, y, z, w, h, d);
	}

	/**
	 * Chifre "liso": vários segmentos curtos que afinam e curvam aos poucos (em vez de 2 ou 3 blocos),
	 * o que dá um contorno arredondado, mais perto dos modelos polidos do Pixelmon.
	 */
	private static void horn(ModelPartData parent, String name, int s, float px, float py, float pz, int segs, float len,
							 float thick, float taper, float yawStep, float rollStep, float pitchStep, int[][] uvs, float baseYaw, float basePitch) {
		ModelPartData cur = parent.addChild(name, ModelPartBuilder.create(), ModelTransform.of(px, py, pz, basePitch, baseYaw, 0));
		float t = thick;
		for (int i = 0; i < segs; i++) {
			int[] uv = uvs[Math.min(uvs.length - 1, i * uvs.length / segs)];
			float x0 = s > 0 ? 0 : -len;
			cur = cur.addChild("s" + i, box(uv, x0, -t / 2, -t / 2, len, t, t),
					ModelTransform.of(i == 0 ? 0 : s * (len - 0.15f), 0, 0, pitchStep, s * yawStep, s * rollStep));
			t = Math.max(0.35f, t * taper);
		}
	}

	private static ModelPart bull() {
		ModelData md = new ModelData();
		ModelPartData root = md.getRoot();
		int[][] uvs = {IVORY, IVORY, TAN, TAN, TIP};
		for (int s = -1; s <= 1; s += 2) {
			horn(root, s > 0 ? "l" : "r", s, s * 3.6f, -6.4f, -1f, 7, 1.15f, 2.1f, 0.85f, -0.05f, -0.24f, 0, uvs, s * -0.2f, 0);
		}
		return TexturedModelData.of(md, 128, 64).createModel();
	}

	/** Chave de corda da Boneca nas costas (gira sem parar) + juntas de boneca nos ombros. */
	/**
	 * Chave de corda da Boneca: haste com anéis, cubo central e as duas "asas" da borboleta com cantos arredondados
	 * (degraus de cubos) e um miolo escuro vazado. Gira no eixo da haste.
	 */
	private static ModelPart dollKey() {
		ModelData md = new ModelData();
		ModelPartData root = md.getRoot();
		ModelPartData key = root.addChild("key", box(BRASS, -0.45f, -0.45f, 0, 0.9f, 0.9f, 3.2f), ModelTransform.of(0, 4.5f, 2.0f, 0, 0, 0));
		key.addChild("collar", box(STONE_DK, -0.8f, -0.8f, 0.2f, 1.6f, 1.6f, 0.5f), ModelTransform.NONE);
		key.addChild("ring", box(BRASS, -0.65f, -0.65f, 1.5f, 1.3f, 1.3f, 0.35f), ModelTransform.NONE);
		ModelPartData head = key.addChild("head", box(BRASS, -0.75f, -0.75f, 2.9f, 1.5f, 1.5f, 1.2f), ModelTransform.NONE);
		head.addChild("cap", box(STONE_DK, -0.4f, -0.4f, 4.05f, 0.8f, 0.8f, 0.25f), ModelTransform.NONE);
		for (int s = -1; s <= 1; s += 2) {
			String n = s > 0 ? "l" : "r";
			head.addChild(n + "_core", box(BRASS, x(s, 0.7f, 2.6f), -1.3f, 3.0f, 2.6f, 2.6f, 0.9f), ModelTransform.NONE);
			head.addChild(n + "_top", box(BRASS, x(s, 1.0f, 2.0f), -1.8f, 3.0f, 2.0f, 0.5f, 0.9f), ModelTransform.NONE);
			head.addChild(n + "_bot", box(BRASS, x(s, 1.0f, 2.0f), 1.3f, 3.0f, 2.0f, 0.5f, 0.9f), ModelTransform.NONE);
			head.addChild(n + "_out", box(BRASS, x(s, 3.3f, 0.5f), -0.9f, 3.0f, 0.5f, 1.8f, 0.9f), ModelTransform.NONE);
			head.addChild(n + "_hole", box(STONE_DK, x(s, 1.5f, 1.2f), -0.6f, 2.95f, 1.2f, 1.2f, 1.0f), ModelTransform.NONE);
		}
		for (int s = -1; s <= 1; s += 2) {
			root.addChild(s > 0 ? "jl" : "jr", box(STONE_DK, -1.1f, -1.1f, -1.1f, 2.2f, 2.2f, 2.2f), ModelTransform.of(s * 4.6f, 1.6f, 0, 0, 0.78f, 0));
		}
		return TexturedModelData.of(md, 128, 64).createModel();
	}

	/** Início em x de um cubo espelhado para o lado s (distância "from" do centro, largura w). */
	private static float x(int s, float from, float w) {
		return s > 0 ? from : -from - w;
	}

	@Override
	public void render(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float limbAngle,
					   float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
		if (e.isInvisible()) return;
		VertexConsumer vc = null;
		if (DakuraConfig.cosmetic(e, "bull_horns")) {
			vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEX));
			m.push();
			getContextModel().getHead().rotate(m);
			BULL.render(m, vc, light, OverlayTexture.DEFAULT_UV);
			m.pop();
		}

		if (DakuraConfig.cosmetic(e, "doll_key")) {
			VertexConsumer kv = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEX));
			boolean dancing = EntityFlags.clientHas(e, "dancing");
			DOLL_KEY.getChild("key").roll = animationProgress * (dancing ? 0.6f : 0.06f);
			m.push();
			getContextModel().body.rotate(m);
			DOLL_KEY.render(m, kv, light, OverlayTexture.DEFAULT_UV);
			m.pop();
		}
	}
}
