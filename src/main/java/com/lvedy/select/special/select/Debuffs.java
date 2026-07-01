package com.lvedy.select.special.select;

import com.lvedy.select.game.GameManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;

import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.List;

public class Debuffs {

    public static class Debuff1 extends AbstractSelectEffect {
        private static final Item[] ITEMS = {
                Items.CREEPER_HEAD,Items.SKELETON_SKULL,Items.ZOMBIE_HEAD,Items.WITHER_SKELETON_SKULL,Items.PIGLIN_HEAD,Items.DRAGON_HEAD
        };
        public Debuff1() {
            super("debuff1", "但是", "你将被永远失去头盔槽", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你将被永远失去头盔槽").withStyle(ChatFormatting.RED));
            Item head = ITEMS[player.getRandom().nextInt(ITEMS.length)];
            ItemStack stack = head.getDefaultInstance();
            var enchantRegister = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var binding = enchantRegister.getOrThrow(Enchantments.BINDING_CURSE);
            stack.enchant(binding, 4);
            player.setItemSlot(EquipmentSlot.HEAD, stack);
        }

        @Override
        public void remove(ServerPlayer player) {
            player.setItemSlot(EquipmentSlot.HEAD, Items.AIR.getDefaultInstance());
        }
    }

    public static class Debuff2 extends AbstractSelectEffect {
        public Debuff2() {
            super("debuff2", "但是", "你死亡时尸体会爆炸", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你死亡时尸体会爆炸").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff3 extends AbstractSelectEffect {
        public Debuff3() {
            super("debuff3", "但是", "你身边的动物将会变成씨발", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你身边的动物将会变成씨발").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff4 extends AbstractSelectEffect {
        public Debuff4() {
            super("debuff4", "但是", "射中你的弹射物会爆炸", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 射中你的弹射物会爆炸").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff5 extends AbstractSelectEffect {
        // 数值为 0 的属性修饰符：不改变实际数值，仅作为「玩家拥有 debuff5」的标记，
        // 借助属性系统自动同步到所有客户端，供渲染端 Mixin 检测以冻结肢体动画。
        private static final ResourceLocation FROZEN_ID =
                ResourceLocation.fromNamespaceAndPath("select", "debuff5_frozen");

        public Debuff5() {
            super("debuff5", "但是", "你不再有动态效果", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你不再有动态效果").withStyle(ChatFormatting.RED));
            var attr = player.getAttribute(Attributes.SCALE);
            if (attr == null) return;
            if (!attr.hasModifier(FROZEN_ID))
                attr.addPermanentModifier(new AttributeModifier(FROZEN_ID, 0.0, AttributeModifier.Operation.ADD_VALUE));
        }
        @Override
        public void remove(ServerPlayer player) {
            var attr = player.getAttribute(Attributes.SCALE);
            if (attr != null) attr.removeModifier(FROZEN_ID);
        }
        @Override
        public void reapply(ServerPlayer player) { apply(player); }
    }

    public static class Debuff6 extends AbstractSelectEffect {
        public Debuff6() {
            super("debuff6", "但是", "你所在的区块立即消失", false, false);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你所在的区块立即消失").withStyle(ChatFormatting.RED));
            if (player.level() instanceof ServerLevel serverLevel) {
                ChunkAccess chunk = serverLevel.getChunk(player.blockPosition());
                BlockPos chunkPos = chunk.getPos().getWorldPosition();
                int minY = serverLevel.getMinBuildHeight();
                int maxY = serverLevel.getMaxBuildHeight();

                BlockPos start = new BlockPos(chunkPos.getX(), minY, chunkPos.getZ());
                BlockPos end = new BlockPos(chunkPos.getX() + 15, maxY - 1, chunkPos.getZ() + 15); // maxY是上限（不包含），所以减1

                BlockPos.betweenClosed(start, end).forEach(pos -> {
                    serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                });
            }
        }
    }

    public static class Debuff7 extends AbstractSelectEffect {
        public Debuff7() {
            super("debuff7", "但是", "随机失去2个正面效果", false, false);
        }
        @Override
        public void apply(ServerPlayer player) {
            GameManager.removeRandomBuffs(player, 2);
            player.sendSystemMessage(Component.literal("- 随机失去2个正面效果").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff8 extends AbstractSelectEffect {
        // 船下沉逻辑在 ModEvents.onBoatTick 中按 hasEffect("debuff8") 判定，无需属性标记。
        public Debuff8() {
            super("debuff8", "但是", "你的船会沉没", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你的船会沉没").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff9 extends AbstractSelectEffect {
        // 数值为 0 的属性修饰符：仅作为「玩家拥有 debuff9」的标记，借属性系统自动同步到
        // 客户端，供 GameRendererMixin 检测以在「主世界 + 头顶见天 + 白天」时模糊视野。
        private static final ResourceLocation SUNLIGHT_ID =
                ResourceLocation.fromNamespaceAndPath("select", "debuff9_sunlight");

        public Debuff9() {
            super("debuff9", "但是", "阳光让你感到刺眼", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 阳光让你感到刺眼").withStyle(ChatFormatting.RED));
            var attr = player.getAttribute(Attributes.SCALE);
            if (attr == null) return;
            if (!attr.hasModifier(SUNLIGHT_ID))
                attr.addPermanentModifier(new AttributeModifier(SUNLIGHT_ID, 0.0, AttributeModifier.Operation.ADD_VALUE));
        }
        @Override
        public void remove(ServerPlayer player) {
            var attr = player.getAttribute(Attributes.SCALE);
            if (attr != null) attr.removeModifier(SUNLIGHT_ID);
        }
        @Override
        public void reapply(ServerPlayer player) { apply(player); }
    }

    public static class Debuff10 extends AbstractSelectEffect {
        // 工作台拦截逻辑在 ModEvents.onUseCraftingTable 中按 hasEffect("debuff10") 判定。
        public Debuff10() {
            super("debuff10", "但是", "你无法使用工作台", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你无法使用工作台").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff11 extends AbstractSelectEffect {
        // 呼吸反转逻辑在 ModEvents.tickDebuff11 中按 hasEffect("debuff11") 判定。
        public Debuff11() {
            super("debuff11", "但是", "你只能在水下呼吸", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你只能在水下呼吸").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff12 extends AbstractSelectEffect {
        // 砍树拦截逻辑在 ModEvents.onDebuff12BreakLog 中按 hasEffect("debuff12") 判定。
        public Debuff12() {
            super("debuff12", "但是", "你需要斧头才能砍树", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你需要斧头才能砍树").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff13 extends AbstractSelectEffect {
        public Debuff13() {
            super("debuff13", "但是", "你失去一半最大生命值", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            addModifier(player);
            player.sendSystemMessage(Component.literal("- 你失去一半最大生命值").withStyle(ChatFormatting.RED));
        }
        @Override
        public void reapply(ServerPlayer player) {
            addModifier(player);
        }
        private void addModifier(ServerPlayer player) {
            var attr = player.getAttribute(Attributes.MAX_HEALTH);
            if (attr == null) return;
            var modifier = new AttributeModifier(
                    ResourceLocation.fromNamespaceAndPath("select", "debuff13_health"),
                    -0.5,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
            if (!attr.hasModifier(modifier.id())) {
                attr.addPermanentModifier(modifier);
            }
        }
        @Override
        public void remove(ServerPlayer player) {
            var attr = player.getAttribute(Attributes.MAX_HEALTH);
            if (attr == null) return;
            attr.removeModifier(ResourceLocation.fromNamespaceAndPath("select", "debuff13_health"));
            float maxHp = (float) attr.getValue();
            if (player.getHealth() > maxHp) player.setHealth(maxHp);
        }
    }

    public static class Debuff14 extends AbstractSelectEffect {
        public Debuff14() {
            super("debuff14", "但是", "你每隔一段时间就会飞起来", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你每隔一段时间就会飞起来").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff15 extends AbstractSelectEffect {
        public Debuff15() {
            super("debuff15", "但是", "你身上的物品数量都变为1", false, false);
        }
        @Override
        public void apply(ServerPlayer player) {
            var inv = player.getInventory();
            for (var compartment : List.of(inv.items, inv.armor, inv.offhand)) {
                for (ItemStack stack : compartment) {
                    if (!stack.isEmpty() && stack.getCount() > 1) stack.setCount(1);
                }
            }
            player.containerMenu.broadcastChanges();
            player.sendSystemMessage(Component.literal("- 你身上的物品数量都变为1").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff16 extends AbstractSelectEffect {
        // 阻止浮出水面的逻辑在 ModEvents.tickDebuff16 中按 hasEffect("debuff16") 判定。
        public Debuff16() {
            super("debuff16", "但是", "你在水里难以行动", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你在水里难以行动").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff17 extends AbstractSelectEffect {
        public Debuff17() {
            super("debuff17", "但是", "你背包里的物品随机消失", false, false);
        }
        @Override
        public void apply(ServerPlayer player) {
            var inv = player.getInventory();
            // 收集所有非空槽位，随机抹掉其中约三分之一（至少 1 个，若有物品）
            List<ItemStack> nonEmpty = new ArrayList<>();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isEmpty()) nonEmpty.add(s);
            }
            if (!nonEmpty.isEmpty()) {
                java.util.Collections.shuffle(nonEmpty, java.util.concurrent.ThreadLocalRandom.current());
                int remove = Math.max(1, nonEmpty.size() / 3);
                for (int i = 0; i < remove; i++) nonEmpty.get(i).setCount(0);
                player.containerMenu.broadcastChanges();
            }
            player.sendSystemMessage(Component.literal("- 你背包里的物品随机消失").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff18 extends AbstractSelectEffect {
        public Debuff18() {
            super("debuff18", "但是", "你会被一只监守者看守", false, false);
        }
        @Override
        public void apply(ServerPlayer player) {
            if (player.level() instanceof ServerLevel level) {
                double angle = player.getRandom().nextDouble() * 2 * Math.PI;
                int x = net.minecraft.util.Mth.floor(player.getX() + 4 * Math.cos(angle));
                int z = net.minecraft.util.Mth.floor(player.getZ() + 4 * Math.sin(angle));
                BlockPos spawnPos = new BlockPos(x, player.getBlockY(), z);
                var warden = EntityType.WARDEN.spawn(level, spawnPos,
                        net.minecraft.world.entity.MobSpawnType.TRIGGERED);
                if (warden != null) {
                    warden.increaseAngerAt(player, 150, false);
                }
            }
            player.sendSystemMessage(Component.literal("- 你会被一只监守者看守").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff19 extends AbstractSelectEffect {
        public Debuff19() {
            super("debuff19", "但是", "你将前往地狱", false, false);
        }
        @Override
        public void apply(ServerPlayer player) {
            MinecraftServer server = player.getServer();
            if (server != null) {
                ServerLevel nether = server.getLevel(Level.NETHER);
                if (nether != null) {
                    // 主世界坐标按 1/8 缩放映射到下界
                    double nx = player.getX() / 8.0;
                    double nz = player.getZ() / 8.0;
                    player.teleportTo(nether, nx, 80, nz, player.getYRot(), player.getXRot());
                }
            }
            player.sendSystemMessage(Component.literal("- 你将前往地狱").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff20 extends AbstractSelectEffect {
        // 中立生物索敌逻辑在 ModEvents.tickDebuff20 中按 hasEffect("debuff20") 判定。
        public Debuff20() {
            super("debuff20", "但是", "中立生物将对你抱有敌意", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 中立生物将对你抱有敌意").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff21 extends AbstractSelectEffect {
        // 伤害翻倍逻辑在 ModEvents.onDebuff21Damage 中按 hasEffect("debuff21") 判定。
        public Debuff21() {
            super("debuff21", "但是", "你受到的伤害翻倍", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你受到的伤害翻倍").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff22 extends AbstractSelectEffect {
        // 火焰不灭逻辑在 ModEvents.tickDebuff22 中按 hasEffect("debuff22") 判定。
        public Debuff22() {
            super("debuff22", "但是", "你身上的火焰无法自然熄灭", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你身上的火焰无法自然熄灭").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff23 extends AbstractSelectEffect {
        // 床爆炸逻辑在 ModEvents.onDebuff23Bed 中按 hasEffect("debuff23") 判定。
        public Debuff23() {
            super("debuff23", "但是", "床对于你来说一直是刻意的游戏设计", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 床对于你来说一直是刻意的游戏设计").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff24 extends AbstractSelectEffect {
        // 只能靠牛肉回复饱食度，其余食物的回复在 ModEvents.onDebuff24Eat 中被抵消。
        public Debuff24() {
            super("debuff24", "但是", "你只吃牛肉", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你只能通过吃牛肉回复饱食度").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff25 extends AbstractSelectEffect {
        // 每 30s 随机播放一个 haha 音效的逻辑在 ModEvents.tickDebuff25 中按 hasEffect("debuff25") 判定。
        public Debuff25() {
            super("debuff25", "但是", "你的耳朵将受尽折磨", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你的耳朵将受尽折磨").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff26 extends AbstractSelectEffect {
        // 逻辑在 ModEvents.tickDebuff26 中按 hasEffect("debuff26") 判定。
        public Debuff26() {
            super("debuff26", "但是", "你将成为嘉豪", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你将成为嘉豪").withStyle(ChatFormatting.RED));
            var enchantRegister = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var binding = enchantRegister.getOrThrow(Enchantments.BINDING_CURSE);
            equipDyed(player, EquipmentSlot.HEAD, Items.LEATHER_HELMET, binding);
            equipDyed(player, EquipmentSlot.CHEST, Items.LEATHER_CHESTPLATE, binding);
            equipDyed(player, EquipmentSlot.LEGS, Items.LEATHER_LEGGINGS, binding);
            equipDyed(player, EquipmentSlot.FEET, Items.LEATHER_BOOTS, binding);
        }
        private void equipDyed(ServerPlayer player, EquipmentSlot slot, Item item,
                               net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> binding) {
            ItemStack stack = item.getDefaultInstance();
            stack.set(net.minecraft.core.component.DataComponents.DYED_COLOR,
                    new net.minecraft.world.item.component.DyedItemColor(0x000000, false));
            stack.enchant(binding, 1);
            player.setItemSlot(slot, stack);
        }
    }

    public static class Debuff27 extends AbstractSelectEffect {
        // 持续饥饿效果在 ModEvents.tickDebuff27 中按 hasEffect("debuff27") 刷新。
        public Debuff27() {
            super("debuff27", "但是", "你将饥饿缠身", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你将永远饥饿缠身").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff28 extends AbstractSelectEffect {
        // 物品获得消失诅咒的逻辑在 ModEvents.tickDebuff28 中按 hasEffect("debuff28") 持续应用。
        public Debuff28() {
            super("debuff28", "但是", "你身上的物品都会获得消失诅咒", false, false);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你身上的物品都会获得消失诅咒").withStyle(ChatFormatting.RED));
            var enchantReg = player.server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var vanishing = enchantReg.getOrThrow(Enchantments.VANISHING_CURSE);
            var inv = player.getInventory();
            boolean changed = false;
            for (var compartment : List.of(inv.items, inv.armor, inv.offhand)) {
                for (ItemStack s : compartment) {
                    if (!s.isEmpty() && s.getEnchantments().getLevel(vanishing) == 0) {
                        s.enchant(vanishing, 1);
                        changed = true;
                    }
                }
            }
            if (changed) player.containerMenu.broadcastChanges();
        }
    }

    public static class Debuff29 extends AbstractSelectEffect {
        // 乱码刷屏逻辑在 ModEvents.tickDebuff29 中按 hasEffect("debuff29") 定时发送。
        public Debuff29() {
            super("debuff29", "但是", "你的聊天栏会被摧毁", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你的聊天栏会被摧毁").withStyle(ChatFormatting.RED));
        }
    }

    public static class Debuff30 extends AbstractSelectEffect {
        // 头顶灰色粒子 + 周身下雨粒子在 ModEvents.tickDebuff30 中按 hasEffect("debuff30") 持续生成。
        public Debuff30() {
            super("debuff30", "但是", "你将被阴云笼罩", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你将被阴云与细雨永远笼罩").withStyle(ChatFormatting.RED));
        }
    }

    public static class DeBuff31 extends AbstractSelectEffect {
        private static final ResourceLocation SCALE_ID = ResourceLocation.fromNamespaceAndPath("select", "buff27_scale");
        public DeBuff31() {super("debuff31", "但是", "你会变成高人", false, true);}
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 变成高人").withStyle(ChatFormatting.RED));
            var attr = player.getAttribute(Attributes.SCALE);
            if (attr == null) return;
            if (!attr.hasModifier(SCALE_ID))
                attr.addPermanentModifier(new AttributeModifier(SCALE_ID, 0.5, AttributeModifier.Operation.ADD_VALUE));
        }
        @Override
        public void remove(ServerPlayer player) {
            var attr = player.getAttribute(Attributes.SCALE);
            if (attr != null) attr.removeModifier(SCALE_ID);
        }
        @Override
        public void reapply(ServerPlayer player) { apply(player); }
    }

    public static class DeBuff32 extends AbstractSelectEffect {
        private static final ResourceLocation SCALE_ID = ResourceLocation.fromNamespaceAndPath("select", "buff13_scale");

        public DeBuff32() {
            super("debuff32", "但是", "你会变成矮人", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("+ 变成矮人").withStyle(ChatFormatting.RED));
            var attr = player.getAttribute(Attributes.SCALE);
            if (attr == null) return;
            if (!attr.hasModifier(SCALE_ID))
                attr.addPermanentModifier(new AttributeModifier(SCALE_ID, -0.25, AttributeModifier.Operation.ADD_VALUE));
        }
        @Override
        public void remove(ServerPlayer player) {
            var attr = player.getAttribute(Attributes.SCALE);
            if (attr != null) attr.removeModifier(SCALE_ID);
        }
        @Override
        public void reapply(ServerPlayer player) { apply(player); }
    }

    public static class DeBuff33 extends AbstractSelectEffect {
        public DeBuff33() {
            super("debuff33", "但是", "你害怕进入黑暗环境", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你害怕进入黑暗环境").withStyle(ChatFormatting.RED));
        }
    }

    public static class DeBuff34 extends AbstractSelectEffect {
        public DeBuff34() {
            super("debuff34", "但是", "小僵尸，大麻烦！", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 小僵尸，大麻烦！").withStyle(ChatFormatting.RED));
        }
    }

    public static class DeBuff35 extends AbstractSelectEffect {
        public DeBuff35() {
            super("debuff35", "但是", "你身边的씨발将会开始操操操", false, true);
        }
        @Override
        public void apply(ServerPlayer player) {
            player.sendSystemMessage(Component.literal("- 你身边的씨발将会开始操操操").withStyle(ChatFormatting.RED));
        }
    }
}
