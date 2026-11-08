package com.dakura.origins.network;

import com.dakura.origins.DakuraOrigins;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Servidor → clientes próximos: toca (durationTicks > 0) ou encerra (0) uma animação
 * de habilidade numa entidade. "channel" escolhe a pose: "breath", "cast"...
 * Genérico: raças futuras só inventam canais novos.
 */
public record AnimPayload(int entityId, String channel, int durationTicks) implements CustomPayload {
	public static final CustomPayload.Id<AnimPayload> ID = new CustomPayload.Id<>(DakuraOrigins.id("anim"));
	public static final PacketCodec<RegistryByteBuf, AnimPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, AnimPayload::entityId,
			PacketCodecs.STRING, AnimPayload::channel,
			PacketCodecs.VAR_INT, AnimPayload::durationTicks,
			AnimPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
