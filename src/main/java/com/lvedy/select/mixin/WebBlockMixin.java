package com.lvedy.select.mixin;

import com.lvedy.select.client.ClientWebHelper;
import com.lvedy.select.game.GameManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WebBlock.class)
public abstract class WebBlockMixin {

    @Inject(method = "entityInside", at = @At("HEAD"), cancellable = true)
    private void buff20SkipCobweb(BlockState state, Level level, BlockPos pos, Entity entity, CallbackInfo ci) {
        if (!(entity instanceof Player player)) return;

        if (level.isClientSide) {
            // 客户端：玩家移动由本地预测主导，必须在这里取消减速
            if (ClientWebHelper.shouldSkipCobweb(player)) {
                ci.cancel();
            }
        } else if (player instanceof ServerPlayer serverPlayer && GameManager.hasEffect(serverPlayer.getUUID(), "buff20")) {
            ci.cancel();
        }
    }
}
