package com.lvedy.select.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.lvedy.select.Select;

/**
 * 服务端 → 客户端：debuff26 触发时通知客户端压制原版背景音乐，并循环播放指定 music 音效。
 * soundId 为要播放的音效注册名（如 "music.faded"）；durationTicks 为压制 / 循环持续的 tick 数。
 */
public record ReplaceMusicPacket(String soundId, int durationTicks) implements CustomPacketPayload {

    public static final Type<ReplaceMusicPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Select.MODID, "replace_music"));

    public static final StreamCodec<ByteBuf, ReplaceMusicPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, ReplaceMusicPacket::soundId,
                    ByteBufCodecs.INT, ReplaceMusicPacket::durationTicks,
                    ReplaceMusicPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
