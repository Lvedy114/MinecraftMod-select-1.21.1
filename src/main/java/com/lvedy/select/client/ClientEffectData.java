package com.lvedy.select.client;

import java.util.ArrayList;
import java.util.List;

/**
 * 客户端侧缓存：保存服务端同步过来的「已拥有的可见效果 ID」列表，
 * 供效果查看界面读取。
 */
public class ClientEffectData {

    private static final List<String> OWNED_EFFECT_IDS = new ArrayList<>();

    public static void set(List<String> ids) {
        OWNED_EFFECT_IDS.clear();
        OWNED_EFFECT_IDS.addAll(ids);
    }

    public static List<String> get() {
        return new ArrayList<>(OWNED_EFFECT_IDS);
    }
}
