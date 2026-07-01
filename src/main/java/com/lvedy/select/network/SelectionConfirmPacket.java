package com.lvedy.select.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.lvedy.select.Select;

/**
 * 客户端 → 服务端：玩家确认了选项
 * selectedIndex: 0 = 选项A，1 = 选项B
 */
public record SelectionConfirmPacket(int selectedIndex) implements CustomPacketPayload {

    public static final Type<SelectionConfirmPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Select.MODID, "selection_confirm"));

    public static final StreamCodec<ByteBuf, SelectionConfirmPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.INT, SelectionConfirmPacket::selectedIndex,
                    SelectionConfirmPacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
