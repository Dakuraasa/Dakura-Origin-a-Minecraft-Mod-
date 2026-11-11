package com.dakura.origins.sound;

import com.dakura.origins.DakuraOrigins;
import net.minecraft.entity.Entity;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/** Sons originais do mod (sintetizados por ferramentas/sons/gerar_sons.py; nenhum som vanilla). */
public final class ModSounds {
	public static final SoundEvent CAULDRON_THROW = register("cauldron_throw");
	public static final SoundEvent CAULDRON_SHATTER = register("cauldron_shatter");
	public static final SoundEvent POISON_BUBBLE = register("poison_bubble");
	public static final SoundEvent POISON_BURST = register("poison_burst");
	public static final SoundEvent MUSHROOM_RISE = register("mushroom_rise");
	public static final SoundEvent MAGMA_LAUNCH = register("magma_launch");
	public static final SoundEvent MAGMA_EXPLODE = register("magma_explode");
	public static final SoundEvent DRAGON_GRAB = register("dragon_grab");
	public static final SoundEvent DRAGON_SLAM = register("dragon_slam");
	public static final SoundEvent WING_BEAT = register("wing_beat");
	public static final SoundEvent PUNCH_HIT = register("punch_hit");
	public static final SoundEvent PUNCH_FINAL = register("punch_final");
	public static final SoundEvent MANA_CAST = register("mana_cast");
	public static final SoundEvent MANA_BURST = register("mana_burst");
	public static final SoundEvent FLOWER_BLOOM = register("flower_bloom");
	public static final SoundEvent FIRE_ROAR = register("fire_roar");
	public static final SoundEvent DICE_ROLL = register("dice_roll");
	public static final SoundEvent JACKPOT = register("jackpot");
	public static final SoundEvent DARK_COMET = register("dark_comet");
	public static final SoundEvent CHAIN_BIND = register("chain_bind");
	public static final SoundEvent DEVOUR = register("devour");

	private ModSounds() {}

	private static SoundEvent register(String name) {
		return Registry.register(Registries.SOUND_EVENT, DakuraOrigins.id(name), SoundEvent.of(DakuraOrigins.id(name)));
	}

	public static void play(Entity e, SoundEvent s, float vol, float pitch) {
		e.getWorld().playSound(null, e.getX(), e.getY(), e.getZ(), s, SoundCategory.PLAYERS, vol, pitch);
	}

	public static void play(World w, Vec3d v, SoundEvent s, float vol, float pitch) {
		w.playSound(null, v.x, v.y, v.z, s, SoundCategory.PLAYERS, vol, pitch);
	}

	public static void init() {}
}
