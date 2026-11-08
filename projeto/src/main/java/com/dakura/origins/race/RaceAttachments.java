package com.dakura.origins.race;

import com.dakura.origins.DakuraOrigins;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;

/**
 * Guarda a raça escolhida dentro do próprio jogador (salva no mundo e sobrevive à morte).
 */
public final class RaceAttachments {
	public static final AttachmentType<String> RACE = AttachmentRegistry.<String>create(
			DakuraOrigins.id("race"),
			builder -> builder.persistent(Codec.STRING).copyOnDeath());

	private RaceAttachments() {}

	/** Só força o carregamento da classe durante a inicialização. */
	public static void init() {}
}
