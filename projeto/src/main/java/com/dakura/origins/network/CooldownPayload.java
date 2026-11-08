package com.dakura.origins.network;

import com.dakura.origins.DakuraOrigins;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Servidor → jogador: uma habilidade entrou em recarga (total e restante, em ticks).
 * slot = -1 zera todas (troca de raça, entrada no mundo). Só é enviado quando algo muda.
 */
public record CooldownPayload(int slot, int totalTicks, int remainingTicks) implements CustomPayload {
	public static final CustomPayload.Id<CooldownPayload> ID = new CustomPayload.Id<>(DakuraOrigins.id("cooldown"));
	public static final PacketCodec<RegistryByteBuf, CooldownPayload> CODEC = PacketCodec.tuple(
			PacketCodecs.VAR_INT, CooldownPayload::slot,
			PacketCodecs.VAR_INT, CooldownPayload::totalTicks,
			PacketCodecs.VAR_INT, CooldownPayload::remainingTicks,
			CooldownPayload::new);

	public static CooldownPayload reset() {
		return new CooldownPayload(-1, 0, 0);
	}

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
