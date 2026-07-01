package com.lvedy.select.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.lvedy.select.Select;

/**
 * 客户端 → 服务端：请求同步当前已拥有的效果列表。
 */
public record RequestOwnedEffectsPacket() implements CustomPacketPayload {

    public static final Type<RequestOwnedEffectsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Select.MODID, "request_owned_effects"));

    public static final StreamCodec<ByteBuf, RequestOwnedEffectsPacket> STREAM_CODEC =
            StreamCodec.unit(new RequestOwnedEffectsPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
