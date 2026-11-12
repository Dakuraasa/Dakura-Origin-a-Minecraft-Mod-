package com.dakura.origins.power.types;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.network.MaturityPayload;
import com.dakura.origins.power.JsonParams;
import com.dakura.origins.power.RacePower;
import com.dakura.origins.race.Race;
import com.dakura.origins.race.RaceAccess;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * "dakura_origins:maturity" — progressão própria da raça, no lugar da experiência.
 * <pre>
 * { "type": "dakura_origins:maturity", "thresholds": [500, 1500], "points_per_health": 0.5,
 *   "only_monsters": true, "colors": ["#7ED957", "#F2B33D", "#D9364A"] }
 * </pre>
 * - Matar monstros dá pontos (vida máxima do monstro × points_per_health).
 * - thresholds = pontos para entrar no 2º e 3º estágio (o último também é o máximo).
 * - A experiência vanilla é guardada ao virar a raça, bloqueada enquanto ela durar
 *   e devolvida se o jogador trocar de raça.
 * - Outros poderes (ex.: "dakura_origins:attribute" com "stage_values" ou "maturity_range")
 *   leem o estágio/progresso por {@link #stage} e {@link #progress}.
 */
public final class MaturityPower implements RacePower {
	public static final AttachmentType<Integer> POINTS = AttachmentRegistry.<Integer>create(
			DakuraOrigins.id("maturity"), b -> b.persistent(Codec.INT).copyOnDeath());
	public static final AttachmentType<Integer> STORED_XP = AttachmentRegistry.<Integer>create(
			DakuraOrigins.id("stored_xp"), b -> b.persistent(Codec.INT).copyOnDeath());

	/** Liberado só enquanto devolvemos a experiência guardada. */
	private static boolean restoringXp = false;

	private final int[] thresholds;
	private final float pointsPerHealth;
	private final boolean onlyMonsters;
	private final int[] colors;
	private final String growMessage;
	private final boolean undead;
	/** Pontos fixos por abate (0 = proporcional à vida da vítima). */
	private final int perKill;
	/** Kitsune: a experiência ganha vira maturidade (em vez de ir para a barra vanilla). */
	private final boolean fromXp;
	/** false = matar não dá maturidade. */
	private final boolean fromKills;
	private final boolean kitsune;

	public MaturityPower(JsonObject json) {
		List<Integer> t = new ArrayList<>();
		if (json.has("thresholds")) for (JsonElement e : json.getAsJsonArray("thresholds")) t.add(e.getAsInt());
		if (t.isEmpty()) t = List.of(500, 1500);
		this.thresholds = t.stream().mapToInt(Integer::intValue).toArray();
		this.pointsPerHealth = JsonParams.num(json, "points_per_health", 0.5f);
		this.onlyMonsters = JsonParams.bool(json, "only_monsters", true);
		List<String> c = JsonParams.strings(json, "colors");
		if (c.isEmpty()) c = List.of("#7ED957", "#F2B33D", "#D9364A");
		this.growMessage = JsonParams.str(json, "grow_message", "dakura_origins.message.maturity_grow");
		String style = JsonParams.str(json, "style", "dragon");
		this.undead = "undead".equals(style);
		this.kitsune = "kitsune".equals(style);
		this.fromXp = JsonParams.bool(json, "from_xp", false);
		this.fromKills = JsonParams.bool(json, "from_kills", true);
		this.perKill = JsonParams.integer(json, "per_kill", 0);
		this.colors = c.stream().mapToInt(s -> 0xFF000000 | Integer.parseInt(s.replace("#", ""), 16)).toArray();
	}

	// ================================================================== consulta (cliente e servidor)

	public int maxPoints() {
		return thresholds[thresholds.length - 1];
	}

	public int stageCount() {
		return thresholds.length + 1;
	}

	/** Estágio 0 (jovem), 1 (adulto), 2 (ancião)... */
	public int stageOf(int points) {
		int s = 0;
		for (int th : thresholds) if (points >= th) s++;
		return s;
	}

	/** Progresso total 0..1. */
	public float totalProgress(int points) {
		return Math.min(1f, points / (float) maxPoints());
	}

	/** Progresso dentro do estágio atual 0..1 (o último estágio fica cheio). */
	public float stageProgress(int points) {
		int s = stageOf(points);
		if (s >= thresholds.length) return 1f;
		int start = s == 0 ? 0 : thresholds[s - 1];
		return (points - start) / (float) (thresholds[s] - start);
	}

	/** Cor da barra: muda gradualmente ao longo de toda a progressão. */
	public int barColor(int points) {
		float f = totalProgress(points) * (colors.length - 1);
		int i = Math.min(colors.length - 2, (int) f);
		return Race.Palette.mix(colors[i], colors[i + 1], f - i);
	}

	/** Cópia no cliente (só do jogador local), atualizada pelo MaturityPayload. */
	public static volatile int clientPoints = 0;

	/** Força o registro dos attachments durante a inicialização do mod. */
	public static void init() {}

	public static int points(PlayerEntity player) {
		if (player.getWorld().isClient) return clientPoints;
		Integer p = player.getAttached(POINTS);
		return p == null ? 0 : p;
	}

	/** Poder de maturidade da raça do jogador, se houver. */
	public static Optional<MaturityPower> of(PlayerEntity player) {
		return RaceAccess.getRace(player).flatMap(r -> r.powers().stream()
				.filter(p -> p instanceof MaturityPower).map(p -> (MaturityPower) p).findFirst());
	}

	/** Estágio atual (0 se a raça não tem maturidade). */
	public static int stage(PlayerEntity player) {
		return of(player).map(m -> m.stageOf(points(player))).orElse(0);
	}

	/**
	 * Multiplicador por estágio (ex.: dano das habilidades): values[estágio], repetindo o último.
	 * Sem valores ou sem maturidade → 1.
	 */
	public static float multiplier(PlayerEntity player, float[] values) {
		if (values == null || values.length == 0) return 1f;
		return values[Math.min(Math.max(0, stage(player)), values.length - 1)];
	}

	/** Progresso total 0..1 (0 se a raça não tem maturidade). */
	public static float progress(PlayerEntity player) {
		return of(player).map(m -> m.totalProgress(points(player))).orElse(0f);
	}

	/** Usado pelo mixin: esta raça não ganha nem usa experiência vanilla. */
	public static boolean blocksExperience(PlayerEntity player) {
		return !restoringXp && of(player).isPresent();
	}

	// ================================================================== ciclo de vida (servidor)

	@Override
	public void onApply(ServerPlayerEntity player) {
		if (player.getAttached(STORED_XP) == null) {
			player.setAttached(STORED_XP, player.totalExperience);
			player.setExperienceLevel(0);
			player.setExperiencePoints(0);
			player.totalExperience = 0;
		}
		if (player.getAttached(POINTS) == null) player.setAttached(POINTS, 0);
		sync(player);
		flags(player);
	}

	/** Estágio visível para os outros jogadores (ex.: quantas caudas a Kitsune mostra). */
	private void flags(ServerPlayerEntity player) {
		int st = stageOf(points(player));
		com.dakura.origins.power.EntityFlags.set(player, "mat_1", st >= 1);
		com.dakura.origins.power.EntityFlags.set(player, "mat_2", st >= 2);
	}

	@Override
	public void tick(ServerPlayerEntity player) {
		if (player.age % 100 == 0) flags(player);
	}

	/** Experiência que viraria nível vira maturidade (raças com "from_xp"). */
	public static void gainXp(PlayerEntity player, int amount) {
		if (amount <= 0 || !(player instanceof ServerPlayerEntity sp)) return;
		of(player).filter(m -> m.fromXp).ifPresent(m -> m.addPoints(sp, amount));
	}

	/** /do full maturity: vai direto ao último estágio. false se a raça não tem maturidade. */
	public static boolean maxOut(ServerPlayerEntity player) {
		var m = of(player);
		if (m.isEmpty()) return false;
		int before = points(player);
		if (before < m.get().maxPoints()) m.get().addPoints(player, m.get().maxPoints() - before);
		return true;
	}

	/** Soma maturidade vinda de fora (ex.: devorar da Kitsune Corrompida). */
	public static void grant(ServerPlayerEntity player, int gain) {
		if (gain > 0) of(player).ifPresent(m -> m.addPoints(player, gain));
	}

	private void addPoints(ServerPlayerEntity player, int gain) {
		int before = points(player);
		if (before >= maxPoints()) return;
		int after = Math.min(maxPoints(), before + gain);
		player.setAttached(POINTS, after);
		sync(player);
		if (stageOf(after) > stageOf(before)) {
			celebrate(player);
			flags(player);
		}
	}

	@Override
	public void onRemove(ServerPlayerEntity player) {
		Integer stored = player.getAttached(STORED_XP);
		player.removeAttached(STORED_XP);
		player.removeAttached(POINTS);
		com.dakura.origins.power.EntityFlags.set(player, "mat_1", false);
		com.dakura.origins.power.EntityFlags.set(player, "mat_2", false);
		if (stored != null && stored > 0) {
			restoringXp = true;
			try {
				player.addExperience(stored);
			} finally {
				restoringXp = false;
			}
		}
		sync(player);
	}

	@Override
	public void onKill(ServerPlayerEntity player, LivingEntity victim) {
		if (!fromKills || (onlyMonsters && !(victim instanceof Monster))) return;
		int gain = perKill > 0 ? perKill : Math.max(1, Math.round(victim.getMaxHealth() * pointsPerHealth));
		addPoints(player, gain);
	}

	private void celebrate(ServerPlayerEntity player) {
		player.sendMessage(Text.translatable(growMessage).formatted(undead || kitsune ? Formatting.AQUA : Formatting.GOLD), true);
		if (kitsune) {
			com.dakura.origins.sound.ModSounds.play(player, com.dakura.origins.sound.ModSounds.MANA_BURST, 1.2f, 0.8f);
			com.dakura.origins.power.ability.LegendKit.anim(player, "fan", 30);
			if (player.getWorld() instanceof ServerWorld sw)
				com.dakura.origins.power.ability.LegendKit.fxAt(sw, "boom_mana", player.getPos().add(0, 1, 0), 3.5f, 0, 0);
			return;
		}
		if (undead) {
			player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
					SoundEvents.ENTITY_WITHER_SKELETON_AMBIENT, SoundCategory.PLAYERS, 0.8f, 0.7f);
			if (player.getWorld() instanceof ServerWorld sw) sw.spawnParticles(ParticleTypes.SOUL, player.getX(), player.getBodyY(0.5),
					player.getZ(), 24, 0.4, 0.6, 0.4, 0.03);
			return;
		}
		player.getWorld().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 0.7f, 1.3f);
		if (player.getWorld() instanceof ServerWorld sw) {
			sw.spawnParticles(ParticleTypes.DRAGON_BREATH, player.getX(), player.getBodyY(0.5), player.getZ(),
					40, player.getWidth() * 0.6, player.getHeight() * 0.4, player.getWidth() * 0.6, 0.02);
			sw.spawnParticles(ParticleTypes.FLAME, player.getX(), player.getBodyY(0.3), player.getZ(),
					20, player.getWidth() * 0.5, 0.2, player.getWidth() * 0.5, 0.05);
		}
	}

	public static void sync(ServerPlayerEntity player) {
		if (ServerPlayNetworking.canSend(player, MaturityPayload.ID)) {
			ServerPlayNetworking.send(player, new MaturityPayload(points(player)));
		}
	}
}
