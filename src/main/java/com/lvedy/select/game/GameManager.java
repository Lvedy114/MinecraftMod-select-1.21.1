package com.lvedy.select.game;

import com.lvedy.select.config.Config;
import com.lvedy.select.network.NetworkHandler;
import com.lvedy.select.special.select.SelectEffect;
import com.lvedy.select.special.select.SelectEffects;
import com.lvedy.select.special.select.SelectOption;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class GameManager {

    private static boolean running = false;
    private static int tickCounter = 0;

    /** 一个玩家正在进行的选择会话：包含选项、剩余时间、进入选择前的游戏模式 */
    private static class PendingSelection {
        final SelectOption[] options;
        int remainingTicks;
        final GameType previousGameMode;

        PendingSelection(SelectOption[] options, int remainingTicks, GameType previousGameMode) {
            this.options = options;
            this.remainingTicks = remainingTicks;
            this.previousGameMode = previousGameMode;
        }
    }

    // 正在等待选择的玩家会话
    private static final Map<UUID, PendingSelection> pending = new HashMap<>();

    // 记录每个玩家已拥有的全部效果（buff + debuff，含不可见的）
    private static final Map<UUID, List<SelectEffect>> ownedEffects = new HashMap<>();

    public static boolean isRunning() {
        return running;
    }

    /** 开始 / 恢复游戏。保留已有的计时与效果，便于暂停后继续。 */
    public static void start() {
        running = true;
    }

    /** 暂停游戏。保留计时进度与玩家已拥有的效果。 */
    public static void stop() {
        running = false;
    }

    /** 清除所有玩家的效果并重置计时。会把空效果列表同步给所有在线玩家。 */
    public static void clear(MinecraftServer server) {
        tickCounter = 0;
        pending.clear();
        // 先快照，再清空（clear 后 hasEffect 返回 false，Mixin 不再介入）
        Map<UUID, List<SelectEffect>> snapshot = new HashMap<>(ownedEffects);
        ownedEffects.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            List<SelectEffect> effects = snapshot.getOrDefault(player.getUUID(), List.of());
            for (SelectEffect e : effects) e.remove(player);
            NetworkHandler.sendOwnedEffects(player, List.of());
        }
    }

    /** 获取玩家已拥有的全部效果（只读副本） */
    public static List<SelectEffect> getOwnedEffects(UUID playerId) {
        return new ArrayList<>(ownedEffects.getOrDefault(playerId, List.of()));
    }

    /** 判断玩家是否拥有指定 ID 的效果 */
    public static boolean hasEffect(UUID playerId, String effectId) {
        for (SelectEffect e : ownedEffects.getOrDefault(playerId, List.of())) {
            if (effectId.equals(e.getId())) return true;
        }
        return false;
    }

    /** 随机移除玩家身上至多 count 个 debuff，并同步客户端 */
    public static void removeRandomDebuffs(ServerPlayer player, int count) {
        removeRandom(player, count, false);
    }

    /** 随机移除玩家身上至多 count 个 buff，并同步客户端 */
    public static void removeRandomBuffs(ServerPlayer player, int count) {
        removeRandom(player, count, true);
    }

    /**
     * 随机移除玩家身上至多 count 个效果。removeBuff 为 true 移除 buff，false 移除 debuff。
     * <p>
     * 效果是单例，一次性效果（visible=false）可被反复获得，owned 列表中会出现同一实例的多个引用。
     * 这里按 id 去重后再随机选取，避免把同一个效果重复移除/重复提示；命中后移除该 id 的全部实例。
     */
    private static void removeRandom(ServerPlayer player, int count, boolean removeBuff) {
        List<SelectEffect> owned = ownedEffects.get(player.getUUID());
        if (owned == null || owned.isEmpty()) return;

        // 按 id 去重，收集符合类型的候选效果
        Map<String, SelectEffect> candidates = new java.util.LinkedHashMap<>();
        for (SelectEffect e : owned) {
            if (e.isBuff() == removeBuff) candidates.putIfAbsent(e.getId(), e);
        }
        if (candidates.isEmpty()) return;

        List<SelectEffect> distinct = new ArrayList<>(candidates.values());
        java.util.Collections.shuffle(distinct);
        int remove = Math.min(count, distinct.size());

        List<SelectEffect> removed = new ArrayList<>();
        for (int i = 0; i < remove; i++) {
            SelectEffect e = distinct.get(i);
            if (!e.isVisible()) continue;
            owned.removeIf(o -> o.getId().equals(e.getId())); // 移除该 id 的全部实例
            e.remove(player); // 撤销该效果对玩家的修改（属性修饰符等），一次性效果默认无操作
            removed.add(e);
        }

        notifyRemoved(player, removed, !removeBuff);
        NetworkHandler.sendOwnedEffects(player, owned);
    }

    /**
     * 向玩家提示本次被移除的效果。removedAreDebuff 为 true 表示移除的是 debuff
     * （对玩家是好事，用绿色提示）；false 表示移除的是 buff（用红色提示）。
     */
    private static void notifyRemoved(ServerPlayer player, List<SelectEffect> removed, boolean removedAreDebuff) {
        if (removed.isEmpty()) return;
        StringBuilder names = new StringBuilder();
        for (int i = 0; i < removed.size(); i++) {
            if (i > 0) names.append("、");
            names.append(removed.get(i).getDescription());
        }
        String prefix = removedAreDebuff ? "移除了负面效果：" : "失去了正面效果：";
        net.minecraft.ChatFormatting color = removedAreDebuff
                ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED;
        player.sendSystemMessage(Component.literal(prefix + names).withStyle(color));
    }

    /**
     * 直接给予玩家指定效果（用于 /select give 测试）：执行其 apply，记录到已拥有列表并同步客户端。
     * @param effectId 效果 ID，无效则忽略
     */
    public static void giveEffect(ServerPlayer player, String effectId) {
        SelectEffect effect = SelectEffects.byId(effectId);
        if (effect == null) return;

        effect.apply(player);
        List<SelectEffect> owned = ownedEffects.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        owned.add(effect);
        NetworkHandler.sendOwnedEffects(player, owned);
    }

    /**
     * 玩家死亡重生后调用：重生会重建实体，绑定在旧实体上的属性修饰符会丢失，
     * 这里对其已拥有的全部效果重新调用 reapply 以恢复。一次性效果（发物品、
     * 传送等）的 reapply 默认无操作，不会重复触发。
     */
    public static void reapplyEffects(ServerPlayer player) {
        List<SelectEffect> owned = ownedEffects.get(player.getUUID());
        if (owned == null || owned.isEmpty()) return;
        for (SelectEffect e : owned) e.reapply(player);
    }

    /**
     * 玩家登出时调用：若其正处于选择中（旁观模式），恢复进入选择前的游戏模式，
     * 避免重连后卡在旁观模式。此时 ServerPlayer 仍然有效，可安全设置游戏模式。
     */
    public static void handlePlayerLogout(ServerPlayer player) {
        PendingSelection session = pending.remove(player.getUUID());
        if (session != null) {
            GameType restore = session.previousGameMode != null ? session.previousGameMode : GameType.SURVIVAL;
            player.setGameMode(restore);
        }
    }

    /**
     * 玩家确认选择后调用：根据所选下标找到对应选项并结算。
     * @param selectedIndex 0 = 选项A，1 = 选项B
     */
    public static void resolveSelection(ServerPlayer player, int selectedIndex) {
        PendingSelection session = pending.get(player.getUUID());
        if (session == null || selectedIndex < 0 || selectedIndex >= session.options.length) {
            return;
        }
        pending.remove(player.getUUID());
        applyChoice(player, session, selectedIndex);
    }

    /** 应用所选选项的效果并恢复游戏模式。注意：调用方需自行将该玩家从 pending 中移除。 */
    private static void applyChoice(ServerPlayer player, PendingSelection session, int selectedIndex) {
        SelectOption chosen = session.options[selectedIndex];
        chosen.buff().apply(player);
        chosen.debuff().apply(player);

        List<SelectEffect> owned = ownedEffects.computeIfAbsent(player.getUUID(), k -> new ArrayList<>());
        owned.add(chosen.buff());
        owned.add(chosen.debuff());
        NetworkHandler.sendOwnedEffects(player, owned);

        // 恢复进入选择前的游戏模式（默认回到生存）
        GameType restore = session.previousGameMode != null ? session.previousGameMode : GameType.SURVIVAL;
        player.setGameMode(restore);
    }

    /** 让玩家进入选择状态：切旁观模式并归零速度 */
    private static void enterSelectionState(ServerPlayer player) {
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true; // 强制同步速度到客户端
        player.setGameMode(GameType.SPECTATOR);
    }

    /** 每 tick 调用一次：处理选择计时，并按间隔触发新一轮选择 */
    public static void tick(MinecraftServer server) {
        if (!running) return;

        // 1) 处理正在进行的选择会话：归零速度、递减计时、超时随机选
        tickPendingSelections(server);

        // 2) 按间隔触发新选择
        tickCounter++;
        int intervalTicks = Config.SELECTION_INTERVAL.getAsInt() * 20;
        if (tickCounter >= intervalTicks) {
            tickCounter = 0;
            startSelectionForAll(server);
        }

        // 3) 在动作栏显示距下次选择的倒计时
        showNextSelectionCountdown(server, intervalTicks);
    }

    /** 向生存模式且未在选择中的玩家，在动作栏显示距下次选择的剩余时间 */
    private static void showNextSelectionCountdown(MinecraftServer server, int intervalTicks) {
        int remainingSeconds = (intervalTicks - tickCounter + 19) / 20; // 向上取整
        if (remainingSeconds < 0) remainingSeconds = 0;

        Component message = Component.literal("下次选择：" + remainingSeconds + " 秒")
                .withStyle(net.minecraft.ChatFormatting.AQUA);

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            // 正在选择中的玩家不显示（其界面已有自己的倒计时）
            if (pending.containsKey(player.getUUID())) continue;
            if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) continue;
            player.displayClientMessage(message, true); // true = 显示在动作栏
        }
    }

    private static void tickPendingSelections(MinecraftServer server) {
        Iterator<Map.Entry<UUID, PendingSelection>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, PendingSelection> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            PendingSelection session = entry.getValue();

            if (player == null) {
                // 玩家离线，丢弃该会话
                it.remove();
                continue;
            }

            // 选择期间持续归零速度，防止旁观漂移
            player.setDeltaMovement(Vec3.ZERO);

            session.remainingTicks--;
            if (session.remainingTicks <= 0) {
                // 超时：随机选择一组，并通知客户端关闭界面
                it.remove();
                int randomIndex = ThreadLocalRandom.current().nextInt(session.options.length);
                applyChoice(player, session, randomIndex);
                NetworkHandler.sendCloseSelection(player);
            }
        }
    }

    private static void startSelectionForAll(MinecraftServer server) {
        int limitTicks = Config.SELECTION_TIME_LIMIT.getAsInt() * 20;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (pending.containsKey(player.getUUID())) continue;

            // 只有生存模式的玩家才需要做出选择
            if (player.gameMode.getGameModeForPlayer() != GameType.SURVIVAL) continue;

            SelectOption[] options = SelectEffects.pickTwoOptions(getOwnedEffects(player.getUUID()));
            pending.put(player.getUUID(), new PendingSelection(options, limitTicks, player.gameMode.getGameModeForPlayer()));
            enterSelectionState(player);
            NetworkHandler.sendOpenSelection(player, options, Config.SELECTION_TIME_LIMIT.getAsInt());
        }
    }
}
