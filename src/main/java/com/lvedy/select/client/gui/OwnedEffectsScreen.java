package com.lvedy.select.client.gui;

import com.lvedy.select.client.ClientEffectData;
import com.lvedy.select.special.select.SelectEffect;
import com.lvedy.select.special.select.SelectEffects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 效果查看界面：上方一排显示已拥有的 buff，下方一排显示已拥有的 debuff，
 * 底部有一个横向滚动条，可左右拖动查看超出屏幕的效果。
 * 仅显示 isVisible() 为 true 的效果。
 */
public class OwnedEffectsScreen extends Screen {

    private static final int CARD_WIDTH = 120;
    private static final int CARD_HEIGHT = 70;
    private static final int CARD_GAP = 10;
    private static final int ROW_GAP = 20;

    private static final int SCROLLBAR_HEIGHT = 8;

    private final List<SelectEffect> buffs = new ArrayList<>();
    private final List<SelectEffect> debuffs = new ArrayList<>();

    private double scrollX = 0;        // 当前横向滚动偏移（像素）
    private double maxScrollX = 0;      // 最大可滚动距离
    private boolean draggingScrollbar = false;

    private int viewLeft, viewRight, viewTop;
    private int scrollbarY;

    public OwnedEffectsScreen() {
        super(Component.literal("已拥有的效果"));
    }

    @Override
    protected void init() {
        buffs.clear();
        debuffs.clear();
        for (String id : ClientEffectData.get()) {
            SelectEffect effect = SelectEffects.byId(id);
            if (effect == null || !effect.isVisible()) continue;
            if (effect.isBuff()) {
                buffs.add(effect);
            } else {
                debuffs.add(effect);
            }
        }

        // 视图区域：屏幕中部留白边。左侧多留出空间给「增益/负面」行标签，避免与第一个卡片贴太近
        viewLeft = 40;
        viewRight = width - 20;
        viewTop = 40;

        // 计算内容总宽度（取 buff 行与 debuff 行较宽者）
        int contentWidth = Math.max(rowWidth(buffs.size()), rowWidth(debuffs.size()));
        int viewWidth = viewRight - viewLeft;
        maxScrollX = Math.max(0, contentWidth - viewWidth);
        scrollX = Math.min(scrollX, maxScrollX);

        scrollbarY = viewTop + CARD_HEIGHT + ROW_GAP + CARD_HEIGHT + 16;
    }

    private int rowWidth(int count) {
        if (count == 0) return 0;
        return count * CARD_WIDTH + (count - 1) * CARD_GAP;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 自绘半透明暗化背景，避免原版 renderBackground 的模糊后处理糊掉界面文字
        graphics.fill(0, 0, width, height, 0xC0101010);

        graphics.drawCenteredString(font, "已拥有的效果", width / 2, 16, 0xFFFFFF);

        int buffRowY = viewTop;
        int debuffRowY = viewTop + CARD_HEIGHT + ROW_GAP;

        // 行标签
        graphics.drawString(font, "增益", 4, buffRowY + CARD_HEIGHT / 2 - font.lineHeight / 2, 0xFF55FF55);
        graphics.drawString(font, "负面", 4, debuffRowY + CARD_HEIGHT / 2 - font.lineHeight / 2, 0xFFFF5555);

        // 裁剪到视图区域，避免卡片画到边框外
        graphics.enableScissor(viewLeft, viewTop, viewRight, debuffRowY + CARD_HEIGHT);
        drawRow(graphics, buffs, buffRowY, true);
        drawRow(graphics, debuffs, debuffRowY, false);
        graphics.disableScissor();

        // 滚动条
        renderScrollbar(graphics);

        // 手动渲染控件（不调用 super.render，避免父类模糊后处理与菜单背景覆盖内容）
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    private void drawRow(GuiGraphics graphics, List<SelectEffect> effects, int rowY, boolean isBuff) {
        int x = viewLeft - (int) scrollX;
        for (SelectEffect effect : effects) {
            drawCard(graphics, x, rowY, effect, isBuff);
            x += CARD_WIDTH + CARD_GAP;
        }
    }

    private void drawCard(GuiGraphics graphics, int x, int y, SelectEffect effect, boolean isBuff) {
        graphics.fill(x, y, x + CARD_WIDTH, y + CARD_HEIGHT, 0xCC000000);
        graphics.renderOutline(x, y, CARD_WIDTH, CARD_HEIGHT, isBuff ? 0xFF55FF55 : 0xFFFF5555);

        int titleColor = isBuff ? 0xFF55FF55 : 0xFFFF5555;
        graphics.drawString(font, effect.getDisplayName(), x + 4, y + 4, titleColor);

        List<net.minecraft.util.FormattedCharSequence> lines =
                font.split(Component.literal(effect.getDescription()), CARD_WIDTH - 8);
        int ty = y + 4 + font.lineHeight + 2;
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            if (ty + font.lineHeight > y + CARD_HEIGHT) break;
            graphics.drawString(font, line, x + 4, ty, 0xFFDDDDDD);
            ty += font.lineHeight;
        }
    }

    private void renderScrollbar(GuiGraphics graphics) {
        int trackLeft = viewLeft;
        int trackRight = viewRight;
        int trackWidth = trackRight - trackLeft;

        // 轨道
        graphics.fill(trackLeft, scrollbarY, trackRight, scrollbarY + SCROLLBAR_HEIGHT, 0xFF333333);

        if (maxScrollX <= 0) {
            // 无需滚动，画一个占满的滑块
            graphics.fill(trackLeft, scrollbarY, trackRight, scrollbarY + SCROLLBAR_HEIGHT, 0xFF888888);
            return;
        }

        int contentWidth = trackWidth + (int) maxScrollX;
        int handleWidth = Math.max(20, (int) ((float) trackWidth * trackWidth / contentWidth));
        int handleX = trackLeft + (int) ((scrollX / maxScrollX) * (trackWidth - handleWidth));
        graphics.fill(handleX, scrollbarY, handleX + handleWidth, scrollbarY + SCROLLBAR_HEIGHT, 0xFFAAAAAA);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseY >= scrollbarY && mouseY <= scrollbarY + SCROLLBAR_HEIGHT
                && mouseX >= viewLeft && mouseX <= viewRight) {
            draggingScrollbar = true;
            updateScrollFromMouse(mouseX);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingScrollbar) {
            updateScrollFromMouse(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingScrollbar = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
        if (maxScrollX > 0) {
            scrollX = Math.max(0, Math.min(maxScrollX, scrollX - scrollDeltaY * 20));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDeltaX, scrollDeltaY);
    }

    private void updateScrollFromMouse(double mouseX) {
        int trackWidth = viewRight - viewLeft;
        double ratio = (mouseX - viewLeft) / trackWidth;
        scrollX = Math.max(0, Math.min(maxScrollX, ratio * maxScrollX));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
