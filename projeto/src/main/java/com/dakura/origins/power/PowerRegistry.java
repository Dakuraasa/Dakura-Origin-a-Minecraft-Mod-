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
