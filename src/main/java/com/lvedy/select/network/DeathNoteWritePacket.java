package com.lvedy.select.network;

import com.lvedy.select.Select;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * 客户端 → 服务端：提交死亡笔记中写下的全部文本（每行一个名字）。
 * 服务端据此查找同名在线玩家并以「心脏麻痹」将其击杀，并对持有者施加冷却。
 */
public record DeathNoteWritePacket(String text, boolean mainHand) implements CustomPacketPayload {

    public static final Type<DeathNoteWritePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Select.MODID, "death_note_write"));

    public static final StreamCodec<ByteBuf, DeathNoteWritePacket> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8, DeathNoteWritePacket::text,
                    ByteBufCodecs.BOOL, DeathNoteWritePacket::mainHand,
                    DeathNoteWritePacket::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
