package com.dakura.origins.client.render;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.client.anim.AbilityAnimations;
import com.dakura.origins.client.config.DakuraConfig;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.types.FlightPower;
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
 * animado de forma independente.
 * <p>
 * PADRÃO GLOBAL (todas as raças aladas):
 * <ul>
 * <li>Gárgula e Fada: no chão as asas ficam imóveis; no voo batem (cada uma com seu peso e ritmo).</li>
 * <li>Fênix, Serafim e Vastaya: duas sprites. Em solo a sprite RECOLHIDA fica pendurada como uma capa, com
 * física própria (mola amortecida: balança com o movimento, abre ao cair, bate ao pousar, quica); no voo
 * a sprite ABERTA bate. A troca entre as duas é feita por interpolação, sem salto.</li>
 * <li>"Voando" vem do servidor (flag fly_air do poder de voo); sprint/pose deitada vem de fly_lean / sprint.</li>
 * </ul>
 * Tamanhos (1 bloco = 16 px do modelo): Fada = altura do corpo; Serafim, Fênix e Vastaya = 2 blocos.
 * A escala da raça (a Fada é pequena) já é aplicada pelo renderer do jogador, então os números valem para todas.
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

	// ---- tamanhos: pixels do modelo por pixel da imagem
	/** Fada: asa de 160 px de imagem → 32 px = a altura do corpo. */
	private static final float FADA_SIZE = 0.20f;
	/** Gárgula: sem medida pedida, mantida. */
	private static final float GARG_SIZE = 0.15f;
	/** Serafim, par de trás/meio/da frente: 200 px de imagem → 32 px (2 blocos) no par de trás. */
	private static final float[] SERA_OPEN = {0.160f, 0.145f, 0.130f};
	private static final float[] SERA_FOLD = {0.150f, 0.135f, 0.120f};

	/** Asa de penas (a mesma arte da Fênix e da Vastaya Ave): constantes de cada raça. */
	private record Plume(float foldSize, float openSize, float k, float c,
						 float dBase0, float dBase1, float dAmp0, float dAmp1,
						 float sBase0, float sBase1, float sAmp0, float sAmp1, float ax, float ay) {}

	/** Fênix: 256 px de imagem → 32 px (2 blocos) aberta. Bate largo; no sprint fecha e acelera. Penas leves e quicantes. */
	private static final Plume PHOENIX = new Plume(0.110f, 0.125f, 0.16f, 0.17f,
			0.20f, 0.38f, 0.55f, 0.36f, 0.32f, 0.18f, 0.26f, 0.16f, 2.4f, 2.0f);
	/**
	 * Vastaya de Ave: 2 blocos também. ESTRUTURA EXCLUSIVA: a dinâmica de voo própria dela entra aqui
	 * (hoje só planeia: abre ao planar, com batida suave) sem herdar nada da Fênix.
	 */
	private static final Plume VASTAYA = new Plume(0.105f, 0.125f, 0.20f, 0.20f,
			0.25f, 0.30f, 0.18f, 0.18f, 0.35f, 0.30f, 0.14f, 0.14f, 1.6f, 2.2f);

	// ---- estado suave por jogador (float[40])
	private static final int S_OPEN = 0, S_PHASE = 1, S_LAST = 2, S_DT = 3, S_FAST = 4, S_SPRING = 8;
	private static final Map<AbstractClientPlayerEntity, float[]> STATE = new WeakHashMap<>();
	private static final Vector3f SCRATCH = new Vector3f();
	/** "Vento" do movimento do quadro: {frente, lado, vertical, giro, velocidade horizontal}. */
	private static final float[] WIND = new float[5];

	public WingFeatureRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) {
		super(ctx);
	}

	// ================================================================== utilidades

	private static float[] state(AbstractClientPlayerEntity e, float t) {
		float[] st = STATE.computeIfAbsent(e, k -> {
			float[] a = new float[40];
			a[S_LAST] = Float.NaN;
			return a;
		});
		float dt = Float.isNaN(st[S_LAST]) ? 100f : t - st[S_LAST];
		if (dt < 0f || dt > 5f) dt = 100f; // primeiro quadro / mundo recarregado: encaixa direto no alvo
		st[S_LAST] = t;
		st[S_DT] = dt;
		return st;
	}

	/** Aproxima o canal {@code slot} do alvo (constante de tempo ~ 1/rate ticks). */
	private static float approach(float[] st, int slot, float target, float rate) {
		float k = 1f - (float) Math.exp(-st[S_DT] * rate);
		st[slot] += (target - st[slot]) * k;
		return st[slot];
	}

	/** Avança a fase da batida (contínua mesmo quando a frequência muda). */
	private static void advance(float[] st, float freq) {
		st[S_PHASE] += Math.min(st[S_DT], 5f) * freq;
		if (st[S_PHASE] > 6283f) st[S_PHASE] -= 6283f;
	}

	/** Movimento do jogador neste quadro, nos eixos do corpo (frente / lado direito / vertical / giro). */
	private static float[] wind(AbstractClientPlayerEntity e) {
		double vx = e.getX() - e.prevX, vy = e.getY() - e.prevY, vz = e.getZ() - e.prevZ;
		double yaw = Math.toRadians(e.bodyYaw);
		double fx = -Math.sin(yaw), fz = Math.cos(yaw);
		WIND[0] = (float) (vx * fx + vz * fz);
		WIND[1] = (float) (-vx * fz + vz * fx);
		WIND[2] = (float) vy;
		WIND[3] = MathHelper.clamp(MathHelper.wrapDegrees(e.bodyYaw - e.prevBodyYaw), -40f, 40f);
		WIND[4] = (float) Math.sqrt(vx * vx + vz * vz);
		return WIND;
	}

	/** Mola amortecida de duas variáveis (inclinação no plano "droop" e varredura "sweep") guardada em st[b..b+3]. */
	private static void spring(float[] st, int b, float eqDroop, float eqSweep, float k, float c, float dt) {
		int n = Math.max(1, (int) Math.ceil(dt));
		float h = dt / n;
		for (int i = 0; i < n; i++) {
			st[b + 1] += (k * (eqDroop - st[b]) - c * st[b + 1]) * h;
			st[b] += st[b + 1] * h;
			st[b + 3] += (k * (eqSweep - st[b + 2]) - c * st[b + 3]) * h;
			st[b + 2] += st[b + 3] * h;
		}
	}

	/**
	 * Física de uma asa pendurada (capa): o movimento muda para onde a mola quer chegar e a mola quica até assentar.
	 * Andar para a frente joga a asa para trás; cair abre (ela "pega vento"); pular ou pousar faz ela bater para baixo e
	 * quicar; andar de lado leva uma asa para fora e a outra para perto do corpo; girar abre as duas.
	 */
	private static void hang(float[] st, int slot, int side, float[] w, float t, float k, float c, float gain, float phaseOff) {
		float sweepEq = MathHelper.clamp(-w[0] * 2.6f, -0.55f, 0.45f) * gain + 0.02f * MathHelper.sin(t * 0.05f + 1f + phaseOff);
		float droopEq = MathHelper.clamp(w[2] * 0.9f, -0.5f, 0.3f) * gain
				+ side * MathHelper.clamp(w[1] * 2.0f, -0.4f, 0.4f) * gain
				- Math.min(Math.abs(w[3]) * 0.03f, 0.35f) * gain
				+ 0.03f * MathHelper.sin(t * 0.07f + phaseOff);
		spring(st, S_SPRING + slot * 4, droopEq, sweepEq, k, c, Math.min(st[S_DT], 5f));
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
	 * @param droop  inclinação no plano da asa (rad): + abaixa a ponta (asa pendurada), - abre para fora/cima
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

	/** Gárgula: asas de morcego de pedra. IMÓVEIS no chão (e como estátua); no voo batem pesado: golpe forte, retorno lento. */
	private void gargoyle(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float[] st, float[] w) {
		boolean stone = EntityFlags.clientHas(e, "statue") || EntityFlags.clientHas(e, "meteor");
		boolean flying = EntityFlags.clientHas(e, FlightPower.FLAG_AIR) && !stone;
		float open = approach(st, S_OPEN, flying ? 1f : 0f, 0.28f);
		if (flying) advance(st, 0.46f * (1f + Math.min(w[4], 0.5f) * 0.6f));
		float ph = st[S_PHASE];
		// peso: o golpe para baixo tem o dobro do impulso, a subida demora, e a amplitude é a maior de todas as raças
		float beat = MathHelper.sin(ph) + 0.35f * MathHelper.sin(2f * ph - 0.6f);
		float droop = MathHelper.lerp(open, 1.15f, -0.08f + 0.70f * beat);
		float sweep = MathHelper.lerp(open, 0.75f, 0.22f + 0.34f * MathHelper.sin(ph - 1.2f));
		for (int side = -1; side <= 1; side += 2) {
			wing(m, vcp, light, side < 0 ? GARG_L : GARG_R, side, 1f, 2.2f, 1.0f, 3.3f, GARG_SIZE, sweep, droop, 0f, 255);
		}
	}

	/** Fênix: duas asas (a mesma arte espelhada). Pendura no chão como capa; voa como Elytra, normal ou rápido (sprint). */
	private void phoenix(MatrixStack m, VertexConsumerProvider vcp, AbstractClientPlayerEntity e, float t, float[] st, float[] w) {
		boolean show = EntityFlags.clientHas(e, FlightPower.FLAG_AIR) || EntityFlags.clientHas(e, "solar_dive")
				|| EntityFlags.clientHas(e, "reborn") || AbilityAnimations.active("wings_spread").contains(e.getId());
		float open = approach(st, S_OPEN, show ? 1f : 0f, 0.22f);
		float fast = approach(st, S_FAST, show && e.isSprinting() ? 1f : 0f, 0.15f);
		if (open > 0.5f) advance(st, MathHelper.lerp(fast, 0.30f, 0.42f) * (1f + Math.min(w[4], 0.6f) * 0.5f));
		for (int side = -1; side <= 1; side += 2) {
			featherWing(m, vcp, st, PHOENIX, side, side < 0 ? 0 : 1, open, fast, st[S_PHASE], t, w);
		}
	}

	/** Vastaya de Ave: UMA asa (mesma arte da Fênix), pendurada nas costas; só abre ao planar. Dinâmica própria em VASTAYA. */
	private void birdWing(MatrixStack m, VertexConsumerProvider vcp, AbstractClientPlayerEntity e, float t, float[] st, float[] w) {
		boolean glide = EntityFlags.clientHas(e, "gliding");
		float open = approach(st, S_OPEN, glide ? 1f : 0f, 0.25f);
		if (open > 0.5f) advance(st, 0.22f);
		featherWing(m, vcp, st, VASTAYA, -1, 0, open, 0f, st[S_PHASE], t, w);
	}

	/** Uma asa de penas completa: repouso pendurado com física → transição → aberta batendo. */
	private void featherWing(MatrixStack m, VertexConsumerProvider vcp, float[] st, Plume p, int side, int slot,
							 float open, float fast, float ph, float t, float[] w) {
		hang(st, slot, side, w, t, p.k, p.c, 1f, slot);
		int b = S_SPRING + slot * 4;
		float mid = 0.5f * (p.foldSize + p.openSize);
		Sprite sp;
		float size, droop, sweep;
		if (open < 0.5f) {
			float k = open * 2f;
			sp = FENIX_FOLDED;
			size = MathHelper.lerp(k, p.foldSize, mid);
			droop = MathHelper.lerp(k, 0.92f + st[b], 0.55f);
			sweep = MathHelper.lerp(k, 0.05f + st[b + 2], 0.55f);
		} else {
			float k = (open - 0.5f) * 2f;
			sp = FENIX_OPEN;
			size = MathHelper.lerp(k, mid, p.openSize);
			float dB = MathHelper.lerp(fast, p.dBase0, p.dBase1), dA = MathHelper.lerp(fast, p.dAmp0, p.dAmp1);
			float sB = MathHelper.lerp(fast, p.sBase0, p.sBase1), sA = MathHelper.lerp(fast, p.sAmp0, p.sAmp1);
			droop = MathHelper.lerp(k, 0.55f, dB + dA * MathHelper.sin(ph));
			sweep = MathHelper.lerp(k, 0.55f, sB + sA * MathHelper.sin(ph - 1.2f));
		}
		wing(m, vcp, FULL_BRIGHT, sp, side, 1f, p.ax, p.ay, 3.3f, size, sweep, droop, 0f, 255);
	}

	/** Fada: asas do tamanho do corpo. IMÓVEIS no chão; no voo batem muito rápido, leves e delicadas. */
	private void fairy(MatrixStack m, VertexConsumerProvider vcp, AbstractClientPlayerEntity e, float[] st, float[] w) {
		boolean flying = EntityFlags.clientHas(e, FlightPower.FLAG_AIR);
		float amp = approach(st, S_OPEN, flying ? 1f : 0f, 0.30f);
		if (flying) advance(st, 1.7f * (1f + Math.min(w[4], 0.5f) * 0.4f));
		float ph = st[S_PHASE];
		float beat = MathHelper.sin(ph) + 0.2f * MathHelper.sin(ph * 2f + 0.8f);
		float sweep = MathHelper.lerp(amp, 0.22f, 0.30f + 0.45f * beat);
		float droop = MathHelper.lerp(amp, -0.05f, -0.05f + 0.10f * MathHelper.sin(ph + 1.57f));
		for (int side = -1; side <= 1; side += 2) {
			wing(m, vcp, FULL_BRIGHT, side < 0 ? FADA_L : FADA_R, side, 1f, 1.2f, 3.8f, 3.3f, FADA_SIZE, sweep, droop, 0f, 255);
		}
	}

	/**
	 * Serafim: três pares (seis asas) da mesma arte, de 2 blocos. Pousado: capa de seis asas penduradas na diagonal,
	 * com física (cada par balança um pouco diferente). No ar: leque batendo; no voo rápido (sprint, corpo deitado)
	 * bate mais forte e mais depressa, como um pássaro.
	 */
	private void seraph(MatrixStack m, VertexConsumerProvider vcp, AbstractClientPlayerEntity e, float t, float[] st, float[] w) {
		boolean flying = EntityFlags.clientHas(e, FlightPower.FLAG_AIR);
		float open = approach(st, S_OPEN, flying ? 1f : 0f, 0.16f);
		float fast = approach(st, S_FAST, EntityFlags.clientHas(e, FlightPower.FLAG_LEAN) ? 1f : 0f, 0.14f);
		if (open > 0.02f) advance(st, MathHelper.lerp(fast, 0.24f, 0.40f) * (1f + Math.min(w[4], 0.6f) * 0.4f));
		float ph = st[S_PHASE];
		// 0 = par de trás (maior, mais escuro), 2 = par da frente (menor, mais claro)
		float[] ayFold = {1.0f, 1.8f, 2.6f}, ayOpen = {0.0f, 2.4f, 4.8f};
		float[] zOff = {0f, 0.8f, 1.6f};
		float[] droopFold = {-0.10f, -0.24f, -0.40f}, droopOpen = {0.62f, 0.34f, 0.06f};
		int[] shade = {185, 225, 255};
		float amp = MathHelper.lerp(fast, 0.09f, 0.14f);
		// mesma varredura nos três pares: os planos ficam paralelos e a ordem das camadas nunca se inverte no meio da batida
		float sweepOpen = MathHelper.lerp(fast, 0.14f, 0.10f) + MathHelper.lerp(fast, 0.36f, 0.30f) * MathHelper.sin(ph);
		for (int side = -1; side <= 1; side += 2) {
			int sideSlot = side < 0 ? 0 : 1;
			for (int i = 0; i < 3; i++) {
				int slot = i * 2 + sideSlot;
				hang(st, slot, side, w, t, 0.12f, 0.14f, 1f - i * 0.15f, i * 0.9f + sideSlot * 0.5f);
				int b = S_SPRING + slot * 4;
				int b0 = S_SPRING + sideSlot * 4; // a varredura do par de trás vale para os três (planos paralelos)
				float lag = ph - i * 0.35f; // só o balanço no plano da asa tem atraso, para a batida ficar fluida
				float droop = MathHelper.lerp(open, droopFold[i] + st[b], droopOpen[i] + amp * MathHelper.sin(lag + 1.2f));
				float sweep = MathHelper.lerp(open, 0.10f + st[b0 + 2], sweepOpen);
				float size = MathHelper.lerp(open, SERA_FOLD[i], SERA_OPEN[i]);
				float ay = MathHelper.lerp(open, ayFold[i], ayOpen[i]);
				float pitch = MathHelper.lerp(open, 0f, -0.12f);
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
		float[] w = wind(e);
		m.push();
		getContextModel().body.rotate(m);
		m.scale(1f / 16f, 1f / 16f, 1f / 16f); // a partir daqui 1 unidade = 1 pixel do modelo
		if (garg) gargoyle(m, vcp, light, e, st, w);
		if (phoenix) phoenix(m, vcp, e, animationProgress, st, w);
		if (fairy) fairy(m, vcp, e, st, w);
		if (seraph) seraph(m, vcp, e, animationProgress, st, w);
		if (bird) birdWing(m, vcp, e, animationProgress, st, w);
		m.pop();
	}
}
