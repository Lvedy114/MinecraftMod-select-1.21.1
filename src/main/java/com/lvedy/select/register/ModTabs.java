package com.lvedy.select.register;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import static com.lvedy.select.Select.MODID;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> SELECT_TAB = TABS.register(
            "select_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.select"))
                    .icon(() -> new ItemStack(ModItems.WEATHER_CONTROLLER.get()))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.WEATHER_CONTROLLER.get());
                        output.accept(ModItems.MAMBA.get());
                        output.accept(ModItems.DEATH_NOTE.get());
                        output.accept(ModItems.CHOCOLITE.get());
                        output.accept(ModItems.SPRITE.get());
                    })
                    .build()
    );

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
