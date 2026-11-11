package com.dakura.origins.client.render;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.client.anim.AbilityAnimations;
import com.dakura.origins.client.config.DakuraConfig;
import com.dakura.origins.client.geo.GeoModel;
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

import java.util.HashMap;
import java.util.Map;

/**
 * Peças do corpo feitas no Blockbench (assets/dakura_origins/geo): asas do Serafim e da Fênix, chifres dos demônios
 * e do Dracônico, chapéu da Dama Cogumelo, cauda da Sereia, asas e cauda dos dragões, caudas de raposa (Kitsune,
 * Raposa de Fogo e de Neve), orelhas da Vastaya Coelha e as asas em pixel art da Fada.
 * Tudo com física secundária (inércia, balanço, quique) e, nas peças mágicas, brilho emissivo ({@link Emissive}).
 */
public class GeoPartsRenderer extends FeatureRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {
	private static Identifier tex(String name) {
		return DakuraOrigins.id("textures/entity/geo/" + name + ".png");
	}

	private static final Identifier SERAPH = tex("seraph_wings"), HORNS = tex("demon_horns"),
			CAP = tex("mushroom_cap"), MERMAID = tex("mermaid_tail"), MERMAID_DARK = tex("mermaid_tail_dark"),
			DWINGS_RED = tex("dragon_wings"), DWINGS_BROWN = tex("dragon_wings_brown"), DTAIL_RED = tex("dragon_tail"),
			DTAIL_BROWN = tex("dragon_tail_brown"), DHORNS = tex("dragon_horns"), FAIRY = tex("fairy_wings"),
			FOX_FIRE = tex("fox_tail"), FOX_SNOW = tex("fox_tail_snow"), FOX_WHITE = tex("fox_tail_white"), FOX_DARK = tex("fox_tail_dark"),
			BUNNY = tex("bunny_ears"), PHOENIX = tex("phoenix_wing");
	/** Física de capa por jogador: {balanço frente/trás, velocidade, balanço lateral, velocidade, quique}. */
	private static final Map<Integer, float[]> PHYS = new HashMap<>();
	/** Física das orelhas: {pitch, vel, yaw, vel, headYaw anterior, headPitch anterior}. */
	private static final Map<Integer, float[]> EARS = new HashMap<>();
	/** Abertura suave das asas (0 = recolhidas, 1 = abertas) por jogador e peça. */
	private static final Map<Long, Float> OPEN = new HashMap<>();

	public GeoPartsRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) {
		super(ctx);
	}

	// ------------------------------------------------------------------ estado

	private static boolean airborne(AbstractClientPlayerEntity e) {
		return !e.isOnGround() && !e.isTouchingWater() && !e.hasVehicle();
	}

	private static float[] phys(AbstractClientPlayerEntity e) {
		float[] st = PHYS.computeIfAbsent(e.getId(), k -> new float[6]);
		double dx = e.getX() - e.prevX, dz = e.getZ() - e.prevZ, dy = e.getY() - e.prevY;
		double yaw = Math.toRadians(e.bodyYaw);
		double fwd = -dx * Math.sin(yaw) + dz * Math.cos(yaw), side = dx * Math.cos(yaw) + dz * Math.sin(yaw);
		float tx = (float) MathHelper.clamp(fwd * 2.4, -0.4, 0.9), ty = (float) MathHelper.clamp(side * 2.0, -0.6, 0.6);
		st[1] += ((tx - st[0]) * 0.3f - st[1] * 0.2f) * 0.5f;
		st[0] += st[1] * 0.5f;
		st[3] += ((ty - st[2]) * 0.3f - st[3] * 0.2f) * 0.5f;
		st[2] += st[3] * 0.5f;
		float tb = (float) MathHelper.clamp(-dy * 2.0, -0.6, 0.6);
		st[4] += (tb - st[4]) * 0.25f;
		// mudança brusca de direção: o corpo gira e as peças ficam para trás
		float turn = MathHelper.wrapDegrees(e.bodyYaw - e.prevBodyYaw) * 0.02f;
		st[5] += (MathHelper.clamp(turn, -0.5f, 0.5f) - st[5]) * 0.2f;
		return st;
	}

	private static float speed(AbstractClientPlayerEntity e) {
		double dx = e.getX() - e.prevX, dz = e.getZ() - e.prevZ;
		return (float) Math.sqrt(dx * dx + dz * dz);
	}

	/** Transição suave entre recolhida e aberta. */
	private static float open(AbstractClientPlayerEntity e, int part, boolean target, float rate) {
		long key = ((long) e.getId() << 4) | part;
		float v = OPEN.getOrDefault(key, 0f);
		v += ((target ? 1f : 0f) - v) * rate;
		OPEN.put(key, v);
		return v;
	}

	// ------------------------------------------------------------------ desenho

	@Override
	public void render(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float limbAngle,
					   float limbDistance, float tickDelta, float t, float headYaw, float headPitch) {
		if (e.isInvisible()) return;
		if (DakuraConfig.cosmetic(e, "seraph_wings")) seraph(m, vcp, e, t);
		boolean day = e.getWorld().getDimension().hasSkyLight() && (e.getWorld().getTimeOfDay() % 24000L < 12542
				|| e.getWorld().getTimeOfDay() % 24000L > 23460);
		if (DakuraConfig.cosmetic(e, "demon_horns") || (DakuraConfig.cosmetic(e, "succubus_horns") && !day)) horns(m, vcp, light, t);
		if (DakuraConfig.cosmetic(e, "mushroom_cap")) mushroom(m, vcp, light, e, t);
		if (DakuraConfig.cosmetic(e, "mermaid_tail") && e.isTouchingWater()) mermaid(m, vcp, light, e, t, true);
		boolean red = DakuraConfig.cosmetic(e, "dragon_red"), brown = DakuraConfig.cosmetic(e, "dragon_brown");
		if (red || brown) dragon(m, vcp, light, e, t, brown, red);
		if (DakuraConfig.cosmetic(e, "fairy_wings")) fairy(m, vcp, light, e, t);
		if (DakuraConfig.cosmetic(e, "kitsune_tails")) foxTails(m, vcp, e, t, FOX_WHITE, kitsuneTails(e), 0xCFE8FF);
		else if (DakuraConfig.cosmetic(e, "kitsune_tails_dark"))
			foxTails(m, vcp, e, t, FOX_DARK, kitsuneTails(e), EntityFlags.clientHas(e, "purple_fire") ? 0xC24DFF : 0x7A4CC0);
		else if (DakuraConfig.cosmetic(e, "fox_tail")) foxTails(m, vcp, e, t, FOX_FIRE, 1, 0xFF9A4A);
		else if (DakuraConfig.cosmetic(e, "fox_tail_snow")) foxTails(m, vcp, e, t, FOX_SNOW, 1, 0xDDEBFF);
		if (DakuraConfig.cosmetic(e, "bunny_ears")) bunny(m, vcp, light, e, t);
		if (DakuraConfig.cosmetic(e, "phoenix_wings")) phoenix(m, vcp, e, t);
	}

	/** Número de caudas da Kitsune pelo estágio de maturidade: 3, 6, 9. */
	public static int kitsuneTails(AbstractClientPlayerEntity e) {
		return EntityFlags.clientHas(e, "mat_2") ? 9 : EntityFlags.clientHas(e, "mat_1") ? 6 : 3;
	}

	// ------------------------------------------------------------------ Serafim

	/**
	 * Seis asas. No chão: recolhidas e dobradas sobre si mesmas, caídas como uma capa, com balanço e quique.
	 * No voo: abertas, batendo devagar e "preguiçosas", cada par num ritmo próprio. Brilho branco permanente.
	 */
	private void seraph(MatrixStack m, VertexConsumerProvider vcp, AbstractClientPlayerEntity e, float t) {
		GeoModel g = GeoModel.get("seraph_wings");
		g.reset();
		float[] st = phys(e);
		boolean air = airborne(e);
		float o = open(e, 0, air, 0.06f);
		boolean fast = air && e.isSprinting();
		for (int p = 0; p < 3; p++) {
			// cada par tem frequência e fase próprias: nada de bater tudo junto
			float freq = (fast ? 0.2f : 0.12f) + p * 0.023f, amp = (fast ? 0.5f : 0.38f) - p * 0.04f;
			float ph = t * freq - p * 1.35f;
			float flap = MathHelper.sin(ph) * amp;
			for (int s = 1; s >= -1; s -= 2) {
				String w = "wing_" + p + "_" + (s > 0 ? "l" : "r");
				// repouso: asas fechadinhas, inclinadas para baixo e para trás, balançando com o corpo
				float capeRoll = s * (1.45f - p * 0.12f) + s * st[2] * 0.3f - s * st[5] * 0.4f;
				float capeYaw = -s * (0.85f + p * 0.1f) + s * st[5] * 0.3f;
				float capePitch = 0.18f + p * 0.06f + st[0] * 0.6f + st[4] * 0.35f + MathHelper.sin(t * 0.05f + p) * 0.025f;
				float flyRoll = -s * (0.4f - p * 0.4f) - s * flap;
				float flyYaw = -s * ((fast ? 0.5f : 0.2f) + MathHelper.cos(ph) * 0.08f);
				float flyPitch = fast ? 0.25f : 0.05f;
				g.set(w, MathHelper.lerp(o, capePitch, flyPitch), MathHelper.lerp(o, capeYaw, flyYaw), MathHelper.lerp(o, capeRoll, flyRoll));
				// antebraço dobrado sobre o braço em repouso; no voo segue o batimento com atraso
				g.turn(w + "_fore", 0, MathHelper.lerp(o, -s * 0.5f, 0), MathHelper.lerp(o, s * 1.05f, -s * MathHelper.sin(ph - 0.5f) * amp * 0.5f));
				for (int k = 0; k < 5; k++) {
					float[] r = g.restOf(w + "_pri_" + k);
					g.turn(w + "_pri_" + k, 0, 0, MathHelper.lerp(o, -r[2] * 0.85f, MathHelper.sin(ph - 0.9f - k * 0.15f) * 0.06f * k));
				}
				for (int k = 0; k < 4; k++) {
					float[] r = g.restOf(w + "_sec_" + k);
					g.turn(w + "_sec_" + k, 0, 0, MathHelper.lerp(o, -r[2] * 0.6f, 0));
				}
			}
		}
		m.push();
		getContextModel().body.rotate(m);
		Emissive.geo(g, m, vcp, SERAPH, Emissive.pulse(t, 0.05f, 0.35f, 0.5f), 0xFFFFFF);
		m.pop();
	}

	// ------------------------------------------------------------------ Fênix

	/** Asa de fogo espelhada dos dois lados: recolhida nas costas no chão, aberta e batendo no voo, brilho que cresce nos ataques. */
	private void phoenix(MatrixStack m, VertexConsumerProvider vcp, AbstractClientPlayerEntity e, float t) {
		GeoModel g = GeoModel.get("phoenix_wing");
		float[] st = phys(e);
		boolean power = EntityFlags.clientHas(e, "solar_dive") || EntityFlags.clientHas(e, "reborn")
				|| AbilityAnimations.active("wings_spread").contains(e.getId());
		boolean air = airborne(e);
		float o = open(e, 2, air || power, 0.1f);
		float flap = MathHelper.sin(t * (power ? 0.55f : 0.32f)) * (power ? 0.55f : 0.42f);
		g.reset();
		float roll = MathHelper.lerp(o, 1.3f + st[2] * 0.2f, -0.3f - flap);
		float yaw = MathHelper.lerp(o, -0.75f + st[5] * 0.3f, -0.15f);
		float pitch = MathHelper.lerp(o, 0.2f + st[0] * 0.5f + st[4] * 0.3f, 0.05f);
		g.set("wing", pitch, yaw, roll);
		g.turn("phx_arm", 0, 0, 0);
		g.turn("phx_fore", 0, MathHelper.lerp(o, -0.4f, 0), MathHelper.lerp(o, 0.9f, -MathHelper.sin(t * 0.32f - 0.6f) * 0.25f));
		for (int k = 0; k < 5; k++) {
			float[] r = g.restOf("phx_pri_" + k);
			g.turn("phx_pri_" + k, 0, 0, MathHelper.lerp(o, -r[2] * 0.8f, MathHelper.sin(t * 0.32f - 1f - k * 0.2f) * 0.07f * k));
		}
		float glow = power ? Emissive.pulse(t, 0.6f, 0.75f, 1f) : air ? Emissive.pulse(t, 0.2f, 0.5f, 0.65f) : 0.28f;
		m.push();
		getContextModel().body.rotate(m);
		for (int s = 1; s >= -1; s -= 2) {
			m.push();
			if (s < 0) m.scale(-1, 1, 1);
			Emissive.geo(g, m, vcp, PHOENIX, glow, 0xFFB050);
			m.pop();
		}
		m.pop();
	}

	// ------------------------------------------------------------------ chifres, chapéu, sereia

	private void horns(MatrixStack m, VertexConsumerProvider vcp, int light, float t) {
		GeoModel g = GeoModel.get("demon_horns");
		g.reset();
		for (int s = 1; s >= -1; s -= 2) {
			String sn = s > 0 ? "l" : "r";
			g.turn("flame_" + sn, 0, 0, MathHelper.sin(t * 0.5f + s) * 0.18f);
			g.turn("flame_" + sn + "_tip", 0, 0, MathHelper.sin(t * 0.7f + s * 2) * 0.3f);
			g.turn("wisp_" + sn, 0, 0, MathHelper.sin(t * 0.2f + s) * 0.1f);
		}
		m.push();
		getContextModel().getHead().rotate(m);
		g.render(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(HORNS)), light);
		m.pop();
	}

	private void mushroom(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float t) {
		GeoModel g = GeoModel.get("mushroom_cap");
		g.reset();
		float[] st = phys(e);
		g.turn("hair_l", st[0] * 0.3f, 0, -st[2] * 0.2f + MathHelper.sin(t * 0.08f) * 0.03f);
		g.turn("hair_r", st[0] * 0.3f, 0, -st[2] * 0.2f - MathHelper.sin(t * 0.08f) * 0.03f);
		g.turn("hair_back", st[0] * 0.4f, 0, 0);
		g.turn("cap", 0, 0, MathHelper.sin(t * 0.05f) * 0.015f);
		m.push();
		getContextModel().getHead().rotate(m);
		g.render(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(CAP)), light);
		m.pop();
	}

	private void mermaid(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float t, boolean dark) {
		GeoModel g = GeoModel.get("mermaid_tail");
		g.reset();
		boolean swim = e.isSwimming();
		float speed = swim ? 0.42f : 0.16f, amp = swim ? 0.26f : 0.1f;
		for (int k = 0; k < 8; k++) g.turn("tail_" + k, MathHelper.sin(t * speed - k * 0.7f) * amp * (0.5f + k * 0.12f) - (swim ? 0.06f * k : 0), 0,
				MathHelper.sin(t * speed * 0.5f - k * 0.5f) * 0.03f);
		g.turn("fluke", MathHelper.sin(t * speed - 6f) * 0.3f, 0, 0);
		for (int j = 0; j < 7; j++) g.turn("fluke_ray_" + j, MathHelper.sin(t * speed * 1.3f - j * 0.6f) * 0.12f, 0, 0);
		g.turn("hip_fin_l", 0, 0, -MathHelper.sin(t * 0.2f) * 0.2f);
		g.turn("hip_fin_r", 0, 0, MathHelper.sin(t * 0.2f) * 0.2f);
		m.push();
		getContextModel().body.rotate(m);
		g.render(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(dark ? MERMAID_DARK : MERMAID)), light);
		m.pop();
	}

	// ------------------------------------------------------------------ dragões

	private void dragon(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float t, boolean brown, boolean horns) {
		GeoModel w = GeoModel.get("dragon_wings");
		w.reset();
		float[] st = phys(e);
		boolean grabbing = EntityFlags.clientHas(e, "skyfall");
		boolean air = airborne(e);
		float o = open(e, 1, air || grabbing, 0.12f);
		float flap = MathHelper.sin(t * (grabbing ? 0.45f : 0.26f)) * 0.55f;
		for (int s = 1; s >= -1; s -= 2) {
			String sn = s > 0 ? "l" : "r";
			w.turn("wing_" + sn, MathHelper.lerp(o, 0.2f + st[0] * 0.3f, 0), MathHelper.lerp(o, -s * 0.95f, -s * 0.1f),
					MathHelper.lerp(o, s * 1.55f, -s * flap));
			w.turn("wing_" + sn + "_fore", 0, 0, MathHelper.lerp(o, s * 1.1f, s * flap * 0.35f));
			float[] fan = {-0.25f, 0.35f, 0.95f, 1.55f};
			for (int k = 0; k < 4; k++) w.turn("wing_" + sn + "_finger_" + k, 0, 0, MathHelper.lerp(o, -s * fan[k] * 0.8f, s * flap * 0.1f * k));
		}
		// cauda: ondulação lateral com defasagem entre os gomos, mais ampla e rápida conforme a velocidade
		GeoModel tl = GeoModel.get("dragon_tail");
		tl.reset();
		float v = Math.min(0.35f, speed(e));
		float freq = 0.07f + v * 1.5f, amp = 0.05f + v * 0.85f;
		float lift = air ? 0.4f : 0f;
		for (int k = 0; k < 12; k++) {
			float yaw = MathHelper.sin(t * freq - k * 0.55f) * amp * (0.3f + k * 0.06f) + st[2] * 0.07f + st[5] * 0.12f * (k + 1) / 12f;
			float pitch = (k == 0 ? lift + st[0] * 0.25f - st[4] * 0.2f : 0) + MathHelper.sin(t * freq * 2f - k * 0.45f) * amp * 0.12f
					+ (air ? -0.03f : 0f) + st[4] * 0.04f;
			tl.turn("tail_" + k, pitch, yaw, 0);
		}
		tl.turn("tail_tip", 0, MathHelper.sin(t * freq - 7f) * amp * 0.6f, 0);
		m.push();
		getContextModel().body.rotate(m);
		w.render(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(brown ? DWINGS_BROWN : DWINGS_RED)), light);
		tl.render(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(brown ? DTAIL_BROWN : DTAIL_RED)), light);
		m.pop();
		if (horns) {
			GeoModel h = GeoModel.get("dragon_horns");
			h.reset();
			m.push();
			getContextModel().getHead().rotate(m);
			h.render(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(DHORNS)), light);
			m.pop();
		}
	}

	// ------------------------------------------------------------------ raposas

	/**
	 * Caudas de raposa enormes e felpudas. Uma só (Raposa de Fogo/Neve) ou 3/6/9 em leque (Kitsune, conforme a
	 * maturidade). Inércia: ficam para trás nas corridas, balançam nas curvas, afundam e quicam ao aterrissar.
	 */
	private void foxTails(MatrixStack m, VertexConsumerProvider vcp, AbstractClientPlayerEntity e, float t, Identifier tex, int n, int tint) {
		GeoModel g = GeoModel.get("fox_tail");
		float[] st = phys(e);
		float v = Math.min(0.35f, speed(e));
		boolean fan = AbilityAnimations.active("fan").contains(e.getId());
		float spreadMax = n <= 1 ? 0 : n <= 3 ? 0.55f : n <= 6 ? 0.9f : 1.15f;
		float scale = n <= 1 ? 1.0f : n <= 3 ? 0.9f : n <= 6 ? 0.82f : 0.75f;
		float glow = Emissive.pulse(t, 0.08f, 0.2f, 0.32f);
		m.push();
		getContextModel().body.rotate(m);
		for (int i = 0; i < n; i++) {
			float k = n <= 1 ? 0 : (i / (float) (n - 1)) * 2f - 1f;   // -1..1
			float ph = i * 1.7f;
			g.reset();
			float yaw = k * spreadMax * (fan ? 1.3f : 1f) + st[2] * 0.5f + st[5] * 0.6f;
			// correndo, as caudas se esticam para trás; no ar sobem; ao aterrissar afundam e quicam
			float pitch = -0.1f * Math.abs(k) + st[0] * 0.55f - v * 0.6f + st[4] * 0.45f - (fan ? 0.3f : 0);
			g.turn("tail", pitch, yaw, k * 0.25f);
			for (int s = 1; s < 10; s++) {
				float wave = MathHelper.sin(t * (0.09f + v * 0.6f) + ph - s * 0.55f) * (0.045f + v * 0.25f);
				g.turn("fox_" + s, wave * 0.5f + st[4] * 0.04f - v * 0.05f, wave + st[2] * 0.05f + st[5] * 0.04f, 0);
			}
			m.push();
			m.scale(scale, scale, scale);
			Emissive.geo(g, m, vcp, tex, glow, tint);
			m.pop();
		}
		m.pop();
	}

	// ------------------------------------------------------------------ coelha

	/** Orelhas de coelho selvagem: pesadas e tombadas para trás, com inércia à cabeça, aos saltos e às corridas. */
	private void bunny(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float t) {
		float[] st = EARS.computeIfAbsent(e.getId(), k -> new float[]{0, 0, 0, 0, e.headYaw, e.getPitch()});
		double dx = e.getX() - e.prevX, dz = e.getZ() - e.prevZ, dy = e.getY() - e.prevY;
		double yawR = Math.toRadians(e.bodyYaw);
		double fwd = -dx * Math.sin(yawR) + dz * Math.cos(yawR), side = dx * Math.cos(yawR) + dz * Math.sin(yawR);
		float dHead = MathHelper.wrapDegrees(e.headYaw - st[4]), dPitch = e.getPitch() - st[5];
		st[4] = e.headYaw;
		st[5] = e.getPitch();
		// alvo: correndo ficam mais deitadas; subindo num salto ficam para baixo (atrasam); caindo, se erguem
		float targetP = (float) MathHelper.clamp(-fwd * 2.4 - dy * 1.8, -0.8, 0.7) - dPitch * 0.01f;
		float targetY = (float) MathHelper.clamp(side * 2.0, -0.5, 0.5) - MathHelper.clamp(dHead * 0.02f, -0.5f, 0.5f);
		st[1] += ((targetP - st[0]) * 0.25f - st[1] * 0.16f) * 0.5f;
		st[0] += st[1] * 0.5f;
		st[3] += ((targetY - st[2]) * 0.25f - st[3] * 0.16f) * 0.5f;
		st[2] += st[3] * 0.5f;
		boolean charge = EntityFlags.clientHas(e, "moon_charge") || EntityFlags.clientHas(e, "barrage");
		boolean rush = EntityFlags.clientHas(e, "rushing") || EntityFlags.clientHas(e, "moon_air");
		GeoModel g = GeoModel.get("bunny_ears");
		g.reset();
		for (int s = 1; s >= -1; s -= 2) {
			String sn = s > 0 ? "l" : "r";
			float twitch = ((int) (t + s * 37) % 131) < 3 ? MathHelper.sin(t * 2.2f) * 0.12f : 0f;
			g.turn("ear_" + sn, st[0] * 0.45f + (rush ? -0.4f : 0) + (charge ? -0.2f : 0) + MathHelper.sin(t * 0.06f + s) * 0.02f,
					st[2] * 0.35f, s * st[2] * 0.25f + s * twitch);
			for (int k = 1; k < 5; k++) {
				// cada gomo cai um pouco mais, com atraso: a ponta é a parte mais pesada
				float lag = MathHelper.sin(t * 0.08f + s - k * 0.6f) * 0.025f;
				g.turn("ear_" + sn + "_" + k, st[0] * 0.18f * k / 4f - st[1] * 0.3f + lag + (rush ? -0.08f : 0), st[2] * 0.08f, 0);
			}
		}
		m.push();
		getContextModel().getHead().rotate(m);
		g.render(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(BUNNY)), light);
		m.pop();
	}

	// ------------------------------------------------------------------ fada

	/**
	 * Asas de mariposa em pixel art (sprite desenhado pelo Artur), maiores que o corpo. No ar batem rápido e leve;
	 * pousada, param e ficam recolhidas para trás, juntinhas. Leve brilho mágico.
	 */
	private void fairy(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float t) {
		boolean air = airborne(e) || AbilityAnimations.active("perch").contains(e.getId());
		float o = open(e, 3, air, 0.18f);
		float beat = Math.abs(MathHelper.sin(t * 1.9f)) * o;
		VertexConsumer vc = vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(FAIRY));
		VertexConsumer gv = vcp.getBuffer(RenderLayer.getEyes(FAIRY));
		m.push();
		getContextModel().body.rotate(m);
		m.translate(0, 2.0f / 16f, 2.3f / 16f);
		float sc = 1.45f;
		m.scale(sc, sc, sc);
		for (int s = -1; s <= 1; s += 2) {
			m.push();
			// pousada: recolhidas para trás (quase fechadas uma contra a outra); voando: abertas e batendo
			float rest = 1.15f, flyOpen = 0.3f + beat * 0.9f;
			m.multiply(RotationAxis.POSITIVE_Y.rotation(s * MathHelper.lerp(o, rest, flyOpen)));
			m.multiply(RotationAxis.POSITIVE_X.rotation(MathHelper.lerp(o, 0.18f, 0f)));
			float wpx = 16f / 16f, top = -4.6f / 16f, bot = (30f - 4.6f) / 16f;
			float u0 = s < 0 ? 0f : 64f / 128f, u1 = s < 0 ? 52f / 128f : 116f / 128f;
			float x0 = s < 0 ? -wpx : 0, x1 = s < 0 ? 0 : wpx;
			quad(m, vc, x0, x1, top, bot, u0, u1, 0f, 98f / 128f, light, 0xFFFFFFFF);
			quad(m, gv, x0, x1, top, bot, u0, u1, 0f, 98f / 128f, Emissive.FULL_BRIGHT, Emissive.argb(0.18f + 0.12f * o, 0xC8FFB0));
			m.pop();
		}
		m.pop();
	}

	private static void quad(MatrixStack m, VertexConsumer vc, float x0, float x1, float y0, float y1, float u0, float u1, float v0,
							 float v1, int light, int argb) {
		MatrixStack.Entry en = m.peek();
		Matrix4f mat = en.getPositionMatrix();
		int r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
		vc.vertex(mat, x0, y0, 0).color(r, g, b, 255).texture(u0, v0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, 0, 0, 1);
		vc.vertex(mat, x0, y1, 0).color(r, g, b, 255).texture(u0, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, 0, 0, 1);
		vc.vertex(mat, x1, y1, 0).color(r, g, b, 255).texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, 0, 0, 1);
		vc.vertex(mat, x1, y0, 0).color(r, g, b, 255).texture(u1, v0).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(en, 0, 0, 1);
	}
}
