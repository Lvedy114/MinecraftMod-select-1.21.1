package com.lvedy.select.client;

import com.lvedy.select.Select;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * debuff26：BGM 触发时切换到第三人称，3s 内旋转一圈，结束后切回第一人称。
 */
@EventBusSubscriber(modid = Select.MODID, value = Dist.CLIENT)
public class ClientSpinManager {

    private static final int SPIN_TICKS = 60; // 3s
    private static int remaining = 0;

    private static void setCameraType(CameraType type) {
        try {
            java.lang.reflect.Field f = net.minecraft.client.Options.class.getDeclaredField("cameraType");
            f.setAccessible(true);
            f.set(Minecraft.getInstance().options, type);
        } catch (ReflectiveOperationException ignored) {}
    }

    /** 由网络层在客户端主线程调用。 */
    public static void startSpin() {
        remaining = SPIN_TICKS;
        setCameraType(CameraType.THIRD_PERSON_BACK);
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (remaining <= 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { remaining = 0; return; }
        mc.player.setYRot(mc.player.getYRot() + 360f / SPIN_TICKS);
        mc.player.yHeadRot = mc.player.getYRot();
        if (--remaining == 0) {
            setCameraType(CameraType.FIRST_PERSON);
        }
    }
}
