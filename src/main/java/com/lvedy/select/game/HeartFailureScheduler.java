package com.lvedy.select.game;

import com.lvedy.select.special.items.SpriteItem;
import com.lvedy.select.register.ModDamageTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * 心脏麻痹延迟死亡调度器：为玩家登记一个倒计时，期间持续喷射紫色粒子，
 * 倒计时结束后以「心脏麻痹」伤害将其击杀。每服务端 tick 由 ModEvents 驱动。
 */
public class HeartFailureScheduler {

    /** 玩家 UUID → 剩余 tick 数 */
    private static final Map<UUID, Integer> PENDING = new HashMap<>();

    /** 登记一名玩家在 delayTicks 后因心脏麻痹死亡 */
    public static void schedule(ServerPlayer player, int delayTicks) {
        PENDING.put(player.getUUID(), delayTicks);
    }

    public static void tick(MinecraftServer server) {
        if (PENDING.isEmpty()) return;

        Iterator<Map.Entry<UUID, Integer>> it = PENDING.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null || !player.isAlive()) {
                it.remove();
                continue;
            }

            int remaining = entry.getValue() - 1;
            if (remaining <= 0) {
                it.remove();
                ServerLevel level = (ServerLevel) player.level();
                player.hurt(ModDamageTypes.heartFailure(level), Float.MAX_VALUE);
            } else {
                entry.setValue(remaining);
                SpriteItem.emitPurpleParticles(player);
            }
        }
    }

    private HeartFailureScheduler() {}
}
