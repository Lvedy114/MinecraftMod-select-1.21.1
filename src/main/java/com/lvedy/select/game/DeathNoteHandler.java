package com.lvedy.select.game;

import com.lvedy.select.special.items.DeathNoteItem;
import com.lvedy.select.register.ModDamageTypes;
import com.lvedy.select.register.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Set;

/**
 * 死亡笔记结算逻辑：玩家写完并合上书后，服务端在此查找与所写名字相同的在线玩家，
 * 将其以「心脏麻痹」击杀（死亡信息为「XXX死于心脏麻痹」），随后对持有者施加 5 分钟冷却。
 */
public class DeathNoteHandler {

    public static void handleWrite(ServerPlayer writer, String text, boolean mainHand) {
        // 始终施加冷却：只要合上了书本，无论是否命中
        InteractionHand hand = mainHand ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        ItemStack held = writer.getItemInHand(hand);
        if (held.getItem() instanceof DeathNoteItem note) {
            writer.getCooldowns().addCooldown(note, DeathNoteItem.COOLDOWN_TICKS);
        } else {
            // 兜底：确保物品仍存在时也加冷却
            writer.getCooldowns().addCooldown(ModItems.DEATH_NOTE.get(), DeathNoteItem.COOLDOWN_TICKS);
        }

        if (text == null || text.isBlank()) return;

        MinecraftServer server = writer.server;

        // 收集书中每一行的非空名字（去重）
        Set<String> names = new HashSet<>();
        for (String line : text.split("\r?\n")) {
            String name = line.trim();
            if (!name.isEmpty()) names.add(name);
        }

        for (String name : names) {
            ServerPlayer target = server.getPlayerList().getPlayerByName(name);
            if (target == null || !target.isAlive()) continue;
            ServerLevel level = (ServerLevel) target.level();
            target.hurt(ModDamageTypes.heartFailure(level), Float.MAX_VALUE);
        }

        writer.displayClientMessage(Component.literal("死亡笔记已合上……"), true);
    }

    private DeathNoteHandler() {}
}
