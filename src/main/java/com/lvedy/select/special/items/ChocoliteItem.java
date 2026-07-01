package com.lvedy.select.special.items;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 巧乐兹：可食用，吃下后给予玩家「速度 V」效果（等级 4），并提供 10 饱食度、10 饱和度。
 * 食物属性在 {@link com.lvedy.select.register.ModItems} 中通过 FoodProperties 配置。
 */
public class ChocoliteItem extends Item {

    private static final int SPEED5_DURATION = 20 * 30; // 速度 V 持续 30 秒

    public ChocoliteItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide) {
            // 速度 V = amplifier 4
            entity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, SPEED5_DURATION, 4));
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
