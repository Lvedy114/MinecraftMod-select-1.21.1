package com.lvedy.select.register;

import com.lvedy.select.Select;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;

/**
 * 自定义伤害类型。具体的伤害类型定义在数据文件
 * {@code data/select/damage_type/heart_failure.json} 中，此处仅持有其 ResourceKey
 * 并提供构造 DamageSource 的工具方法。
 */
public class ModDamageTypes {

    /** 心脏麻痹：死亡笔记与雪碧共用的致死伤害，死亡信息为「XXX死于心脏麻痹」 */
    public static final ResourceKey<DamageType> HEART_FAILURE =
            ResourceKey.create(Registries.DAMAGE_TYPE, Select.prefix("heart_failure"));

    /** 构造一个「心脏麻痹」伤害源（无攻击者） */
    public static DamageSource heartFailure(ServerLevel level) {
        return new DamageSource(
                level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(HEART_FAILURE)
        );
    }

    private ModDamageTypes() {}
}
