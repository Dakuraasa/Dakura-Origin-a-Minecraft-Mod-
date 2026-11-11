package com.dakura.origins.power.ability;

import com.dakura.origins.DakuraOrigins;
import com.dakura.origins.network.AnimPayload;
import com.dakura.origins.network.ModNetworking;
import com.dakura.origins.power.Ability;
import com.dakura.origins.power.EntityFlags;
import com.dakura.origins.power.JsonParams;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Box;

/**
 * "dakura_origins:fox_form" — vira a raposa vanilla (vermelha, ou da neve com "snow": true), sem limite
 * de tempo: mais rápida, pula mais alto, não toma dano de queda, regenera e deixa um rastro de fogo/gelo
 * (só visual). { "snow": false, "speed": 0.35, "jump": 0.3 }  Apertar de novo volta (se houver espaço).
 */
public final class FoxFormAbility implements Ability {
	public static final String FLAG = "fox_form", SNOW_FLAG = "fox_snow";
	private static final Identifier SPEED_ID = DakuraOrigins.id("fox_form_speed"), JUMP_ID = DakuraOrigins.id("fox_form_jump");
	private final boolean snow;
	private final double speed, jump;

	public FoxFormAbility(JsonObject json) {
		this.snow = JsonParams.bool(json, "snow", false);
		this.speed = JsonParams.num(json, "speed", 0.35f);
		this.jump = JsonParams.num(json, "jump", 0.3f);
	}

	/** Os dois lados. */
	public static boolean isFox(PlayerEntity p) {
		return p.getWorld().isClient ? EntityFlags.clientHas(p, FLAG) : p instanceof ServerPlayerEntity sp && EntityFlags.has(sp, FLAG);
	}

	public static void bootstrap() {
		ServerTickEvents.END_SERVER_TICK.register(FoxFormAbility::tickAll);
		ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> {
			if (EntityFlags.has(newP, FLAG)) {
				EntityFlags.set(newP, FLAG, false);
				EntityFlags.set(newP, SNOW_FLAG, false);
			}
		});
		UseItemCallback.EVENT.register((p, w, h) -> isFox(p)
				? TypedActionResult.fail(p.getStackInHand(h)) : TypedActionResult.pass(p.getStackInHand(h)));
		UseBlockCallback.EVENT.register((p, w, h, hit) -> isFox(p) ? ActionResult.FAIL : ActionResult.PASS);
		net.fabricmc.fabric.api.event.player.AttackEntityCallback.EVENT.register((p, w, h, e, hit) -> isFox(p) ? ActionResult.FAIL : ActionResult.PASS);
		net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.ALLOW_DAMAGE.register((e, src, amt) ->
				!(BURSTING != null && e == BURSTING));
	}

	private static Entity BURSTING;

	/** Na forma de raposa, o botão de ataque vira uma explosão de fogo (ou gelo) que quebra blocos e devolve a forma humana. */
	@Override
	public void attackKey(ServerPlayerEntity p) {
		if (!EntityFlags.has(p, FLAG)) return;
		var w = p.getServerWorld();
		double x = p.getX(), y = p.getY() + 0.4, z = p.getZ();
		BURSTING = p;
		try {
			w.createExplosion(p, x, y, z, 2.6f, !snow, net.minecraft.world.World.ExplosionSourceType.TNT);
		} finally {
			BURSTING = null;
		}
		if (snow) {
			for (Entity e : w.getOtherEntities(p, p.getBoundingBox().expand(4.5))) {
				if (e instanceof net.minecraft.entity.LivingEntity le) {
					le.setFrozenTicks(Math.max(le.getFrozenTicks(), 260));
					le.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 2));
				}
			}
			w.spawnParticles(net.minecraft.particle.ParticleTypes.SNOWFLAKE, x, y, z, 80, 1.8, 0.9, 1.8, 0.15);
			w.playSound(null, x, y, z, SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.PLAYERS, 1.0f, 0.6f);
			w.playSound(null, x, y, z, SoundEvents.ENTITY_PLAYER_HURT_FREEZE, SoundCategory.PLAYERS, 1.0f, 0.8f);
		} else {
			w.spawnParticles(net.minecraft.particle.ParticleTypes.FLAME, x, y, z, 60, 1.6, 0.8, 1.6, 0.12);
			// fogo de verdade nos atingidos (queimadura real, carne assada)
			DragonFire.burnArea(w, new net.minecraft.util.math.Vec3d(x, y, z), 4.5, p, 2f, 5);
		}
		ModNetworking.sendAround(new AnimPayload(p.getId(), "fx:fox_burst", snow ? 1 : 0), p);
		Box standing = new Box(p.getX() - 0.3, p.getY(), p.getZ() - 0.3, p.getX() + 0.3, p.getY() + p.getScale() * 1.8, p.getZ() + 0.3);
		if (p.getWorld().isSpaceEmpty(p, standing)) setForm(p, false);
	}

	@Override
	public boolean activate(ServerPlayerEntity p) {
		if (EntityFlags.has(p, FLAG)) {
			Box standing = new Box(p.getX() - 0.3, p.getY(), p.getZ() - 0.3, p.getX() + 0.3, p.getY() + p.getScale() * 1.8, p.getZ() + 0.3);
			if (!p.getWorld().isSpaceEmpty(p, standing)) {
				p.sendMessage(Text.translatable("dakura_origins.message.bat_no_room").formatted(Formatting.GRAY), true);
				return false;
			}
			setForm(p, false);
			return true;
		}
		setForm(p, true);
		return true;
	}

	@Override
	public void onRemove(ServerPlayerEntity p) {
		if (EntityFlags.has(p, FLAG)) setForm(p, false);
	}

	private void setForm(ServerPlayerEntity p, boolean on) {
		EntityFlags.set(p, SNOW_FLAG, on && snow);
		EntityFlags.set(p, FLAG, on);
		modifier(p, EntityAttributes.GENERIC_MOVEMENT_SPEED, SPEED_ID, on ? speed : 0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		modifier(p, EntityAttributes.GENERIC_JUMP_STRENGTH, JUMP_ID, on ? jump : 0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_BASE);
		if (!on) {
			StatusEffectInstance r = p.getStatusEffect(StatusEffects.REGENERATION);
			if (r != null && r.isAmbient()) p.removeStatusEffect(StatusEffects.REGENERATION);
		}
		p.calculateDimensions();
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 0.45f, on ? 1.2f : 0.9f);
		p.getWorld().playSound(null, p.getX(), p.getY(), p.getZ(), on ? SoundEvents.ENTITY_FOX_AMBIENT : SoundEvents.ENTITY_FOX_SNIFF,
				SoundCategory.PLAYERS, 0.8f, 1.1f);
	}

	private static void modifier(ServerPlayerEntity p, RegistryEntry<EntityAttribute> attr, Identifier id, double amount,
								 EntityAttributeModifier.Operation op) {
		var inst = p.getAttributeInstance(attr);
		if (inst == null) return;
		inst.removeModifier(id);
		if (amount != 0) inst.addTemporaryModifier(new EntityAttributeModifier(id, amount, op));
	}

	private static void tickAll(MinecraftServer server) {
		if (server.getTicks() % 20 != 0) return;
		for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
			if (!EntityFlags.has(p, FLAG)) continue;
			p.fallDistance = 0;
			p.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 60, 0, true, false, false));
		}
	}
}
