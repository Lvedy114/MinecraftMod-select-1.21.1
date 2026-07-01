package com.lvedy.select.register;

import com.lvedy.select.special.items.ChocoliteItem;
import com.lvedy.select.special.items.DeathNoteItem;
import com.lvedy.select.special.items.MambaItem;
import com.lvedy.select.special.items.SpriteItem;
import com.lvedy.select.special.items.WeatherControllerItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tiers;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.lvedy.select.Select.MODID;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    // 巧乐兹：10 饱食度（nutrition）、10 饱和度（saturation modifier 取 0.5 → saturation = 2*10*0.5 = 10）
    private static final FoodProperties CHOCOLITE_FOOD = new FoodProperties.Builder()
            .nutrition(10)
            .saturationModifier(0.5f)
            .alwaysEdible()
            .build();

    // 雪碧：4 饱食度、4 饱和度
    private static final FoodProperties SPRITE_FOOD = new FoodProperties.Builder()
            .nutrition(4)
            .saturationModifier(0.5f)
            .alwaysEdible()
            .build();

    // 天气控制器
    public static final DeferredItem<Item> WEATHER_CONTROLLER = ITEMS.register("weather_controller",
            () -> new WeatherControllerItem(new Item.Properties().stacksTo(1)));

    // 曼巴（下界合金层级的剑）
    public static final DeferredItem<Item> MAMBA = ITEMS.register("mamba",
            () -> new MambaItem(Tiers.NETHERITE,
                    new Item.Properties().attributes(SwordItem.createAttributes(Tiers.NETHERITE, 3, -2.4f))));

    // 死亡笔记
    public static final DeferredItem<Item> DEATH_NOTE = ITEMS.register("death_note",
            () -> new DeathNoteItem(new Item.Properties().stacksTo(1)));

    // 巧乐兹
    public static final DeferredItem<Item> CHOCOLITE = ITEMS.register("chocolite",
            () -> new ChocoliteItem(new Item.Properties().food(CHOCOLITE_FOOD)));

    // 雪碧（饮品，喝完返还空瓶可选，这里直接消耗）
    public static final DeferredItem<Item> SPRITE = ITEMS.register("sprite",
            () -> new SpriteItem(new Item.Properties().food(SPRITE_FOOD).stacksTo(16)));

    public static void register(IEventBus bus) {
        ITEMS.register(bus);
    }
}
