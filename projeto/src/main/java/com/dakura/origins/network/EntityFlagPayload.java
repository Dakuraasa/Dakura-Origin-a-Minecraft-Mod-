package com.dakura.origins.network;

import com.dakura.origins.DakuraOrigins;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Servidor → clientes: liga/desliga um estado visual de uma entidade (ex.: "cramped"). */
public record EntityFlagPayload(int entityId, String flag, boolean value) implements CustomPayload {
	public static final CustomPayload.Id<EntityFlagPayload> ID = new CustomPayload.Id<>(DakuraOrigins.id("entity_flag"));
	public static final PacketCodec<RegistryByteBuf, EntityFlagPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, EntityFlagPayload::entityId,
			PacketCodecs.STRING, EntityFlagPayload::flag,
			PacketCodecs.BOOL, EntityFlagPayload::value,
			EntityFlagPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
