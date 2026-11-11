package com.dakura.origins.block;

import com.dakura.origins.DakuraOrigins;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.MapColor;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

/**
 * Blocos temporários das habilidades (não têm item nem drop e somem sozinhos):
 * o cogumelo gigante da Dama Cogumelo e o caldeirão venenoso.
 */
public final class ModBlocks {
	public static final Block FUNGAL_STEM = register("fungal_stem", MapColor.WHITE_GRAY, 1.0f, BlockSoundGroup.WOOD, false);
	public static final Block FUNGAL_CAP = register("fungal_cap", MapColor.RED, 0.8f, BlockSoundGroup.WOOD, false);
	public static final Block FUNGAL_CAP_SPOTS = register("fungal_cap_spots", MapColor.RED, 0.8f, BlockSoundGroup.WOOD, false);
	public static final Block FUNGAL_GILLS = register("fungal_gills", MapColor.PALE_YELLOW, 0.8f, BlockSoundGroup.WOOD, false);
	public static final Block TOXIC_CAULDRON = register("toxic_cauldron", MapColor.PURPLE, 2.0f, BlockSoundGroup.METAL, true);

	private ModBlocks() {}

	private static Block register(String name, MapColor color, float strength, BlockSoundGroup sound, boolean nonOpaque) {
		AbstractBlock.Settings s = AbstractBlock.Settings.create().mapColor(color).strength(strength).sounds(sound)
				.dropsNothing().pistonBehavior(PistonBehavior.BLOCK);
		if (nonOpaque) s = s.nonOpaque();
		return Registry.register(Registries.BLOCK, DakuraOrigins.id(name), new Block(s));
	}

	public static boolean isTemporary(Block b) {
		return b == FUNGAL_STEM || b == FUNGAL_CAP || b == FUNGAL_CAP_SPOTS || b == FUNGAL_GILLS || b == TOXIC_CAULDRON;
	}

	/** Força o carregamento da classe na inicialização. */
	public static void init() {}
}
