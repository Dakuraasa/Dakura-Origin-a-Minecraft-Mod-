package com.dakura.origins.network;

import com.dakura.origins.DakuraOrigins;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Servidor → jogador: pontos de maturidade atuais (só enviado quando mudam). */
public record MaturityPayload(int points) implements CustomPayload {
	public static final CustomPayload.Id<MaturityPayload> ID = new CustomPayload.Id<>(DakuraOrigins.id("maturity"));
	public static final PacketCodec<RegistryByteBuf, MaturityPayload> CODEC =
			PacketCodec.tuple(PacketCodecs.VAR_INT, MaturityPayload::points, MaturityPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
