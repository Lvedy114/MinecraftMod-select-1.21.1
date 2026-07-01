package com.lvedy.select.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    /** 每次弹出选项的间隔时间，单位：秒，最小 10 秒，默认 300 秒（5 分钟） */
    public static final ModConfigSpec.IntValue SELECTION_INTERVAL = BUILDER
            .comment("How many seconds between each selection prompt shown to the player (default: 300s = 5 minutes, min: 10s)")
            .defineInRange("selectionInterval", 100, 10, Integer.MAX_VALUE);

    /** 每次选择的限制时间，单位：秒，最小 5 秒，默认 30 秒。超时将随机选择一组效果 */
    public static final ModConfigSpec.IntValue SELECTION_TIME_LIMIT = BUILDER
            .comment("How many seconds the player has to make a selection before one is chosen at random (default: 30s, min: 5s)")
            .defineInRange("selectionTimeLimit", 5, 5, Integer.MAX_VALUE);

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** 设置选择间隔（秒）并写入配置文件 */
    public static void setSelectionInterval(int seconds) {
        SELECTION_INTERVAL.set(seconds);
        SELECTION_INTERVAL.save();
    }

    /** 设置选择超时（秒）并写入配置文件 */
    public static void setSelectionTimeLimit(int seconds) {
        SELECTION_TIME_LIMIT.set(seconds);
        SELECTION_TIME_LIMIT.save();
    }

    /**
     * 重新读取配置：清除所有配置值的缓存，使下次读取从已加载的配置中重新获取。
     * 配合 NeoForge 的配置文件自动监视，手动编辑配置文件后调用此方法即可让新值生效。
     */
    public static void reload() {
        SPEC.afterReload();
    }
}
