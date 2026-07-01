package com.lvedy.select.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.lvedy.select.Select;

import java.util.List;

/**
 * 客户端 → 服务端：请求把指定的一批效果给予自己（用于 /select give 测试界面）。
 */
public record GiveEffectPacket(List<String> effectIds) implements CustomPacketPayload {

    public static final Type<GiveEffectPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Select.MODID, "give_effect"));

    public static final StreamCodec<ByteBuf, GiveEffectPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    GiveEffectPacket::effectIds,
                    GiveEffectPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
