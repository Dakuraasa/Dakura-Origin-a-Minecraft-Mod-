package com.dakura.origins.client.screen;

import com.dakura.origins.client.DakuraOriginsClient;
import com.dakura.origins.power.ability.BlessingAltarAbility;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * The Fairy's blessing altar: a dark green panel framed by pixel-art flowers, the held item floating in the middle
 * and the rolled enchantments listed as plates to pick from.
 */
public class AltarScreen extends Screen {
	private static final int W = 260, H = 196;
	private static final int BORDER = 0xFF1E3A22, FRAME = 0xFF2F5A33, PANEL = 0xFF16281A, PANEL_HI = 0xFF1D3522;
	private static final int TITLE = 0xFFB9E89A, TEXT = 0xFFD8F2C4, HINT = 0xFF8DBF7A;
	private static final int[] PETALS = {0xFFF4A6C8, 0xFFFFE27A, 0xFFF2F2F2, 0xFFC9A6F4, 0xFFFF9E7A};
	private final List<Text> names = new ArrayList<>();
	private final long opened = System.currentTimeMillis();
	private int hover = -1;
	private boolean chosen;

	public AltarScreen(List<int[]> options) {
		super(Text.translatable("dakura_origins.altar.title"));
		var mc = net.minecraft.client.MinecraftClient.getInstance();
		if (mc.world != null) {
			var reg = mc.world.getRegistryManager().get(RegistryKeys.ENCHANTMENT);
			for (int[] o : options) {
				Enchantment e = reg.get(o[0]);
				names.add(e == null ? Text.literal("?") : Enchantment.getName(reg.getEntry(e), o[1]));
			}
		}
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	private int left() { return (width - W) / 2; }
	private int top() { return (height - H) / 2; }
	private int optY(int i) { return top() + 98 + i * 24; }

	@Override
	public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
		ctx.fillGradient(0, 0, width, height, 0x66102414, 0xCC07120A);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		float t = (System.currentTimeMillis() - opened) / 1000f;
		float in = Math.min(1f, t / 0.35f);
		int x = left(), y = top() + Math.round((1 - in) * 20);

		round(ctx, x, y, W, H, BORDER);
		round(ctx, x + 2, y + 2, W - 4, H - 4, FRAME);
		round(ctx, x + 5, y + 5, W - 10, H - 10, PANEL);
		ctx.fillGradient(x + 6, y + 6, x + W - 6, y + 60, 0x3048A040, 0x00000000);
		flowers(ctx, x, y, t);

		Text title = Text.translatable("dakura_origins.altar.title").formatted(Formatting.BOLD);
		ctx.getMatrices().push();
		ctx.getMatrices().translate(x + W / 2f, y + 12, 0);
		ctx.getMatrices().scale(1.4f, 1.4f, 1f);
		ctx.drawText(textRenderer, title, -textRenderer.getWidth(title) / 2, 0, TITLE, false);
		ctx.getMatrices().pop();

		ItemStack hand = client != null && client.player != null ? client.player.getMainHandStack() : ItemStack.EMPTY;
		float bob = (float) Math.sin(t * 2.2f) * 3f;
		int cx = x + W / 2, cy = y + 56;
		for (int i = 0; i < 20; i++) {
			double a = t * 0.8 + i * Math.PI / 10;
			int hx = cx + (int) Math.round(Math.cos(a) * 26), hy = cy + 12 + (int) Math.round(Math.sin(a) * 6);
			ctx.fill(hx - 1, hy - 1, hx + 1, hy + 1, i % 2 == 0 ? 0xFF7FD36A : 0xFFB9E89A);
		}
		ctx.getMatrices().push();
		ctx.getMatrices().translate(cx - 16, cy - 20 + bob, 0);
		ctx.getMatrices().scale(2f, 2f, 1f);
		ctx.drawItem(hand, 0, 0);
		ctx.getMatrices().pop();

		for (int i = 0; i < 14; i++) {
			float k = ((t * 0.35f + i * 0.071f) % 1f);
			int sx = x + 20 + (i * 37) % (W - 40), sy = y + H - 12 - Math.round(k * (H - 30));
			int a = Math.round(MathHelper.sin(k * (float) Math.PI) * 200);
			ctx.fill(sx, sy, sx + 2, sy + 2, (a << 24) | 0xA8F08C);
		}

		hover = -1;
		for (int i = 0; i < names.size(); i++) {
			int oy = optY(i), ox = x + 24, ow = W - 48;
			boolean h = !chosen && mouseX >= ox && mouseX < ox + ow && mouseY >= oy && mouseY < oy + 20;
			if (h) hover = i;
			round(ctx, ox, oy, ow, 20, h ? 0xFF6FBF55 : 0xFF2F5A33);
			round(ctx, ox + 1, oy + 1, ow - 2, 18, h ? 0xFF2E5A2A : PANEL_HI);
			flower(ctx, ox + 6, oy + 7, PETALS[i % PETALS.length]);
			flower(ctx, ox + ow - 11, oy + 7, PETALS[(i + 2) % PETALS.length]);
			Text n = names.get(i);
			ctx.drawText(textRenderer, n, ox + (ow - textRenderer.getWidth(n)) / 2, oy + 6, h ? 0xFFFFFFFF : TEXT, false);
		}
		Text hint = Text.translatable("dakura_origins.altar.hint").formatted(Formatting.ITALIC);
		ctx.drawText(textRenderer, hint, x + (W - textRenderer.getWidth(hint)) / 2, y + H - 16, HINT, false);
	}

	/** Pixel-art flowers growing along the panel frame, swaying slightly. */
	private void flowers(DrawContext ctx, int x, int y, float t) {
		int n = 0;
		for (int fx = x + 10; fx < x + W - 10; fx += 22) {
			int sway = Math.round(MathHelper.sin(t * 1.6f + n) * 1f);
			stem(ctx, fx + 2, y + H - 4, 6);
			flower(ctx, fx + sway, y + H - 13, PETALS[n % PETALS.length]);
			if (n % 2 == 0) flower(ctx, fx + 9, y - 3, PETALS[(n + 3) % PETALS.length]);
			n++;
		}
		for (int fy = y + 24; fy < y + H - 20; fy += 26) {
			flower(ctx, x - 3, fy, PETALS[n % PETALS.length]);
			flower(ctx, x + W - 2, fy + 11, PETALS[(n + 1) % PETALS.length]);
			n++;
		}
	}

	private static void stem(DrawContext ctx, int x, int bottom, int h) {
		ctx.fill(x, bottom - h, x + 1, bottom, 0xFF3F8A3A);
		ctx.fill(x + 1, bottom - h / 2 - 1, x + 3, bottom - h / 2, 0xFF5FB04F);
	}

	private static void flower(DrawContext ctx, int x, int y, int petal) {
		ctx.fill(x + 1, y, x + 4, y + 1, petal);
		ctx.fill(x, y + 1, x + 5, y + 4, petal);
		ctx.fill(x + 1, y + 4, x + 4, y + 5, petal);
		ctx.fill(x + 2, y + 2, x + 3, y + 3, 0xFFFFD23A);
		ctx.fill(x + 1, y + 1, x + 2, y + 2, 0x40FFFFFF);
	}

	private static void round(DrawContext ctx, int x, int y, int w, int h, int c) {
		ctx.fill(x + 2, y, x + w - 2, y + h, c);
		ctx.fill(x, y + 2, x + w, y + h - 2, c);
		ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, c);
	}

	@Override
	public boolean mouseClicked(double mx, double my, int button) {
		if (button == 0 && hover >= 0 && !chosen) {
			chosen = true;
			DakuraOriginsClient.sendSpecial(BlessingAltarAbility.class, 5, hover);
			if (client != null) client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.6f, 0.8f));
			close();
			return true;
		}
		return super.mouseClicked(mx, my, button);
	}
}
