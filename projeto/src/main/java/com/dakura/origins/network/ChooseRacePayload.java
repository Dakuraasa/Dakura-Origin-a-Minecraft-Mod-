package com.dakura.origins.network;

import com.dakura.origins.DakuraOrigins;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/** Cliente → servidor: "escolhi esta raça". */
public record ChooseRacePayload(String raceId) implements CustomPayload {
	public static final CustomPayload.Id<ChooseRacePayload> ID =
			new CustomPayload.Id<>(DakuraOrigins.id("choose_race"));
	public static final PacketCodec<RegistryByteBuf, ChooseRacePayload> CODEC =
			PacketCodec.tuple(PacketCodecs.STRING, ChooseRacePayload::raceId, ChooseRacePayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}
