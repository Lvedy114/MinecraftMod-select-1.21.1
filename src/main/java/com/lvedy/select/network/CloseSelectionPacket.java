package com.lvedy.select.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.lvedy.select.Select;

/**
 * 服务端 → 客户端：要求关闭选择界面（用于超时由服务端随机结算后，强制收起 GUI）。
 */
public record CloseSelectionPacket() implements CustomPacketPayload {

    public static final Type<CloseSelectionPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Select.MODID, "close_selection"));

    public static final StreamCodec<ByteBuf, CloseSelectionPacket> STREAM_CODEC =
            StreamCodec.unit(new CloseSelectionPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
