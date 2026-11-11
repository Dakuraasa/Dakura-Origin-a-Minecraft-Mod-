package com.dakura.origins.power.types;

import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonObject;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * "dakura_origins:lily_pad" — Vitória-régia (Fada): ao pousar na superfície da água, uma vitória-régia surge sob os pés
 * e a mantém boiando (ela anda sobre a água em cima dela). A planta a acompanha e some quando ela sai da água,
 * voa ou agacha (agachar mergulha). A física roda nos dois lados (PlayerEntityMixin); o desenho é do cliente (flag "lily_pad").
 */
public final class LilyPadPower implements RacePower {
	public static final String FLAG = "lily_pad";

	public LilyPadPower(JsonObject json) {}

	public static boolean has(PlayerEntity p) {
		return RaceAccess.getRace(p).map(r -> r.powers().stream().anyMatch(x -> x instanceof LilyPadPower)).orElse(false);
	}

	/** Altura da superfície da água sob o jogador, ou NaN se ele não está boiando nela. */
	public static double surface(PlayerEntity p) {
		if (p.getAbilities().flying || p.isSneaking() || p.hasVehicle() || p.isSpectator()) return Double.NaN;
		World w = p.getWorld();
		BlockPos bp = BlockPos.ofFloored(p.getX(), p.getY() - 0.05, p.getZ());
		for (int i = 0; i < 2; i++, bp = bp.up()) {
			FluidState fs = w.getFluidState(bp);
			if (!fs.isIn(FluidTags.WATER) || w.getFluidState(bp.up()).isIn(FluidTags.WATER)) continue;
			double top = bp.getY() + fs.getHeight(w, bp);
			if (p.getY() <= top + 0.05 && p.getY() > top - 1.4) return top;
		}
		return Double.NaN;
	}

	/** Chamado antes do movimento (cliente e servidor): mantém a Fada em cima da vitória-régia. */
	public static void support(PlayerEntity p) {
		double top = surface(p);
		if (Double.isNaN(top)) return;
		if (p.getY() < top) p.setPosition(p.getX(), top, p.getZ());
		var v = p.getVelocity();
		if (v.y < 0) p.setVelocity(v.x, 0, v.z);
		p.setOnGround(true);
		p.fallDistance = 0;
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		EntityFlags.set(player, FLAG, !Double.isNaN(surface(player)));
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		EntityFlags.set(player, FLAG, false);
	}
}
