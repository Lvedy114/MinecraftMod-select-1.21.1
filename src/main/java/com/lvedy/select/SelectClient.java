package com.lvedy.select;

import com.lvedy.select.client.ClientEffectData;
import com.lvedy.select.client.ClientMusicManager;
import com.lvedy.select.client.gui.GiveEffectsScreen;
import com.lvedy.select.client.gui.OwnedEffectsScreen;
import com.lvedy.select.client.gui.SelectionScreen;
import com.lvedy.select.event.ModEvents;
import com.lvedy.select.client.ClientSpinManager;
import com.lvedy.select.network.CloseSelectionPacket;
import com.lvedy.select.network.OpenGiveScreenPacket;
import com.lvedy.select.network.OpenSelectionPacket;
import com.lvedy.select.network.ReplaceMusicPacket;
import com.lvedy.select.network.RequestOwnedEffectsPacket;
import com.lvedy.select.network.SpinCameraPacket;
import com.lvedy.select.network.SyncOwnedEffectsPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = Select.MODID, value = Dist.CLIENT)
public class SelectClient {

    public SelectClient(ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    @SubscribeEvent
    static void onClientSetup(FMLClientSetupEvent event) {
        Select.LOGGER.info("Select mod: client setup");
    }

    @SubscribeEvent
    static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(Select.MODID);

        // S→C：打开选项 GUI
        registrar.playToClient(
                OpenSelectionPacket.TYPE,
                OpenSelectionPacket.STREAM_CODEC,
                (packet, ctx) -> ctx.enqueueWork(() ->
                        Minecraft.getInstance().setScreen(new SelectionScreen(
                                packet.buffAId(), packet.debuffAId(),
                                packet.buffBId(), packet.debuffBId(),
                                packet.timeLimitSeconds()))
                )
        );

        // S→C：同步已拥有效果
        registrar.playToClient(
                SyncOwnedEffectsPacket.TYPE,
                SyncOwnedEffectsPacket.STREAM_CODEC,
                (packet, ctx) -> ctx.enqueueWork(() -> ClientEffectData.set(packet.effectIds()))
        );

        // S→C：关闭选择界面（超时随机结算后由服务端触发）
        registrar.playToClient(
                CloseSelectionPacket.TYPE,
                CloseSelectionPacket.STREAM_CODEC,
                (packet, ctx) -> ctx.enqueueWork(() -> {
                    Minecraft mc = Minecraft.getInstance();
                    if (mc.screen instanceof SelectionScreen) {
                        mc.setScreen(null);
                    }
                })
        );

        // S→C：打开给予效果测试界面（/select give 触发）
        registrar.playToClient(
                OpenGiveScreenPacket.TYPE,
                OpenGiveScreenPacket.STREAM_CODEC,
                (packet, ctx) -> ctx.enqueueWork(() ->
                        Minecraft.getInstance().setScreen(new GiveEffectsScreen()))
        );

        // S→C：debuff26 替换背景音乐
        registrar.playToClient(
                ReplaceMusicPacket.TYPE,
                ReplaceMusicPacket.STREAM_CODEC,
                (packet, ctx) -> ctx.enqueueWork(() ->
                        ClientMusicManager.replace(packet.soundId(), packet.durationTicks()))
        );

        // S→C：debuff26 BGM 触发时切换第三人称旋转一圈
        registrar.playToClient(
                SpinCameraPacket.TYPE,
                SpinCameraPacket.STREAM_CODEC,
                (packet, ctx) -> ctx.enqueueWork(ClientSpinManager::startSpin)
        );
    }

    /** 在背包界面左侧添加「效果一览」文本框，点击进入效果查看界面 */
    @SubscribeEvent
    static void onInventoryInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof InventoryScreen invScreen)) return;

        String label = "效果一览";
        net.minecraft.client.gui.Font font = Minecraft.getInstance().font;
        int w = font.width(label) + 8;
        int h = font.lineHeight + 6;
        int x = invScreen.getGuiLeft() - w - 2;
        int y = invScreen.getGuiTop();

        event.addListener(new AbstractWidget(x, y, w, h, Component.empty()) {
            @Override
            protected void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                int bg = isHovered ? 0xCC333333 : 0xCC000000;
                g.fill(getX(), getY(), getX() + width, getY() + height, bg);
                float hue = (System.currentTimeMillis() % 3000) / 3000.0f;
                int rainbow = 0xFF000000 | (java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f) & 0x00FFFFFF);
                g.renderOutline(getX(), getY(), width, height, rainbow);
                g.drawCenteredString(font, label, getX() + width / 2, getY() + (height - font.lineHeight) / 2, 0xFFFFFFFF);
            }

            @Override
            public void updateWidgetNarration(NarrationElementOutput n) {
                defaultButtonNarrationText(n);
            }

            @Override
            public void onClick(double mx, double my) {
                PacketDistributor.sendToServer(new RequestOwnedEffectsPacket());
                Minecraft.getInstance().setScreen(new OwnedEffectsScreen());
            }
        });
    }
}
