package com.dakura.origins.entity;

import com.dakura.origins.DakuraOrigins;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModEntities {
	public static final EntityType<HuliCloneEntity> HULI_CLONE = Registry.register(Registries.ENTITY_TYPE,
			DakuraOrigins.id("huli_clone"),
			EntityType.Builder.create(HuliCloneEntity::new, SpawnGroup.MISC).dimensions(0.6f, 1.8f).maxTrackingRange(8)
					.build("huli_clone"));

	public static final EntityType<UndeadMinionEntity> UNDEAD_MINION = Registry.register(Registries.ENTITY_TYPE,
			DakuraOrigins.id("undead_minion"),
			EntityType.Builder.<UndeadMinionEntity>create(UndeadMinionEntity::new, SpawnGroup.MISC).dimensions(0.6f, 1.95f)
					.eyeHeight(1.74f).maxTrackingRange(8).build("undead_minion"));
	public static final EntityType<FeatherEntity> FEATHER = Registry.register(Registries.ENTITY_TYPE,
			DakuraOrigins.id("feather"),
			EntityType.Builder.<FeatherEntity>create(FeatherEntity::new, SpawnGroup.MISC).dimensions(0.4f, 0.4f)
					.maxTrackingRange(4).trackingTickInterval(20).build("feather"));

	public static final EntityType<GeoProjectileEntity> GEO_PROJECTILE = Registry.register(Registries.ENTITY_TYPE,
			DakuraOrigins.id("geo_projectile"),
			EntityType.Builder.<GeoProjectileEntity>create(GeoProjectileEntity::new, SpawnGroup.MISC).dimensions(0.6f, 0.6f)
					.maxTrackingRange(8).trackingTickInterval(1).alwaysUpdateVelocity(true).build("geo_projectile"));

	private ModEntities() {}

	public static void init() {
		FabricDefaultAttributeRegistry.register(HULI_CLONE, HuliCloneEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(UNDEAD_MINION, UndeadMinionEntity.createAttributes());
	}
}
