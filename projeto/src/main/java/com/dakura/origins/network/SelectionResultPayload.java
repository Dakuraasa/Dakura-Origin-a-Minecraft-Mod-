package com.dakura.origins.network;

import com.dakura.origins.DakuraOrigins;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Servidor → cliente: resultado da escolha (também usado para sincronizar a raça ao entrar). */
public record SelectionResultPayload(boolean success, String raceId) implements CustomPayload {
	public static final CustomPayload.Id<SelectionResultPayload> ID =
			new CustomPayload.Id<>(DakuraOrigins.id("selection_result"));
	public static final PacketCodec<RegistryByteBuf, SelectionResultPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.BOOL, SelectionResultPayload::success,
			PacketCodecs.STRING, SelectionResultPayload::raceId,
			SelectionResultPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
