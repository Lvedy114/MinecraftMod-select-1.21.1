package com.lvedy.select.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class PlayerRendererMixin {

    @Unique
    private static final ResourceLocation BUFF13_ID = ResourceLocation.fromNamespaceAndPath("select", "buff13_scale");
    @Unique
    private static final ResourceLocation BUFF27_ID = ResourceLocation.fromNamespaceAndPath("select", "buff27_scale");
    @Unique
    private static final ResourceLocation BUFF35_ID = ResourceLocation.fromNamespaceAndPath("select", "buff35_scale");


    // 渲染前：推入 X/Z 2× 补偿（抵消 SCALE=0.5 在宽度方向的缩小）
    @Inject(method = "render", at = @At("HEAD"))
    private void buff13Push(LivingEntity entity, float entityYaw, float partialTick,
                            PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer)) return;
        var attr = entity.getAttribute(Attributes.SCALE);
        if (attr != null && attr.hasModifier(BUFF13_ID)) {
            poseStack.pushPose();
            poseStack.scale(1.333f, 1.0f, 1.333f);
        }
        if (attr != null && attr.hasModifier(BUFF27_ID)) {
            poseStack.pushPose();
            poseStack.scale(0.666f, 1.0f, 0.666f);
        }
        if (attr != null && attr.hasModifier(BUFF35_ID)) {
            poseStack.pushPose();
            poseStack.scale(1.3f, 1.0f, 1.3f);
        }
    }

    // 渲染后：弹出补偿层
    @Inject(method = "render", at = @At("RETURN"))
    private void buff13Pop(LivingEntity entity, float entityYaw, float partialTick,
                           PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (!(entity instanceof AbstractClientPlayer)) return;
        var attr = entity.getAttribute(Attributes.SCALE);
        if (attr != null && attr.hasModifier(BUFF13_ID)) {
            poseStack.popPose();
        }
        if (attr != null && attr.hasModifier(BUFF27_ID)) {
            poseStack.popPose();
        }
        if (attr != null && attr.hasModifier(BUFF35_ID)) {
            poseStack.popPose();
        }
    }
}
