package com.dakura.origins.network;

import com.dakura.origins.DakuraOrigins;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Servidor → clientes próximos: começa (durationTicks > 0) ou termina (0)
 * a animação da Mordida Vampírica entre o vampiro e a vítima.
 */
public record BiteAnimPayload(int vampireId, int victimId, int durationTicks) implements CustomPayload {
	public static final CustomPayload.Id<BiteAnimPayload> ID = new CustomPayload.Id<>(DakuraOrigins.id("bite_anim"));
	public static final PacketCodec<RegistryByteBuf, BiteAnimPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, BiteAnimPayload::vampireId,
			PacketCodecs.VAR_INT, BiteAnimPayload::victimId,
			PacketCodecs.VAR_INT, BiteAnimPayload::durationTicks,
			BiteAnimPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
