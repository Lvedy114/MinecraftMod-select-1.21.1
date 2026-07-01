package com.lvedy.select.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.lvedy.select.Select;

/**
 * 服务端 → 客户端：通知客户端打开选项 GUI，并携带两个选项的效果 ID 与本次选择限时（秒）。
 * 选项 A = (buffAId, debuffAId)，选项 B = (buffBId, debuffBId)。
 */
public record OpenSelectionPacket(String buffAId, String debuffAId,
                                  String buffBId, String debuffBId,
                                  int timeLimitSeconds) implements CustomPacketPayload {

    public static final Type<OpenSelectionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Select.MODID, "open_selection"));

    public static final StreamCodec<ByteBuf, OpenSelectionPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, OpenSelectionPacket::buffAId,
                    ByteBufCodecs.STRING_UTF8, OpenSelectionPacket::debuffAId,
                    ByteBufCodecs.STRING_UTF8, OpenSelectionPacket::buffBId,
                    ByteBufCodecs.STRING_UTF8, OpenSelectionPacket::debuffBId,
                    ByteBufCodecs.INT, OpenSelectionPacket::timeLimitSeconds,
                    OpenSelectionPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
