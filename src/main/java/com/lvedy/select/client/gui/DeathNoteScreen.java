package com.lvedy.select.client.gui;

import com.lvedy.select.network.DeathNoteWritePacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 死亡笔记书写界面。玩家在多行输入框中写下名字（每行一个）。
 * 只有点击「合上书本」才会把内容发送给服务端进行结算（并使物品进入冷却）；
 * 点击「取消」、按 ESC 或以其他方式关闭界面，都视为直接返回，不使用物品、不进冷却。
 */
public class DeathNoteScreen extends Screen {

    private final InteractionHand hand;
    private MultiLineEditBox editBox;
    private boolean submitted = false;

    public DeathNoteScreen(InteractionHand hand) {
        super(Component.literal("死亡笔记"));
        this.hand = hand;
    }

    @Override
    protected void init() {
        int boxW = Math.min(280, this.width - 40);
        int boxH = this.height - 90;
        int boxX = (this.width - boxW) / 2;
        int boxY = 40;

        this.editBox = new MultiLineEditBox(
                this.font, boxX, boxY, boxW, boxH,
                Component.literal("在此写下名字，每行一个……"),
                Component.literal("死亡笔记")
        );
        this.addRenderableWidget(this.editBox);
        this.setInitialFocus(this.editBox);

        int btnW = 100;
        int gap = 8;
        int totalW = btnW * 2 + gap;
        int startX = (this.width - totalW) / 2;
        int btnY = this.height - 36;

        // 左侧：取消（不使用物品，直接返回，不提交、不进冷却）
        this.addRenderableWidget(Button.builder(
                Component.literal("取消"),
                b -> this.onClose()
        ).bounds(startX, btnY, btnW, 20).build());

        // 右侧：合上书本（提交内容并结算）
        this.addRenderableWidget(Button.builder(
                Component.literal("合上书本"),
                b -> {
                    this.submit();
                    this.onClose();
                }
        ).bounds(startX + btnW + gap, btnY, btnW, 20).build());
    }

    /** 提交书写内容给服务端。仅由「合上书本」按钮调用一次。 */
    private void submit() {
        if (submitted) return;
        submitted = true;
        String text = editBox == null ? "" : editBox.getValue();
        boolean mainHand = hand == InteractionHand.MAIN_HAND;
        PacketDistributor.sendToServer(new DeathNoteWritePacket(text, mainHand));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 24, 0xFFFFFFFF);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
