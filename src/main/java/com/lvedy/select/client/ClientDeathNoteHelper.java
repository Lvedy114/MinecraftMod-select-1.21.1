package com.lvedy.select.client;

import com.lvedy.select.client.gui.DeathNoteScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

/**
 * 客户端侧打开死亡笔记界面的入口。单独成类，避免在专用服务器上加载客户端专属类。
 */
public class ClientDeathNoteHelper {

    public static void openScreen(InteractionHand hand) {
        Minecraft.getInstance().setScreen(new DeathNoteScreen(hand));
    }

    private ClientDeathNoteHelper() {}
}
