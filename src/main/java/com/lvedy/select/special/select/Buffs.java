package com.lvedy.select.special.select;

import com.lvedy.select.game.GameManager;
import com.lvedy.select.register.ModItems;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.Unbreakable;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.common.Tags;

import java.awt.*;
import java.util.List;


public class Buffs {

    public static class Buff1 extends AbstractSelectEffect {
        public Buff1() {
            super("buff1", "你将", "身上的装备获得不可破坏", true, false);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 身上的装备获得不可破坏").withStyle(ChatFormatting.GREEN));
            // 给所有装备槽的非空物品加 Unbreakable 组件
            for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty()) {
                    stack.set(DataComponents.UNBREAKABLE, new Unbreakable(true));
                }
            }
        }
        @Override
        public void remove(ServerPlayer player) {
            for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                ItemStack stack = player.getItemBySlot(slot);
                if (!stack.isEmpty()) stack.remove(DataComponents.UNBREAKABLE);
            }
        }
    }

    public static class Buff2 extends AbstractSelectEffect {
        public Buff2() {
            super("buff2", "你将", "身上的物品获得随机附魔", true, false);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 身上的物品获得随机附魔").withStyle(ChatFormatting.GREEN));
            RandomSource rng = player.getRandom();
            var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var inv = player.getInventory();

            for (var compartment : java.util.List.of(inv.items, inv.armor, inv.offhand)) {
                for (ItemStack stack : compartment) {
                    if (stack.isEmpty()) continue;

                    // 收集所有附魔
                    var compatible = registry.listElements()
                            .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
                    if (compatible.isEmpty()) continue;

                    java.util.Collections.shuffle(compatible, java.util.concurrent.ThreadLocalRandom.current());
                    int count = 1 + rng.nextInt(3); // 随机 1~3 种
                    for (int i = 0; i < Math.min(count, compatible.size()); i++) {
                        var holder = compatible.get(i);
                        int max = holder.value().getMaxLevel();
                        int level = max <= 1 ? 1 : 1 + rng.nextInt(max);
                        stack.enchant(holder, level);
                    }
                }
            }
        }
    }

    public static class Buff3 extends AbstractSelectEffect {
        public Buff3() {
            super("buff3", "你将", "获得20点额外生命值", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 获得20点额外生命值").withStyle(ChatFormatting.GREEN));
            addModifier(player);
            // 把当前生命值也提升满
            var attr = player.getAttribute(Attributes.MAX_HEALTH);
            if (attr != null) player.setHealth(Math.min(player.getHealth() + 20f, (float) attr.getValue()));
        }
        @Override
        public void reapply(ServerPlayer player) {
            // 重生后实体被重建，修饰符丢失，需要重新添加。重生时已是满血，无需额外补血。
            addModifier(player);
        }
        private void addModifier(ServerPlayer player) {
            var attr = player.getAttribute(Attributes.MAX_HEALTH);
            if (attr == null) return;
            var modifier = new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath("select", "buff3_health"),
                    20,
                    AttributeModifier.Operation.ADD_VALUE
            );
            if (!attr.hasModifier(modifier.id())) {
                attr.addPermanentModifier(modifier);
            }
        }
        @Override
        public void remove(ServerPlayer player) {
            var attr = player.getAttribute(Attributes.MAX_HEALTH);
            if (attr == null) return;
            attr.removeModifier(ResourceLocation.fromNamespaceAndPath("select", "buff3_health"));
            float maxHp = (float) attr.getValue();
            if (player.getHealth() > maxHp) player.setHealth(maxHp);
        }
    }

    public static class Buff4 extends AbstractSelectEffect {
        public Buff4() {
            super("buff4", "你将", "获得50%额外移动速度", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 获得50%额外移动速度").withStyle(ChatFormatting.GREEN));
            var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attr == null) return;
            var modifier = new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath("select", "buff4_speed"),
                    0.5,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            if (!attr.hasModifier(modifier.id())) {
                attr.addPermanentModifier(modifier);
            }
        }
        @Override
        public void remove(ServerPlayer player) {
            var attr = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (attr != null) attr.removeModifier(ResourceLocation.fromNamespaceAndPath("select", "buff4_speed"));
        }
        @Override
        public void reapply(ServerPlayer player) { apply(player); }
    }

    public static class Buff5 extends AbstractSelectEffect {
        public Buff5() {
            super("buff5", "你将", "获得100%额外跳跃高度", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 获得100%额外跳跃高度").withStyle(ChatFormatting.GREEN));
            var attr = player.getAttribute(Attributes.JUMP_STRENGTH);
            if (attr == null) return;
            var modifier = new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath("select", "buff5_jump"),
                    0.5,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE
            );
            if (!attr.hasModifier(modifier.id())) {
                attr.addPermanentModifier(modifier);
            }
        }
        @Override
        public void remove(ServerPlayer player) {
            var attr = player.getAttribute(Attributes.JUMP_STRENGTH);
            if (attr != null) attr.removeModifier(ResourceLocation.fromNamespaceAndPath("select", "buff5_jump"));
        }
        @Override
        public void reapply(ServerPlayer player) { apply(player); }
    }

    public static class Buff6 extends AbstractSelectEffect {
        public Buff6() {
            super("buff6", "你将", "不再被苦力怕伤害", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 不再被苦力怕伤害").withStyle(ChatFormatting.GREEN));
        }
    }

    public static class Buff7 extends AbstractSelectEffect {
        public Buff7() {
            super("buff7", "你将", "直接从生物身上获得熟肉", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 直接从生物身上获得熟肉").withStyle(ChatFormatting.GREEN));
        }
    }

    public static class Buff8 extends AbstractSelectEffect {
        public Buff8() {
            super("buff8", "你将", "不再需要熔炼矿物", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 不再需要熔炼矿物").withStyle(ChatFormatting.GREEN));
        }
    }

    public static class Buff9 extends AbstractSelectEffect {
        public Buff9() {
            super("buff9", "你将", "可以从任何生物身上取奶", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 可以从任何生物身上取奶").withStyle(ChatFormatting.GREEN));
        }
    }

    public static class Buff10 extends AbstractSelectEffect {
        public Buff10() {
            super("buff10", "你将", "随机移除你身上的3个debuff", true, false);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 随机移除你身上的3个debuff").withStyle(ChatFormatting.GREEN));
            GameManager.removeRandomDebuffs(player, 3);
        }
    }

    public static class Buff11 extends AbstractSelectEffect {
        public Buff11() {
            super("buff11", "你将", "轻而易举的取人首级", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 轻而易举的取人首级").withStyle(ChatFormatting.GREEN));
        }
    }

    public static class Buff12 extends AbstractSelectEffect {
        public Buff12() {
            super("buff12", "你将", "拿着剑时获得杀戮光环", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 拿着剑时获得杀戮光环").withStyle(ChatFormatting.GREEN));
        }
    }

    public static class Buff13 extends AbstractSelectEffect {
        public Buff13() {
            super("buff13", "你将", "越煮越鲜活", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 越煮越鲜活(受到伤害后恢复4点生命值)").withStyle(ChatFormatting.GREEN));
        }
    }

    public static class Buff14 extends AbstractSelectEffect {
        public Buff14() {
            super("buff14", "你将", "生命值越少就获得越高的伤害", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 生命值越少就获得越高的伤害").withStyle(ChatFormatting.GREEN));
        }
    }

    public static class Buff15 extends AbstractSelectEffect {
        public Buff15() {
            super("buff15", "你将", "免疫溺水伤害", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 免疫溺水伤害").withStyle(ChatFormatting.GREEN));
        }
    }

    public static class Buff16 extends AbstractSelectEffect {
        public Buff16() {
            super("buff16", "你将", "免疫火焰伤害", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 免疫火焰伤害").withStyle(ChatFormatting.GREEN));
        }
    }

    public static class Buff17 extends AbstractSelectEffect {
        public Buff17() {
            super("buff17", "你将", "可以和部分生物繁衍", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 可以和部分生物繁衍").withStyle(ChatFormatting.GREEN));
        }
    }

    public static class Buff18 extends AbstractSelectEffect {
        public Buff18() {
            super("buff18", "你将", "攻击敌人时额外施加100%击退效果", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 攻击敌人时额外施加100%击退效果").withStyle(ChatFormatting.GREEN));
        }
    }

    public static class Buff19 extends AbstractSelectEffect {
        public Buff19() {
            super("buff19", "你将", "附近的树叶掉落的苹果替换为金苹果", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 附近的树叶掉落的苹果替换为金苹果").withStyle(ChatFormatting.GREEN));
        }
    }

    public static class Buff20 extends AbstractSelectEffect {
        public Buff20() {
            super("buff20", "你将", "不再被蜘蛛网阻滞", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 不再被蜘蛛网阻滞").withStyle(ChatFormatting.GREEN));
        }
    }

    public static class Buff21 extends AbstractSelectEffect {
        private static final String[] OVERWORLD_STRUCTURES = {
                "village", "ancient_city", "pillager_outpost", "shipwreck", "igloo"
        };
        private static final String[] NETHER_STRUCTURES = {
                "fortress", "bastion_remnant"
        };
        private static final String[] END_STRUCTURES = {
                "end_city"
        };
        public Buff21() {
            super("buff21", "你将", "被传送至随机某个附近结构的上空", true, false);
        }

        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 被传送至随机某个附近结构的上空").withStyle(ChatFormatting.GREEN));
            ServerLevel level = (ServerLevel) player.level();
            String[] structures;
            ResourceKey<Level> dim = level.dimension();
            if (dim == Level.NETHER) {
                structures = NETHER_STRUCTURES;
            } else if (dim == Level.END) {
                structures = END_STRUCTURES;
            } else {
                structures = OVERWORLD_STRUCTURES;
            }

            String picked = structures[player.getRandom().nextInt(structures.length)];
            var key = ResourceKey.create(Registries.STRUCTURE, ResourceLocation.withDefaultNamespace(picked));

            // 查找最近的结构
            var found = level.registryAccess().registryOrThrow(Registries.STRUCTURE)
                    .getHolder(key)
                    .map(h -> level.getChunkSource().getGenerator()
                            .findNearestMapStructure(level, HolderSet.direct(h), player.blockPosition(), 500, false))
                    .orElse(null);

            if (found != null) {
                BlockPos pos = found.getFirst();
                double y;
                if ("ancient_city".equals(picked)) {
                    y = -49;
                } else {
                    int topY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
                    y = topY + 1;
                }
                player.teleportTo(pos.getX() + 0.5, y, pos.getZ() + 0.5);
                player.displayClientMessage(Component.literal("已传送至最近的 " + picked), true);
            } else {
                player.displayClientMessage(Component.literal("附近500区块内未找到 " + picked), true);
            }
        }
    }

    public static class Buff22 extends AbstractSelectEffect {
        public Buff22() {super("buff22", "你将", "立即获得10个矿石\n煤炭40%;铁25%;金25%\n钻石8%;下届合金锭2%", true, false);}
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 立即获得10个矿石").withStyle(ChatFormatting.GREEN));
            RandomSource rng = player.getRandom();
            for (int i = 0; i < 10; i++) {
                float r = rng.nextFloat();
                var item = r < 0.40f ? Items.COAL
                        : r < 0.65f ? Items.IRON_INGOT
                        : r < 0.90f ? Items.GOLD_INGOT
                        : r < 0.98f ? Items.DIAMOND
                        : Items.NETHERITE_INGOT;
                player.getInventory().add(new ItemStack(item));
            }
        }
    }

    public static class Buff23 extends AbstractSelectEffect {
        public Buff23() {super("buff23", "你将", "被猪包围", true, false);}
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 被猪包围").withStyle(ChatFormatting.GREEN));
            ServerLevel level = (ServerLevel) player.level();
            int count = 20;
            for (int i = 0; i < count; i++) {
                double angle = 2 * Math.PI * i / count;
                double x = player.getX() + 3 * Math.cos(angle);
                double z = player.getZ() + 3 * Math.sin(angle);
                Pig pig = EntityType.PIG.create(level);
                if (pig == null) continue;
                pig.moveTo(x, player.getY(), z, player.getRandom().nextFloat() * 360f, 0f);
                level.addFreshEntity(pig);
            }
        }
    }

    public static class Buff24 extends AbstractSelectEffect {
        public Buff24() {super("buff24", "你将", "能够呼风唤雨", true, false);}
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 能够呼风唤雨").withStyle(ChatFormatting.GREEN));
            player.getInventory().add(new ItemStack((Holder<Item>) ModItems.WEATHER_CONTROLLER, 1));
        }
    }

    public static class Buff25 extends AbstractSelectEffect {
        public Buff25() {super("buff25", "你将", "立即获得20个能直接将其丢出的TNT", true, false);}
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 立即获得20个能直接将其丢出的TNT").withStyle(ChatFormatting.GREEN));
            // 每个 TNT 都打上 "tnt" 标记，右键时投掷
            ItemStack tnt = new ItemStack(Items.TNT, 20);
            com.lvedy.select.special.items.SpecialItemTags.markTnt(tnt);
            player.getInventory().add(tnt);
        }
    }

    public static class Buff26 extends AbstractSelectEffect {
        public Buff26() {super("buff26", "你将", "立即获得AK47和2组弹药", true, false);}
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 立即获得AK47和2组弹药").withStyle(ChatFormatting.GREEN));
            ItemStack gun = new ItemStack(Items.STICK);
            gun.set(DataComponents.CUSTOM_NAME,
                    Component.literal("AK47").withStyle(s -> s.withColor(ChatFormatting.GOLD).withItalic(false)));
            com.lvedy.select.special.items.SpecialItemTags.markAk(gun);
            player.getInventory().add(gun);
            player.getInventory().add(new ItemStack(Items.ARROW, 64));
            player.getInventory().add(new ItemStack(Items.ARROW, 64));
        }
    }

    public static class Buff27 extends AbstractSelectEffect {
        public Buff27() {super("buff27", "你将", "获得抗性提升1", true, true);}
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 获得抗性提升1").withStyle(ChatFormatting.GREEN));
        }
    }

    public static class Buff28 extends AbstractSelectEffect {
        public Buff28() {super("buff28", "你将", "获得肘击之力", true, false);}
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 获得肘击之力").withStyle(ChatFormatting.GREEN));
            ItemStack manba = new ItemStack((ItemLike) ModItems.MAMBA);
            var enchantRegister = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var knockback = enchantRegister.getOrThrow(Enchantments.KNOCKBACK);
            manba.enchant(knockback, 20);
            player.getInventory().add(manba);
        }
    }

    public static class Buff29 extends AbstractSelectEffect {
        public Buff29() {super("buff29", "你将", "能够从村民身上搜刮", true, true);}
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 能够从村民身上搜刮").withStyle(ChatFormatting.GREEN));
        }
    }

    public static class Buff30 extends AbstractSelectEffect {
        public Buff30() {super("buff30", "你将", "获得一组神秘套餐", true, false);}
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 获得一组神秘套餐").withStyle(ChatFormatting.GREEN));
            player.getInventory().add(new ItemStack((Holder<Item>) ModItems.CHOCOLITE, 4));
            player.getInventory().add(new ItemStack((Holder<Item>) ModItems.SPRITE, 4));
        }
    }

    public static class Buff31 extends AbstractSelectEffect {
        public Buff31() {super("buff31", "你将", "成为卡密", true, false);}
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 成为卡密").withStyle(ChatFormatting.GREEN));
            player.getInventory().add(new ItemStack((Holder<Item>) ModItems.DEATH_NOTE, 1));
        }
    }

    public static class Buff32 extends AbstractSelectEffect {
        public Buff32() {super("buff32", "你将", "身上的物品数量变为一组", true, true);}
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 身上的物品数量变为一组").withStyle(ChatFormatting.GREEN));
            var inv = player.getInventory();
            for (var compartment : List.of(inv.items, inv.armor, inv.offhand)) {
                for (ItemStack stack : compartment) {
                    if (!stack.isEmpty()) stack.setCount(stack.getMaxStackSize());
                }
            }
        }
    }

    public static class Buff33 extends AbstractSelectEffect {
        public Buff33() {
            super("buff33", "你将", "攻击时回复伤害的20%生命值", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 攻击时回复伤害的20%生命值").withStyle(ChatFormatting.GREEN));
        }
    }

    public static class Buff34 extends AbstractSelectEffect {
        public Buff34() {
            super("buff34", "你将", "失去生命值时获得饱食度和饱腹度", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 失去生命值时获得饱食度和饱腹度").withStyle(ChatFormatting.GREEN));
        }
    }

    public static class Buff35 extends AbstractSelectEffect {
        private static final ResourceLocation SCALE_ID = ResourceLocation.fromNamespaceAndPath("select", "buff35_scale");
        public Buff35() {
            super("buff35", "你将", "获得大卫带", true, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 获得大卫带").withStyle(ChatFormatting.GREEN));
        }
        @Override
        public void remove(ServerPlayer player) {
            var attr = player.getAttribute(Attributes.SCALE);
            if (attr != null) attr.removeModifier(SCALE_ID);
        }
        @Override
        public void reapply(ServerPlayer player) { apply(player); }
    }
}
