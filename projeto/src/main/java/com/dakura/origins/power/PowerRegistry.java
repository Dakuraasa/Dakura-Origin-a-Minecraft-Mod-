package com.dakura.origins.power;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.power.ability.BatFormAbility;
import com.dakura.origins.power.ability.BreathAbility;
import com.dakura.origins.power.ability.FireballAbility;
import com.dakura.origins.power.types.ClaustrophobiaPower;
import com.dakura.origins.power.types.DamageImmunityPower;
import com.dakura.origins.power.types.MaturityPower;
import com.dakura.origins.power.types.NoArmorPower;
import com.dakura.origins.power.ability.VampireBiteAbility;
import com.dakura.origins.power.types.AttributePower;
import com.dakura.origins.power.types.DietPower;
import com.dakura.origins.power.types.RegenerationPower;
import com.dakura.origins.power.types.StatusEffectPower;
import com.dakura.origins.power.types.SunlightPower;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Registro dos TIPOS de poder e de habilidade.
 *
 * O JSON de uma raça só referencia tipos ("type": "dakura_origins:attribute"),
 * então criar uma raça nova normalmente não exige uma linha de Java.
 * Só quando você inventa uma mecânica nova é que registra um tipo novo aqui
 * (ou num addon, via {@link DakuraOriginsPlugin}).
 */
public final class PowerRegistry {
	@FunctionalInterface
	public interface PowerFactory {
		RacePower create(JsonObject json);
	}

	@FunctionalInterface
	public interface AbilityFactory {
		Ability create(JsonObject json);
	}

	private static final Map<Identifier, PowerFactory> POWER_TYPES = new HashMap<>();
	private static final Map<Identifier, AbilityFactory> ABILITY_TYPES = new HashMap<>();
	private static boolean bootstrapped = false;

	private PowerRegistry() {}

	public static void registerPowerType(Identifier id, PowerFactory factory) {
		POWER_TYPES.put(id, factory);
	}

	public static void registerAbilityType(Identifier id, AbilityFactory factory) {
		ABILITY_TYPES.put(id, factory);
	}

	public static PowerFactory powerType(Identifier id) {
		return POWER_TYPES.get(id);
	}

	public static AbilityFactory abilityType(Identifier id) {
		return ABILITY_TYPES.get(id);
	}

	/** Registra os tipos embutidos e chama os addons. Deve rodar antes de carregar as raças. */
	public static void bootstrap() {
		if (bootstrapped) return;
		bootstrapped = true;

		// ---- poderes passivos
		registerPowerType(DakuraOrigins.id("attribute"), AttributePower::new);
		registerPowerType(DakuraOrigins.id("status_effect"), StatusEffectPower::new);
		registerPowerType(DakuraOrigins.id("regeneration"), RegenerationPower::new);
		registerPowerType(DakuraOrigins.id("sunlight_damage"), SunlightPower::new);
		registerPowerType(DakuraOrigins.id("restricted_diet"), DietPower::new);
		registerPowerType(DakuraOrigins.id("maturity"), MaturityPower::new);
		registerPowerType(DakuraOrigins.id("damage_immunity"), DamageImmunityPower::new);
		registerPowerType(DakuraOrigins.id("no_armor"), NoArmorPower::new);
		registerPowerType(DakuraOrigins.id("claustrophobia"), ClaustrophobiaPower::new);
		registerPowerType(DakuraOrigins.id("no_hunger"), com.dakura.origins.power.types.NoHungerPower::new);
		registerPowerType(DakuraOrigins.id("monster_truce"), com.dakura.origins.power.types.MonsterTrucePower::new);
		registerPowerType(DakuraOrigins.id("environment_damage"), com.dakura.origins.power.types.EnvironmentDamagePower::new);
		registerPowerType(DakuraOrigins.id("inverted_potions"), com.dakura.origins.power.types.InvertedPotionPower::new);
		registerPowerType(DakuraOrigins.id("second_chance"), com.dakura.origins.power.types.SecondChancePower::new);
		registerPowerType(DakuraOrigins.id("translucent"), com.dakura.origins.power.types.TranslucentPower::new);
		registerPowerType(DakuraOrigins.id("cosmetic"), com.dakura.origins.power.types.CosmeticPower::new);
		registerPowerType(DakuraOrigins.id("hit_effect"), com.dakura.origins.power.types.HitEffectPower::new);
		registerPowerType(DakuraOrigins.id("stealth"), com.dakura.origins.power.types.StealthPower::new);
		registerPowerType(DakuraOrigins.id("fox_friend"), com.dakura.origins.power.types.FoxFriendPower::new);
		registerPowerType(DakuraOrigins.id("no_swim"), com.dakura.origins.power.types.MarkerPower::noSwim);
		registerPowerType(DakuraOrigins.id("powder_snow_walker"), com.dakura.origins.power.types.MarkerPower::snowWalker);
		registerPowerType(DakuraOrigins.id("photosynthesis"), com.dakura.origins.power.types.PhotosynthesisPower::new);
		registerPowerType(DakuraOrigins.id("growth_aura"), com.dakura.origins.power.types.GrowthAuraPower::new);
		registerPowerType(DakuraOrigins.id("conditional_attribute"), com.dakura.origins.power.types.ConditionalAttributePower::new);
		registerPowerType(DakuraOrigins.id("effect_immunity"), com.dakura.origins.power.types.EffectImmunityPower::new);
		registerPowerType(DakuraOrigins.id("damage_multiplier"), com.dakura.origins.power.types.DamageMultiplierPower::new);
		registerPowerType(DakuraOrigins.id("nature_oath"), com.dakura.origins.power.types.NatureOathPower::new);
		registerPowerType(DakuraOrigins.id("flower_crown"), com.dakura.origins.power.types.FlowerCrownPower::new);
		registerPowerType(DakuraOrigins.id("slow_hunger"), com.dakura.origins.power.types.SlowHungerPower::new);
		registerPowerType(DakuraOrigins.id("archery"), com.dakura.origins.power.types.ArcheryPower::new);
		registerPowerType(DakuraOrigins.id("marker"), com.dakura.origins.power.types.MarkerPower::generic);
		registerPowerType(DakuraOrigins.id("food_multiplier"), com.dakura.origins.power.types.FoodMultiplierPower::new);
		registerPowerType(DakuraOrigins.id("sleep_altitude"), com.dakura.origins.power.types.SleepAltitudePower::new);
		registerPowerType(DakuraOrigins.id("berserk"), com.dakura.origins.power.types.BerserkPower::new);
		registerPowerType(DakuraOrigins.id("flight"), com.dakura.origins.power.types.FlightPower::new);
		registerPowerType(DakuraOrigins.id("statue"), com.dakura.origins.power.types.StatuePower::new);
		registerPowerType(DakuraOrigins.id("damage_cap"), com.dakura.origins.power.types.DamageCapPower::new);
		registerPowerType(DakuraOrigins.id("desperate_regeneration"), com.dakura.origins.power.types.DesperateRegenPower::new);
		registerPowerType(DakuraOrigins.id("enchanted_weakness"), com.dakura.origins.power.types.EnchantWeaknessPower::new);
		registerPowerType(DakuraOrigins.id("forest_stealth"), com.dakura.origins.power.types.ForestStealthPower::new);
		registerPowerType(DakuraOrigins.id("flag"), com.dakura.origins.power.types.FlagPower::new);
		registerPowerType(DakuraOrigins.id("xp_multiplier"), com.dakura.origins.power.types.XpMultiplierPower::new);
		registerPowerType(DakuraOrigins.id("glide"), com.dakura.origins.power.types.GlidePower::new);
		registerPowerType(DakuraOrigins.id("ally_aura"), com.dakura.origins.power.types.AllyAuraPower::new);
		registerPowerType(DakuraOrigins.id("noble_tools"), com.dakura.origins.power.types.ToolRestrictionPower::new);
		registerPowerType(DakuraOrigins.id("greed"), com.dakura.origins.power.types.GreedPower::new);
		registerPowerType(DakuraOrigins.id("cursed_rhythm"), com.dakura.origins.power.types.CursedRhythmPower::new);
		registerPowerType(DakuraOrigins.id("pacifist"), com.dakura.origins.power.types.PacifistPower::new);
		registerPowerType(DakuraOrigins.id("aquatic"), com.dakura.origins.power.types.AquaticPower::new);
		registerPowerType(DakuraOrigins.id("rebirth"), com.dakura.origins.power.types.RebirthPower::new);
		registerPowerType(DakuraOrigins.id("rabbit_frenzy"), com.dakura.origins.power.types.RabbitFrenzyPower::new);
		registerPowerType(DakuraOrigins.id("lily_pad"), com.dakura.origins.power.types.LilyPadPower::new);
		com.dakura.origins.power.types.RebirthPower.bootstrap();
		com.dakura.origins.power.types.GlidePower.bootstrap();
		com.dakura.origins.power.types.FlightPower.bootstrap();
		registerPowerType(DakuraOrigins.id("golden_ward"), com.dakura.origins.power.types.GoldenWardPower::new);
		com.dakura.origins.power.types.GoldenWardPower.bootstrap();
		registerPowerType(DakuraOrigins.id("toxic_affinity"), com.dakura.origins.power.types.ToxicAffinityPower::new);
		registerPowerType(DakuraOrigins.id("fungal_emergency"), com.dakura.origins.power.types.FungalEmergencyPower::new);
		com.dakura.origins.power.types.FungalEmergencyPower.bootstrap();
		com.dakura.origins.power.types.RabbitFrenzyPower.bootstrap();
		com.dakura.origins.power.types.CursedRhythmPower.bootstrap();
		com.dakura.origins.power.types.PacifistPower.bootstrap();
		com.dakura.origins.power.types.ToolRestrictionPower.bootstrap();
		com.dakura.origins.power.types.GreedPower.bootstrap();
		registerPowerType(DakuraOrigins.id("vanilla_perk"), com.dakura.origins.power.types.VanillaPerkPower::new);
		com.dakura.origins.power.types.VanillaPerkPower.bootstrap();
		registerPowerType(DakuraOrigins.id("moon_mark"), com.dakura.origins.power.types.MoonMarkPower::new);
		com.dakura.origins.power.types.MoonMarkPower.bootstrap();
		com.dakura.origins.power.Control.bootstrap();
		registerPowerType(DakuraOrigins.id("soul_harvest"), com.dakura.origins.power.types.SoulHarvestPower::new);
		com.dakura.origins.power.types.SoulHarvestPower.bootstrap();
		registerAbilityType(DakuraOrigins.id("blood_shield"), com.dakura.origins.power.ability.BloodShieldAbility::new);
		com.dakura.origins.power.ability.BloodShieldAbility.bootstrap();
		registerPowerType(DakuraOrigins.id("devour"), com.dakura.origins.power.types.DevourPower::new);
		com.dakura.origins.power.types.DevourPower.bootstrap();
		com.dakura.origins.power.types.SleepAltitudePower.bootstrap();
		com.dakura.origins.power.types.EnchantWeaknessPower.bootstrap();
		MaturityPower.init();
		EntityFlags.bootstrap();

		// ---- habilidades ativas
		registerAbilityType(DakuraOrigins.id("vampire_bite"), VampireBiteAbility::new);
		registerAbilityType(DakuraOrigins.id("bat_form"), BatFormAbility::new);
		registerAbilityType(DakuraOrigins.id("fireball"), FireballAbility::new);
		registerAbilityType(DakuraOrigins.id("breath"), BreathAbility::new);
		registerAbilityType(DakuraOrigins.id("phase"), com.dakura.origins.power.ability.PhaseAbility::new);
		registerAbilityType(DakuraOrigins.id("stomp"), com.dakura.origins.power.ability.StompAbility::new);
		com.dakura.origins.power.ability.StompAbility.bootstrap();
		registerAbilityType(DakuraOrigins.id("fox_form"), com.dakura.origins.power.ability.FoxFormAbility::new);
		registerAbilityType(DakuraOrigins.id("fox_clones"), com.dakura.origins.power.ability.FoxClonesAbility::new);
		com.dakura.origins.power.ability.FoxFormAbility.bootstrap();
		com.dakura.origins.power.types.HitEffectPower.bootstrap();
		registerAbilityType(DakuraOrigins.id("vine_snare"), com.dakura.origins.power.ability.VineSnareAbility::new);
		registerAbilityType(DakuraOrigins.id("solar_beam"), com.dakura.origins.power.ability.SolarBeamAbility::new);
		registerAbilityType(DakuraOrigins.id("dash"), com.dakura.origins.power.ability.ElfDashAbility::new);
		registerAbilityType(DakuraOrigins.id("keen_sight"), com.dakura.origins.power.ability.KeenSightAbility::new);
		com.dakura.origins.power.ability.VineSnareAbility.bootstrap();
		com.dakura.origins.power.ability.SolarBeamAbility.bootstrap();
		com.dakura.origins.power.types.NatureOathPower.bootstrap();
		com.dakura.origins.power.types.ArcheryPower.bootstrap();
		com.dakura.origins.power.types.SecondChancePower.bootstrap();
		com.dakura.origins.power.ability.PhaseAbility.bootstrap();
		VampireBiteAbility.bootstrap();
		BatFormAbility.bootstrap();
		FireballAbility.bootstrap();
		BreathAbility.bootstrap();

		registerAbilityType(DakuraOrigins.id("passive"), com.dakura.origins.power.ability.PassiveAbility::new);
		registerAbilityType(DakuraOrigins.id("charged_jump"), com.dakura.origins.power.ability.ChargedJumpAbility::new);
		registerAbilityType(DakuraOrigins.id("headbutt"), com.dakura.origins.power.ability.HeadbuttAbility::new);
		registerAbilityType(DakuraOrigins.id("boulder"), com.dakura.origins.power.ability.BoulderAbility::new);
		registerAbilityType(DakuraOrigins.id("meteor"), com.dakura.origins.power.ability.MeteorAbility::new);
		registerAbilityType(DakuraOrigins.id("summon_undead"), com.dakura.origins.power.ability.SummonUndeadAbility::new);
		registerAbilityType(DakuraOrigins.id("charm"), com.dakura.origins.power.ability.CharmAbility::new);
		registerAbilityType(DakuraOrigins.id("morph"), com.dakura.origins.power.ability.MorphAbility::new);
		registerAbilityType(DakuraOrigins.id("feather_barrage"), com.dakura.origins.power.ability.FeatherBarrageAbility::new);
		registerAbilityType(DakuraOrigins.id("buff_mode"), com.dakura.origins.power.ability.BuffModeAbility::new);
		registerAbilityType(DakuraOrigins.id("tether"), com.dakura.origins.power.ability.TetherAbility::new);
		registerAbilityType(DakuraOrigins.id("mirror_shield"), com.dakura.origins.power.ability.MirrorShieldAbility::new);
		registerAbilityType(DakuraOrigins.id("shadow_orb"), com.dakura.origins.power.ability.ShadowOrbAbility::new);
		registerAbilityType(DakuraOrigins.id("shadow_step"), com.dakura.origins.power.ability.ShadowStepAbility::new);
		registerAbilityType(DakuraOrigins.id("drain_kiss"), com.dakura.origins.power.ability.DrainKissAbility::new);
		registerAbilityType(DakuraOrigins.id("mimic"), com.dakura.origins.power.ability.MimicAbility::new);
		registerAbilityType(DakuraOrigins.id("war_cry"), com.dakura.origins.power.ability.WarCryAbility::new);
		registerAbilityType(DakuraOrigins.id("brutal_charge"), com.dakura.origins.power.ability.BrutalChargeAbility::new);
		registerAbilityType(DakuraOrigins.id("fairy_perch"), com.dakura.origins.power.ability.FairyPerchAbility::new);
		registerAbilityType(DakuraOrigins.id("fairy_dust"), com.dakura.origins.power.ability.FairyDustAbility::new);
		registerAbilityType(DakuraOrigins.id("golden_fist"), com.dakura.origins.power.ability.GoldenFistAbility::new);
		registerAbilityType(DakuraOrigins.id("casino"), com.dakura.origins.power.ability.CasinoAbility::new);
		registerAbilityType(DakuraOrigins.id("cursed_threads"), com.dakura.origins.power.ability.CursedThreadsAbility::new);
		registerAbilityType(DakuraOrigins.id("waterfowl_dance"), com.dakura.origins.power.ability.WaterfowlDanceAbility::new);
		registerAbilityType(DakuraOrigins.id("foxfire"), com.dakura.origins.power.ability.FoxfireAbility::new);
		registerAbilityType(DakuraOrigins.id("nine_tails"), com.dakura.origins.power.ability.NineTailsBlessingAbility::new);
		registerAbilityType(DakuraOrigins.id("tidal_song"), com.dakura.origins.power.ability.TidalSongAbility::new);
		registerAbilityType(DakuraOrigins.id("tsunami"), com.dakura.origins.power.ability.TsunamiAbility::new);
		registerAbilityType(DakuraOrigins.id("solar_dive"), com.dakura.origins.power.ability.SolarDiveAbility::new);
		registerAbilityType(DakuraOrigins.id("dawn_wings"), com.dakura.origins.power.ability.DawnWingsAbility::new);
		registerAbilityType(DakuraOrigins.id("rabbit_rush"), com.dakura.origins.power.ability.RabbitRushAbility::new);
		registerAbilityType(DakuraOrigins.id("moon_leap"), com.dakura.origins.power.ability.MoonLeapAbility::new);
		registerAbilityType(DakuraOrigins.id("fist_barrage"), com.dakura.origins.power.ability.FistBarrageAbility::new);
		registerAbilityType(DakuraOrigins.id("spirit_orb"), com.dakura.origins.power.ability.SpiritOrbAbility::new);
		registerAbilityType(DakuraOrigins.id("dark_comet"), com.dakura.origins.power.ability.DarkCometAbility::new);
		registerAbilityType(DakuraOrigins.id("steel_tempest"), com.dakura.origins.power.ability.SteelTempestAbility::new);
		registerPowerType(DakuraOrigins.id("doll_leap"), com.dakura.origins.power.types.DollLeapPower::new);
		registerAbilityType(DakuraOrigins.id("natural_growth"), com.dakura.origins.power.ability.NaturalGrowthAbility::new);
		registerAbilityType(DakuraOrigins.id("mana_orbs"), com.dakura.origins.power.ability.ManaOrbsAbility::new);
		registerAbilityType(DakuraOrigins.id("flower_sanctuary"), com.dakura.origins.power.ability.FlowerSanctuaryAbility::new);
		com.dakura.origins.power.ability.LegendKit.bootstrap();
		registerAbilityType(DakuraOrigins.id("toxic_cauldron"), com.dakura.origins.power.ability.ToxicCauldronAbility::new);
		registerAbilityType(DakuraOrigins.id("magma_volley"), com.dakura.origins.power.ability.MagmaVolleyAbility::new);
		registerAbilityType(DakuraOrigins.id("skyfall_grab"), com.dakura.origins.power.ability.SkyfallGrabAbility::new);
		com.dakura.origins.power.ability.AreaZones.bootstrap();
		com.dakura.origins.power.ability.ToxicCauldronAbility.bootstrap();
		com.dakura.origins.power.ability.MagmaVolleyAbility.bootstrap();
		com.dakura.origins.power.ability.SkyfallGrabAbility.bootstrap();
		com.dakura.origins.power.ability.ElfDashAbility.bootstrap();
		com.dakura.origins.power.ability.WaterfowlDanceAbility.bootstrap();
		com.dakura.origins.power.ability.FoxfireAbility.bootstrap();
		com.dakura.origins.power.ability.Channeling.bootstrap();
		com.dakura.origins.power.ability.TetherAbility.bootstrap();
		com.dakura.origins.power.ability.ManaOrbsAbility.bootstrap();
		com.dakura.origins.power.types.MonsterTrucePower.bootstrap();
		com.dakura.origins.power.ability.MirrorShieldAbility.bootstrap();
		com.dakura.origins.power.ability.ShadowOrbAbility.bootstrap();
		com.dakura.origins.power.ability.DrainKissAbility.bootstrap();
		registerAbilityType(DakuraOrigins.id("night_swords"), com.dakura.origins.power.ability.NightSwordsAbility::new);
		registerAbilityType(DakuraOrigins.id("moon_beam"), com.dakura.origins.power.ability.MoonBeamAbility::new);
		registerAbilityType(DakuraOrigins.id("moonfall"), com.dakura.origins.power.ability.MoonfallAbility::new);
		com.dakura.origins.power.ability.NightSwordsAbility.bootstrap();
		com.dakura.origins.power.ability.MoonfallAbility.bootstrap();
		com.dakura.origins.power.ability.MimicAbility.bootstrap();
		com.dakura.origins.power.ability.BrutalChargeAbility.bootstrap();
		com.dakura.origins.power.ability.FairyPerchAbility.bootstrap();
		com.dakura.origins.power.ability.CasinoAbility.bootstrap();
		com.dakura.origins.power.ability.BoulderAbility.bootstrap();
		com.dakura.origins.power.ability.MeteorAbility.bootstrap();
		com.dakura.origins.power.ability.CharmAbility.bootstrap();
		com.dakura.origins.power.ability.MorphAbility.bootstrap();
		com.dakura.origins.power.ability.FeatherBarrageAbility.bootstrap();
		com.dakura.origins.power.ability.BuffModeAbility.bootstrap();

		// ---- addons
		for (DakuraOriginsPlugin plugin : FabricLoader.getInstance()
				.getEntrypoints(DakuraOrigins.MOD_ID, DakuraOriginsPlugin.class)) {
			try {
				plugin.registerTypes();
			} catch (Exception e) {
				DakuraOrigins.LOGGER.error("Addon falhou ao registrar tipos: {}", plugin.getClass().getName(), e);
			}
		}
	}
}
