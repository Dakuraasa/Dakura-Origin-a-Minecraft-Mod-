package com.dakura.origins.network;

import com.dakura.origins.DakuraOrigins;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Servidor → cliente: "abra o livro de origens". preselect = página que abre primeiro. */
public record OpenSelectionPayload(String preselect) implements CustomPayload {
	public static final CustomPayload.Id<OpenSelectionPayload> ID =
			new CustomPayload.Id<>(DakuraOrigins.id("open_selection"));
	public static final PacketCodec<RegistryByteBuf, OpenSelectionPayload> CODEC =
			PacketCodec.tuple(PacketCodecs.STRING, OpenSelectionPayload::preselect, OpenSelectionPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
