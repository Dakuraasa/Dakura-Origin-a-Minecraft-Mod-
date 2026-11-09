package com.dakura.origins.client.anim;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Texto dourado flutuando sobre a cabeça ("JACKPOT", "×3!"): sobe, pulsa e some. */
public final class FloatingText {
	private record Entry(int entityId, Text text, int color, long start, int duration) {}

	private static final List<Entry> ENTRIES = new ArrayList<>();

	private FloatingText() {}

	public static void add(int entityId, String text, int color, int duration) {
		var w = MinecraftClient.getInstance().world;
		if (w == null) return;
		ENTRIES.add(new Entry(entityId, Text.literal(text).formatted(Formatting.BOLD), color, w.getTime(), duration));
	}

	public static void render(WorldRenderContext ctx) {
		if (ENTRIES.isEmpty() || ctx.consumers() == null) return;
		MinecraftClient mc = MinecraftClient.getInstance();
		if (mc.world == null) return;
		float delta = ctx.tickCounter().getTickDelta(true);
		Vec3d cam = ctx.camera().getPos();
		TextRenderer tr = mc.textRenderer;
		MatrixStack m = ctx.matrixStack();
		if (m == null) return;
		Iterator<Entry> it = ENTRIES.iterator();
		while (it.hasNext()) {
			Entry e = it.next();
			float age = mc.world.getTime() - e.start + delta;
			Entity ent = mc.world.getEntityById(e.entityId);
			if (ent == null || age > e.duration) {
				it.remove();
				continue;
			}
			float k = age / e.duration;
			float pop = age < 6 ? 0.6f + 0.4f * (age / 6f) + 0.25f * (float) Math.sin(age / 6f * Math.PI) : 1f;
			float scale = 0.035f * pop;
			Vec3d pos = ent.getLerpedPos(delta).add(0, ent.getHeight() + 0.55 + k * 0.9, 0);
			m.push();
			m.translate(pos.x - cam.x, pos.y - cam.y, pos.z - cam.z);
			m.multiply(ctx.camera().getRotation());
			m.scale(scale, -scale, scale);
			int alpha = (int) (255 * Math.min(1f, (1f - k) * 3f));
			int color = (Math.max(8, alpha) << 24) | e.color;
			float x = -tr.getWidth(e.text) / 2f;
			tr.drawWithOutline(e.text.asOrderedText(), x, 0, color, (Math.max(8, alpha) << 24) | 0x5A3A00, m.peek().getPositionMatrix(),
					ctx.consumers(), 0xF000F0);
			m.pop();
		}
	}
}
