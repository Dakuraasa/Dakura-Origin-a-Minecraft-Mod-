package com.dakura.origins.network;

import com.dakura.origins.DakuraOrigins;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Servidor → clientes: o jogador (entityId) virou ou deixou de ser morcego. */
public record BatStatePayload(int entityId, boolean bat) implements CustomPayload {
	public static final CustomPayload.Id<BatStatePayload> ID = new CustomPayload.Id<>(DakuraOrigins.id("bat_state"));
	public static final PacketCodec<RegistryByteBuf, BatStatePayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, BatStatePayload::entityId,
			PacketCodecs.BOOL, BatStatePayload::bat,
			BatStatePayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
