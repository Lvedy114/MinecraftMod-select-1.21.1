package com.lvedy.select.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import com.lvedy.select.Select;

/** 服务端 → 客户端：debuff26 BGM 触发时，通知客户端进入第三人称并旋转一圈。 */
public record SpinCameraPacket() implements CustomPacketPayload {
    public static final Type<SpinCameraPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Select.MODID, "spin_camera"));
    public static final StreamCodec<ByteBuf, SpinCameraPacket> STREAM_CODEC =
            StreamCodec.unit(new SpinCameraPacket());
    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
