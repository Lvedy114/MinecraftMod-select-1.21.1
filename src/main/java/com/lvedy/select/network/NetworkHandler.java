package com.lvedy.select.network;

import com.lvedy.select.Select;
import com.lvedy.select.game.GameManager;
import com.lvedy.select.special.select.SelectEffect;
import com.lvedy.select.special.select.SelectOption;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;

public class NetworkHandler {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(NetworkHandler::onRegisterPayloads);
    }

    /** 注册 C→S 包。S→C 包在客户端专属的 SelectClient 中注册。 */
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Select.MODID);

        registrar.playToServer(
                SelectionConfirmPacket.TYPE,
                SelectionConfirmPacket.STREAM_CODEC,
                (packet, ctx) -> ctx.enqueueWork(() -> {
                    ServerPlayer player = (ServerPlayer) ctx.player();
                    Select.LOGGER.info("Player {} selected option {}",
                            player.getName().getString(), packet.selectedIndex());
                    GameManager.resolveSelection(player, packet.selectedIndex());
                })
        );

        // 客户端 → 服务端：请求同步已拥有效果（打开效果查看界面时调用）
        registrar.playToServer(
                RequestOwnedEffectsPacket.TYPE,
                RequestOwnedEffectsPacket.STREAM_CODEC,
                (packet, ctx) -> ctx.enqueueWork(() -> {
                    ServerPlayer player = (ServerPlayer) ctx.player();
                    sendOwnedEffects(player, GameManager.getOwnedEffects(player.getUUID()));
                })
        );

        // 客户端 → 服务端：给予指定效果（/select give 测试界面点击卡片）
        registrar.playToServer(
                GiveEffectPacket.TYPE,
                GiveEffectPacket.STREAM_CODEC,
                (packet, ctx) -> ctx.enqueueWork(() -> {
                    ServerPlayer player = (ServerPlayer) ctx.player();
                    for (String id : packet.effectIds()) {
                        GameManager.giveEffect(player, id);
                    }
                })
        );

        // 客户端 → 服务端：提交死亡笔记书写内容
        registrar.playToServer(
                DeathNoteWritePacket.TYPE,
                DeathNoteWritePacket.STREAM_CODEC,
                (packet, ctx) -> ctx.enqueueWork(() -> {
                    ServerPlayer player = (ServerPlayer) ctx.player();
                    com.lvedy.select.game.DeathNoteHandler.handleWrite(player, packet.text(), packet.mainHand());
                })
        );
    }

    public static void sendOpenSelection(ServerPlayer player, SelectOption[] options, int timeLimitSeconds) {
        OpenSelectionPacket packet = new OpenSelectionPacket(
                options[0].buff().getId(), options[0].debuff().getId(),
                options[1].buff().getId(), options[1].debuff().getId(),
                timeLimitSeconds
        );
        PacketDistributor.sendToPlayer(player, packet);
    }

    /** 要求客户端关闭选择界面（超时随机结算后调用） */
    public static void sendCloseSelection(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new CloseSelectionPacket());
    }

    /** 要求客户端打开「给予效果」测试界面（/select give 调用） */
    public static void sendOpenGiveScreen(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new OpenGiveScreenPacket());
    }

    /** 把玩家已拥有的「可见」效果 ID 同步给客户端 */
    public static void sendOwnedEffects(ServerPlayer player, List<SelectEffect> owned) {
        List<String> visibleIds = new ArrayList<>();
        for (SelectEffect effect : owned) {
            if (effect.isVisible()) {
                visibleIds.add(effect.getId());
            }
        }
        PacketDistributor.sendToPlayer(player, new SyncOwnedEffectsPacket(visibleIds));
    }
}
