package com.lvedy.select.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * debuff5：玩家拥有该效果时，禁用游泳/滑翔时整个身体的倾斜旋转，使其始终保持直立。
 * PlayerRenderer.setupRotations 会在游泳/鞘翅飞行时让模型前倾平躺，这里在 HEAD
 * 处拦截：若玩家带有 debuff5 标记，则只施加基类（站立）所需的朝向旋转后取消原方法，
 * 跳过那些倾斜。配合 HumanoidModelMixin 清零四肢动画，整体表现为「无任何肢体动作」。
 */
@Mixin(PlayerRenderer.class)
public abstract class PlayerSetupRotationsMixin {

    @Unique
    private static final ResourceLocation FROZEN_ID =
            ResourceLocation.fromNamespaceAndPath("select", "debuff5_frozen");

    @Inject(method = "setupRotations(Lnet/minecraft/client/player/AbstractClientPlayer;Lcom/mojang/blaze3d/vertex/PoseStack;FFFF)V",
            at = @At("HEAD"), cancellable = true)
    private void debuff5KeepUpright(AbstractClientPlayer entity, PoseStack poseStack, float bob,
                                    float yBodyRot, float partialTick, float scale, CallbackInfo ci) {
        var attr = entity.getAttribute(Attributes.SCALE);
        if (attr == null || !attr.hasModifier(FROZEN_ID)) return;

        // 睡觉姿态交给原版处理（躺平朝向由基类完成），不在此拦截。
        if (entity.hasPose(net.minecraft.world.entity.Pose.SLEEPING)) return;

        // 复刻 LivingEntityRenderer.setupRotations 的「站立」朝向旋转，跳过游泳/滑翔倾斜。
        // 死亡倒地动画保留（避免死亡时僵直站立显得突兀）。
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yBodyRot));
        if (entity.deathTime > 0) {
            float f = ((float) entity.deathTime + partialTick - 1.0F) / 20.0F * 1.6F;
            f = Mth.sqrt(f);
            if (f > 1.0F) f = 1.0F;
            poseStack.mulPose(Axis.ZP.rotationDegrees(f * 90.0F));
        }
        ci.cancel();
    }
}
