package com.dakura.origins.client.screen;

import com.dakura.origins.client.DakuraOriginsClient;
import com.dakura.origins.power.ability.CelestialAltarAbility;
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
 * Altar Celestial do Serafim: a "mesa de encantamentos" do mod — painel branco e dourado, asas de anjo batendo
 * nas laterais, o item flutuando no centro e as bênçãos sorteadas como placas para escolher.
 */
public class SeraphAltarScreen extends Screen {
	private static final int W = 260, H = 196;
	private final List<Text> names = new ArrayList<>();
	private final long opened = System.currentTimeMillis();
	private int hover = -1;
	private boolean chosen;

	public SeraphAltarScreen(List<int[]> options) {
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
		ctx.fillGradient(0, 0, width, height, 0x88FFF8E0, 0xCC2A2410);
	}

	@Override
	public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
		super.render(ctx, mouseX, mouseY, delta);
		float t = (System.currentTimeMillis() - opened) / 1000f;
		float in = Math.min(1f, t / 0.35f);
		int x = left(), y = top() + Math.round((1 - in) * 20);

		// asas de anjo nas laterais, batendo devagar
		drawWing(ctx, x - 8, y + 40, -1, t);
		drawWing(ctx, x + W + 8, y + 40, 1, t);

		// painel arredondado com borda dourada dupla
		round(ctx, x, y, W, H, 0xFFE9C66A);
		round(ctx, x + 2, y + 2, W - 4, H - 4, 0xFFFFFDF4);
		round(ctx, x + 5, y + 5, W - 10, H - 10, 0xFFFFF6DC);
		ctx.fillGradient(x + 6, y + 6, x + W - 6, y + 60, 0x40FFE7A0, 0x00FFFFFF);

		// título
		Text title = Text.translatable("dakura_origins.altar.title").formatted(Formatting.BOLD);
		ctx.getMatrices().push();
		ctx.getMatrices().translate(x + W / 2f, y + 12, 0);
		ctx.getMatrices().scale(1.4f, 1.4f, 1f);
		ctx.drawText(textRenderer, title, -textRenderer.getWidth(title) / 2, 0, 0xFFB08417, false);
		ctx.getMatrices().pop();

		// item flutuando sobre um halo
		ItemStack hand = client != null && client.player != null ? client.player.getMainHandStack() : ItemStack.EMPTY;
		float bob = (float) Math.sin(t * 2.2f) * 3f;
		int cx = x + W / 2, cy = y + 56;
		for (int i = 0; i < 20; i++) {
			double a = t * 0.8 + i * Math.PI / 10;
			int hx = cx + (int) Math.round(Math.cos(a) * 26), hy = cy + 12 + (int) Math.round(Math.sin(a) * 6);
			ctx.fill(hx - 1, hy - 1, hx + 1, hy + 1, 0xFFF2C94C);
		}
		ctx.getMatrices().push();
		ctx.getMatrices().translate(cx - 16, cy - 20 + bob, 0);
		ctx.getMatrices().scale(2f, 2f, 1f);
		ctx.drawItem(hand, 0, 0);
		ctx.getMatrices().pop();

		// faíscas subindo
		for (int i = 0; i < 14; i++) {
			float k = ((t * 0.35f + i * 0.071f) % 1f);
			int sx = x + 20 + (i * 37) % (W - 40), sy = y + H - 12 - Math.round(k * (H - 30));
			int a = Math.round(MathHelper.sin(k * (float) Math.PI) * 200);
			ctx.fill(sx, sy, sx + 2, sy + 2, (a << 24) | 0xFFE07A);
		}

		// opções
		hover = -1;
		for (int i = 0; i < names.size(); i++) {
			int oy = optY(i), ox = x + 24, ow = W - 48;
			boolean h = !chosen && mouseX >= ox && mouseX < ox + ow && mouseY >= oy && mouseY < oy + 20;
			if (h) hover = i;
			round(ctx, ox, oy, ow, 20, h ? 0xFFE9B93A : 0xFFE6D3A0);
			round(ctx, ox + 1, oy + 1, ow - 2, 18, h ? 0xFFFFF0C2 : 0xFFFFFBEF);
			// asinhas na placa
			for (int s = -1; s <= 1; s += 2) {
				int wx = s < 0 ? ox + 8 : ox + ow - 8;
				ctx.fill(wx - 3, oy + 7, wx + 3, oy + 9, 0xFFE9C66A);
				ctx.fill(wx - 2 * s - 2, oy + 5, wx - 2 * s + 2, oy + 7, 0xFFF2DD98);
			}
			Text n = names.get(i);
			ctx.drawText(textRenderer, n, ox + (ow - textRenderer.getWidth(n)) / 2, oy + 6, h ? 0xFF8A5A00 : 0xFF6B5320, false);
		}
		Text hint = Text.translatable("dakura_origins.altar.hint").formatted(Formatting.ITALIC);
		ctx.drawText(textRenderer, hint, x + (W - textRenderer.getWidth(hint)) / 2, y + H - 16, 0xFFA48B4C, false);
	}

	/** Asa de anjo em pixel art: fileiras de penas arredondadas, abrindo e fechando. */
	private static void drawWing(DrawContext ctx, int ax, int ay, int dir, float t) {
		float flap = (float) Math.sin(t * 1.8f) * 0.18f;
		for (int row = 0; row < 6; row++) {
			int len = 46 - row * 5;
			double ang = -0.9 + row * 0.32 + flap;
			for (int k = 0; k < len; k += 2) {
				int px = ax + dir * (int) Math.round(Math.cos(ang) * k), py = ay + (int) Math.round(Math.sin(ang) * k * 0.9) + row * 6;
				int c = k > len - 8 ? 0xFFF4E6B8 : 0xFFFFFFFF;
				ctx.fill(px - 2, py - 2, px + 3, py + 3, 0x30000000);
				ctx.fill(px - 2, py - 2, px + 2, py + 2, c);
			}
		}
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
			DakuraOriginsClient.sendSpecial(CelestialAltarAbility.class, 5, hover);
			if (client != null) client.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, 1.6f, 0.8f));
			close();
			return true;
		}
		return super.mouseClicked(mx, my, button);
	}
}
