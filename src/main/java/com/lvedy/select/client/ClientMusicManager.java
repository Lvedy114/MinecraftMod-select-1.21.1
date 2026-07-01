package com.lvedy.select.client;

import com.lvedy.select.Select;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * debuff26 客户端背景乐替换器。
 * 收到 {@link com.lvedy.select.network.ReplaceMusicPacket} 后，循环播放指定 music 音效，
 * 并在持续期内每 tick 压制原版背景音乐；倒计时结束后停止替换乐，原版音乐自然恢复。
 */
@EventBusSubscriber(modid = Select.MODID, value = Dist.CLIENT)
public class ClientMusicManager {

    private static LoopingMusicInstance current;
    private static int remainingTicks;

    /** 由网络层在客户端线程调用：开始替换背景乐。 */
    public static void replace(String soundId, int durationTicks) {
        Minecraft mc = Minecraft.getInstance();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Select.MODID, soundId);
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(id);
        if (event == null) return;

        stop();
        // 压制当前正在播放的原版背景音乐
        mc.getMusicManager().stopPlaying();
        current = new LoopingMusicInstance(event);
        remainingTicks = durationTicks;
        mc.getSoundManager().play(current);
    }

    private static void stop() {
        if (current != null) {
            Minecraft.getInstance().getSoundManager().stop(current);
            current = null;
        }
        remainingTicks = 0;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (current == null) return;
        Minecraft mc = Minecraft.getInstance();
        // 玩家退出存档时清理
        if (mc.level == null || mc.player == null) {
            stop();
            return;
        }
        // 持续压制原版背景乐，避免它在间隙抢回播放
        mc.getMusicManager().stopPlaying();
        // 替换乐播放完一遍后若仍在持续期，重新播放以实现循环
        if (!mc.getSoundManager().isActive(current)) {
            mc.getSoundManager().play(current);
        }
        if (--remainingTicks <= 0) {
            stop();
        }
    }

    /** 相对听者、无衰减的循环背景乐实例。 */
    private static class LoopingMusicInstance extends AbstractTickableSoundInstance {
        LoopingMusicInstance(SoundEvent event) {
            super(event, SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.looping = true;
            this.delay = 0;
            this.volume = 1.0F;
            this.relative = true;
        }

        @Override
        public void tick() {
            // 循环与时长由 ClientMusicManager 统一管理，无需逐 tick 处理
        }
    }
}
