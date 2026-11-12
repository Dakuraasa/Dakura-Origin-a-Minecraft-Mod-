package com.dakura.origins.client.render;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.client.anim.AbilityAnimations;
import com.dakura.origins.client.config.DakuraConfig;
import com.dakura.origins.client.geo.GeoModel;
import com.dakura.origins.power.EntityFlags;
import net.minecraft.client.MinecraftClient;
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
			BUNNY = tex("bunny_ears"), BUNNY_TAIL = tex("bunny_tail"), PHOENIX = tex("phoenix_wing");
	private static Identifier wing(String n) {
		return DakuraOrigins.id("textures/entity/wings/" + n + ".png");
	}
	private static final Identifier SPR_SERAPH = wing("seraph"), SPR_PHOENIX = wing("phoenix"), SPR_VASTAYA = wing("vastaya"),
			SPR_DRAGON_RED = wing("dragon_red"), SPR_DRAGON_BROWN = wing("dragon_brown");
	/** Física de capa por jogador: {balanço frente/trás, velocidade, balanço lateral, velocidade, quique}. */
	private static final Map<Integer, float[]> PHYS = new HashMap<>();
	/** Física das orelhas: {pitch, vel, yaw, vel, headYaw anterior, headPitch anterior}. */
	/** Abertura suave das asas (0 = recolhidas, 1 = abertas) por jogador e peça. */
	private static final Map<Long, Float> OPEN = new HashMap<>();

	public GeoPartsRenderer(FeatureRendererContext<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> ctx) {
		super(ctx);
	}

	// ------------------------------------------------------------------ estado

	private static boolean airborne(AbstractClientPlayerEntity e) {
		return !e.isOnGround() && !e.isTouchingWater() && !e.hasVehicle();
	}

	/** Fração do tick do quadro atual (para interpolar a física, que só anda uma vez por tick). */
	private static float pt;
	/** Fase acumulada por jogador/peça: mudar a frequência não faz a onda "pular" (era a causa do tremor ao começar a andar). */
	private static final Map<Long, float[]> PHASE = new HashMap<>();

	private static float phase(AbstractClientPlayerEntity e, int part, float freq, float t) {
		float[] ph = PHASE.computeIfAbsent(((long) e.getId() << 8) | part, k -> new float[]{0, t});
		float dt = MathHelper.clamp(t - ph[1], 0f, 2f);
		ph[1] = t;
		ph[0] += freq * dt;
		if (ph[0] > 10000f) ph[0] -= 6283.1853f;
		return ph[0];
	}

	/**
	 * Física de capa: integra uma vez por tick (antes integrava a cada quadro e a cada peça que chamava, por isso
	 * tremia e mudava com o FPS) e devolve o estado interpolado entre o tick anterior e o atual.
	 * {balanço frente/trás, vel, lateral, vel, quique, giro}; índice 6..11 = tick anterior, 12 = idade, 13 = velocidade suave.
	 */
	private static float[] phys(AbstractClientPlayerEntity e) {
		float[] st = PHYS.computeIfAbsent(e.getId(), k -> new float[16]);
		if (st[12] != e.age) {
			System.arraycopy(st, 0, st, 6, 6);
			st[15] = st[13];
			st[12] = e.age;
			step(e, st);
		}
		float[] out = new float[7];
		for (int i = 0; i < 6; i++) out[i] = MathHelper.lerp(pt, st[6 + i], st[i]);
		out[6] = MathHelper.lerp(pt, st[15], st[13]);
		return out;
	}

	private static void step(AbstractClientPlayerEntity e, float[] st) {
		double dx = e.getX() - e.prevX, dz = e.getZ() - e.prevZ, dy = e.getY() - e.prevY;
		st[13] += ((float) Math.sqrt(dx * dx + dz * dz) - st[13]) * 0.35f;
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
	}

	/** Velocidade horizontal suavizada (por tick, interpolada). */
	private static float speed(AbstractClientPlayerEntity e) {
		return phys(e)[6];
	}

	/** Transição suave entre recolhida e aberta. */
	private static float open(AbstractClientPlayerEntity e, int part, boolean target, float rate) {
		long key = ((long) e.getId() << 4) | part;
		float v = OPEN.getOrDefault(key, 0f);
		v += ((target ? 1f : 0f) - v) * Math.min(1f, rate * MinecraftClient.getInstance().getRenderTickCounter().getLastFrameDuration());
		OPEN.put(key, v);
		return v;
	}

	// ------------------------------------------------------------------ desenho

	@Override
	public void render(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float limbAngle,
					   float limbDistance, float tickDelta, float t, float headYaw, float headPitch) {
		if (e.isInvisible()) return;
		pt = tickDelta;
		if (DakuraConfig.cosmetic(e, "seraph_wings")) seraph(m, vcp, e, t);
		if (DakuraConfig.cosmetic(e, "bird_wing")) vastaya(m, vcp, light, e, t);
		if (DakuraConfig.cosmetic(e, "goat_horns")) goatHorns(m, vcp, light);
		boolean day = e.getWorld().getDimension().hasSkyLight() && (e.getWorld().getTimeOfDay() % 24000L < 12542
				|| e.getWorld().getTimeOfDay() % 24000L > 23460);
		if (DakuraConfig.cosmetic(e, "demon_horns") || (DakuraConfig.cosmetic(e, "succubus_horns") && !day)) horns(m, vcp, light, t);
		if (DakuraConfig.cosmetic(e, "mushroom_cap")) mushroom(m, vcp, light, e, t);
		if (DakuraConfig.cosmetic(e, "mermaid_tail")) mermaid(m, vcp, light, e, t, true);
		boolean red = DakuraConfig.cosmetic(e, "dragon_red"), brown = DakuraConfig.cosmetic(e, "dragon_brown");
		if (red || brown) dragon(m, vcp, light, e, t, brown, red);
		if (DakuraConfig.cosmetic(e, "fairy_wings")) fairy(m, vcp, light, e, t);
		if (DakuraConfig.cosmetic(e, "kitsune_tails")) foxTails(m, vcp, e, t, FOX_WHITE, kitsuneTails(e), 0xCFE8FF);
		else if (DakuraConfig.cosmetic(e, "kitsune_tails_dark"))
			foxTails(m, vcp, e, t, FOX_DARK, kitsuneTails(e), EntityFlags.clientHas(e, "purple_fire") ? 0xC24DFF : 0x8C8C9C);
		else if (DakuraConfig.cosmetic(e, "fox_tail")) foxTails(m, vcp, e, t, FOX_FIRE, 1, 0xFF9A4A);
		else if (DakuraConfig.cosmetic(e, "fox_tail_snow")) foxTails(m, vcp, e, t, FOX_SNOW, -1, 0xDDEBFF);
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
		float o = open(e, 0, airborne(e), 0.08f);
		float glow = Emissive.pulse(t, 0.05f, 0.35f, 0.5f);
		float[] size = {40f, 34f, 28f};             // par de cima ~2,5 blocos
		float[] rootY = {1.5f, 4.5f, 7.5f};
		float[] flyRoll = {-0.4f, 0.05f, 0.5f};     // leque: cima, meio, baixo
		float[] restRoll = {1.0f, 1.2f, 1.4f};      // no chão: caídas e recolhidas atrás
		m.push();
		getContextModel().body.rotate(m);
		for (int p = 2; p >= 0; p--) {
			float breathe = MathHelper.sin(t * 0.06f + p * 1.3f) * 0.035f * o;
			float roll = MathHelper.lerp(o, restRoll[p], flyRoll[p]) + breathe;
			float yaw = MathHelper.lerp(o, -1.0f, -0.25f - p * 0.08f);
			float pitch = MathHelper.lerp(o, 0.12f, 0.03f);
			for (int s = 1; s >= -1; s -= 2)
				spriteWing(m, vcp, SPR_SERAPH, Emissive.FULL_BRIGHT, s, 1.0f, rootY[p], 2.3f + p * 0.15f, size[p], 0.62f, 0.02f,
						pitch, yaw, roll, glow, 0xFFFFFF);
		}
		m.pop();
	}

	/**
	 * Asa em pixel art (sprites do Artur em textures/entity/wings): um quadro com a raiz no pivô, girado como os
	 * ossos antigos (pitch, yaw, roll). s = 1 lado esquerdo, -1 direito (espelhado). Tamanho em pixels do modelo.
	 * glow > 0 soma uma camada emissiva (shaders tratam como brilho).
	 */
	private static void spriteWing(MatrixStack m, VertexConsumerProvider vcp, Identifier tex, int light, int s, float px, float py, float pz,
								   float widthPx, float pivotY, float pivotX, float pitch, float yaw, float roll, float glow, int tint) {
		WingSprite sp = WingSprite.of(tex);
		float w = widthPx / 16f, h = w * sp.aspect;
		m.push();
		m.translate(s * px / 16f, py / 16f, pz / 16f);
		// ângulos são do lado esquerdo; o direito é o reflexo exato (yaw e roll invertidos), sem escala negativa
		m.multiply(new org.joml.Quaternionf().rotationY(s * yaw).rotateZ(s * roll).rotateX(pitch));
		float a0 = -pivotX * w, a1 = a0 + w, y0 = -pivotY * h, y1 = y0 + h;
		float x0 = s > 0 ? a0 : -a1, x1 = s > 0 ? a1 : -a0;
		float u0 = s > 0 ? 0 : 1, u1 = s > 0 ? 1 : 0;
		quad(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(tex)), x0, x1, y0, y1, u0, u1, 0, 1, light, 0xFFFFFFFF);
		if (glow > 0) quad(m, vcp.getBuffer(RenderLayer.getEyes(tex)), x0, x1, y0, y1, u0, u1, 0, 1, Emissive.FULL_BRIGHT, Emissive.argb(glow, tint));
		m.pop();
	}

	/** Proporção altura/largura de cada sprite (lida uma vez da textura). */
	private record WingSprite(float aspect) {
		private static final Map<Identifier, WingSprite> CACHE = new HashMap<>();

		static WingSprite of(Identifier id) {
			return CACHE.computeIfAbsent(id, k -> {
				try (var in = MinecraftClient.getInstance().getResourceManager().open(k)) {
					var img = net.minecraft.client.texture.NativeImage.read(in);
					float a = img.getHeight() / (float) img.getWidth();
					img.close();
					return new WingSprite(a);
				} catch (Exception ex) {
					return new WingSprite(0.66f);
				}
			});
		}
	}

	// ------------------------------------------------------------------ Fênix

	/** Asa de fogo espelhada dos dois lados: recolhida nas costas no chão, aberta e batendo no voo, brilho que cresce nos ataques. */
	/** Asas de fogo em sprite: somem no chão; no voo (ou nos ataques) abertas e batendo, brilho que cresce nos ataques. */
	private void phoenix(MatrixStack m, VertexConsumerProvider vcp, AbstractClientPlayerEntity e, float t) {
		boolean power = EntityFlags.clientHas(e, "solar_dive") || EntityFlags.clientHas(e, "reborn")
				|| AbilityAnimations.active("wings_spread").contains(e.getId());
		if (!airborne(e) && !power) return;
		float breathe = MathHelper.sin(t * 0.07f) * 0.035f;
		float glow = power ? Emissive.pulse(t, 0.6f, 0.75f, 1f) : Emissive.pulse(t, 0.2f, 0.5f, 0.65f);
		m.push();
		getContextModel().body.rotate(m);
		for (int s = 1; s >= -1; s -= 2)
			spriteWing(m, vcp, SPR_PHOENIX, Emissive.FULL_BRIGHT, s, 1.0f, 2.0f, 2.3f, 32f, 0.62f, 0.02f, 0.03f, -0.28f, -0.2f + breathe, glow, 0xFFB050);
		m.pop();
	}

	/** Vastaya Ave: uma asa só (lado esquerdo), visível apenas planando; aberta como élitro, com batidas leves. */
	private void vastaya(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float t) {
		if (!e.isFallFlying() && !EntityFlags.clientHas(e, "gliding")) return;
		float flap = MathHelper.sin(phase(e, 30, 0.12f + speed(e) * 0.4f, t)) * 0.12f;
		m.push();
		getContextModel().body.rotate(m);
		spriteWing(m, vcp, SPR_VASTAYA, light, 1, 1.5f, 3.0f, 2.4f, 35f, 0.665f, 0.06f, 0.1f, -0.1f, -0.15f - flap, 0, 0);
		m.pop();
	}

	/** Cabra: o mesmo modelo de chifres do Dragão, menor. */
	private void goatHorns(MatrixStack m, VertexConsumerProvider vcp, int light) {
		GeoModel h = GeoModel.get("dragon_horns");
		h.reset();
		m.push();
		getContextModel().getHead().rotate(m);
		m.translate(0, -8f / 16f, 0);
		m.scale(0.67f, 0.63f, 0.71f); // chifres um pouco mais grossos, mesmo formato e posição
		m.translate(0, 8f / 16f, 0);
		h.render(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(DHORNS)), light);
		m.pop();
	}

	// ------------------------------------------------------------------ chifres, chapéu, sereia

	private void horns(MatrixStack m, VertexConsumerProvider vcp, int light, float t) {
		GeoModel g = GeoModel.get("demon_horns");
		g.reset(); // pose fixa: nada balança (as pontas animadas eram o defeito)
		m.push();
		getContextModel().getHead().rotate(m);
		m.translate(0, -8f / 16f, 0);
		m.scale(0.78f, 0.78f, 0.78f);
		m.translate(0, 8f / 16f, 0);
		g.render(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(HORNS)), light);
		m.pop();
	}

	private void mushroom(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float t) {
		GeoModel g = GeoModel.get("mushroom_cap");
		g.reset();
		float[] st = phys(e);
		for (String b : new String[]{"hair_l", "hair_r", "hair_back"}) {
			var part = g.bone(b);
			if (part != null) part.visible = false; // abas laranja em volta da cabeça/pescoço: removidas
		}
		g.turn("cap", 0, 0, MathHelper.sin(t * 0.05f) * 0.015f);
		m.push();
		getContextModel().getHead().rotate(m);
		m.translate(0, -8f / 16f, 0);
		m.scale(0.86f, 0.86f, 0.86f);
		m.translate(0, 8f / 16f, 0);
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
		float[] st = phys(e);
		boolean grabbing = EntityFlags.clientHas(e, "skyfall");
		boolean air = airborne(e);
		float o = open(e, 1, air || grabbing, 0.12f);
		float breathe = MathHelper.sin(t * 0.06f) * 0.03f * o;
		m.push();
		getContextModel().body.rotate(m);
		for (int s = 1; s >= -1; s -= 2) {
			// repouso: caídas e recolhidas para trás, pesadas; voo: abertas em leve "V", paradas
			float pitch = MathHelper.lerp(o, 0.12f, 0.04f);
			float yaw = MathHelper.lerp(o, -1.1f, -0.25f);
			float roll = MathHelper.lerp(o, 0.85f, -0.15f) + breathe;
			spriteWing(m, vcp, brown ? SPR_DRAGON_BROWN : SPR_DRAGON_RED, light, s, 1.2f, 2.0f, 2.3f, 32f, 0.625f, 0.02f,
					pitch, yaw, roll, 0, 0);
		}
		m.pop();
		// cauda: ondulação lateral com defasagem entre os gomos, mais ampla e rápida conforme a velocidade
		GeoModel tl = GeoModel.get("dragon_tail");
		tl.reset();
		float v = Math.min(0.35f, speed(e));
		float freq = 0.07f + v * 1.5f, amp = 0.05f + v * 0.85f;
		// no voo o corpo fica deitado: a cauda desce ao longo dele, apontando para os pés
		// dormindo: a cauda repousa deitada ao longo do corpo, quase parada
		boolean sleeping = e.isSleeping();
		float rest = sleeping ? 1f : o;
		float lift = MathHelper.lerp(rest, 0f, -1.35f);
		amp *= 1f - 0.6f * rest;
		if (sleeping) amp *= 0.3f;
		float tph = phase(e, 41, freq, t), tph2 = phase(e, 42, freq * 2f, t);
		for (int k = 0; k < 12; k++) {
			float yaw = MathHelper.sin(tph - k * 0.55f) * amp * (0.3f + k * 0.06f) + st[2] * 0.07f + st[5] * 0.12f * (k + 1) / 12f;
			float pitch = (k == 0 ? lift + st[0] * 0.25f - st[4] * 0.2f : 0) + MathHelper.sin(tph2 - k * 0.45f) * amp * 0.12f
					+ st[4] * 0.04f * (1f - o);
			tl.turn("tail_" + k, pitch, yaw, 0);
		}
		tl.turn("tail_tip", 0, MathHelper.sin(tph - 7f) * amp * 0.6f, 0);
		m.push();
		getContextModel().body.rotate(m);
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
		boolean pivotScale = n != 1;
		float scale = n < 0 ? 1.2f : n <= 1 ? 1.0f : n <= 3 ? 1.32f : n <= 6 ? 1.2f : 1.1f;   // -1 = Raposa da Neve (cauda maior)
		n = Math.abs(n);
		float spreadMax = n <= 1 ? 0 : n <= 3 ? 0.75f : n <= 6 ? 1.15f : 1.45f;
		float glow = Emissive.pulse(t, 0.08f, 0.2f, 0.32f);
		boolean grounded = e.isOnGround() || !e.getWorld().isSpaceEmpty(e, e.getBoundingBox().offset(0, -0.6, 0));
		m.push();
		getContextModel().body.rotate(m);
		m.translate(0, -1.6f / 16f, 0);
		for (int i = 0; i < n; i++) {
			float k = n <= 1 ? 0 : (i / (float) (n - 1)) * 2f - 1f;   // -1..1
			// cada cauda com fase e ritmo próprios (fase acumulada: sem saltos quando a velocidade muda)
			float ph = phase(e, 50 + i, (0.09f + v * 0.6f) * (1f + i * 0.07f), t) + i * 1.7f;
			g.reset();
			// camadas alternadas (em cima/embaixo) para as caudas não se atravessarem
			float layer = n > 3 ? ((i % 2) * 2 - 1) * 0.16f : 0f;
			float yaw = k * spreadMax * (fan ? 1.3f : 1f) + st[2] * 0.5f + st[5] * 0.6f;
			// correndo, as caudas se esticam para trás acompanhando o corpo; no ar sobem; ao aterrissar afundam e quicam
			float stretch = MathHelper.clamp(v * 3.2f, 0f, 1f);
			float pitch = 0.14f - 0.1f * Math.abs(k) + layer + st[0] * 0.55f - stretch * 0.75f + st[4] * 0.45f - (fan ? 0.3f : 0);
			float tailYaw = yaw * (1f - stretch * 0.35f);
			g.turn("tail", pitch, tailYaw, k * 0.25f);
			for (int s = 1; s < 10; s++) {
				float wave = MathHelper.sin(ph - s * 0.55f) * (0.045f + v * 0.18f) * (1f - stretch * 0.5f);
				g.turn("fox_" + s, wave * 0.5f + st[4] * 0.04f - stretch * 0.04f, wave + st[2] * 0.05f + st[5] * 0.04f, 0);
			}
			// Kitsune: perto do chão, a cauda sobe o necessário para não entrar nos blocos.
			// Antes: a subida só existia "no chão" (sumia de uma vez ao pular), ia em degraus de 0.08, o lado era
			// redescoberto a cada quadro e a suavização dependia do FPS — daí os trancos. Agora a subida-alvo é
			// contínua (refinada por bisseção), o lado fica travado enquanto a cauda está erguida e a suavização
			// usa o tempo real entre quadros, nos dois sentidos.
			if (n > 1) {
				float[] lf = TAIL_LIFT.computeIfAbsent(e.getId() * 16 + i, x -> new float[]{0, t, 0});
				float target = 0f;
				float low = lowestPoint(g, pivotScale, scale);
				if (grounded && low > FLOOR) {
					float dir = lf[2];
					if (dir == 0) {
						g.turn("tail", pitch + 0.08f, tailYaw, k * 0.25f);
						dir = lowestPoint(g, pivotScale, scale) < low ? 1f : -1f;
						lf[2] = dir;
					}
					float okLift = 0f, badLift = 0f;
					boolean found = false;
					for (int it = 1; it <= 16; it++) {
						float tryLift = 0.08f * it * dir;
						g.turn("tail", pitch + tryLift, tailYaw, k * 0.25f);
						if (lowestPoint(g, pivotScale, scale) <= FLOOR) {
							okLift = tryLift;
							badLift = 0.08f * (it - 1) * dir;
							found = true;
							break;
						}
					}
					if (found) {
						for (int r = 0; r < 5; r++) {
							float mid = (okLift + badLift) * 0.5f;
							g.turn("tail", pitch + mid, tailYaw, k * 0.25f);
							if (lowestPoint(g, pivotScale, scale) <= FLOOR) okLift = mid;
							else badLift = mid;
						}
						target = okLift;
					} else target = 0.08f * 16 * dir;
				}
				float dt = MathHelper.clamp(t - lf[1], 0f, 3f);
				lf[1] = t;
				// subir rápido (para não afundar), descer devagar; independente do FPS
				float rate = Math.abs(target) > Math.abs(lf[0]) ? 0.55f : 0.12f;
				float alpha = 1f - (float) Math.pow(1f - rate, dt);
				lf[0] = MathHelper.lerp(alpha, lf[0], target);
				if (Math.abs(lf[0]) < 0.005f && target == 0f) {
					lf[0] = 0f;
					lf[2] = 0f; // cauda solta: na próxima vez o lado é redescoberto
				}
				g.turn("tail", pitch + lf[0], tailYaw, k * 0.25f);
			}
			m.push();
			// mais redondas e volumosas: engrossa a cauda sem alongar
			if (pivotScale) {
				// Kitsune: a cauda cresce a partir da própria raiz, que fica na mesma altura da Raposa de Fogo
				m.translate(0, (10.8f - 10.5f + 1.6f) / 16f, 0);
				m.translate(0, 10.5f / 16f, 2f / 16f);
				m.scale(scale * 1.18f, scale * 1.18f, scale);
				m.translate(0, -10.5f / 16f, -2f / 16f);
			} else m.scale(scale * 1.18f, scale * 1.18f, scale);
			g.render(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(tex)), Emissive.FULL_BRIGHT);
			m.pop();
		}
		m.pop();
	}

	/** Chão no espaço do corpo (pés = 24 px abaixo do pescoço), com folga para a espessura da cauda. */
	private static final float FLOOR = 1.5f - 0.22f;
	private static final Map<Integer, float[]> TAIL_LIFT = new HashMap<>();
	private static final String[] TAIL_CHAIN = {"tail", "fox_0", "fox_1", "fox_2", "fox_3", "fox_4", "fox_5", "fox_6", "fox_7", "fox_8", "fox_9"};

	/** Ponto mais baixo da cauda (y para baixo, espaço do corpo), seguindo a mesma escala usada no desenho. */
	private static float lowestPoint(GeoModel g, boolean pivotScale, float scale) {
		MatrixStack ms = new MatrixStack();
		ms.translate(0, -1.6f / 16f, 0);
		if (pivotScale) {
			ms.translate(0, (10.8f - 10.5f + 1.6f) / 16f, 0);
			ms.translate(0, 10.5f / 16f, 2f / 16f);
			ms.scale(scale * 1.18f, scale * 1.18f, scale);
			ms.translate(0, -10.5f / 16f, -2f / 16f);
		} else ms.scale(scale * 1.18f, scale * 1.18f, scale);
		g.root.rotate(ms);
		float low = -99f;
		org.joml.Vector3f v = new org.joml.Vector3f();
		for (String b : TAIL_CHAIN) {
			if (!g.has(b)) continue;
			g.bone(b).rotate(ms);
			v.set(0, 0, 0);
			ms.peek().getPositionMatrix().transformPosition(v);
			low = Math.max(low, v.y);
		}
		return low;
	}

	// ------------------------------------------------------------------ coelha

	/** Orelhas de coelho menores e paradas, e um pompom na traseira que treme de vez em quando. */
	private void bunny(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float t) {
		GeoModel g = GeoModel.get("bunny_ears");
		g.reset();
		m.push();
		getContextModel().getHead().rotate(m);
		m.translate(0, -0.5f, 0);
		m.scale(0.65f, 0.65f, 0.65f);
		m.translate(0, 0.5f, 0);
		g.render(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(BUNNY)), light);
		m.pop();
		GeoModel tail = GeoModel.get("bunny_tail");
		tail.reset();
		float cyc = (t + e.getId() * 13) % 80f;
		if (cyc > 72f) {
			float k = (80f - cyc) / 8f;
			float w = MathHelper.sin(cyc * 3.2f) * 0.14f * k;
			tail.turn("tail", 0, w, w * 0.75f);
		}
		m.push();
		getContextModel().body.rotate(m);
		tail.render(m, vcp.getBuffer(RenderLayer.getEntityCutoutNoCull(BUNNY_TAIL)), light);
		m.pop();
	}

	// ------------------------------------------------------------------ fada

	/**
	 * Asas de mariposa em pixel art (sprite desenhado pelo Artur), maiores que o corpo. No ar batem rápido e leve;
	 * pousada, param e ficam recolhidas para trás, juntinhas. Leve brilho mágico.
	 */
	private void fairy(MatrixStack m, VertexConsumerProvider vcp, int light, AbstractClientPlayerEntity e, float t) {
		boolean air = airborne(e) || AbilityAnimations.active("perch").contains(e.getId());
		if (!air) return; // no chão as asas somem
		float o = 1f;
		float beat = Math.abs(MathHelper.sin(phase(e, 60, e.isSprinting() || e.isFallFlying() ? 2.6f : 1.9f, t)));
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
			// sinal invertido: girando com +s as asas iam para a frente do corpo
			m.multiply(RotationAxis.POSITIVE_Y.rotation(-s * MathHelper.lerp(o, rest, flyOpen)));
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
