package com.dakura.origins.mixin.client;

import com.dakura.origins.client.anim.AbilityAnimations;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Poses das habilidades no modelo humanoide (jogadores, zumbis, aldeões...).
 * Sempre MISTURA a pose vanilla com a pose da habilidade (nada de "teleporte" de membros),
 * então andar, olhar e balançar continuam naturais por baixo.
 * Custo quando nada está tocando: um mapa vazio checado por canal.
 */
@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin {
	@Shadow @Final public ModelPart head;
	@Shadow @Final public ModelPart hat;
	@Shadow @Final public ModelPart rightArm;
	@Shadow @Final public ModelPart leftArm;
	@Shadow @Final public ModelPart rightLeg;
	@Shadow @Final public ModelPart leftLeg;
	@Shadow @Final public ModelPart body;

	@Inject(method = "setAngles(Lnet/minecraft/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
	private void dakura$abilityPoses(LivingEntity entity, float limbAngle, float limbDistance, float animationProgress,
									 float headYaw, float headPitch, CallbackInfo ci) {
		float p = AbilityAnimations.progress(entity, "bite", animationProgress);
		if (p >= 0f) poseBite(p, animationProgress);

		float v = AbilityAnimations.progress(entity, "bitten", animationProgress);
		if (v >= 0f) poseBitten(v, animationProgress);

		float b = AbilityAnimations.progress(entity, "breath", animationProgress);
		if (b >= 0f) poseBreath(b, animationProgress);

		float c = AbilityAnimations.progress(entity, "cast", animationProgress);
		if (c >= 0f) poseCast(c);

		float s = AbilityAnimations.progress(entity, "stomp", animationProgress);
		if (s >= 0f) poseStomp(s);

		float sun = AbilityAnimations.progress(entity, "solar", animationProgress);
		if (sun >= 0f) poseSolar(sun, animationProgress);

		float cr = AbilityAnimations.progress(entity, "crouch", animationProgress);
		if (cr >= 0f) poseCrouch(cr);

		float ram = AbilityAnimations.progress(entity, "ram", animationProgress);
		if (ram >= 0f) poseRam(ram);

		float lift = AbilityAnimations.progress(entity, "lift", animationProgress);
		if (lift >= 0f) poseLift(lift);

		float reach = AbilityAnimations.progress(entity, "reach", animationProgress);
		if (reach >= 0f) poseReach(reach, animationProgress);

		float punch = AbilityAnimations.progress(entity, "punch", animationProgress);
		if (punch >= 0f) posePunch(punch);

		float roar = AbilityAnimations.progress(entity, "roar", animationProgress);
		if (roar >= 0f) poseRoar(roar);

		float dance = AbilityAnimations.progress(entity, "dance", animationProgress);
		if (dance >= 0f) poseDance(dance, animationProgress);

		float puppet = AbilityAnimations.progress(entity, "puppet", animationProgress);
		if (puppet >= 0f) posePuppet(puppet, animationProgress);

		float fan = AbilityAnimations.progress(entity, "fan", animationProgress);
		if (fan >= 0f) poseFan(fan, animationProgress);

		float pray = AbilityAnimations.progress(entity, "pray", animationProgress);
		if (pray >= 0f) posePray(pray, animationProgress);

		float akaza = AbilityAnimations.progress(entity, "akaza_stomp", animationProgress);
		if (akaza >= 0f) poseAkaza(akaza);

		float dl = AbilityAnimations.progress(entity, "dodge_l", animationProgress);
		if (dl >= 0f) poseDodge(dl, 1f);
		float dr = AbilityAnimations.progress(entity, "dodge_r", animationProgress);
		if (dr >= 0f) poseDodge(dr, -1f);

		// ---- Sereia, Anúbis, Fênix, Vastaya Coelho
		float sing = AbilityAnimations.progress(entity, "sing", animationProgress);
		if (sing >= 0f) poseSing(sing, animationProgress);
		float surf = AbilityAnimations.progress(entity, "surf", animationProgress);
		if (surf >= 0f) poseSurf(surf, animationProgress);
		float judge = AbilityAnimations.progress(entity, "judge", animationProgress);
		if (judge >= 0f) poseJudge(judge, animationProgress);
		float sup = AbilityAnimations.progress(entity, "summon_up", animationProgress);
		if (sup >= 0f) poseSummonUp(sup);
		float wup = AbilityAnimations.progress(entity, "wings_up", animationProgress);
		if (wup >= 0f) poseWingsUp(wup);
		float dive = AbilityAnimations.progress(entity, "dive", animationProgress);
		if (dive >= 0f) poseDive(dive);
		float wsp = AbilityAnimations.progress(entity, "wings_spread", animationProgress);
		if (wsp >= 0f) poseWingsSpread(wsp, animationProgress);
		float land = AbilityAnimations.progress(entity, "land_pose", animationProgress);
		if (land >= 0f) poseLand(land);
		float reb = AbilityAnimations.progress(entity, "rebirth", animationProgress);
		if (reb >= 0f) poseRebirth(reb, animationProgress);
		float kr = AbilityAnimations.progress(entity, "kick_rush", animationProgress);
		if (kr >= 0f) poseKickRush(kr, animationProgress);
		float kh = AbilityAnimations.progress(entity, "kick_high", animationProgress);
		if (kh >= 0f) poseKickHigh(kh);
		float leap = AbilityAnimations.progress(entity, "leap", animationProgress);
		if (leap >= 0f) poseLeap(leap);
	}

	// ------------------------------------------------------------------ leva lendária

	/** Canto das Marés: braços abertos ondulando, cabeça para trás, como quem canta para o céu. */
	private void poseSing(float p, float t) {
		float w = AbilityAnimations.window(p, 0.00f, 0.08f, 0.92f, 1.00f);
		float wave = MathHelper.sin(t * 0.25f) * 0.25f;
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, -0.6f + wave);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, -0.6f - wave);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 1.1f + wave * 0.5f);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, -1.1f + wave * 0.5f);
		head.pitch = MathHelper.lerp(w, head.pitch, -0.45f);
		head.roll = MathHelper.lerp(w, head.roll, MathHelper.sin(t * 0.12f) * 0.15f);
		body.roll = MathHelper.lerp(w, body.roll, MathHelper.sin(t * 0.12f) * 0.06f);
		hat.copyTransform(head);
	}

	/** Maremoto: em pé na crista, de lado, braços equilibrando. */
	private void poseSurf(float p, float t) {
		float w = AbilityAnimations.window(p, 0.00f, 0.12f, 0.88f, 1.00f);
		body.yaw = MathHelper.lerp(w, body.yaw, 0.6f);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 1.3f + MathHelper.sin(t * 0.4f) * 0.15f);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, -1.3f - MathHelper.sin(t * 0.4f) * 0.15f);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, -0.3f);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, 0.3f);
		rightLeg.pitch = MathHelper.lerp(w, rightLeg.pitch, -0.35f);
		leftLeg.pitch = MathHelper.lerp(w, leftLeg.pitch, 0.3f);
		rightLeg.roll = MathHelper.lerp(w, rightLeg.roll, 0.15f);
		leftLeg.roll = MathHelper.lerp(w, leftLeg.roll, -0.15f);
	}

	/** Pesagem do Coração: braço estendido com a palma aberta, o outro no peito. */
	private void poseJudge(float p, float t) {
		float w = AbilityAnimations.window(p, 0.00f, 0.15f, 0.85f, 1.00f);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, -1.6f + MathHelper.sin(t * 0.3f) * 0.04f);
		rightArm.yaw = MathHelper.lerp(w, rightArm.yaw, -0.1f);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, -0.9f);
		leftArm.yaw = MathHelper.lerp(w, leftArm.yaw, 0.7f);
		head.pitch = MathHelper.lerp(w, head.pitch, head.pitch - 0.15f);
		hat.copyTransform(head);
	}

	/** Areias do Duat: os dois braços sobem de uma vez, abrindo o portal. */
	private void poseSummonUp(float p) {
		float w = AbilityAnimations.window(p, 0.00f, 0.25f, 0.70f, 1.00f);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, -2.9f);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, -2.9f);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 0.35f);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, -0.35f);
		head.pitch = MathHelper.lerp(w, head.pitch, -0.35f);
		hat.copyTransform(head);
	}

	/** Mergulho Solar (subida): corpo esticado, braços para trás como asas fechadas. */
	private void poseWingsUp(float p) {
		float w = AbilityAnimations.window(p, 0.00f, 0.15f, 0.80f, 1.00f);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, 0.5f);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, 0.5f);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 0.4f);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, -0.4f);
		rightLeg.pitch = MathHelper.lerp(w, rightLeg.pitch, 0.25f);
		leftLeg.pitch = MathHelper.lerp(w, leftLeg.pitch, 0.25f);
		head.pitch = MathHelper.lerp(w, head.pitch, -0.6f);
		hat.copyTransform(head);
	}

	/** Mergulho Solar (descida): punhos à frente, pernas juntas, como um cometa. */
	private void poseDive(float p) {
		float w = AbilityAnimations.window(p, 0.00f, 0.10f, 0.90f, 1.00f);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, -2.8f);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, -2.8f);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, -0.15f);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, 0.15f);
		rightLeg.pitch = MathHelper.lerp(w, rightLeg.pitch, 0.1f);
		leftLeg.pitch = MathHelper.lerp(w, leftLeg.pitch, 0.1f);
		rightLeg.roll = MathHelper.lerp(w, rightLeg.roll, -0.05f);
		leftLeg.roll = MathHelper.lerp(w, leftLeg.roll, 0.05f);
	}

	/** Asas da Aurora: braços bem abertos, peito para a frente, batendo como asas. */
	private void poseWingsSpread(float p, float t) {
		float w = AbilityAnimations.window(p, 0.00f, 0.12f, 0.80f, 1.00f);
		float flap = MathHelper.sin(t * 0.6f) * 0.2f;
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 1.45f + flap);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, -1.45f - flap);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, 0.25f);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, 0.25f);
		body.pitch = MathHelper.lerp(w, body.pitch, -0.12f);
		head.pitch = MathHelper.lerp(w, head.pitch, -0.3f);
		hat.copyTransform(head);
	}

	/** Pouso de super-herói: um joelho no chão, punho apoiado. */
	private void poseLand(float p) {
		float w = AbilityAnimations.window(p, 0.00f, 0.10f, 0.65f, 1.00f);
		rightLeg.pitch = MathHelper.lerp(w, rightLeg.pitch, -1.2f);
		leftLeg.pitch = MathHelper.lerp(w, leftLeg.pitch, 0.6f);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, -0.3f);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 0.2f);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, -0.9f);
		body.pitch = MathHelper.lerp(w, body.pitch, 0.45f);
		head.pitch = MathHelper.lerp(w, head.pitch, -0.3f);
		hat.copyTransform(head);
	}

	/** Renascimento: encolhida (o ovo) e depois explodindo para fora, braços ao céu. */
	private void poseRebirth(float p, float t) {
		float curl = AbilityAnimations.window(p, 0.00f, 0.10f, 0.40f, 0.50f);
		float burst = AbilityAnimations.window(p, 0.45f, 0.55f, 0.85f, 1.00f);
		rightArm.pitch = MathHelper.lerp(Math.max(curl, burst), rightArm.pitch, curl >= burst ? -1.2f : -2.9f);
		leftArm.pitch = MathHelper.lerp(Math.max(curl, burst), leftArm.pitch, curl >= burst ? -1.2f : -2.9f);
		rightArm.roll = MathHelper.lerp(burst, rightArm.roll, 0.6f);
		leftArm.roll = MathHelper.lerp(burst, leftArm.roll, -0.6f);
		body.pitch = MathHelper.lerp(curl, body.pitch, 0.5f);
		head.pitch = MathHelper.lerp(Math.max(curl, burst), head.pitch, curl >= burst ? 0.6f : -0.5f);
		rightLeg.pitch = MathHelper.lerp(curl, rightLeg.pitch, -0.9f);
		leftLeg.pitch = MathHelper.lerp(curl, leftLeg.pitch, -0.9f);
		hat.copyTransform(head);
	}

	/** Chuva de Chutes: chutes alternados muito rápidos, braços para trás. */
	private void poseKickRush(float p, float t) {
		float w = AbilityAnimations.window(p, 0.00f, 0.08f, 0.92f, 1.00f);
		float k = MathHelper.sin(t * 2.2f);
		rightLeg.pitch = MathHelper.lerp(w, rightLeg.pitch, -1.5f * Math.max(0, k) + 0.3f);
		leftLeg.pitch = MathHelper.lerp(w, leftLeg.pitch, -1.5f * Math.max(0, -k) + 0.3f);
		rightLeg.roll = MathHelper.lerp(w, rightLeg.roll, 0.15f * k);
		leftLeg.roll = MathHelper.lerp(w, leftLeg.roll, 0.15f * k);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, 0.9f);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, 0.9f);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 0.5f);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, -0.5f);
		body.pitch = MathHelper.lerp(w, body.pitch, -0.2f);
	}

	/** Chute Lunar: perna sobe num arco por cima da cabeça. */
	private void poseKickHigh(float p) {
		float up = AbilityAnimations.window(p, 0.00f, 0.30f, 0.55f, 1.00f);
		rightLeg.pitch = MathHelper.lerp(up, rightLeg.pitch, -2.6f);
		leftArm.pitch = MathHelper.lerp(up, leftArm.pitch, -0.5f);
		leftArm.roll = MathHelper.lerp(up, leftArm.roll, -1.0f);
		rightArm.roll = MathHelper.lerp(up, rightArm.roll, 1.0f);
		body.pitch = MathHelper.lerp(up, body.pitch, -0.3f);
	}

	/** Salto Lunar: corpo esticado subindo, braços para cima, pernas juntas. */
	private void poseLeap(float p) {
		float w = AbilityAnimations.window(p, 0.00f, 0.15f, 0.75f, 1.00f);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, -3.0f);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, -3.0f);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 0.25f);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, -0.25f);
		rightLeg.pitch = MathHelper.lerp(w, rightLeg.pitch, 0.3f);
		leftLeg.pitch = MathHelper.lerp(w, leftLeg.pitch, 0.3f);
	}

	// ------------------------------------------------------------------ última leva

	/** Dança das Aves Aquáticas: giro veloz, braços em lâmina alternando, uma perna estendida como bailarina. */
	private void poseDance(float p, float t) {
		float w = AbilityAnimations.window(p, 0.00f, 0.06f, 0.92f, 1.00f);
		float spin = t * 1.1f;
		body.yaw = MathHelper.lerp(w, body.yaw, (float) Math.sin(spin) * 0.9f);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, -1.6f + (float) Math.sin(spin * 1.3f) * 1.1f);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 0.6f + (float) Math.cos(spin) * 0.6f);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, -1.6f + (float) Math.sin(spin * 1.3f + 3.1f) * 1.1f);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, -0.6f - (float) Math.cos(spin) * 0.6f);
		rightLeg.pitch = MathHelper.lerp(w, rightLeg.pitch, -0.9f + (float) Math.sin(spin) * 0.3f);
		rightLeg.roll = MathHelper.lerp(w, rightLeg.roll, 0.25f);
		leftLeg.pitch = MathHelper.lerp(w, leftLeg.pitch, 0.35f);
		head.pitch = MathHelper.lerp(w, head.pitch, 0.15f);
		hat.copyTransform(head);
	}

	/** Fios da Marionete: as duas mãos erguidas, dedos puxando fios, com tranco no fim. */
	private void posePuppet(float p, float t) {
		float w = AbilityAnimations.window(p, 0.00f, 0.15f, 0.55f, 1.00f);
		float jerk = p > 0.45f && p < 0.6f ? 0.6f : 0f;
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, -2.2f + jerk + (float) Math.sin(t * 2f) * 0.05f);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, -2.0f + jerk + (float) Math.cos(t * 2f) * 0.05f);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 0.35f);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, -0.35f);
		head.pitch = MathHelper.lerp(w, head.pitch, -0.2f);
		hat.copyTransform(head);
	}

	/** Kitsune: abre os braços como um leque, palmas para cima, girando levemente. */
	private void poseFan(float p, float t) {
		float w = AbilityAnimations.window(p, 0.00f, 0.18f, 0.70f, 1.00f);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 1.25f);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, -1.25f);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, -0.4f);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, -0.4f);
		body.yaw = MathHelper.lerp(w, body.yaw, (float) Math.sin(t * 0.3f) * 0.2f);
		head.pitch = MathHelper.lerp(w, head.pitch, -0.3f);
		hat.copyTransform(head);
	}

	/** Serafim: mãos unidas em prece, erguidas diante do rosto, e o corpo levemente inclinado para trás. */
	private void posePray(float p, float t) {
		float w = AbilityAnimations.window(p, 0.00f, 0.15f, 0.85f, 1.00f);
		float breathe = (float) Math.sin(t * 0.15f) * 0.04f;
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, -2.3f + breathe);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, -2.3f + breathe);
		rightArm.yaw = MathHelper.lerp(w, rightArm.yaw, -0.45f);
		leftArm.yaw = MathHelper.lerp(w, leftArm.yaw, 0.45f);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 0f);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, 0f);
		head.pitch = MathHelper.lerp(w, head.pitch, -0.55f);
		hat.copyTransform(head);
		body.pitch = MathHelper.lerp(w, body.pitch, -0.08f);
	}

	/** Pisão do Akaza: ergue o joelho bem alto, braços em guarda, e desce o pé com tudo. */
	private void poseAkaza(float p) {
		float lift = AbilityAnimations.window(p, 0.00f, 0.30f, 0.42f, 0.50f);
		float slam = AbilityAnimations.window(p, 0.42f, 0.52f, 0.80f, 1.00f);
		rightLeg.pitch = MathHelper.lerp(Math.max(lift, slam), rightLeg.pitch, lift >= slam ? -1.7f : 0.15f);
		leftLeg.pitch = MathHelper.lerp(slam, leftLeg.pitch, -0.25f);
		rightArm.pitch = MathHelper.lerp(Math.max(lift, slam), rightArm.pitch, lift >= slam ? -1.3f : 0.2f);
		leftArm.pitch = MathHelper.lerp(Math.max(lift, slam), leftArm.pitch, lift >= slam ? -0.9f : 0.2f);
		rightArm.roll = MathHelper.lerp(slam, rightArm.roll, 0.55f);
		leftArm.roll = MathHelper.lerp(slam, leftArm.roll, -0.55f);
		body.pitch = MathHelper.lerp(slam, body.pitch, 0.22f);
		head.pitch = MathHelper.lerp(slam, head.pitch, 0.1f);
		hat.copyTransform(head);
	}

	/** Desvio do Oni: o corpo inclina e desliza de lado, pernas abertas. */
	private void poseDodge(float p, float side) {
		float w = AbilityAnimations.window(p, 0.00f, 0.15f, 0.60f, 1.00f);
		body.roll = MathHelper.lerp(w, body.roll, 0.35f * side);
		body.yaw = MathHelper.lerp(w, body.yaw, 0.25f * side);
		rightLeg.roll = MathHelper.lerp(w, rightLeg.roll, 0.35f);
		leftLeg.roll = MathHelper.lerp(w, leftLeg.roll, -0.35f);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 0.8f * Math.max(0, side) + 0.1f);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, -0.8f * Math.max(0, -side) - 0.1f);
		head.roll = MathHelper.lerp(w, head.roll, -0.15f * side);
		hat.copyTransform(head);
	}

	// ------------------------------------------------------------------ segunda leva

	/** Mão estendida à frente (Laço Áureo, Égide, Beijo Drenante): braço esquerdo firme, leve tremor de esforço. */
	private void poseReach(float p, float t) {
		float w = AbilityAnimations.window(p, 0.00f, 0.08f, 0.94f, 1.00f);
		float shake = (float) Math.sin(t * 1.3f) * 0.025f;
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, head.pitch - 1.5f + shake);
		leftArm.yaw = MathHelper.lerp(w, leftArm.yaw, head.yaw + 0.1f);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, 0f);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, 0.25f);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 0.15f);
		body.yaw = MathHelper.lerp(w, body.yaw, -0.12f);
	}

	/** Soco de ouro: puxa e dispara o braço direito, girando o tronco. */
	private void posePunch(float p) {
		float wind = AbilityAnimations.window(p, 0.00f, 0.20f, 0.25f, 0.35f);
		float hit = AbilityAnimations.window(p, 0.25f, 0.38f, 0.60f, 1.00f);
		rightArm.pitch = MathHelper.lerp(Math.max(wind, hit), rightArm.pitch, wind >= hit ? 0.6f : head.pitch - 1.55f);
		rightArm.yaw = MathHelper.lerp(hit, rightArm.yaw, head.yaw - 0.1f);
		body.yaw = MathHelper.lerp(Math.max(wind, hit), body.yaw, wind >= hit ? 0.35f : -0.3f);
		leftArm.pitch = MathHelper.lerp(hit, leftArm.pitch, 0.5f);
	}

	/** Grito de guerra: peito aberto, braços abertos para trás, cabeça erguida. */
	private void poseRoar(float p) {
		float w = AbilityAnimations.window(p, 0.00f, 0.15f, 0.70f, 1.00f);
		head.pitch = MathHelper.lerp(w, head.pitch, -0.45f);
		hat.copyTransform(head);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 0.9f);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, -0.9f);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, 0.3f);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, 0.3f);
		body.pitch = MathHelper.lerp(w, body.pitch, -0.1f);
	}

	// ------------------------------------------------------------------ Cabra / Minotauro

	/** Carregando o salto: tronco baixo, braços para trás. */
	private void poseCrouch(float p) {
		float w = AbilityAnimations.window(p, 0.00f, 0.15f, 0.97f, 1.00f);
		body.pitch = MathHelper.lerp(w, body.pitch, 0.3f);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, 0.55f);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, 0.55f);
		head.pitch = MathHelper.lerp(w, head.pitch, head.pitch * 0.5f - 0.1f);
		hat.copyTransform(head);
	}

	/** Cabeçada: cabeça baixa com os chifres à frente, braços para trás. */
	private void poseRam(float p) {
		float w = AbilityAnimations.window(p, 0.00f, 0.15f, 0.55f, 1.00f);
		head.pitch = MathHelper.lerp(w, head.pitch, 0.75f);
		hat.copyTransform(head);
		body.pitch = MathHelper.lerp(w, body.pitch, 0.4f);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, 0.8f);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, 0.8f);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 0.15f);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, -0.15f);
	}

	/** Arranca a pedra: braços sobem sobre a cabeça e depois arremessam para a frente. */
	private void poseLift(float p) {
		float up = AbilityAnimations.window(p, 0.00f, 0.35f, 0.62f, 0.70f);
		float thr = AbilityAnimations.window(p, 0.62f, 0.72f, 0.82f, 1.00f);
		float target = up >= thr ? -3.0f : -1.3f;
		float w = Math.max(up, thr);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, target);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, target);
		rightArm.roll = MathHelper.lerp(up, rightArm.roll, -0.2f);
		leftArm.roll = MathHelper.lerp(up, leftArm.roll, 0.2f);
		body.pitch = MathHelper.lerp(thr, body.pitch, 0.25f);
	}

	// ------------------------------------------------------------------ Mordida Vampírica

	private void poseBite(float p, float t) {
		float arms = AbilityAnimations.window(p, 0.00f, 0.18f, 0.82f, 1.00f);
		float grip = AbilityAnimations.window(p, 0.12f, 0.30f, 0.82f, 1.00f);
		float bite = AbilityAnimations.window(p, 0.26f, 0.34f, 0.78f, 0.92f);
		float gulp = (float) Math.sin(t * 0.95f) * bite;

		float pitch = MathHelper.lerp(grip, -1.55f, -1.12f) + gulp * 0.03f;
		float yaw = MathHelper.lerp(grip, -0.12f, -0.62f) + gulp * 0.025f;
		float roll = 0.08f * grip;

		rightArm.pitch = MathHelper.lerp(arms, rightArm.pitch, pitch);
		rightArm.yaw = MathHelper.lerp(arms, rightArm.yaw, yaw);
		rightArm.roll = MathHelper.lerp(arms, rightArm.roll, roll);
		leftArm.pitch = MathHelper.lerp(arms, leftArm.pitch, pitch);
		leftArm.yaw = MathHelper.lerp(arms, leftArm.yaw, -yaw);
		leftArm.roll = MathHelper.lerp(arms, leftArm.roll, -roll);

		head.pitch = MathHelper.lerp(bite, head.pitch, 0.52f + gulp * 0.07f);
		head.yaw = MathHelper.lerp(bite, head.yaw, 0.32f);
		head.roll = MathHelper.lerp(bite, head.roll, 0.18f);
		hat.copyTransform(head);
	}

	private void poseBitten(float p, float t) {
		float held = AbilityAnimations.window(p, 0.05f, 0.25f, 0.85f, 1.00f);
		float tremble = (float) Math.sin(t * 2.3f) * 0.05f * held;

		head.roll = MathHelper.lerp(held, head.roll, -0.48f);
		head.pitch = MathHelper.lerp(held, head.pitch, -0.28f);
		head.yaw = MathHelper.lerp(held, head.yaw, -0.22f);
		hat.copyTransform(head);

		rightArm.pitch = MathHelper.lerp(held, rightArm.pitch, 0.18f + tremble);
		rightArm.roll = MathHelper.lerp(held, rightArm.roll, 0.14f);
		leftArm.pitch = MathHelper.lerp(held, leftArm.pitch, 0.12f - tremble);
		leftArm.roll = MathHelper.lerp(held, leftArm.roll, -0.14f);
	}

	// ------------------------------------------------------------------ Sopro Dracônico

	/**
	 * 0.00–0.12 inspira: cabeça recua para trás, braços se abrem e recolhem (carregando o fôlego)
	 * 0.12–0.88 rajada: cabeça projeta para a frente seguindo a mira, braços abertos e firmes,
	 *           pequeno recuo rítmico como se o fogo empurrasse o corpo
	 * 0.88–1.00 recuperação suave
	 */
	private void poseBreath(float p, float t) {
		float inhale = AbilityAnimations.window(p, 0.00f, 0.10f, 0.10f, 0.16f);
		float blast = AbilityAnimations.window(p, 0.10f, 0.18f, 0.86f, 1.00f);
		float recoil = (float) Math.sin(t * 1.6f) * 0.045f * blast;
		float brace = Math.max(inhale, blast);

		// cabeça: recua no fôlego, projeta e "empurra" durante a rajada (mantendo a mira do jogador)
		float headPitch = head.pitch + inhale * -0.42f + blast * (0.12f + recoil);
		head.pitch = headPitch;
		head.roll = MathHelper.lerp(blast, head.roll, recoil * 0.6f);
		hat.copyTransform(head);

		// braços: abertos para trás e para os lados, como asas firmando o corpo
		float armPitch = MathHelper.lerp(blast, 0.55f, 0.32f) - recoil * 0.5f;
		float armRoll = MathHelper.lerp(blast, 0.35f, 0.62f);
		rightArm.pitch = MathHelper.lerp(brace, rightArm.pitch, armPitch);
		rightArm.yaw = MathHelper.lerp(brace, rightArm.yaw, 0.18f);
		rightArm.roll = MathHelper.lerp(brace, rightArm.roll, armRoll);
		leftArm.pitch = MathHelper.lerp(brace, leftArm.pitch, armPitch);
		leftArm.yaw = MathHelper.lerp(brace, leftArm.yaw, -0.18f);
		leftArm.roll = MathHelper.lerp(brace, leftArm.roll, -armRoll);
	}

	// ------------------------------------------------------------------ Pisão Dracônico

	/**
	 * 0.00–0.50 ergue a perna direita e abre os braços, tronco para trás (carregando o peso)
	 * 0.50–0.60 desce com tudo: perna planta, tronco e braços vêm para a frente
	 * 0.60–1.00 volta suave à pose normal
	 */
	private void poseStomp(float p) {
		float raise = AbilityAnimations.window(p, 0.00f, 0.42f, 0.42f, 0.54f);
		float slam = AbilityAnimations.window(p, 0.48f, 0.56f, 0.66f, 1.00f);
		rightLeg.pitch = MathHelper.lerp(raise, rightLeg.pitch, -1.15f);
		body.pitch = MathHelper.lerp(Math.max(raise, slam), body.pitch, raise * -0.12f + slam * 0.32f);
		float armPitch = raise * -0.55f + slam * -0.95f;
		float armRoll = raise * 0.7f + slam * 0.25f;
		float w = Math.max(raise, slam);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, armPitch);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, armRoll);
		leftArm.pitch = MathHelper.lerp(w, leftArm.pitch, armPitch);
		leftArm.roll = MathHelper.lerp(w, leftArm.roll, -armRoll);
		head.pitch = MathHelper.lerp(slam, head.pitch, head.pitch + 0.25f);
		hat.copyTransform(head);
	}

	// ------------------------------------------------------------------ Raio Solar

	/** Carga: braços erguidos ao céu, balançando de leve. Disparo: braços estendidos na direção do olhar. */
	private void poseSolar(float p, float t) {
		float up = AbilityAnimations.window(p, 0.00f, 0.12f, 0.50f, 0.58f);
		float shoot = AbilityAnimations.window(p, 0.52f, 0.60f, 0.92f, 1.00f);
		float sway = (float) Math.sin(t * 0.25f) * 0.06f * up;
		float pitchUp = -2.9f + sway, pitchAim = head.pitch - 1.5f;
		// só um braço sobe segurando a esfera de sol (estilo Fresh Animations); o outro fica solto, balançando de leve
		rightArm.pitch = MathHelper.lerp(Math.max(up, shoot), rightArm.pitch, up >= shoot ? pitchUp : pitchAim);
		rightArm.yaw = MathHelper.lerp(shoot, rightArm.yaw, head.yaw - 0.05f);
		rightArm.roll = MathHelper.lerp(up, rightArm.roll, 0.12f);
		leftArm.pitch = MathHelper.lerp(Math.max(up, shoot), leftArm.pitch, 0.18f + sway * 2f);
		leftArm.roll = MathHelper.lerp(Math.max(up, shoot), leftArm.roll, -0.22f);
		body.yaw = MathHelper.lerp(shoot, body.yaw, -0.18f);
		body.pitch = MathHelper.lerp(shoot, body.pitch, -0.08f);
		head.pitch = MathHelper.lerp(up, head.pitch, -0.45f);
		hat.copyTransform(head);
	}

	// ------------------------------------------------------------------ Bola de Fogo

	/** Braço direito avança e "arremessa" na direção da mira, depois volta. */
	private void poseCast(float p) {
		float w = AbilityAnimations.window(p, 0.00f, 0.22f, 0.45f, 1.00f);
		rightArm.pitch = MathHelper.lerp(w, rightArm.pitch, head.pitch - 1.45f);
		rightArm.yaw = MathHelper.lerp(w, rightArm.yaw, head.yaw - 0.12f);
		rightArm.roll = MathHelper.lerp(w, rightArm.roll, 0.0f);
		leftArm.pitch = MathHelper.lerp(w * 0.6f, leftArm.pitch, 0.35f);
		leftArm.roll = MathHelper.lerp(w * 0.6f, leftArm.roll, -0.25f);
	}
}
