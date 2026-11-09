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
	// regiões do atlas (16×16 cada)
	private static final int[] IVORY = {0, 0}, TAN = {16, 0}, TIP = {32, 0}, BLACK = {48, 0}, BLACK_HI = {0, 16},
			STONE = {16, 16}, STONE_DK = {32, 16}, STONE_HI = {48, 16}, CAT = {0, 32}, PINK = {16, 32}, WOLF = {32, 32},
			WOLF_IN = {48, 32}, RED = {0, 48}, CREAM = {16, 48}, RED_DK = {32, 48},
			SUC = {64, 0}, SUC_HI = {80, 0}, SUC_TIP = {96, 0}, TUSK = {112, 0},
			WING = {64, 16}, WING_SH = {80, 16}, WING_TIP = {96, 16}, WING_BONE = {112, 16},
			FAIRY = {64, 32}, FAIRY_VEIN = {80, 32}, FAIRY_GLOW = {96, 32}, BRASS = {112, 32},
			FUR = {64, 48}, FUR_SH = {80, 48}, HALO = {96, 48}, ONI = {112, 48};
	private static final ModelPart BULL = bull(), DEMON = demon(), CAT_EARS = ears(false), WOLF_EARS = ears(true),
			FEATHERS = feathers(), WINGS = wings(), SUCCUBUS = succubus(), TUSKS = tusks(), BIRD_WING = birdWing(),
			FAIRY_WINGS = fairyWings(), TAIL = tail(), SERAPH_WING = seraphWing(), HALO_RING = halo(), ONI_HORNS = oniHorns(),
			DOLL_KEY = dollKey();
	/** Estado da física das caudas por jogador: {balanço X, velocidade X, balanço Y, velocidade Y, recolhimento}. */
	private static final java.util.Map<Integer, float[]> TAIL_PHYS = new java.util.HashMap<>();

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

	/** Chifres negros do Demônio: mais grossos, sobem e varrem para trás em curva contínua. */
	private static ModelPart demon() {
		ModelData md = new ModelData();
		ModelPartData root = md.getRoot();
		for (int s = -1; s <= 1; s += 2) {
			ModelPartData base = root.addChild(s > 0 ? "l" : "r", ModelPartBuilder.create(),
					ModelTransform.of(s * 2.4f, -7.6f, -1.4f, 0, 0, s * 0.3f));
			ModelPartData cur = base;
			float t = 2.1f;
			for (int i = 0; i < 6; i++) {
				cur = cur.addChild("s" + i, box(i == 0 ? BLACK_HI : BLACK, -t / 2, -1.25f, -t / 2, t, 1.35f, t),
						ModelTransform.of(0, i == 0 ? 0 : -1.15f, 0, -0.26f, 0, s * 0.03f));
				t = Math.max(0.4f, t * 0.8f);
			}
		}
		return TexturedModelData.of(md, 128, 64).createModel();
	}

	/** Chifres da Succubus: vermelhos, finos e elegantes, curvando para trás e para cima (nada de bode). */
	private static ModelPart succubus() {
		ModelData md = new ModelData();
		ModelPartData root = md.getRoot();
		for (int s = -1; s <= 1; s += 2) {
			ModelPartData cur = root.addChild(s > 0 ? "l" : "r", ModelPartBuilder.create(),
					ModelTransform.of(s * 2.8f, -7.4f, -0.6f, 0.35f, 0, s * 0.55f));
			float t = 1.3f;
			float[] pitch = {-0.55f, -0.45f, -0.3f, 0.05f, 0.35f, 0.5f};
			for (int i = 0; i < pitch.length; i++) {
				int[] uv = i < 2 ? SUC_HI : i < 5 ? SUC : SUC_TIP;
				cur = cur.addChild("s" + i, box(uv, -t / 2, -1.2f, -t / 2, t, 1.3f, t),
						ModelTransform.of(0, i == 0 ? 0 : -1.1f, 0, pitch[i], 0, s * -0.04f));
				t = Math.max(0.3f, t * 0.8f);
			}
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

	/** Asa única da Vastaya Ave: fechada nas costas; ao planar, abre de lado em leque. */
	private static ModelPart birdWing() {
		ModelData md = new ModelData();
		ModelPartData root = md.getRoot();
		ModelPartData arm = root.addChild("wing", box(WING_BONE, -0.6f, 0, -0.6f, 1.2f, 6, 1.2f), ModelTransform.of(1.2f, 1.0f, 2.4f, 0, 0, 0));
		ModelPartData hand = arm.addChild("hand", box(WING_BONE, -0.45f, 0, -0.45f, 0.9f, 5, 0.9f), ModelTransform.of(0, 5.6f, 0, 0, 0, 0));
		for (int i = 0; i < 5; i++) {
			float len = 7 - i * 0.6f;
			arm.addChild("c" + i, box(WING_SH, -0.2f + i * 0.9f - 2f, 0.5f + i * 0.9f, -0.15f, 1.0f, len * 0.6f, 0.3f),
					ModelTransform.of(0, 0, 0, 0, 0, 0.05f * i));
			hand.addChild("p" + i, box(i < 3 ? WING : WING_SH, -0.5f, 0, -0.15f, 1.0f, len, 0.3f),
					ModelTransform.of(-0.4f - i * 0.15f, 0.2f + i * 0.9f, 0, 0, 0, 0.12f * i));
			hand.addChild("t" + i, box(WING_TIP, -0.5f, len, -0.15f, 1.0f, 1.6f, 0.3f),
					ModelTransform.of(-0.4f - i * 0.15f, 0.2f + i * 0.9f, 0, 0, 0, 0.12f * i));
		}
		return TexturedModelData.of(md, 128, 64).createModel();
	}

	/** Asas da Fada: dois pares translúcidos com nervuras. */
	private static ModelPart fairyWings() {
		ModelData md = new ModelData();
		ModelPartData root = md.getRoot();
		for (int s = -1; s <= 1; s += 2) {
			float x0 = s > 0 ? 0 : -7;
			ModelPartData up = root.addChild(s > 0 ? "lu" : "ru", box(FAIRY, x0, -7, 0, 7, 8, 0.1f),
					ModelTransform.of(s * 0.8f, 3.5f, 2.3f, 0, s * -0.35f, s * -0.25f));
			up.addChild("v", box(FAIRY_VEIN, s > 0 ? 0.5f : -6.5f, -6, -0.05f, 6, 0.4f, 0.15f), ModelTransform.of(0, 0, 0, 0, 0, s * 0.6f));
			up.addChild("g", box(FAIRY_GLOW, s > 0 ? 5 : -6.5f, -6.5f, -0.04f, 1.5f, 1.5f, 0.12f), ModelTransform.NONE);
			ModelPartData lo = root.addChild(s > 0 ? "ll" : "rl", box(FAIRY, s > 0 ? 0 : -5, 0, 0, 5, 5, 0.1f),
					ModelTransform.of(s * 0.8f, 4.5f, 2.3f, 0, s * -0.3f, s * 0.35f));
			lo.addChild("v", box(FAIRY_VEIN, s > 0 ? 0.4f : -4.4f, 0.4f, -0.05f, 4, 0.35f, 0.15f), ModelTransform.of(0, 0, 0, 0, 0, s * -0.7f));
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

	/** Uma cauda caricata: gomos arredondados que engordam no meio e afinam na ponta (pompom branco). */
	private static ModelPart tail() {
		ModelData md = new ModelData();
		ModelPartData cur = md.getRoot().addChild("t", ModelPartBuilder.create(), ModelTransform.NONE);
		float[] size = {2.2f, 3.4f, 4.4f, 5.0f, 5.2f, 4.6f, 3.4f, 1.9f};
		for (int i = 0; i < size.length; i++) {
			float s2 = size[i];
			ModelPartBuilder b = box(i >= size.length - 2 ? FUR_SH : FUR, -s2 / 2, 0, -s2 / 2, s2, 2.4f, s2);
			// "arredondado": um miolo um pouco maior no meio de cada gomo
			b.uv(FUR[0], FUR[1]).cuboid(-s2 / 2 - 0.35f, 0.6f, -s2 / 2 - 0.35f, s2 + 0.7f, 1.2f, s2 + 0.7f);
			cur = cur.addChild("s" + i, b, ModelTransform.of(0, i == 0 ? 0 : 2.1f, 0, 0, 0, 0));
		}
		return TexturedModelData.of(md, 128, 64).createModel();
	}

	/** Asa de serafim: osso + leque de penas brancas arredondadas. */
	private static ModelPart seraphWing() {
		ModelData md = new ModelData();
		ModelPartData w = md.getRoot().addChild("w", box(FUR_SH, -0.6f, -0.6f, -0.6f, 7, 1.2f, 1.2f), ModelTransform.NONE);
		for (int i = 0; i < 6; i++) {
			float len = 7.5f - Math.abs(i - 2) * 0.9f;
			ModelPartData f = w.addChild("f" + i, box(FUR, -0.7f, 0, -0.2f, 1.6f, len, 0.4f),
					ModelTransform.of(0.6f + i * 1.15f, 0.3f, 0, 0, 0, -0.22f + i * 0.09f));
			f.addChild("r", box(FUR, -0.45f, len - 0.4f, -0.2f, 1.1f, 1.1f, 0.4f), ModelTransform.NONE);
		}
		return TexturedModelData.of(md, 128, 64).createModel();
	}

	/** Auréola: anel dourado de pequenos gomos (parece um aro liso visto de longe). */
	private static ModelPart halo() {
		ModelData md = new ModelData();
		ModelPartData root = md.getRoot();
		for (int i = 0; i < 18; i++) {
			double a = i * Math.PI * 2 / 18;
			root.addChild("h" + i, box(HALO, -0.8f, -0.35f, -0.35f, 1.6f, 0.7f, 0.7f),
					ModelTransform.of((float) Math.cos(a) * 4.2f, 0, (float) Math.sin(a) * 4.2f, 0, (float) (-a + Math.PI / 2), 0));
		}
		return TexturedModelData.of(md, 128, 64).createModel();
	}

	/** Chifrinhos de Oni: curtos, grossos e curvos, bem caricatos. */
	private static ModelPart oniHorns() {
		ModelData md = new ModelData();
		ModelPartData root = md.getRoot();
		for (int s = -1; s <= 1; s += 2) {
			ModelPartData cur = root.addChild(s > 0 ? "l" : "r", ModelPartBuilder.create(), ModelTransform.of(s * 2.0f, -7.7f, -2.6f, -0.2f, 0, s * 0.35f));
			float t = 1.9f;
			for (int i = 0; i < 4; i++) {
				cur = cur.addChild("s" + i, box(i < 3 ? ONI : TIP, -t / 2, -1.0f, -t / 2, t, 1.1f, t),
						ModelTransform.of(0, i == 0 ? 0 : -0.95f, 0, -0.18f, 0, s * -0.12f));
				t = Math.max(0.35f, t * 0.72f);
			}
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

	/** Física das caudas: balançam atrasadas em relação ao corpo, quicam ao pousar e se recolhem perto de blocos. */
	private static float[] tailPhysics(AbstractClientPlayerEntity e, float tickDelta) {
		float[] st = TAIL_PHYS.computeIfAbsent(e.getId(), k -> new float[5]);
		double dx = e.getX() - e.prevX, dz = e.getZ() - e.prevZ, dy = e.getY() - e.prevY;
		double yaw = Math.toRadians(e.bodyYaw);
		double fwd = -dx * Math.sin(yaw) + dz * Math.cos(yaw), side = dx * Math.cos(yaw) + dz * Math.sin(yaw);
		// molas amortecidas (atualizadas por quadro, suave mesmo com FPS alto)
		float dt = 0.5f;
		float targetX = (float) MathHelper.clamp(-fwd * 2.2, -0.9, 0.6), targetY = (float) MathHelper.clamp(side * 2.0, -0.6, 0.6);
		st[1] += ((targetX - st[0]) * 0.35f - st[1] * 0.22f) * dt;
		st[0] += st[1] * dt;
		float bounceTarget = (float) MathHelper.clamp(-dy * 1.6, -0.5, 0.5);
		st[3] += ((targetY + bounceTarget * 0.4f - st[2]) * 0.3f - st[3] * 0.2f) * dt;
		st[2] += st[3] * dt;
		// não atravessar blocos: se há algo sólido atrás, as caudas se recolhem para cima/perto do corpo
		Vec3d back = new Vec3d(Math.sin(yaw), 0, -Math.cos(yaw));
		boolean blocked = false;
		for (int i = 1; i <= 3; i++) {
			var bp = net.minecraft.util.math.BlockPos.ofFloored(e.getX() + back.x * i * 0.55, e.getY() + 0.9, e.getZ() + back.z * i * 0.55);
			var bs = e.getWorld().getBlockState(bp);
			if (!bs.getCollisionShape(e.getWorld(), bp).isEmpty()) {
				blocked = true;
				break;
			}
		}
		st[4] += ((blocked ? 1f : 0f) - st[4]) * 0.15f;
		return st;
	}

	private void renderTails(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float t, float tickDelta) {
		VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEX));
		float[] st = tailPhysics(e, tickDelta);
		boolean fan = com.dakura.origins.client.anim.AbilityAnimations.active("fan").contains(e.getId());
		float spread = fan ? 1.35f : 1.0f;
		float curl = st[4];
		m.push();
		getContextModel().body.rotate(m);
		m.translate(0, 10.5f / 16f, 2.2f / 16f);
		ModelPart root = TAIL.getChild("t");
		for (int i = 0; i < 9; i++) {
			float k = (i - 4) / 4f;                       // -1..1, da esquerda para a direita
			float yawA = k * 1.05f * spread + st[2] * 0.6f;
			float pitchA = 0.55f + Math.abs(k) * 0.25f + st[0] * 0.5f - curl * 0.55f - (fan ? 0.25f : 0f);
			float wave = MathHelper.sin(t * 0.09f + i * 0.7f) * 0.07f;
			root.pitch = pitchA + wave;
			root.yaw = yawA;
			root.roll = k * 0.35f;
			float shrink = 1f - curl * 0.35f;
			for (int sgm = 0; sgm < 8; sgm++) {
				ModelPart sp = root.getChild("s0");
				for (int d = 1; d <= sgm; d++) sp = sp.getChild("s" + d);
				// a cauda curva para cima no fim, com um pouco de balanço que se propaga
				sp.pitch = (sgm == 0 ? 0 : -0.22f - curl * 0.12f) + MathHelper.sin(t * 0.12f + i - sgm * 0.6f) * 0.05f + st[0] * 0.06f;
				sp.roll = st[2] * 0.08f * sgm / 8f;
			}
			m.push();
			m.scale(shrink, shrink, shrink);
			root.render(m, vc, light, OverlayTexture.DEFAULT_UV);
			m.pop();
		}
		m.pop();
	}

	private void renderSeraph(MatrixStack m, VertexConsumerProvider vcp, AbstractClientPlayerEntity e, float t) {
		VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEX));
		boolean flying = !e.isOnGround() && !e.isTouchingWater();
		float open = flying ? 1f : 0f;
		float flap = flying ? MathHelper.sin(t * 0.35f) * 0.35f : MathHelper.sin(t * 0.05f) * 0.03f;
		m.push();
		getContextModel().body.rotate(m);
		ModelPart w = SERAPH_WING.getChild("w");
		for (int pair = 0; pair < 3; pair++) {
			for (int s = -1; s <= 1; s += 2) {
				// dobradas: penduradas junto às costas; abertas: um leque em três alturas
				float folded = s * (0.15f + pair * 0.12f);
				float spreadRoll = s * (-0.7f + pair * 0.7f);
				w.roll = MathHelper.lerp(open, s * 1.35f + folded, spreadRoll) + s * flap * (1 - pair * 0.25f);
				w.yaw = MathHelper.lerp(open, s * 1.25f, s * 0.35f);
				w.pitch = MathHelper.lerp(open, 0.15f + pair * 0.1f, 0f);
				m.push();
				m.translate(s * 1.2f / 16f, (1.5f + pair * 3.2f) / 16f, 2.3f / 16f);
				if (s < 0) m.scale(-1, 1, 1);
				w.render(m, vc, 0xF000F0, OverlayTexture.DEFAULT_UV);
				m.pop();
			}
		}
		m.pop();
		// auréola flutuando e girando sobre a cabeça, sempre acesa
		m.push();
		getContextModel().getHead().rotate(m);
		m.translate(0, (-10.5f + MathHelper.sin(t * 0.1f) * 0.4f) / 16f, 0);
		HALO_RING.yaw = t * 0.02f;
		HALO_RING.render(m, vc, 0xF000F0, OverlayTexture.DEFAULT_UV);
		m.pop();
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
		else if (DakuraConfig.cosmetic(e, "demon_horns")) head = DEMON;
		else if (DakuraConfig.cosmetic(e, "succubus_horns") && !day) head = SUCCUBUS; // de dia os chifres somem
		else if (DakuraConfig.cosmetic(e, "orc_tusks")) head = TUSKS;
		else if (DakuraConfig.cosmetic(e, "oni_horns")) head = ONI_HORNS;
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

		if (DakuraConfig.cosmetic(e, "kitsune_tails")) renderTails(m, vcp, light, e, animationProgress, tickDelta);
		if (DakuraConfig.cosmetic(e, "seraph_wings")) renderSeraph(m, vcp, e, animationProgress);
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
			if (vc == null) vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(TEX));
			boolean glide = EntityFlags.clientHas(e, "gliding");
			ModelPart wing = BIRD_WING.getChild("wing");
			float open = glide ? 1f : 0f;
			float beat = glide ? MathHelper.sin(animationProgress * 0.25f) * 0.08f : MathHelper.sin(animationProgress * 0.06f) * 0.03f;
			// fechada: pendurada ao longo das costas; aberta: estendida para o lado, quase horizontal
			wing.roll = MathHelper.lerp(open, 0.12f, -1.45f) + beat;
			wing.pitch = MathHelper.lerp(open, 0.25f, 0.1f);
			wing.yaw = MathHelper.lerp(open, 0f, -0.25f);
			wing.getChild("hand").roll = MathHelper.lerp(open, 0.05f, -0.25f);
			m.push();
			getContextModel().body.rotate(m);
			BIRD_WING.render(m, vc, light, OverlayTexture.DEFAULT_UV);
			m.pop();
		}

		if (DakuraConfig.cosmetic(e, "fairy_wings")) {
			VertexConsumer tvc = vcp.getBuffer(RenderLayer.getEntityTranslucent(TEX));
			float flap = MathHelper.sin(animationProgress * (e.isOnGround() ? 0.35f : 1.6f)) * (e.isOnGround() ? 0.12f : 0.45f);
			FAIRY_WINGS.getChild("lu").yaw = -0.35f + flap;
			FAIRY_WINGS.getChild("ll").yaw = -0.3f + flap * 0.8f;
			FAIRY_WINGS.getChild("ru").yaw = 0.35f - flap;
			FAIRY_WINGS.getChild("rl").yaw = 0.3f - flap * 0.8f;
			m.push();
			getContextModel().body.rotate(m);
			FAIRY_WINGS.render(m, tvc, 0xF000F0, OverlayTexture.DEFAULT_UV);
			m.pop();
		}

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
