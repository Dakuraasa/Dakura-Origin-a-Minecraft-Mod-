package com.dakura.origins.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Projétil 3D de verdade (não preso à grade): posição e velocidade contínuas, gravidade e arrasto próprios,
 * colisão com blocos e criaturas. O desenho é um modelo do Blockbench girando (GeoProjectileRenderer),
 * escolhido pelo "kind". O que acontece no impacto é registrado por quem cria o projétil (onImpact).
 */
public class GeoProjectileEntity extends ProjectileEntity {
	public static final int CAULDRON = 0, MAGMA = 1, ARCANE = 2, MANA = 3, DARK = 4, VIOLET = 5;
	private static final TrackedData<Integer> KIND = DataTracker.registerData(GeoProjectileEntity.class, TrackedDataHandlerRegistry.INTEGER);
	private static final TrackedData<Float> SIZE = DataTracker.registerData(GeoProjectileEntity.class, TrackedDataHandlerRegistry.FLOAT);
	/** Reação ao impacto por tipo (registrada pelas habilidades no servidor). */
	private static final Map<Integer, BiConsumer<GeoProjectileEntity, Vec3d>> IMPACT = new HashMap<>();
	private static final Map<Integer, BiConsumer<GeoProjectileEntity, Entity>> HIT_ENTITY = new HashMap<>();

	private double gravity = 0.0, drag = 1.0;
	private int maxAge = 100;
	public float power = 1f;
	/** Controlado por uma habilidade (trajeto e colisão decididos fora): só anda pela velocidade, sem colidir sozinho. */
	public boolean scripted = false;

	public GeoProjectileEntity(EntityType<? extends GeoProjectileEntity> type, World world) {
		super(type, world);
	}

	public GeoProjectileEntity(World world, LivingEntity owner, int kind, double gravity, double drag, int maxAge) {
		this(ModEntities.GEO_PROJECTILE, world);
		setOwner(owner);
		this.dataTracker.set(KIND, kind);
		this.gravity = gravity;
		this.drag = drag;
		this.maxAge = maxAge;
		Vec3d eye = owner.getEyePos().add(owner.getRotationVec(1f).multiply(0.8));
		setPosition(eye.x, eye.y - 0.3, eye.z);
	}

	public static void onImpact(int kind, BiConsumer<GeoProjectileEntity, Vec3d> fn) {
		IMPACT.put(kind, fn);
	}

	public static void onEntityHit(int kind, BiConsumer<GeoProjectileEntity, Entity> fn) {
		HIT_ENTITY.put(kind, fn);
	}

	public int kind() {
		return dataTracker.get(KIND);
	}

	public float size() {
		return dataTracker.get(SIZE);
	}

	public void setSize(float s) {
		dataTracker.set(SIZE, s);
	}

	@Override
	protected void initDataTracker(DataTracker.Builder builder) {
		builder.add(KIND, 0);
		builder.add(SIZE, 1f);
	}

	@Override
	public void tick() {
		super.tick();
		if (!getWorld().isClient && scripted) {
			if (age > 600) discard();
		} else if (!getWorld().isClient) {
			HitResult hit = ProjectileUtil.getCollision(this, this::canHit);
			if (hit.getType() != HitResult.Type.MISS) {
				impact(hit);
				return;
			}
			if (age > maxAge) {
				impact(null);
				return;
			}
		}
		Vec3d v = getVelocity();
		setPosition(getX() + v.x, getY() + v.y, getZ() + v.z);
		setVelocity(v.multiply(drag).add(0, -gravity, 0));
		ProjectileUtil.setRotationFromVelocity(this, 0.4f);
	}

	private void impact(HitResult hit) {
		Vec3d at = hit != null ? hit.getPos() : getPos();
		if (hit instanceof EntityHitResult eh) {
			BiConsumer<GeoProjectileEntity, Entity> fn = HIT_ENTITY.get(kind());
			if (fn != null) fn.accept(this, eh.getEntity());
		}
		BiConsumer<GeoProjectileEntity, Vec3d> fn = IMPACT.get(kind());
		if (fn != null && getWorld() instanceof ServerWorld) fn.accept(this, at);
		discard();
	}

	@Override
	protected boolean canHit(Entity entity) {
		return super.canHit(entity) && entity != getOwner();
	}

	@Override
	public boolean shouldRender(double distance) {
		return distance < 128 * 128;
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		super.writeCustomDataToNbt(nbt);
		nbt.putInt("Kind", kind());
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		super.readCustomDataFromNbt(nbt);
		dataTracker.set(KIND, nbt.getInt("Kind"));
		discard(); // projéteis de habilidade não sobrevivem a um recarregamento do mundo
	}
}
