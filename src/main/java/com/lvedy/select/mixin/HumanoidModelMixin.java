package com.lvedy.select.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * debuff5：玩家拥有该效果时冻结全部肢体动画。
 * 在 setupAnim 末尾把四肢与躯干的旋转清零（保留头部视角跟随），
 * 这样走路摆臂、攻击挥手、游泳划水、划船划桨等动作都不再呈现。
 * 通过 SCALE 属性上数值为 0 的标记修饰符判断玩家是否拥有 debuff5。
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin {

    @Unique
    private static final ResourceLocation FROZEN_ID =
            ResourceLocation.fromNamespaceAndPath("select", "debuff5_frozen");

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void debuff5FreezeLimbs(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                    float ageInTicks, float netHeadYaw, float headPitch, CallbackInfo ci) {
        var attr = entity.getAttribute(Attributes.SCALE);
        if (attr == null || !attr.hasModifier(FROZEN_ID)) return;

        HumanoidModel<?> model = (HumanoidModel<?>) (Object) this;

        // 躯干保持直立
        model.body.xRot = 0.0f;
        model.body.yRot = 0.0f;
        model.body.zRot = 0.0f;

        // 四肢全部归零（自然下垂的静止姿态）
        model.rightArm.xRot = 0.0f;
        model.rightArm.yRot = 0.0f;
        model.rightArm.zRot = 0.0f;
        model.leftArm.xRot = 0.0f;
        model.leftArm.yRot = 0.0f;
        model.leftArm.zRot = 0.0f;
        model.rightLeg.xRot = 0.0f;
        model.rightLeg.yRot = 0.0f;
        model.rightLeg.zRot = 0.0f;
        model.leftLeg.xRot = 0.0f;
        model.leftLeg.yRot = 0.0f;
        model.leftLeg.zRot = 0.0f;

        // 头部保持与躯干一致，避免歪头/低头残留
        model.hat.copyFrom(model.head);
    }
}
