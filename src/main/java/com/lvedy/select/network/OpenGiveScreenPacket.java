package com.lvedy.select.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.lvedy.select.Select;

/**
 * 服务端 → 客户端：要求客户端打开「给予效果」测试界面（由 /select give 触发）。
 */
public record OpenGiveScreenPacket() implements CustomPacketPayload {

    public static final Type<OpenGiveScreenPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Select.MODID, "open_give_screen"));

    public static final StreamCodec<ByteBuf, OpenGiveScreenPacket> STREAM_CODEC =
            StreamCodec.unit(new OpenGiveScreenPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
