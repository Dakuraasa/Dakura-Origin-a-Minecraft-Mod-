package com.dakura.origins.client.render;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.client.anim.AbilityAnimations;
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
import net.minecraft.util.math.MathHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Traços da última leva, num atlas próprio (race_features2.png, regiões de 32×32):
 * cauda de sereia (só dentro d'água), orelhas e diadema de Anúbis, asas e crista de fogo da Fênix
 * e orelhas de coelho com física. Formas arredondadas e caricatas.
 */
public class LegendFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	private static final Identifier TEX = DakuraOrigins.id("textures/entity/race_features2.png");
	private static final int[] SCALE = {0, 0}, SCALE_DK = {32, 0}, FIN = {64, 0}, PEARL = {96, 0},
			JACKAL = {0, 32}, JACKAL_IN = {32, 32}, GOLD = {64, 32}, LAPIS = {96, 32},
			FLAME = {0, 64}, FLAME_HOT = {32, 64}, FLAME_RED = {64, 64}, EMBER = {96, 64},
			BUNNY = {0, 96}, BUNNY_IN = {32, 96}, BUNNY_SH = {64, 96};
	private static final ModelPart MERMAID = mermaidTail(), ANUBIS = anubisEars(), FIRE_WING = fireWing(), CREST = crest(),
			BUNNY_EARS = bunnyEars();
	/** Física das orelhas de coelho: {ângulo, velocidade, lado, velocidade lateral}. */
	private static final Map<Integer, float[]> EAR_PHYS = new HashMap<>();

	public LegendFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) {
		super(ctx);
	}

	private static ModelPartBuilder box(int[] uv, float x, float y, float z, float w, float h, float d) {
		return ModelPartBuilder.create().uv(uv[0], uv[1]).cuboid(x, y, z, w, h, d);
	}

	private static ModelPart build(ModelData md) {
		return TexturedModelData.of(md, 128, 128).createModel();
	}

	// ================================================================== modelos

	/** Cauda de sereia: seis gomos de escamas que afinam e uma barbatana dupla, larga e arredondada. */
	private static ModelPart mermaidTail() {
		ModelData md = new ModelData();
		ModelPartData cur = md.getRoot().addChild("t", ModelPartBuilder.create(), ModelTransform.NONE);
		float[] w = {8.2f, 7.6f, 6.6f, 5.4f, 4.2f, 3.0f};
		float[] d = {4.4f, 4.2f, 3.8f, 3.2f, 2.6f, 2.0f};
		for (int i = 0; i < w.length; i++) {
			ModelPartBuilder b = box(i % 2 == 0 ? SCALE : SCALE_DK, -w[i] / 2, 0, -d[i] / 2, w[i], 3.2f, d[i]);
			// gomo "inchado" no meio, para ficar arredondado
			b.uv(SCALE[0], SCALE[1]).cuboid(-w[i] / 2 - 0.3f, 0.8f, -d[i] / 2 - 0.3f, w[i] + 0.6f, 1.6f, d[i] + 0.6f);
			cur = cur.addChild("s" + i, b, ModelTransform.of(0, i == 0 ? 0 : 3.0f, 0, 0, 0, 0));
		}
		// barbatana: dois lóbulos grandes com uma borda clara
		for (int s = -1; s <= 1; s += 2) {
			ModelPartData lobe = cur.addChild(s < 0 ? "fl" : "fr", box(FIN, s < 0 ? -9f : 0f, 0, -0.4f, 9f, 5.5f, 0.8f),
					ModelTransform.of(0, 2.6f, 0, 0, 0, s * -0.55f));
			lobe.addChild("edge", box(PEARL, s < 0 ? -9.6f : 0.4f, 4.6f, -0.45f, 9.2f, 1.6f, 0.9f), ModelTransform.NONE);
			lobe.addChild("ray", box(FIN, s < 0 ? -10.2f : 7.2f, 1.5f, -0.35f, 3.0f, 4.0f, 0.7f), ModelTransform.NONE);
		}
		// barbatanas laterais pequenas no quadril
		ModelPartData root = md.getRoot().getChild("t");
		root.addChild("hl", box(FIN, -7.2f, 0.5f, -0.3f, 3.0f, 3.5f, 0.6f), ModelTransform.of(0, 0, 0, 0, 0, 0.5f));
		root.addChild("hr", box(FIN, 4.2f, 0.5f, -0.3f, 3.0f, 3.5f, 0.6f), ModelTransform.of(0, 0, 0, 0, 0, -0.5f));
		return build(md);
	}

	/** Orelhas de chacal altas com faixas de ouro e um diadema com uma gema de lápis-lazúli. */
	private static ModelPart anubisEars() {
		ModelData md = new ModelData();
		ModelPartData root = md.getRoot();
		for (int s = -1; s <= 1; s += 2) {
			ModelPartData ear = root.addChild(s < 0 ? "l" : "r",
					box(JACKAL, -1.5f, -10f, -0.7f, 3.0f, 10f, 1.4f), ModelTransform.of(s * 2.6f, -7.6f, 0.4f, -0.12f, 0, s * 0.16f));
			ear.addChild("in", box(JACKAL_IN, -0.9f, -8.5f, -0.85f, 1.8f, 7.5f, 0.3f), ModelTransform.NONE);
			ear.addChild("tip", box(JACKAL, -0.9f, -11.2f, -0.6f, 1.8f, 1.4f, 1.2f), ModelTransform.NONE);
			ear.addChild("tip2", box(JACKAL, -0.4f, -12.0f, -0.5f, 0.8f, 1.0f, 1.0f), ModelTransform.NONE);
			ear.addChild("b1", box(GOLD, -1.75f, -2.2f, -0.95f, 3.5f, 0.9f, 1.9f), ModelTransform.NONE);
			ear.addChild("b2", box(GOLD, -1.6f, -4.6f, -0.9f, 3.2f, 0.7f, 1.8f), ModelTransform.NONE);
		}
		ModelPartData band = root.addChild("band", ModelPartBuilder.create(), ModelTransform.NONE);
		band.addChild("f", box(GOLD, -4.4f, -7.3f, -4.45f, 8.8f, 1.4f, 0.5f), ModelTransform.NONE);
		band.addChild("b", box(GOLD, -4.4f, -7.3f, 3.95f, 8.8f, 1.4f, 0.5f), ModelTransform.NONE);
		band.addChild("l", box(GOLD, -4.45f, -7.3f, -4.4f, 0.5f, 1.4f, 8.8f), ModelTransform.NONE);
		band.addChild("r", box(GOLD, 3.95f, -7.3f, -4.4f, 0.5f, 1.4f, 8.8f), ModelTransform.NONE);
		band.addChild("gem", box(LAPIS, -0.9f, -8.0f, -4.85f, 1.8f, 1.8f, 0.6f), ModelTransform.NONE);
		band.addChild("gemc", box(GOLD, -1.3f, -8.4f, -4.7f, 2.6f, 2.6f, 0.3f), ModelTransform.NONE);
		return build(md);
	}

	/** Asa de fogo: um osso e um leque de nove penas que crescem até a ponta. */
	private static ModelPart fireWing() {
		ModelData md = new ModelData();
		ModelPartData w = md.getRoot().addChild("w", box(FLAME_HOT, -0.7f, -0.7f, -0.7f, 9f, 1.4f, 1.4f), ModelTransform.NONE);
		for (int i = 0; i < 9; i++) {
			float len = 6.5f + i * 1.1f - (i > 6 ? (i - 6) * 1.6f : 0);
			int[] uv = i < 3 ? FLAME_HOT : (i < 7 ? FLAME : FLAME_RED);
			ModelPartData f = w.addChild("f" + i, box(uv, -0.9f, 0, -0.25f, 1.8f, len, 0.5f),
					ModelTransform.of(0.4f + i * 1.05f, 0.4f, 0, 0, 0, -0.35f + i * 0.1f));
			f.addChild("r", box(uv == FLAME_HOT ? FLAME : FLAME_RED, -0.6f, len - 0.3f, -0.25f, 1.2f, 1.4f, 0.5f), ModelTransform.NONE);
		}
		return build(md);
	}

	/** Crista de fogo na cabeça: três plumas para trás. */
	private static ModelPart crest() {
		ModelData md = new ModelData();
		ModelPartData root = md.getRoot();
		for (int i = 0; i < 3; i++) {
			float len = 5.5f - Math.abs(i - 1) * 1.5f;
			ModelPartData f = root.addChild("p" + i, box(i == 1 ? FLAME : FLAME_HOT, -0.7f, -len, -0.35f, 1.4f, len, 0.7f),
					ModelTransform.of((i - 1) * 1.6f, -7.6f, 2.5f, -0.75f, 0, (i - 1) * 0.35f));
			f.addChild("t", box(FLAME_RED, -0.45f, -len - 1.1f, -0.3f, 0.9f, 1.3f, 0.6f), ModelTransform.NONE);
		}
		return build(md);
	}

	/** Orelhas de coelho: dois gomos cada (a de cima dobra), parte de dentro rosa, pontas arredondadas. */
	private static ModelPart bunnyEars() {
		ModelData md = new ModelData();
		ModelPartData root = md.getRoot();
		for (int s = -1; s <= 1; s += 2) {
			ModelPartData low = root.addChild(s < 0 ? "l" : "r", box(BUNNY, -1.4f, -6.5f, -0.6f, 2.8f, 6.5f, 1.2f),
					ModelTransform.of(s * 1.9f, -7.6f, 0.6f, -0.1f, 0, s * 0.12f));
			low.addChild("in", box(BUNNY_IN, -0.85f, -6.0f, -0.75f, 1.7f, 5.4f, 0.3f), ModelTransform.NONE);
			low.addChild("base", box(BUNNY_SH, -1.6f, -1.2f, -0.75f, 3.2f, 1.2f, 1.5f), ModelTransform.NONE);
			ModelPartData up = low.addChild("up", box(BUNNY, -1.5f, -6.0f, -0.6f, 3.0f, 6.0f, 1.2f), ModelTransform.of(0, -6.3f, 0, 0, 0, 0));
			up.addChild("in", box(BUNNY_IN, -0.9f, -5.4f, -0.75f, 1.8f, 5.0f, 0.3f), ModelTransform.NONE);
			up.addChild("tip", box(BUNNY, -1.1f, -6.8f, -0.55f, 2.2f, 1.0f, 1.1f), ModelTransform.NONE);
			up.addChild("tip2", box(BUNNY, -0.6f, -7.4f, -0.5f, 1.2f, 0.7f, 1.0f), ModelTransform.NONE);
		}
		return build(md);
	}

	// ================================================================== desenho

	private void renderMermaid(MatrixStack m, VertexConsumer vc, int light, AbstractClientPlayerEntity e, float t) {
		ModelPart root = MERMAID.getChild("t");
		boolean swim = e.isSwimming();
		float speed = swim ? 0.45f : 0.18f, amp = swim ? 0.32f : 0.14f;
		ModelPart seg = root;
		for (int i = 0; i < 6; i++) {
			seg = seg.getChild("s" + i);
			seg.pitch = MathHelper.sin(t * speed - i * 0.75f) * amp * (0.4f + i * 0.15f);
			seg.roll = MathHelper.sin(t * speed * 0.5f - i * 0.5f) * 0.03f;
		}
		float fan = MathHelper.sin(t * speed - 4.5f) * 0.18f;
		seg.getChild("fl").roll = 0.55f + fan;
		seg.getChild("fr").roll = -0.55f - fan;
		m.push();
		getContextModel().body.rotate(m);
		m.translate(0, 11.5f / 16f, 0);
		MERMAID.render(m, vc, light, OverlayTexture.DEFAULT_UV);
		m.pop();
	}

	private void renderPhoenix(MatrixStack m, VertexConsumer vc, AbstractClientPlayerEntity e, float t) {
		boolean air = !e.isOnGround() && !e.isTouchingWater();
		boolean show = air || EntityFlags.clientHas(e, "solar_dive") || EntityFlags.clientHas(e, "reborn")
				|| AbilityAnimations.active("wings_spread").contains(e.getId());
		float open = show ? 1f : 0f;
		float flap = show ? MathHelper.sin(t * (air ? 0.4f : 0.6f)) * 0.4f : MathHelper.sin(t * 0.06f) * 0.04f;
		ModelPart w = FIRE_WING.getChild("w");
		m.push();
		getContextModel().body.rotate(m);
		for (int s = -1; s <= 1; s += 2) {
			// fechadas: dobradas para baixo nas costas; abertas: estendidas e batendo, bem largas
			w.roll = MathHelper.lerp(open, s < 0 ? -1.1f : -1.1f, -0.15f) + flap;
			w.yaw = MathHelper.lerp(open, 1.1f, 0.3f);
			w.pitch = MathHelper.lerp(open, 0.25f, 0f);
			for (int i = 0; i < 9; i++) {
				ModelPart f = w.getChild("f" + i);
				f.roll = MathHelper.lerp(open, -0.1f + i * 0.04f, -0.35f + i * 0.13f) + MathHelper.sin(t * 0.3f + i) * 0.03f;
			}
			m.push();
			m.translate(s * 1.5f / 16f, 2.5f / 16f, 2.4f / 16f);
			float sc = 1.25f;
			m.scale(sc, sc, sc);
			if (s < 0) m.scale(-1, 1, 1);
			w.render(m, vc, 0xF000F0, OverlayTexture.DEFAULT_UV);
			m.pop();
		}
		m.pop();
		m.push();
		getContextModel().getHead().rotate(m);
		for (int i = 0; i < 3; i++) CREST.getChild("p" + i).pitch = -0.75f + MathHelper.sin(t * 0.2f + i) * 0.08f;
		CREST.render(m, vc, 0xF000F0, OverlayTexture.DEFAULT_UV);
		m.pop();
	}

	private float[] earPhysics(AbstractClientPlayerEntity e) {
		float[] st = EAR_PHYS.computeIfAbsent(e.getId(), k -> new float[4]);
		double dx = e.getX() - e.prevX, dz = e.getZ() - e.prevZ, dy = e.getY() - e.prevY;
		double yaw = Math.toRadians(e.bodyYaw);
		double fwd = -dx * Math.sin(yaw) + dz * Math.cos(yaw), side = dx * Math.cos(yaw) + dz * Math.sin(yaw);
		float target = (float) MathHelper.clamp(-fwd * 2.6 + dy * 2.2, -1.2, 0.8);
		float targetS = (float) MathHelper.clamp(side * 2.2, -0.6, 0.6);
		st[1] += ((target - st[0]) * 0.3f - st[1] * 0.18f) * 0.5f;
		st[0] += st[1] * 0.5f;
		st[3] += ((targetS - st[2]) * 0.3f - st[3] * 0.18f) * 0.5f;
		st[2] += st[3] * 0.5f;
		return st;
	}

	private void renderBunny(MatrixStack m, VertexConsumer vc, int light, AbstractClientPlayerEntity e, float t) {
		float[] st = earPhysics(e);
		boolean charge = EntityFlags.clientHas(e, "moon_charge");
		boolean rush = EntityFlags.clientHas(e, "rushing") || EntityFlags.clientHas(e, "moon_air");
		float tremble = charge ? MathHelper.sin(t * 3.1f) * 0.06f : 0f;
		float swept = rush ? -0.9f : 0f;
		for (int s = -1; s <= 1; s += 2) {
			ModelPart low = BUNNY_EARS.getChild(s < 0 ? "l" : "r");
			ModelPart up = low.getChild("up");
			low.pitch = -0.1f + st[0] * 0.5f + swept * 0.6f + tremble;
			low.roll = s * 0.12f + st[2] * 0.4f + s * (charge ? 0.25f : 0f);
			// a orelha direita é "dobrada" de desenho animado; a esquerda só balança
			float flop = s > 0 && !rush ? 0.95f : 0.1f;
			up.pitch = flop * 0.6f + st[0] * 0.7f + swept * 0.4f + MathHelper.sin(t * 0.15f + s) * 0.05f;
			up.roll = s > 0 && !rush ? 0.5f : 0f;
		}
		m.push();
		getContextModel().getHead().rotate(m);
		BUNNY_EARS.render(m, vc, light, OverlayTexture.DEFAULT_UV);
		m.pop();
	}

	@Override
	public void render(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float limbAngle,
					   float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
		if (e.isInvisible()) return;
		boolean mermaid = DakuraConfig.cosmetic(e, "mermaid_tail") && e.isTouchingWater();
		boolean anubis = DakuraConfig.cosmetic(e, "anubis_ears");
		boolean phoenix = DakuraConfig.cosmetic(e, "phoenix_wings");
		boolean bunny = DakuraConfig.cosmetic(e, "bunny_ears");
		if (!mermaid && !anubis && !phoenix && !bunny) return;
		VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEX));
		if (mermaid) renderMermaid(m, vc, light, e, animationProgress);
		if (anubis) {
			float twitch = ((int) animationProgress % 113) < 3 ? 0.15f * MathHelper.sin(animationProgress * 2.5f) : 0f;
			ANUBIS.getChild("l").roll = -0.16f - twitch;
			ANUBIS.getChild("r").roll = 0.16f;
			m.push();
			getContextModel().getHead().rotate(m);
			ANUBIS.render(m, vc, light, OverlayTexture.DEFAULT_UV);
			m.pop();
		}
		if (phoenix) renderPhoenix(m, vc, e, animationProgress);
		if (bunny) renderBunny(m, vc, light, e, animationProgress);
	}
}
