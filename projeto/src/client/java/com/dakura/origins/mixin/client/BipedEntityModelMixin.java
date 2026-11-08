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
		rightArm.pitch = MathHelper.lerp(Math.max(up, shoot), rightArm.pitch, up >= shoot ? pitchUp : pitchAim);
		leftArm.pitch = MathHelper.lerp(Math.max(up, shoot), leftArm.pitch, up >= shoot ? pitchUp : pitchAim);
		rightArm.yaw = MathHelper.lerp(shoot, rightArm.yaw, head.yaw - 0.15f);
		leftArm.yaw = MathHelper.lerp(shoot, leftArm.yaw, head.yaw + 0.15f);
		rightArm.roll = MathHelper.lerp(up, rightArm.roll, 0.25f);
		leftArm.roll = MathHelper.lerp(up, leftArm.roll, -0.25f);
		head.pitch = MathHelper.lerp(up, head.pitch, -0.6f);
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
