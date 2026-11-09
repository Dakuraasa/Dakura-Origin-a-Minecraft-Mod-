package com.dakura.origins.client.render;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.client.anim.AbilityAnimations;
import com.dakura.origins.client.config.DakuraConfig;
import com.dakura.origins.power.EntityFlags;
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
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Asas em pixel art 2D (sprites planos, sem modelo 3D) de cinco raças:
 * Gárgula (stone_wings), Fênix (phoenix_wings), Fada (fairy_wings), Serafim (seraph_wings) e
 * Vastaya de Ave (bird_wing). Cada asa é UM quadrado texturizado, com ponto de fixação no ombro,
 * e anima de forma independente: varredura (gira em torno do eixo vertical e dá a impressão de bater),
 * inclinação no próprio plano e transição suave entre "recolhida" e "aberta".
 * <p>
 * Texturas em assets/dakura_origins/textures/entity/wings/. Somem com a opção "Aparências de raça" desligada.
 */
public class WingFeatureRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	private static final int FULL_BRIGHT = 0xF000F0;

	/** Uma imagem de asa: textura, tamanho em pixels, ombro (raiz) e ponto de "pendurar" (capa), em pixels da imagem. */
	private static final class Sprite {
		final Identifier tex;
		final int w, h;
		final float rootX, rootY, hangX, hangY;
		/** +1 = a ponta da asa fica à direita da imagem; -1 = à esquerda. */
		final int dir;

		Sprite(String file, int w, int h, float rootX, float rootY, int dir) {
			this(file, w, h, rootX, rootY, rootX, rootY, dir);
		}

		Sprite(String file, int w, int h, float rootX, float rootY, float hangX, float hangY, int dir) {
			this.tex = DakuraOrigins.id("textures/entity/wings/" + file + ".png");
			this.w = w;
			this.h = h;
			this.rootX = rootX;
			this.rootY = rootY;
			this.hangX = hangX;
			this.hangY = hangY;
			this.dir = dir;
		}
	}

	private static final Sprite GARG_L = new Sprite("gargula_l", 120, 85, 92, 16, -1);
	private static final Sprite GARG_R = new Sprite("gargula_r", 120, 86, 28, 16, 1);
	private static final Sprite FENIX_FOLDED = new Sprite("fenix_recolhida", 256, 165, 40, 45, 1);
	private static final Sprite FENIX_OPEN = new Sprite("fenix_aberta", 256, 169, 28, 100, 1);
	private static final Sprite FADA_L = new Sprite("fada_l", 96, 160, 90, 98, -1);
	private static final Sprite FADA_R = new Sprite("fada_r", 97, 160, 6, 98, 1);
	private static final Sprite SERA_L = new Sprite("serafim_l", 88, 200, 82, 44, 70, 26, -1);
	private static final Sprite SERA_R = new Sprite("serafim_r", 88, 200, 6, 44, 18, 26, 1);

	/** Estado suave por jogador: {abertura, (reserva), (reserva), último tempo, dt do quadro}. */
	private static final Map<AbstractClientPlayerEntity, float[]> STATE = new WeakHashMap<>();
	private static final Vector3f SCRATCH = new Vector3f();

	public WingFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) {
		super(ctx);
	}

	// ================================================================== utilidades

	private static float[] state(AbstractClientPlayerEntity e, float t) {
		float[] st = STATE.computeIfAbsent(e, k -> {
			float[] a = new float[5];
			a[3] = Float.NaN;
			return a;
		});
		float dt = Float.isNaN(st[3]) ? 100f : MathHelper.clamp(t - st[3], 0f, 5f);
		if (t - st[3] > 5f || t < st[3]) dt = 100f; // mundo recarregado: encaixa direto no alvo
		st[3] = t;
		st[4] = dt;
		return st;
	}

	/** Aproxima o canal {@code slot} do alvo (constante de tempo ~ 1/rate ticks). */
	private static float approach(float[] st, int slot, float target, float rate) {
		float k = 1f - (float) Math.exp(-st[4] * rate);
		st[slot] += (target - st[slot]) * k;
		return st[slot];
	}

	private static void vertex(VertexConsumer vc, Matrix4f mat, float x, float y, float z, float u, float v, int shade, int light) {
		mat.transformPosition(x, y, z, SCRATCH);
		vc.vertex(SCRATCH.x, SCRATCH.y, SCRATCH.z).color(shade, shade, shade, 255).texture(u, v)
				.overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0f, 1f, 0f);
	}

	/**
	 * Desenha UMA asa. A pilha já está no espaço do corpo, em "pixels" (1/16 de bloco), +Z = costas.
	 *
	 * @param side   -1 = lado esquerdo de quem olha o jogador de trás, +1 = lado direito
	 * @param hang   0..1: 0 = presa pelo ponto de capa (pendurada), 1 = presa pelo ombro (aberta)
	 * @param ax     afastamento do ombro em relação ao centro das costas (sempre positivo)
	 * @param size   pixels do modelo por pixel da imagem
	 * @param sweep  varredura em volta do eixo vertical (rad): + leva a ponta para a frente
	 * @param droop  inclinação no plano da asa (rad): + abaixa a ponta para fora/baixo, como girar o ponteiro de um relógio
	 * @param pitch  inclinação para trás/frente (rad): + inclina o topo para o corpo
	 * @param shade  0..255, escurece a camada (dá profundidade quando as asas se sobrepõem)
	 */
	private void wing(MatrixStack m, VertexConsumerProvider vcp, int light, Sprite sp, int side, float hang,
					  float ax, float ay, float az, float size, float sweep, float droop, float pitch, int shade) {
		boolean mirror = sp.dir != side;
		float px = MathHelper.lerp(hang, sp.hangX, sp.rootX);
		float py = MathHelper.lerp(hang, sp.hangY, sp.rootY);
		m.push();
		m.translate(-side * ax, ay, az); // no modelo, +X é o lado esquerdo de quem olha de trás
		m.scale(-1f, 1f, 1f);            // daqui em diante +x local = direita de quem olha de trás
		// ordem: varredura (Y) > inclinação (X) > giro no plano (Z). O giro no plano não muda a normal da asa,
		// então asas com a mesma varredura/inclinação ficam em planos paralelos (camadas nunca se cruzam).
		m.multiply(RotationAxis.POSITIVE_Y.rotation(side * sweep));
		m.multiply(RotationAxis.POSITIVE_X.rotation(pitch));
		m.multiply(RotationAxis.POSITIVE_Z.rotation(side * droop));
		float sg = mirror ? -1f : 1f;
		float x0 = -px * size * sg, x1 = (sp.w - px) * size * sg;
		float y0 = -py * size, y1 = (sp.h - py) * size;
		Matrix4f mat = m.peek().getPositionMatrix();
		VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(sp.tex));
		vertex(vc, mat, x0, y0, 0f, 0f, 0f, shade, light);
		vertex(vc, mat, x1, y0, 0f, 1f, 0f, shade, light);
		vertex(vc, mat, x1, y1, 0f, 1f, 1f, shade, light);
		vertex(vc, mat, x0, y1, 0f, 0f, 1f, shade, light);
		m.pop();
	}

	// ================================================================== raças

	/** Gárgula: asas de morcego de pedra. Dobradas e imóveis no chão (e como estátua); batem largas no ar. */
	private void gargoyle(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float t, float[] st) {
		boolean stone = EntityFlags.clientHas(e, "statue") || EntityFlags.clientHas(e, "meteor");
		boolean air = !e.isOnGround() && !e.isTouchingWater() && !stone;
		float open = approach(st, 0, air ? 1f : 0f, 0.28f);
		float phase = t * 0.34f;
		float idle = stone ? 0f : MathHelper.sin(t * 0.07f) * 0.03f;
		float droop = MathHelper.lerp(open, 1.15f + idle, -0.12f + 0.55f * MathHelper.sin(phase));
		float sweep = MathHelper.lerp(open, 0.75f, 0.20f + 0.30f * MathHelper.sin(phase - 1.1f));
		for (int side = -1; side <= 1; side += 2) {
			wing(m, vcp, light, side < 0 ? GARG_L : GARG_R, side, 1f, 2.2f, 1.0f, 3.3f, 0.15f, sweep, droop, 0f, 255);
		}
	}

	/** Fênix: duas asas (a mesma arte espelhada). Recolhidas nas costas no chão; abertas e batendo em sincronia no voo. */
	private void phoenix(MatrixStack m, VertexConsumerProvider vcp, AbstractClientPlayerEntity e, float t, float[] st) {
		boolean air = !e.isOnGround() && !e.isTouchingWater();
		boolean show = air || EntityFlags.clientHas(e, "solar_dive") || EntityFlags.clientHas(e, "reborn")
				|| AbilityAnimations.active("wings_spread").contains(e.getId());
		float open = approach(st, 0, show ? 1f : 0f, 0.22f);
		float phase = t * 0.30f;
		float droop;
		float sweep;
		Sprite sp;
		float size;
		if (open < 0.5f) {
			float k = open / 0.5f;
			sp = FENIX_FOLDED;
			size = 0.075f;
			droop = MathHelper.lerp(k, 0.92f + MathHelper.sin(t * 0.06f) * 0.03f, 0.55f);
			sweep = MathHelper.lerp(k, 0.05f, 0.55f);
		} else {
			float k = (open - 0.5f) / 0.5f;
			sp = FENIX_OPEN;
			size = MathHelper.lerp(k, 0.095f, 0.115f);
			droop = MathHelper.lerp(k, 0.55f, 0.20f + 0.55f * MathHelper.sin(phase));
			sweep = MathHelper.lerp(k, 0.55f, 0.32f + 0.26f * MathHelper.sin(phase - 1.2f));
		}
		for (int side = -1; side <= 1; side += 2) {
			wing(m, vcp, FULL_BRIGHT, sp, side, 1f, 2.4f, 2.0f, 3.3f, size, sweep, droop, 0f, 255);
		}
	}

	/** Vastaya de Ave: UMA asa (a mesma arte da Fênix), recolhida nas costas; só abre ao planar. */
	private void birdWing(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float t, float[] st) {
		boolean glide = EntityFlags.clientHas(e, "gliding");
		float open = approach(st, 0, glide ? 1f : 0f, 0.25f);
		float phase = t * 0.22f;
		Sprite sp;
		float size;
		float droop;
		float sweep;
		if (open < 0.5f) {
			float k = open / 0.5f;
			sp = FENIX_FOLDED;
			size = 0.07f;
			droop = MathHelper.lerp(k, 0.92f + MathHelper.sin(t * 0.05f) * 0.03f, 0.55f);
			sweep = MathHelper.lerp(k, 0.05f, 0.45f);
		} else {
			float k = (open - 0.5f) / 0.5f;
			sp = FENIX_OPEN;
			size = MathHelper.lerp(k, 0.09f, 0.12f);
			droop = MathHelper.lerp(k, 0.55f, 0.25f + 0.18f * MathHelper.sin(phase));
			sweep = MathHelper.lerp(k, 0.45f, 0.35f + 0.14f * MathHelper.sin(phase - 1.0f));
		}
		wing(m, vcp, light, sp, -1, 1f, 1.6f, 2.2f, 3.3f, size, sweep, droop, 0f, 255);
	}

	/** Fada: asas bem maiores que o corpo, batendo rápido e intenso (mais no ar, mais discreto no chão). */
	private void fairy(MatrixStack m, VertexConsumerProvider vcp, AbstractClientPlayerEntity e, float t, float[] st) {
		boolean air = !e.isOnGround() && !e.isTouchingWater();
		float amp = approach(st, 0, air ? 1f : 0f, 0.25f);
		float phase = t * MathHelper.lerp(amp, 0.9f, 1.5f);
		float beat = MathHelper.sin(phase) + 0.2f * MathHelper.sin(phase * 2f + 0.8f);
		float sweep = MathHelper.lerp(amp, 0.22f, 0.30f) + MathHelper.lerp(amp, 0.20f, 0.60f) * beat * 0.85f;
		float droop = -0.05f + MathHelper.lerp(amp, 0.04f, 0.12f) * MathHelper.sin(phase + 1.57f);
		for (int side = -1; side <= 1; side += 2) {
			wing(m, vcp, FULL_BRIGHT, side < 0 ? FADA_L : FADA_R, side, 1f, 1.2f, 3.8f, 3.3f, 0.16f, sweep, droop, 0f, 255);
		}
	}

	/** Serafim: três pares (seis asas) da mesma arte. Pousado: capa dobrada para baixo, na diagonal; no ar: leque batendo. */
	private void seraph(MatrixStack m, VertexConsumerProvider vcp, AbstractClientPlayerEntity e, float t, float[] st) {
		boolean air = !e.isOnGround() && !e.isTouchingWater();
		float open = approach(st, 0, air ? 1f : 0f, 0.16f);
		float phase = t * 0.2f;
		// 0 = par de trás (maior, mais escuro), 2 = par da frente (menor, mais claro)
		float[] sizeFold = {0.125f, 0.110f, 0.095f}, sizeOpen = {0.190f, 0.165f, 0.140f};
		float[] ayFold = {1.0f, 1.8f, 2.6f}, ayOpen = {0.0f, 2.4f, 4.8f};
		float[] zOff = {0f, 0.8f, 1.6f};
		float[] droopFold = {-0.10f, -0.24f, -0.40f}, droopOpen = {0.62f, 0.34f, 0.06f};
		int[] shade = {185, 225, 255};
		// mesma varredura nos três pares: os planos ficam paralelos e a ordem das camadas nunca se inverte no meio da batida
		float sweep = MathHelper.lerp(open, 0.10f, 0.14f + 0.36f * MathHelper.sin(phase));
		for (int i = 0; i < 3; i++) {
			float lag = phase - i * 0.35f; // só o balanço no plano da asa tem atraso, para a batida ficar fluida
			float droop = MathHelper.lerp(open, droopFold[i] + MathHelper.sin(t * 0.05f + i) * 0.02f,
					droopOpen[i] + 0.09f * MathHelper.sin(lag + 1.2f));
			float size = MathHelper.lerp(open, sizeFold[i], sizeOpen[i]);
			float ay = MathHelper.lerp(open, ayFold[i], ayOpen[i]);
			float pitch = MathHelper.lerp(open, 0f, -0.12f);
			for (int side = -1; side <= 1; side += 2) {
				wing(m, vcp, FULL_BRIGHT, side < 0 ? SERA_L : SERA_R, side, open, 1.6f + i * 0.2f, ay, 3.3f + zOff[i], size,
						sweep, droop, pitch, shade[i]);
			}
		}
	}

	// ================================================================== desenho

	@Override
	public void render(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float limbAngle,
					   float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
		if (e.isInvisible()) return;
		boolean garg = DakuraConfig.cosmetic(e, "stone_wings");
		boolean phoenix = DakuraConfig.cosmetic(e, "phoenix_wings");
		boolean fairy = DakuraConfig.cosmetic(e, "fairy_wings");
		boolean seraph = DakuraConfig.cosmetic(e, "seraph_wings");
		boolean bird = DakuraConfig.cosmetic(e, "bird_wing");
		if (!garg && !phoenix && !fairy && !seraph && !bird) return;
		float[] st = state(e, animationProgress);
		m.push();
		getContextModel().body.rotate(m);
		m.scale(1f / 16f, 1f / 16f, 1f / 16f); // a partir daqui 1 unidade = 1 pixel do modelo
		if (garg) gargoyle(m, vcp, light, e, animationProgress, st);
		if (phoenix) phoenix(m, vcp, e, animationProgress, st);
		if (fairy) fairy(m, vcp, e, animationProgress, st);
		if (seraph) seraph(m, vcp, e, animationProgress, st);
		if (bird) birdWing(m, vcp, light, e, animationProgress, st);
		m.pop();
	}
}
