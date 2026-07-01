package com.lvedy.select.special.items;

import com.lvedy.select.client.ClientDeathNoteHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * 死亡笔记：右键打开书写界面（仅客户端）。玩家写下名字并关闭书本后，
 * 客户端会把内容发送给服务端，服务端查找同名在线玩家并将其以「心脏麻痹」击杀，
 * 随后该物品进入 5 分钟冷却。冷却由服务端在收到书写内容后施加。
 */
public class DeathNoteItem extends Item {

    public static final int COOLDOWN_TICKS = 20 * 60 * 5; // 5 分钟

    public DeathNoteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // 冷却中无法打开
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.fail(stack);
        }

        if (level.isClientSide && FMLEnvironment.dist == Dist.CLIENT) {
            ClientDeathNoteHelper.openScreen(hand);
        }
        return InteractionResultHolder.success(stack);
    }
}
