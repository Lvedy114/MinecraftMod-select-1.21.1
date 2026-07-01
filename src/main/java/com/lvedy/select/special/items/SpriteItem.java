package com.lvedy.select.special.items;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 雪碧：可饮用，提供 4 饱食度、4 饱和度。
 * 若玩家饮用时身上带有「速度 V」（amplifier ≥ 4）效果，则身上冒出紫色粒子，
 * 并在 10 秒后因「心脏麻痹」死亡（由 {@link com.lvedy.select.game.HeartFailureScheduler} 调度）。
 */
public class SpriteItem extends Item {

    private static final int DEATH_DELAY_TICKS = 20 * 10; // 10 秒

    public SpriteItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            MobEffectInstance speed = player.getEffect(MobEffects.MOVEMENT_SPEED);
            if (speed != null && speed.getAmplifier() >= 4) {
                com.lvedy.select.game.HeartFailureScheduler.schedule(player, DEATH_DELAY_TICKS);
            }
        }
        return super.finishUsingItem(stack, level, entity);
    }

    /** 在持续致死期间，每 tick 在玩家身上喷射紫色粒子（由调度器调用） */
    public static void emitPurpleParticles(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.WITCH,
                player.getX(), player.getY() + player.getBbHeight() * 0.6, player.getZ(),
                6, 0.3, 0.5, 0.3, 0.02);
    }
}
