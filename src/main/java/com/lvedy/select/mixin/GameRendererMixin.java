package com.lvedy.select.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;

/**
 * debuff9「阳光让你感到刺眼」：当本地玩家拥有该效果，且处于
 * 「主世界 + 白天 + 头顶能看到天空（无方块遮挡）」时，对整个画面叠加一层
 * 自定义散光后处理（assets/select/shaders/post/sunlight_glare.json），
 * 表现为被阳光晃得睁不开眼的放射状散光 + 过曝眩光。
 *
 * 自持一个独立的 PostChain，在每帧渲染完世界之后（render TAIL）调用 process 叠加。
 * 散光强度由 {@link #SELECT$BASE_INTENSITY} 控制，并在着色器内随时间浮动产生晃眼感；
 * 抬头朝天时强度更高、低头看地时减弱（见 select$computeIntensity）。
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Unique
    private static final ResourceLocation SELECT$SUNLIGHT_ID =
            ResourceLocation.fromNamespaceAndPath("select", "debuff9_sunlight");

    /** 自定义散光后处理链路径 */
    @Unique
    private static final ResourceLocation SELECT$GLARE_LOCATION =
            ResourceLocation.fromNamespaceAndPath("select", "shaders/post/sunlight_glare.json");

    /**
     * 散光基础强度（"Intensity" uniform 的基准值）。越大越刺眼。
     * 实际强度 = 基础值 × 抬头看天系数，并在着色器内再叠加时间闪烁。
     */
    @Unique
    private static final float SELECT$BASE_INTENSITY = 1.0F;

    @Unique
    private PostChain select$sunlightGlare;
    @Unique
    private int select$glareWidth = -1;
    @Unique
    private int select$glareHeight = -1;

    @Inject(method = "render(Lnet/minecraft/client/DeltaTracker;Z)V", at = @At("TAIL"))
    private void select$applySunlightGlare(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        if (!renderLevel) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.getCameraEntity() instanceof LocalPlayer player)) return;

        // 仅本地玩家拥有 debuff9 标记时生效
        var attr = player.getAttribute(Attributes.SCALE);
        if (attr == null || !attr.hasModifier(SELECT$SUNLIGHT_ID)) return;

        // 触发条件：主世界 + 太阳在地平线之上（真正的白天）+ 头顶能看到天空（无方块遮挡）
        if (mc.level.dimension() != Level.OVERWORLD) return;
        if (select$sunHeight(mc.level) <= 0.0F) return;
        if (!mc.level.canSeeSky(player.blockPosition().above())) return;

        select$ensureGlare(mc);
        if (select$sunlightGlare == null) return;

        float intensity = select$computeIntensity(player) * select$sunHeight(mc.level);
        // 以游戏运行秒数驱动着色器内的闪烁/流动
        float seconds = (float) (System.nanoTime() / 1.0E9D);

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();
        RenderSystem.resetTextureMatrix();
        select$sunlightGlare.setUniform("Seconds", seconds);
        select$sunlightGlare.setUniform("Intensity", intensity);
        select$sunlightGlare.process(deltaTracker.getGameTimeDeltaTicks());

        // 回写主渲染目标，保证后续 GUI 等正常绘制到屏幕
        mc.getMainRenderTarget().bindWrite(true);
    }

    /**
     * 太阳在地平线之上的高度系数，范围 [0, 1]。
     * 用太阳实际角度判断白天，比 Level.isDay()（依赖 skyDarken 阈值）更可靠，
     * 夜晚返回 0、正午返回约 1，日出日落附近平滑过渡到 0，避免夜晚误触发散光。
     */
    @Unique
    private float select$sunHeight(net.minecraft.world.level.Level level) {
        // getTimeOfDay：0=正午附近的相位起点，正午时 cos≈1、午夜时 cos≈-1
        float timeOfDay = level.getTimeOfDay(1.0F);
        float cos = net.minecraft.util.Mth.cos(timeOfDay * ((float) Math.PI * 2F));
        // cos>0 表示太阳在地平线之上；夜晚 cos<=0 直接归零
        return Math.max(0.0F, cos);
    }

    /**
     * 计算散光强度：抬头朝天空（pitch 越小/越负）时阳光越刺眼，低头看地时减弱。
     * pitch 范围约 [-90, 90]，-90 为正上方、+90 为正下方。
     */
    @Unique
    private float select$computeIntensity(LocalPlayer player) {
        float pitch = player.getXRot();
        // 把 pitch 从 [-90, 90] 映射到看天系数 [1.0, 0.35]
        float lookFactor = 1.0F - (pitch + 90.0F) / 180.0F * 0.65F;
        lookFactor = Math.max(0.35F, Math.min(1.0F, lookFactor));
        return SELECT$BASE_INTENSITY * lookFactor * 4;
    }

    /** 懒加载散光后处理链，并在窗口尺寸变化时重建/resize。 */
    @Unique
    private void select$ensureGlare(Minecraft mc) {
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        if (select$sunlightGlare == null) {
            try {
                ResourceManager rm = mc.getResourceManager();
                select$sunlightGlare = new PostChain(
                        mc.getTextureManager(), rm, mc.getMainRenderTarget(), SELECT$GLARE_LOCATION);
                select$sunlightGlare.resize(w, h);
                select$glareWidth = w;
                select$glareHeight = h;
            } catch (IOException e) {
                select$sunlightGlare = null;
            }
        } else if (w != select$glareWidth || h != select$glareHeight) {
            select$sunlightGlare.resize(w, h);
            select$glareWidth = w;
            select$glareHeight = h;
        }
    }
}
