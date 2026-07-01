package com.lvedy.select.special.select;

import net.minecraft.server.level.ServerPlayer;

/**
 * 选项效果接口。每个 buff / debuff 实现此接口，
 * 在玩家选择该选项后由服务端调用 {@link #apply(ServerPlayer)} 执行效果。
 */
public interface SelectEffect {

    /** 效果的唯一标识，用于网络传输与日志 */
    String getId();

    /** 效果的显示名称 */
    String getDisplayName();

    /** 效果的详细描述，显示在选项文本框与效果查看界面中 */
    String getDescription();

    /** 是否为增益效果（true = buff，false = debuff） */
    boolean isBuff();

    /** 是否在效果查看界面中显示。为 false 时玩家拥有它但不会被列出 */
    boolean isVisible();

    /** 玩家选择该效果后执行的逻辑（在服务端运行） */
    void apply(ServerPlayer player);

    /** clear 时调用，撤销 apply 对玩家的修改（默认无操作） */
    default void remove(ServerPlayer player) {}

    /**
     * 玩家死亡重生后调用，用于恢复那些绑定在实体上、重生后会丢失的状态
     * （主要是属性修饰符）。默认无操作——一次性效果（发放物品、传送等）
     * 不应在重生时重复执行。
     */
    default void reapply(ServerPlayer player) {}

}
