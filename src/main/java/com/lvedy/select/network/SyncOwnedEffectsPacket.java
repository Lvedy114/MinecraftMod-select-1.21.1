package com.lvedy.select.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.lvedy.select.Select;

import java.util.List;

/**
 * 服务端 → 客户端：同步玩家当前已拥有的效果 ID 列表（用于效果查看界面）。
 */
public record SyncOwnedEffectsPacket(List<String> effectIds) implements CustomPacketPayload {

    public static final Type<SyncOwnedEffectsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Select.MODID, "sync_owned_effects"));

    public static final StreamCodec<ByteBuf, SyncOwnedEffectsPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    SyncOwnedEffectsPacket::effectIds,
                    SyncOwnedEffectsPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
