package com.lvedy.select.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * 客户端侧的蜘蛛网判定。
 * 单独成类，确保引用到的客户端专属类（Minecraft / LocalPlayer）
 * 不会在专用服务器上被类加载（mixin 只在 level.isClientSide 时调用本类）。
 */
public class ClientWebHelper {

    /** 仅对「本地玩家自己」生效；其他玩家由其各自客户端判定 */
    public static boolean shouldSkipCobweb(Player player) {
        if (player != Minecraft.getInstance().player) return false;
        return ClientEffectData.get().contains("buff20");
    }
}
