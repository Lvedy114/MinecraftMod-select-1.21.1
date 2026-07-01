package com.lvedy.select.special.items;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 给特殊功能物品打上 / 读取自定义 NBT 标记。
 * 标记存放在 {@link DataComponents#CUSTOM_DATA} 的同一个键下，值用于区分类型，
 * 例如 AK47 = "ak"、可投掷 TNT = "tnt"。
 */
public final class SpecialItemTags {

    /** 存放标记的 NBT 键 */
    public static final String KEY = "select_special";

    /** AK47（占位木棍）的标记值 */
    public static final String AK47 = "ak";
    /** 可投掷 TNT 的标记值 */
    public static final String TNT = "tnt";

    private SpecialItemTags() {}

    /** 给物品打上指定标记（覆盖已有的 select_special 键） */
    public static void mark(ItemStack stack, String value) {
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag tag = existing != null ? existing.copyTag() : new CompoundTag();
        tag.putString(KEY, value);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    /** 标记为 AK47 */
    public static void markAk(ItemStack stack) {
        mark(stack, AK47);
    }

    /** 标记为可投掷 TNT */
    public static void markTnt(ItemStack stack) {
        mark(stack, TNT);
    }

    /** 判断物品是否带有指定标记 */
    public static boolean is(ItemStack stack, String value) {
        if (stack.isEmpty()) return false;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && value.equals(data.copyTag().getString(KEY));
    }

    /** 是否为 AK47 */
    public static boolean isAk(ItemStack stack) {
        return is(stack, AK47);
    }

    /** 是否为可投掷 TNT */
    public static boolean isTnt(ItemStack stack) {
        return is(stack, TNT);
    }
}
