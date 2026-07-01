package com.lvedy.select.register;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

import static com.lvedy.select.Select.MODID;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, MODID);

    public static final List<DeferredHolder<SoundEvent, SoundEvent>> HAHA = new ArrayList<>();
    public static final List<DeferredHolder<SoundEvent, SoundEvent>> MUSIC = new ArrayList<>();

    static {
        haha("beliya");
        haha("aiqingzhuanyi");
        haha("cunriying");
        haha("fukua");
        haha("dilili");
        haha("dudu_da");
        haha("gangguan");
        haha("gugugaga");
        haha("jianguan");
        haha("johnson_dengyan");
        haha("kami_xiao");
        haha("liubei_jiasu");
        haha("man");
        haha("manbo_shaxiao");
        haha("mujika");
        haha("nailong_daxiao");
        haha("tang_xiao");
        haha("tom_jiao1");
        haha("tom_jiao2");
        haha("wenzi");
        haha("zuobian_binge");
        haha("monv");
        haha("power");
        haha("tfboys");
        haha("chouyan");
        haha("tong");
        haha("quanshiai");
        haha("tafei");
        haha("indu");
        haha("indu2");
        haha("indu3");
        haha("taiguan");
        haha("never");

        music("spectre");
        music("all_falls_down");
        music("alone");
        music("faded");
    }

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(MODID, name);
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(id));
    }

    private static void haha(String key) {
        HAHA.add(register("haha." + key));
    }

    private static void music(String key) {
        MUSIC.add(register("music." + key));
    }

    public static void register(IEventBus bus) {
        SOUND_EVENTS.register(bus);
    }
}
