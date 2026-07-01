package com.lvedy.select.mixin;

import com.lvedy.select.event.ModEvents;
import com.lvedy.select.game.GameManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.world.entity.monster.Creeper.class)
public abstract class CreeperMixin extends LivingEntity {

    @Shadow private int swell;
    @Shadow private int maxSwell;

    protected CreeperMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void buff6Check(CallbackInfo ci) {
        if (swell <= maxSwell - 3) return;
        if (!(level() instanceof ServerLevel)) return;
        double x = getX(), y = getY(), z = getZ();
        boolean hasPlayer = level().getEntitiesOfClass(ServerPlayer.class,
                new AABB(x - 8, y - 8, z - 8, x + 8, y + 8, z + 8))
                .stream().anyMatch(p -> GameManager.hasEffect(p.getUUID(), "buff6"));
        if (!hasPlayer) return;
        discard();
        ModEvents.spawnCreeperFirework(level(), x, y, z);
    }
}
