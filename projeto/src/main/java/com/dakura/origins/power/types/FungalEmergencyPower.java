package com.dakura.origins.power.types;

import com.dakura.origins.block.ModBlocks;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.power.ability.AreaZones;
import com.dakura.origins.power.ability.LegendKit;
import com.dakura.origins.race.RaceAccess;
import com.dakura.origins.sound.ModSounds;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "dakura_origins:fungal_emergency" — Dama Cogumelo: ao cair para 4 corações, explode em veneno roxo,
 * um cogumelo gigante brota do chão em camadas e ela é levada para o topo, regenerando por 10 s.
 * O cogumelo é feito de blocos próprios e some depois de "stay" ticks.
 * { "threshold": 8, "cooldown": 3600, "radius": 6, "damage": 6, "stay": 600 }
 */
public final class FungalEmergencyPower implements RacePower {
	private static final Map<UUID, Long> READY_AT = new HashMap<>();
	private final float threshold, damage;
	private final int cooldown, stay;
	private final double radius;

	public FungalEmergencyPower(JsonObject json) {
		this.threshold = JsonParams.num(json, "threshold", 8f);
		this.cooldown = JsonParams.integer(json, "cooldown", 3600);
		this.radius = JsonParams.num(json, "radius", 6f);
		this.damage = JsonParams.num(json, "damage", 6f);
		this.stay = JsonParams.integer(json, "stay", 600);
	}

	public static void bootstrap() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, base, taken, blocked) -> {
			if (!(entity instanceof ServerPlayerEntity p) || !p.isAlive() || p.getHealth() > 8.5f) return;
			RaceAccess.getRace(p).flatMap(r -> r.powers().stream().filter(x -> x instanceof FungalEmergencyPower)
					.map(x -> (FungalEmergencyPower) x).findFirst()).ifPresent(pw -> pw.trigger(p));
		});
	}

	private void trigger(ServerPlayerEntity p) {
		if (p.getHealth() > threshold) return;
		long now = p.getServerWorld().getTime();
		Long ready = READY_AT.get(p.getUuid());
		if (ready != null && now < ready) return;
		READY_AT.put(p.getUuid(), now + cooldown);
		ServerWorld w = p.getServerWorld();
		Vec3d c = p.getPos();
		// 1) explosão venenosa
		for (LivingEntity e : LegendKit.enemies(p, c, radius)) {
			e.timeUntilRegen = 0;
			e.damage(AreaZones.poison(w, p), damage);
			Vec3d push = LegendKit.flat(e.getPos().subtract(c));
			e.addVelocity(push.x * 0.9, 0.5, push.z * 0.9);
			e.velocityModified = true;
		}
		AreaZones.create(w, c, (float) radius, 6, 160, AreaZones.Kind.POISON, p, 1.5f, null);
		LegendKit.fxAt(w, "boom_poison", c.add(0, 1, 0), (float) radius, 0, 0);
		ModSounds.play(p, ModSounds.POISON_BURST, 1.2f, 0.9f);
		p.sendMessage(Text.translatable("dakura_origins.message.fungal_emergency"), true);
		// 2) o cogumelo cresce camada por camada e 3) leva a Dama para o topo
		BlockPos base = groundBelow(w, p.getBlockPos());
		List<BlockPos> placed = new ArrayList<>();
		List<List<Object[]>> layers = layers(base);
		LegendKit.fxAt(w, "spores", Vec3d.ofBottomCenter(base), 5, 40, 0);
		ModSounds.play(w, Vec3d.ofCenter(base), ModSounds.MUSHROOM_RISE, 1.3f, 0.9f);
		// topo = em cima do bloco mais alto do cogumelo (o domo é oco: procurar ar a partir de dentro a prendia lá)
		int topY = base.getY();
		for (List<Object[]> layer : layers)
			for (Object[] o : layer) topY = Math.max(topY, ((BlockPos) o[0]).getY());
		BlockPos top = new BlockPos(base.getX(), topY + 1, base.getZ());
		for (int i = 0; i < 6 && !(w.getBlockState(top).isAir() && w.getBlockState(top.up()).isAir()); i++) top = top.up();
		// a Dama vai para o topo ANTES de o cogumelo surgir, para nunca ficar presa dentro dele
		p.stopRiding();
		p.setVelocity(Vec3d.ZERO);
		p.requestTeleport(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
		p.fallDistance = 0;
		for (List<Object[]> layer : layers)
			for (Object[] o : layer) {
				BlockPos bp = (BlockPos) o[0];
				BlockState old = w.getBlockState(bp);
				if (old.isAir() || old.isReplaceable()) {
					w.setBlockState(bp, ((Block) o[1]).getDefaultState());
					placed.add(bp);
				}
			}
		Vec3d topC = new Vec3d(top.getX() + 0.5, top.getY(), top.getZ() + 0.5);
		LegendKit.fxAt(w, "spores", topC, 3, 30, 0);
		// veneno de verdade em volta e embaixo do cogumelo enquanto ele existe
		Vec3d foot = Vec3d.ofBottomCenter(base);
		AreaZones.create(w, foot, 7f, 6, stay, AreaZones.Kind.POISON, p, 1.0f, null);
		LegendKit.fxAt(w, "boom_poison", foot.add(0, 0.5, 0), 7f, 0, 0);
		// regeneração por 10 s, feita por código (sem efeito de poção)
		LegendKit.run(p, "fungal_regen", 200, (pp, a) -> {
			if (a % 10 == 0) pp.heal(1f);
			pp.fallDistance = 0;
			return true;
		});
		LegendKit.run(p, "fungal_vanish", stay, (pp, a) -> {
			if (a % 10 == 0) LegendKit.fxAt(w, "poison_mist", foot, 7f, 0, 0);
			return true;
		}, pp -> {
			for (BlockPos bp : placed) if (ModBlocks.isTemporary(w.getBlockState(bp).getBlock())) w.removeBlock(bp, false);
		});
	}

	private static BlockPos groundBelow(ServerWorld w, BlockPos from) {
		BlockPos p = from;
		for (int i = 0; i < 8 && w.getBlockState(p.down()).isAir(); i++) p = p.down();
		return p;
	}

	/** Camadas do cogumelo (de baixo para cima): pé com base larga, lamelas e chapéu em domo. */
	private static List<List<Object[]>> layers(BlockPos base) {
		List<List<Object[]>> out = new ArrayList<>();
		for (int y = 0; y <= 9; y++) {
			List<Object[]> l = new ArrayList<>();
			int r = y < 2 ? 2 : 1;
			for (int x = -r; x <= r; x++)
				for (int z = -r; z <= r; z++)
					if (!(Math.abs(x) == 2 && Math.abs(z) == 2)) l.add(new Object[]{base.add(x, y, z), ModBlocks.FUNGAL_STEM});
			out.add(l);
		}
		double[] rad = {4.6, 5.3, 4.6, 3.4, 1.9};
		for (int k = 0; k < rad.length; k++) {
			List<Object[]> l = new ArrayList<>();
			int y = 10 + k;
			int r = (int) Math.ceil(rad[k]);
			for (int x = -r; x <= r; x++)
				for (int z = -r; z <= r; z++) {
					double d = Math.sqrt(x * x + z * z);
					if (d > rad[k]) continue;
					Block b;
					if (k == 0) b = d < 1.6 ? ModBlocks.FUNGAL_STEM : ModBlocks.FUNGAL_GILLS;
					else b = ((x * 7 + z * 13 + k * 5) & 7) == 0 ? ModBlocks.FUNGAL_CAP_SPOTS : ModBlocks.FUNGAL_CAP;
					// o domo é oco por dentro (só a casca), menos o topo onde ela fica de pé
					if (k > 0 && k < rad.length - 1 && d < rad[k + 1] - 0.6) continue;
					l.add(new Object[]{base.add(x, y, z), b});
				}
			out.add(l);
		}
		return out;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		READY_AT.remove(p.getUuid());
	}
}
