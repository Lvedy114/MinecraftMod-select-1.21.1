package com.lvedy.select.client.gui;

import com.lvedy.select.network.SelectionConfirmPacket;
import com.lvedy.select.special.select.SelectEffect;
import com.lvedy.select.special.select.SelectEffects;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

public class SelectionScreen extends Screen {

    private static final int OPTION_WIDTH = 150;
    private static final int TEXT_BOX_HEIGHT = 56;
    private static final int OPTION_GAP = 24;   // 两组选项之间的横向间隔
    private static final int GROUP_HEIGHT = TEXT_BOX_HEIGHT * 2; // 一组 = 上下两个紧贴的文本框

    private final SelectEffect buffA, debuffA, buffB, debuffB;

    private int selectedIndex = -1;
    private Button confirmButton;

    // 倒计时（客户端展示用，单位 tick），到 0 时由服务端结算并关闭界面
    private int remainingTicks;
    // 倒计时总时长（tick），用于计算进度条比例
    private final int totalTimeTicks;

    // 两组的点击区域（覆盖上下两个文本框），index 0 = A 组，1 = B 组
    private int groupTop;
    private int leftX, rightX;

    public SelectionScreen(String buffAId, String debuffAId, String buffBId, String debuffBId, int timeLimitSeconds) {
        super(Component.literal("做出选择"));
        this.buffA = SelectEffects.byId(buffAId);
        this.debuffA = SelectEffects.byId(debuffAId);
        this.buffB = SelectEffects.byId(buffBId);
        this.debuffB = SelectEffects.byId(debuffBId);
        this.remainingTicks = timeLimitSeconds * 20;
        this.totalTimeTicks = timeLimitSeconds * 20;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;

        int totalWidth = OPTION_WIDTH * 2 + OPTION_GAP;
        leftX = cx - totalWidth / 2;
        rightX = leftX + OPTION_WIDTH + OPTION_GAP;

        // 两组文本框纵向居中
        groupTop = cy - GROUP_HEIGHT / 2;

        // 确认按钮放在两组下方
        confirmButton = Button.builder(Component.literal("确认"), btn -> confirm())
                .pos(cx - 40, groupTop + GROUP_HEIGHT + 16)
                .size(80, 20)
                .build();
        confirmButton.active = false;
        addRenderableWidget(confirmButton);
    }

    private void confirm() {
        if (selectedIndex < 0) return;
        PacketDistributor.sendToServer(new SelectionConfirmPacket(selectedIndex));
        onClose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            // 判断点击是否落在某一组的区域内（上下文本框整体）
            if (inGroup(mouseX, mouseY, leftX)) {
                selectGroup(0);
                return true;
            }
            if (inGroup(mouseX, mouseY, rightX)) {
                selectGroup(1);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean inGroup(double mouseX, double mouseY, int groupX) {
        return mouseX >= groupX && mouseX <= groupX + OPTION_WIDTH
                && mouseY >= groupTop && mouseY <= groupTop + GROUP_HEIGHT;
    }

    private void selectGroup(int index) {
        selectedIndex = index;
        confirmButton.active = true;
    }

    @Override
    public void tick() {
        // 客户端倒计时递减（仅用于展示；超时结算由服务端权威处理并发包关闭界面）
        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // 自绘半透明暗化背景（不调用 super.render，避免父类模糊后处理与菜单背景覆盖文字）
        graphics.fill(0, 0, width, height, 0xC0101010);

        // 手动渲染控件（确认按钮），顺序在背景之后、文字之前
        for (net.minecraft.client.gui.components.Renderable renderable : this.renderables) {
            renderable.render(graphics, mouseX, mouseY, partialTick);
        }

        int cx = width / 2;

        // 标题
        graphics.drawCenteredString(font, "做出选择", cx, groupTop - 35, 0xFFFFFF);

        // A 组：上 buff、下 debuff，两框紧贴
        drawGroup(graphics, leftX, 0);
        // B 组
        drawGroup(graphics, rightX, 1);

        // 倒计时：标题下方的进度条 + 文字
        drawCountdownBar(graphics, cx);
    }

    private void drawCountdownBar(GuiGraphics graphics, int cx) {
        int seconds = (remainingTicks + 19) / 20; // 向上取整到秒
        int totalTicks = Math.max(1, totalTimeTicks);
        float progress = Math.max(0f, Math.min(1f, (float) remainingTicks / totalTicks));

        // 进度条尺寸与位置：标题与选项之间
        int barWidth = OPTION_WIDTH * 2 + OPTION_GAP; // 与两组选项总宽一致
        int barHeight = 6;
        int barX = cx - barWidth / 2;
        int barY = groupTop - 14;

        // 最后 5 秒进度条与文字转红以示紧迫
        boolean urgent = seconds <= 5;
        int fillColor = urgent ? 0xFFFF5555 : 0xFF55FF55;
        int textColor = urgent ? 0xFFFF5555 : 0xFFFFFFFF;

        // 倒计时文字（正常字号，居中于进度条上方）
        String text = seconds + " 秒";
        graphics.drawCenteredString(font, text, cx, barY - font.lineHeight - 2, textColor);

        // 进度条背景
        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, 0xCC000000);
        // 进度条填充
        int fillWidth = (int) (barWidth * progress);
        graphics.fill(barX, barY, barX + fillWidth, barY + barHeight, fillColor);
        // 进度条边框
        graphics.renderOutline(barX, barY, barWidth, barHeight, 0xFFAAAAAA);
    }

    /** 绘制一组（上 buff + 下 debuff，紧贴），选中时整组加高亮边框 */
    private void drawGroup(GuiGraphics graphics, int x, int index) {
        SelectEffect buff = index == 0 ? buffA : buffB;
        SelectEffect debuff = index == 0 ? debuffA : debuffB;

        drawTextBox(graphics, x, groupTop, buff, true);
        drawTextBox(graphics, x, groupTop + TEXT_BOX_HEIGHT, debuff, false);

        // 选中组：在整组外围再描一圈高亮边框
        if (selectedIndex == index) {
            graphics.renderOutline(x - 2, groupTop - 2, OPTION_WIDTH + 4, GROUP_HEIGHT + 4, 0xFFFFFFFF);
            graphics.renderOutline(x - 1, groupTop - 1, OPTION_WIDTH + 2, GROUP_HEIGHT + 2, 0xFFFFFF00);
        }
    }

    /** 绘制一个文本框：buff 用绿色标题，debuff 用红色标题，下方自动换行显示描述 */
    private void drawTextBox(GuiGraphics graphics, int x, int y, SelectEffect effect, boolean isBuff) {
        graphics.fill(x, y, x + OPTION_WIDTH, y + TEXT_BOX_HEIGHT, 0xCC000000);
        graphics.renderOutline(x, y, OPTION_WIDTH, TEXT_BOX_HEIGHT, isBuff ? 0xFF55FF55 : 0xFFFF5555);

        if (effect == null) return;

        int titleColor = isBuff ? 0xFF55FF55 : 0xFFFF5555;
        graphics.drawString(font, effect.getDisplayName(), x + 4, y + 4, titleColor);

        // 描述自动换行
        List<net.minecraft.util.FormattedCharSequence> lines =
                font.split(Component.literal(effect.getDescription()), OPTION_WIDTH - 8);
        int ty = y + 4 + font.lineHeight + 2;
        for (net.minecraft.util.FormattedCharSequence line : lines) {
            if (ty + font.lineHeight > y + TEXT_BOX_HEIGHT) break; // 超出文本框不再绘制
            graphics.drawString(font, line, x + 4, ty, 0xFFDDDDDD);
            ty += font.lineHeight;
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
