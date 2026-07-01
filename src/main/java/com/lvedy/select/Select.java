package com.lvedy.select;

import com.lvedy.select.config.Config;
import com.lvedy.select.network.NetworkHandler;
import com.lvedy.select.register.ModBlocks;
import com.lvedy.select.register.ModEffects;
import com.lvedy.select.register.ModEntityTypes;
import com.lvedy.select.register.ModItems;
import com.lvedy.select.register.ModSounds;
import com.lvedy.select.register.ModTabs;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import java.util.Locale;

@Mod(Select.MODID)
public class Select {
    public static final String MODID = "select";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static ResourceLocation prefix(String name) {
        return ResourceLocation.fromNamespaceAndPath(MODID, name.toLowerCase(Locale.ROOT));
    }

    public Select(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);
        ModTabs.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModSounds.register(modEventBus);
        ModEffects.register(modEventBus);
        NetworkHandler.register(modEventBus);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC, "select_config.toml");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Select mod: common setup");
    }
}
