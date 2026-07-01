package com.lvedy.select.special.items;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class WeatherControllerItem extends Item {

    private static final int COOLDOWN_TICKS = 20 * 60; // 1 分钟
    private static final int DURATION_TICKS = 6000;    // 切换后维持 5 分钟

    public WeatherControllerItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide || !(level instanceof ServerLevel serverLevel)) {
            return InteractionResultHolder.success(stack);
        }
        String message;
        if (serverLevel.isThundering()) {
            serverLevel.setWeatherParameters(DURATION_TICKS, 0, false, false);
            message = "我会一直生产阳光";
        } else if (serverLevel.isRaining()) {
            serverLevel.setWeatherParameters(0, DURATION_TICKS, true, true);
            message = "HIM降临";
        } else {
            serverLevel.setWeatherParameters(0, DURATION_TICKS, true, false);
            message = "天空开始变得湿润";
        }
        serverLevel.getServer().getPlayerList().broadcastSystemMessage(
                Component.literal(message),
                false
        );
        level.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 1.0f, 1.0f);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);

        return InteractionResultHolder.success(stack);
    }
}
