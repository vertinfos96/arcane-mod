package arcane.client;

import arcane.data.ArcaneData;
import arcane.net.UpgradePayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Окно прокачки навыков (клавиша K). */
public class SkillScreen extends Screen {
	private static final int ROW_H = 30;
	private final List<ButtonWidget> buttons = new ArrayList<>();

	public SkillScreen() {
		super(Text.translatable("screen.arcane.skills"));
	}

	@Override
	protected void init() {
		buttons.clear();
		int left = this.width / 2 - 150;
		int top = this.height / 2 - 70;
		for (int i = 0; i < 5; i++) {
			final int skill = i;
			ButtonWidget button = ButtonWidget.builder(Text.literal("+"), b ->
							ClientPlayNetworking.send(new UpgradePayload(skill)))
					.dimensions(left + 270, top + 22 + i * ROW_H, 30, 20)
					.build();
			buttons.add(button);
			this.addDrawableChild(button);
		}
		this.addDrawableChild(ButtonWidget.builder(Text.translatable("gui.done"), b -> this.close())
				.dimensions(this.width / 2 - 50, top + 22 + 5 * ROW_H + 8, 100, 20)
				.build());
	}

	@Override
	public boolean shouldPause() {
		return false;
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		ArcaneData d = ArcaneClient.data;
		for (int i = 0; i < buttons.size(); i++) {
			buttons.get(i).active = d.canUpgrade(i);
		}
		super.render(context, mouseX, mouseY, delta);

		int left = this.width / 2 - 150;
		int top = this.height / 2 - 70;
		context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, top - 30, 0xFFFFFF);

		int level = d.level();
		int next = ArcaneData.xpForLevel(level + 1);
		Text header = level >= ArcaneData.MAX_LEVEL
				? Text.translatable("screen.arcane.header_max", level, d.points())
				: Text.translatable("screen.arcane.header", level, d.xp(), next, d.points());
		context.drawCenteredTextWithShadow(this.textRenderer, header, this.width / 2, top - 14, 0xFFD966);

		for (int i = 0; i < 5; i++) {
			int y = top + 22 + i * ROW_H;
			Text name = Text.translatable("skill.arcane." + i);
			context.drawTextWithShadow(this.textRenderer, name, left, y, 0xFFFFFF);
			Text desc = Text.translatable("skill.arcane." + i + ".desc");
			context.drawTextWithShadow(this.textRenderer, desc, left, y + 10, 0xAAAAAA);
			Text rank = Text.literal(d.rank(i) + " / " + ArcaneData.maxRank(i));
			int color = d.rank(i) > 0 ? 0x66FF66 : 0xFF8888;
			context.drawTextWithShadow(this.textRenderer, rank, left + 215, y + 6, color);
			if (d.level() < ArcaneData.requiredLevel(i) && d.rank(i) == 0) {
				Text req = Text.translatable("screen.arcane.need_level", ArcaneData.requiredLevel(i));
				context.drawTextWithShadow(this.textRenderer, req, left + 160, y + 6, 0xFFAA55);
			}
		}
	}
}
