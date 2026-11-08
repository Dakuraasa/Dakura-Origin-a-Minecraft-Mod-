package com.dakura.origins.network;

import com.dakura.origins.DakuraOrigins;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Cliente → servidor: apertou a tecla da habilidade 0 ou 1. */
public record UseAbilityPayload(int slot) implements CustomPayload {
	public static final CustomPayload.Id<UseAbilityPayload> ID =
			new CustomPayload.Id<>(DakuraOrigins.id("use_ability"));
	public static final PacketCodec<RegistryByteBuf, UseAbilityPayload> CODEC =
			PacketCodec.tuple(PacketCodecs.VAR_INT, UseAbilityPayload::slot, UseAbilityPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
