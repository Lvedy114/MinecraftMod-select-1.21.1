package com.lvedy.select.special.items;

import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/**
 * 曼巴：一把近战武器（剑）。本身为普通武器，
 * Buff28 在给予时会为其附魔「击退 XX」，使其拥有夸张的击退之力（肘击之力）。
 */
public class MambaItem extends SwordItem {

    public MambaItem(Tier tier, Properties properties) {
        super(tier, properties);
    }
}
