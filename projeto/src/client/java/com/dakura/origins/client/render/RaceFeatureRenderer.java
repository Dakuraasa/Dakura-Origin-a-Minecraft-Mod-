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
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Traços opcionais das raças novas, todos de um atlas só (race_features.png):
 * chifres de boi (Minotauro), chifres negros (Demônio), asas de pedra (Gárgula),
 * orelhas de gato / de lobo e penas (Vastaya). Somem com "Aparências de raça" desligada.
 */
public class RaceFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	private static final Identifier TEX = DakuraOrigins.id("textures/entity/race_features.png");
	private static final Identifier BIRD_TEX = DakuraOrigins.id("textures/entity/geo/bird_wing.png");
	// regiões do atlas (16×16 cada)
	private static final int[] IVORY = {0, 0}, TAN = {16, 0}, TIP = {32, 0}, BLACK = {48, 0}, BLACK_HI = {0, 16},
			STONE = {16, 16}, STONE_DK = {32, 16}, STONE_HI = {48, 16}, CAT = {0, 32}, PINK = {16, 32}, WOLF = {32, 32},
			WOLF_IN = {48, 32}, RED = {0, 48}, CREAM = {16, 48}, RED_DK = {32, 48},
			SUC = {64, 0}, SUC_HI = {80, 0}, SUC_TIP = {96, 0}, TUSK = {112, 0},
			WING = {64, 16}, WING_SH = {80, 16}, WING_TIP = {96, 16}, WING_BONE = {112, 16},
			FAIRY = {64, 32}, FAIRY_VEIN = {80, 32}, FAIRY_GLOW = {96, 32}, BRASS = {112, 32},
			FUR = {64, 48}, FUR_SH = {80, 48}, HALO = {96, 48}, ONI = {112, 48};
	private static final ModelPart BULL = bull(), CAT_EARS = ears(false), WOLF_EARS = ears(true),
			FEATHERS = feathers(), WINGS = wings(), TUSKS = tusks(), DOLL_KEY = dollKey();
	private static final java.util.Map<Integer, Float> BIRD_OPEN = new java.util.HashMap<>();

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

	/** Presas do Orc saindo do lábio de baixo. */
	private static ModelPart tusks() {
		ModelData md = new ModelData();
		ModelPartData root = md.getRoot();
		for (int s = -1; s <= 1; s += 2) {
			ModelPartData a = root.addChild(s > 0 ? "l" : "r", box(TUSK, -0.5f, -1.2f, -0.5f, 1, 1.2f, 1),
					ModelTransform.of(s * 1.8f, -0.8f, -4.2f, -0.2f, 0, s * 0.15f));
			a.addChild("t", box(TUSK, -0.35f, -1.1f, -0.35f, 0.7f, 1.1f, 0.7f), ModelTransform.of(0, -1.1f, 0, -0.25f, 0, 0));
		}
		return TexturedModelData.of(md, 128, 64).createModel();
	}

	private static ModelPart ears(boolean wolf) {
		ModelData md = new ModelData();
		ModelPartData root = md.getRoot();
		int[] out = wolf ? WOLF : CAT, in = wolf ? WOLF_IN : PINK;
		for (int s = -1; s <= 1; s += 2) {
			ModelPartBuilder b = box(out, -1.5f, -1, -0.5f, 3, 1, 1);
			b.uv(out[0], out[1] + 4).cuboid(-1.1f, -2, -0.5f, 2.2f, 1, 1);
			b.uv(out[0], out[1] + 8).cuboid(-0.7f, wolf ? -3.2f : -2.9f, -0.5f, 1.4f, wolf ? 1.2f : 0.9f, 1);
			if (wolf) b.uv(out[0], out[1] + 12).cuboid(-0.35f, -4.2f, -0.5f, 0.7f, 1, 1);
			b.uv(in[0], in[1]).cuboid(-0.8f, wolf ? -2.8f : -2.2f, -0.65f, 1.6f, wolf ? 2.4f : 1.8f, 0.2f);
			root.addChild(s > 0 ? "l" : "r", b, ModelTransform.of(s * 2.6f, -7.7f, -0.3f, 0, 0, s * 0.18f));
		}
		return TexturedModelData.of(md, 128, 64).createModel();
	}

	private static ModelPart feathers() {
		ModelData md = new ModelData();
		ModelPartData root = md.getRoot();
		for (int s = -1; s <= 1; s += 2) {
			ModelPartData side = root.addChild(s > 0 ? "l" : "r", ModelPartBuilder.create(),
					ModelTransform.of(s * 3.6f, -6.3f, 0.6f, -0.35f, 0, 0));
			float[] rolls = {0.35f, 0.7f, 1.05f};
			for (int i = 0; i < 3; i++) {
				float len = 6 - i;
				ModelPartData f = side.addChild("f" + i, box(RED, -0.5f, -len, -0.25f, 1, len, 0.5f),
						ModelTransform.of(0, 0, i * 0.35f, 0, 0, s * rolls[i]));
				f.addChild("t", box(i == 1 ? CREAM : RED_DK, -0.35f, -2.2f, -0.2f, 0.7f, 2.2f, 0.4f),
						ModelTransform.of(0, -len + 0.3f, 0, -0.2f, 0, 0));
			}
			side.addChild("down", box(CREAM, -0.75f, -1.5f, -0.4f, 1.5f, 1.5f, 0.8f), ModelTransform.of(0, 0.4f, 0, 0, 0, s * 0.2f));
		}
		return TexturedModelData.of(md, 128, 64).createModel();
	}

	private static ModelPart wings() {
		ModelData md = new ModelData();
		ModelPartData root = md.getRoot();
		for (int s = -1; s <= 1; s += 2) {
			float x0 = s > 0 ? 0 : -6.5f;
			ModelPartData w = root.addChild(s > 0 ? "l" : "r", box(STONE_HI, x0, -0.6f, -0.5f, 6.5f, 1.2f, 1),
					ModelTransform.of(s * 1.5f, 1.5f, 2.3f, 0, s * -0.45f, s * -0.35f));
			float px = s > 0 ? 0.3f : -5.8f;
			w.addChild("panel", box(STONE, px, 0.5f, -0.25f, 5.5f, 3.5f, 0.5f), ModelTransform.NONE);
			w.addChild("j1", box(STONE_DK, s > 0 ? 0.8f : -2.3f, 4, -0.25f, 1.5f, 2, 0.5f), ModelTransform.NONE);
			w.addChild("j2", box(STONE_DK, s > 0 ? 3.0f : -4.5f, 4, -0.25f, 1.5f, 3, 0.5f), ModelTransform.NONE);
			w.addChild("j3", box(STONE_DK, s > 0 ? 5.0f : -6.2f, 3.5f, -0.25f, 1.2f, 1.6f, 0.5f), ModelTransform.NONE);
			w.addChild("claw", box(STONE_DK, s > 0 ? 6.2f : -7.2f, -1.4f, -0.4f, 1, 1.4f, 0.8f), ModelTransform.NONE);
		}
		return TexturedModelData.of(md, 128, 64).createModel();
	}

	/** Chave de corda da Boneca nas costas (gira sem parar) + juntas de boneca nos ombros. */
	private static ModelPart dollKey() {
		ModelData md = new ModelData();
		ModelPartData root = md.getRoot();
		ModelPartData key = root.addChild("key", box(BRASS, -0.5f, -0.5f, 0, 1, 1, 3), ModelTransform.of(0, 4.5f, 2.0f, 0, 0, 0));
		ModelPartData head = key.addChild("head", box(BRASS, -0.5f, -0.6f, 2.6f, 1, 1.2f, 1), ModelTransform.NONE);
		for (int s = -1; s <= 1; s += 2) {
			head.addChild(s > 0 ? "l" : "r", box(BRASS, s > 0 ? 0.3f : -3.3f, -1.6f, 2.5f, 3, 3.2f, 1.2f), ModelTransform.NONE);
			head.addChild(s > 0 ? "lh" : "rh", box(STONE_DK, s > 0 ? 1.1f : -2.5f, -0.6f, 2.45f, 1.4f, 1.2f, 1.3f), ModelTransform.NONE);
		}
		for (int s = -1; s <= 1; s += 2) {
			root.addChild(s > 0 ? "jl" : "jr", box(STONE_DK, -1.1f, -1.1f, -1.1f, 2.2f, 2.2f, 2.2f), ModelTransform.of(s * 4.6f, 1.6f, 0, 0, 0.78f, 0));
		}
		return TexturedModelData.of(md, 128, 64).createModel();
	}

	@Override
	public void render(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float limbAngle,
					   float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
		if (e.isInvisible()) return;
		VertexConsumer vc = null;
		boolean stone = EntityFlags.clientHas(e, "statue") || EntityFlags.clientHas(e, "meteor");
		float sway = (float) Math.sin(animationProgress * 0.1f) * 0.04f + limbDistance * (float) Math.sin(limbAngle * 0.6662f) * 0.1f;

		ModelPart head = null;
		long tod = e.getWorld().getTimeOfDay() % 24000L;
		boolean day = e.getWorld().getDimension().hasSkyLight() && (tod < 12542 || tod > 23460);
		if (DakuraConfig.cosmetic(e, "bull_horns")) head = BULL;
		// chifres dos demônios: agora modelo do Blockbench (GeoPartsRenderer)
		else if (DakuraConfig.cosmetic(e, "orc_tusks")) head = TUSKS;
		else if (DakuraConfig.cosmetic(e, "cat_ears")) head = CAT_EARS;
		else if (DakuraConfig.cosmetic(e, "wolf_ears")) head = WOLF_EARS;
		else if (DakuraConfig.cosmetic(e, "bird_feathers")) head = FEATHERS;
		if (head != null) {
			vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEX));
			if (head == CAT_EARS || head == WOLF_EARS) {
				float flick = ((int) animationProgress % 97) < 3 ? 0.2f * MathHelper.sin(animationProgress * 2.5f) : 0f;
				boolean hunt = EntityFlags.clientHas(e, "hunt") || EntityFlags.clientHas(e, "prowl");
				float back = hunt ? -0.45f : 0f;
				head.getChild("l").pitch = back + sway;
				head.getChild("r").pitch = back - sway * 0.8f;
				head.getChild("l").roll = 0.18f + flick;
			} else if (head == FEATHERS) {
				head.getChild("l").pitch = -0.35f + sway * 0.6f;
				head.getChild("r").pitch = -0.35f - sway * 0.6f;
			}
			m.push();
			getContextModel().getHead().rotate(m);
			head.render(m, vc, light, OverlayTexture.DEFAULT_UV);
			m.pop();
		}

		// caudas da Kitsune e das raposas: agora modelo do Blockbench (GeoPartsRenderer)
		// asas do Serafim: agora modelo do Blockbench (GeoPartsRenderer)
		if (DakuraConfig.cosmetic(e, "doll_key")) {
			VertexConsumer kv = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEX));
			boolean dancing = EntityFlags.clientHas(e, "dancing");
			DOLL_KEY.getChild("key").roll = animationProgress * (dancing ? 0.6f : 0.06f);
			m.push();
			getContextModel().body.rotate(m);
			DOLL_KEY.render(m, kv, light, OverlayTexture.DEFAULT_UV);
			m.pop();
		}

		if (DakuraConfig.cosmetic(e, "bird_wing")) {
			com.dakura.origins.client.geo.GeoModel bird = com.dakura.origins.client.geo.GeoModel.get("bird_wing");
			VertexConsumer bvc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(BIRD_TEX));
			boolean glide = EntityFlags.clientHas(e, "gliding") || e.isFallFlying();
			ModelPart wing = bird.bone("wing");
			// abertura suave, como as asas do élitro abrindo e fechando
			float open = BIRD_OPEN.getOrDefault(e.getId(), 0f);
			open += ((glide ? 1f : 0f) - open) * 0.15f;
			BIRD_OPEN.put(e.getId(), open);
			float speed = (float) Math.min(1.0, e.getVelocity().length() / 1.5);
			float beat = glide ? MathHelper.sin(animationProgress * 0.3f) * 0.05f * (1 - speed) : MathHelper.sin(animationProgress * 0.06f) * 0.03f;
			// fechada: dobrada ao longo das costas; aberta: estendida para o lado e levemente para trás (postura de élitro)
			wing.roll = MathHelper.lerp(open, 0.12f, -1.5f + speed * 0.15f) + beat;
			wing.pitch = MathHelper.lerp(open, 0.25f, 0.3f + speed * 0.25f);
			wing.yaw = MathHelper.lerp(open, 0f, -0.2f - speed * 0.15f);
			wing.getChild("hand").roll = MathHelper.lerp(open, 0.05f, -0.2f + speed * 0.1f);
			m.push();
			getContextModel().body.rotate(m);
			bird.render(m, bvc, light);
			m.pop();
		}

		// asas da Fada: agora sprite em pixel art (GeoPartsRenderer)

		if (DakuraConfig.cosmetic(e, "stone_wings")) {
			if (vc == null) vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEX));
			boolean air = !e.isOnGround() && !e.isTouchingWater() && !stone;
			float flap = air ? MathHelper.sin(animationProgress * 0.55f) * 0.55f : (stone ? 0f : MathHelper.sin(animationProgress * 0.07f) * 0.05f);
			float open = air ? 0.35f : 0f;
			WINGS.getChild("l").yaw = -0.45f + open + flap;
			WINGS.getChild("r").yaw = 0.45f - open - flap;
			WINGS.getChild("l").roll = -0.35f - (air ? 0.2f : 0f);
			WINGS.getChild("r").roll = 0.35f + (air ? 0.2f : 0f);
			m.push();
			getContextModel().body.rotate(m);
			WINGS.render(m, vc, light, OverlayTexture.DEFAULT_UV);
			m.pop();
		}
	}
}
