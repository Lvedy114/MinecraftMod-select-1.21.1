package com.lvedy.select.event;

import com.lvedy.select.Select;
import com.lvedy.select.command.SelectCommand;
import com.lvedy.select.game.GameManager;
import com.lvedy.select.game.HeartFailureScheduler;
import com.lvedy.select.network.ReplaceMusicPacket;
import com.lvedy.select.network.SpinCameraPacket;
import com.lvedy.select.register.ModSounds;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.hoglin.Hoglin;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

@EventBusSubscriber(modid = Select.MODID)
public class ModEvents {

    // Buff17：CD（10s）和幼体跟随表
    private static final Map<UUID, Long> BUFF17_CD = new HashMap<>();
    private static final Map<UUID, UUID> BUFF17_BABIES = new HashMap<>(); // babyUUID → playerUUID

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        SelectCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        GameManager.tick(event.getServer());
        tickBuff14(event.getServer());
        tickBuff17(event.getServer());
        tickBuff12(event.getServer());
        tickBuff27(event.getServer());
        //tickBuff35(event.getServer());
        tickDeBuff3(event.getServer());
        tickDeBuff14(event.getServer());
        tickDebuff16(event.getServer());
        tickDebuff20(event.getServer());
        tickDebuff22(event.getServer());
        tickDebuff25(event.getServer());
        tickDebuff26(event.getServer());
        tickDebuff27(event.getServer());
        tickDebuff28(event.getServer());
        tickDebuff29(event.getServer());
        tickDebuff30(event.getServer());
        tickDebuff33(event.getServer());
        tickDeBuff34(event.getServer());
        tickDeBuff35(event.getServer());
        HeartFailureScheduler.tick(event.getServer());
    }

    public static void spawnCreeperFirework(Level level, double x, double y, double z) {
        ItemStack fw = new ItemStack(Items.FIREWORK_ROCKET);
        fw.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(
                new FireworkExplosion(FireworkExplosion.Shape.BURST,
                        new IntArrayList(new int[]{0x00FF88, 0xFF5500, 0xFFDD00, 0x3399FF, 0xFF44CC}),
                        new IntArrayList(new int[]{0xFFFFFF}), true, true)
        )));
        FireworkRocketEntity rocket = new FireworkRocketEntity(level, x, y, z, fw);
        try {
            java.lang.reflect.Field f = FireworkRocketEntity.class.getDeclaredField("lifetime");
            f.setAccessible(true);
            f.set(rocket, 1);
        } catch (ReflectiveOperationException ignored) {}
        level.addFreshEntity(rocket);
    }

    // ── Buff6/15/16：免疫特定伤害 ─────────────────────────────────────────────
    @SubscribeEvent
    public static void onLivingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var src = event.getSource();
        UUID id = player.getUUID();
        if (src.is(DamageTypes.FIREWORKS) && GameManager.hasEffect(id, "buff6")) { event.setCanceled(true); return; }
        if (src.is(DamageTypes.DROWN) && GameManager.hasEffect(id, "buff15")) { event.setCanceled(true); return; }
        if (src.is(DamageTypeTags.IS_FIRE) && GameManager.hasEffect(id, "buff16")) { event.setCanceled(true); return; }
    }

    // ── 重生后重新应用属性类效果（重生会重建实体，修饰符丢失） ──────────────────
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GameManager.reapplyEffects(player);
        }
    }

    // ── 登录时清除残留的属性标记（永久修饰符会持久化到 NBT） ──────────────────
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        var attr = player.getAttribute(Attributes.SCALE);
        if (attr == null) return;
        UUID id = player.getUUID();
        if (!GameManager.hasEffect(id, "debuff5"))
            attr.removeModifier(ResourceLocation.fromNamespaceAndPath("select", "debuff5_frozen"));
        if (!GameManager.hasEffect(id, "debuff9"))
            attr.removeModifier(ResourceLocation.fromNamespaceAndPath("select", "debuff9_sunlight"));
    }

    // ── AK47 / 可投掷 TNT：右键检测自定义 NBT 并触发对应效果 ─────────────────────
    private static final Map<UUID, Long> SPECIAL_ITEM_CD = new HashMap<>();
    private static final long AK_CD_MS = 150L;   // 射速限制
    private static final long TNT_CD_MS = 400L;

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        handleSpecialItem(event.getEntity(), event.getHand(), event);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 对着方块右键时，TNT 会触发放置、木棍无操作；这里拦截以执行自定义行为
        handleSpecialItem(event.getEntity(), event.getHand(), event);
    }

    private static void handleSpecialItem(Player player, InteractionHand hand, PlayerInteractEvent event) {
        ItemStack stack = player.getItemInHand(hand);
        boolean isAk = com.lvedy.select.special.items.SpecialItemTags.isAk(stack);
        boolean isTnt = com.lvedy.select.special.items.SpecialItemTags.isTnt(stack);
        if (!isAk && !isTnt) return;

        // 取消原版行为（TNT 放置 / 蓄力等），并阻止另一只手继续处理。
        if (event instanceof net.neoforged.bus.api.ICancellableEvent cancellable) {
            cancellable.setCanceled(true);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        long now = System.currentTimeMillis();
        long cd = isAk ? AK_CD_MS : TNT_CD_MS;
        if (now - SPECIAL_ITEM_CD.getOrDefault(player.getUUID(), 0L) < cd) return;

        if (isAk) {
            fireArrow(serverPlayer, level);
        } else {
            throwTnt(serverPlayer, level);
        }
        SPECIAL_ITEM_CD.put(player.getUUID(), now);
    }

    /** AK47：消耗背包中一支箭并向前射出 */
    private static void fireArrow(ServerPlayer player, ServerLevel level) {
        boolean creative = player.getAbilities().instabuild;
        if (!creative && !consumeOneArrow(player)) {
            player.displayClientMessage(Component.literal("没有弹药了").withStyle(net.minecraft.ChatFormatting.RED), true);
            return;
        }

        var arrow = new net.minecraft.world.entity.projectile.Arrow(
                level, player, new ItemStack(Items.ARROW), null);
        arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0f, 3.0f, 1.0f);
        if (creative) arrow.pickup = net.minecraft.world.entity.projectile.AbstractArrow.Pickup.CREATIVE_ONLY;
        level.addFreshEntity(arrow);

        level.playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.ARROW_SHOOT, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);
    }

    /** 从背包中找到并消耗一支箭，成功返回 true */
    private static boolean consumeOneArrow(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(Items.ARROW)) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    /** 可投掷 TNT：在玩家视线方向生成已点燃的 TNT 实体并抛出 */
    private static void throwTnt(ServerPlayer player, ServerLevel level) {
        var look = player.getLookAngle();
        double x = player.getX() + look.x;
        double y = player.getEyeY() + look.y - 0.1;
        double z = player.getZ() + look.z;

        var tnt = new net.minecraft.world.entity.item.PrimedTnt(level, x, y, z, player);
        double speed = 1.2;
        tnt.setDeltaMovement(look.x * speed, look.y * speed + 0.2, look.z * speed);
        level.addFreshEntity(tnt);

        level.playSound(null, player.blockPosition(),
                net.minecraft.sounds.SoundEvents.TNT_PRIMED, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.0f);

        if (!player.getAbilities().instabuild) {
            consumeOneTnt(player);
        }
    }

    /** 消耗一个带标记的可投掷 TNT */
    private static void consumeOneTnt(ServerPlayer player) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (com.lvedy.select.special.items.SpecialItemTags.isTnt(s)) {
                s.shrink(1);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        MinecraftServer server = player.getServer();
        if (server == null) return;

        if (player instanceof ServerPlayer serverPlayer) {
            GameManager.handlePlayerLogout(serverPlayer);
        }

        if (GameManager.isRunning() && server.getPlayerList().getPlayerCount() <= 1) {
            GameManager.stop();
            Select.LOGGER.info("最后一个玩家退出，游戏已自动暂停。");
        }
    }

    // ── Buff7：击杀生物时生肉改熟肉 ──────────────────────────────────────────
    private static final Map<Item, Item> RAW_TO_COOKED = Map.of(
            Items.BEEF,    Items.COOKED_BEEF,
            Items.PORKCHOP, Items.COOKED_PORKCHOP,
            Items.CHICKEN, Items.COOKED_CHICKEN,
            Items.MUTTON,  Items.COOKED_MUTTON,
            Items.RABBIT,  Items.COOKED_RABBIT,
            Items.COD,     Items.COOKED_COD,
            Items.SALMON,  Items.COOKED_SALMON
    );

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();

        // Buff7：生肉 → 熟肉
        if (GameManager.hasEffect(id, "buff7")) {
            event.getDrops().forEach(itemEntity -> {
                Item cooked = RAW_TO_COOKED.get(itemEntity.getItem().getItem());
                if (cooked != null) {
                    ItemStack old = itemEntity.getItem();
                    itemEntity.setItem(new ItemStack(cooked, old.getCount()));
                }
            });
        }

        // Buff11：击杀生物获得头颅
        if (GameManager.hasEffect(id, "buff11")) {
            ItemStack skull = getSkullForEntity(event.getEntity());
            if (skull != null) {
                double x = event.getEntity().getX(), y = event.getEntity().getY(), z = event.getEntity().getZ();
                event.getDrops().add(new ItemEntity(event.getEntity().level(), x, y, z, skull));
            }
        }
    }

    // ── Buff11：实体类型 → 头颅物品 ───────────────────────────────────────────
    private static final Map<EntityType<?>, Item> ENTITY_SKULL = Map.of(
            EntityType.CREEPER,          Items.CREEPER_HEAD,
            EntityType.SKELETON,         Items.SKELETON_SKULL,
            EntityType.ZOMBIE,           Items.ZOMBIE_HEAD,
            EntityType.WITHER_SKELETON,  Items.WITHER_SKELETON_SKULL,
            EntityType.PIGLIN,           Items.PIGLIN_HEAD,
            EntityType.ENDER_DRAGON,     Items.DRAGON_HEAD
    );

    private static ItemStack getSkullForEntity(net.minecraft.world.entity.LivingEntity entity) {
        // 玩家头颅（携带皮肤）
        if (entity instanceof ServerPlayer killed) {
            ItemStack head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.PROFILE, new ResolvableProfile(
                    Optional.of(killed.getGameProfile().getName()),
                    Optional.of(killed.getUUID()),
                    killed.getGameProfile().getProperties()
            ));
            return head;
        }
        Item skull = ENTITY_SKULL.get(entity.getType());
        return skull != null ? new ItemStack(skull) : null;
    }

    // ── Buff8：挖矿直接掉落矿锭 ───────────────────────────────────────────────
    private static final Map<Item, Item> RAW_TO_INGOT = Map.of(
            Items.RAW_IRON,   Items.IRON_INGOT,
            Items.RAW_GOLD,   Items.GOLD_INGOT,
            Items.RAW_COPPER, Items.COPPER_INGOT
    );

    @SubscribeEvent
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player)) return;
        if (!GameManager.hasEffect(player.getUUID(), "buff8")) return;

        event.getDrops().forEach(itemEntity -> {
            ItemStack stack = itemEntity.getItem();
            Item ingot = RAW_TO_INGOT.get(stack.getItem());
            if (ingot != null) itemEntity.setItem(new ItemStack(ingot, stack.getCount()));
        });
    }

    // ── Buff9：空桶右键任何生物得到命名奶桶 ────────────────────────────────────
    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!GameManager.hasEffect(player.getUUID(), "buff9")) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!held.is(Items.BUCKET)) return;

        event.setCanceled(true);

        String entityName = event.getTarget().getName().getString();
        ItemStack milk = new ItemStack(Items.MILK_BUCKET);
        milk.set(DataComponents.CUSTOM_NAME, Component.literal(entityName + "的奶"));

        held.shrink(1);
        if (held.isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, milk);
        } else {
            player.getInventory().add(milk);
        }
    }

    // ── Buff12：杀戮光环，每5 tick 自动攻击最近的生物 ────────────────────────────
    private static int buff12Ticker = 0;
    private static void tickBuff12(MinecraftServer server) {
        if (++buff12Ticker < 5) return;
        buff12Ticker = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!GameManager.hasEffect(player.getUUID(), "buff12")) continue;
            if (!(player.getMainHandItem().getItem() instanceof SwordItem)) continue;
            // 找范围内距离最近的非玩家生物
            LivingEntity target = player.level()
                    .getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(4),
                            e -> e != player)
                    .stream()
                    .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                    .orElse(null);
            if (target == null) continue;
            // 朝向目标：仅同步旋转，不移动玩家
            double dx = target.getX() - player.getX();
            double dy = target.getEyeY() - player.getEyeY();
            double dz = target.getZ() - player.getZ();
            float yaw = (float) (Math.atan2(-dx, dz) * (180 / Math.PI));
            float pitch = (float) (-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * (180 / Math.PI));
            player.connection.teleport(player.getX(), player.getY(), player.getZ(), yaw, pitch,
                    Set.of(RelativeMovement.X, RelativeMovement.Y, RelativeMovement.Z));

            // 挥臂动画 + 伤害
            player.swing(InteractionHand.MAIN_HAND);
            player.connection.send(new net.minecraft.network.protocol.game.ClientboundAnimatePacket(player, 0));
            float dmg = (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE);
            target.hurt(player.damageSources().playerAttack(player), dmg);
        }
    }

    // ── Buff13：受到伤害后恢复一定量生命值 ────────────────────────────
    @SubscribeEvent
    private static void Buff13(LivingDamageEvent.Post event){
        if (event.getEntity() instanceof Player player) {
            if (!GameManager.hasEffect(player.getUUID(), "buff13")) return;
            if (player.getHealth()<=0) return;
            player.setHealth(player.getHealth() + 4);
        }
    }

    // ── Buff14：每损失15%生命值获得一级力量 ────────────────────────────────────
    private static int buff14Ticker = 0;

    private static void tickBuff14(MinecraftServer server) {
        if (++buff14Ticker < 40) return;
        buff14Ticker = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!GameManager.hasEffect(player.getUUID(), "buff14")) continue;
            float maxHp = (float) player.getAttributeValue(Attributes.MAX_HEALTH);
            float missing = maxHp - player.getHealth();
            int lvl = (int) (missing / (maxHp * 0.15f));
            if (lvl > 0) player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 60, lvl - 1, false, false));
        }
    }

    // ── Buff17：空手右键生物，生成幼体 ──────────────────────────────────────────
    private static final long BUFF17_CD_MS = 7_000L;

    @SubscribeEvent
    public static void onEntityInteractBuff17(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!GameManager.hasEffect(player.getUUID(), "buff17")) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!player.getMainHandItem().isEmpty()) return;
        if (!(event.getTarget() instanceof LivingEntity target)) return;

        long now = System.currentTimeMillis();
        if (now - BUFF17_CD.getOrDefault(player.getUUID(), 0L) < BUFF17_CD_MS) return;

        ServerLevel level = (ServerLevel) player.level();
        var baby = target.getType().create(level);
        if (baby == null) return;

        boolean valid = false;
        if (baby instanceof AgeableMob m) { m.setAge(-24000); valid = true; }
        else if (baby instanceof Zombie z) { z.setBaby(true); valid = true; }

        if (!valid) { baby.discard(); return; }

        baby.setCustomName(Component.literal(player.getName().getString() + "的孩子"));
        baby.setCustomNameVisible(true);
        baby.moveTo(target.getX(), target.getY(), target.getZ(), target.getYRot(), target.getXRot());
        level.addFreshEntity(baby);
        BUFF17_CD.put(player.getUUID(), now);
        BUFF17_BABIES.put(baby.getUUID(), player.getUUID());

        level.sendParticles(ParticleTypes.HEART,
                target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                8, 0.4, 0.4, 0.4, 0.05);
        event.setCanceled(true);
    }

    private static void tickBuff17(MinecraftServer server) {
        BUFF17_BABIES.entrySet().removeIf(entry -> {
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getValue());
            if (owner == null) return true;
            // 在所有维度中查找该幼体
            for (var level : server.getAllLevels()) {
                var entity = level.getEntity(entry.getKey());
                if (entity instanceof PathfinderMob mob) {
                    if (!mob.isAlive()) return true;
                    mob.getNavigation().moveTo(owner, 1.2);
                    return false;
                }
            }
            return true; // 找不到则清理
        });
    }

    // ── Buff18：额外100%击退 ───────────────────────────────────────────────────
    @SubscribeEvent
    public static void onLivingDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!GameManager.hasEffect(player.getUUID(), "buff18")) return;
        LivingEntity target = event.getEntity();
        target.knockback(0.4, player.getX() - target.getX(), player.getZ() - target.getZ());
    }

    // ── Buff19：附近玩家使树叶掉落金苹果 ──────────────────────────────────────
    @SubscribeEvent
    public static void onBlockDropsBuff19(BlockDropsEvent event) {
        if (!event.getState().is(BlockTags.LEAVES)) return;
        ServerLevel level = event.getLevel();
        boolean any = !level.getEntitiesOfClass(ServerPlayer.class,
                new AABB(event.getPos()).inflate(32),
                p -> GameManager.hasEffect(p.getUUID(), "buff19")).isEmpty();
        if (!any) return;
        event.getDrops().forEach(e -> {
            if (e.getItem().is(Items.APPLE))
                e.setItem(new ItemStack(Items.GOLDEN_APPLE, e.getItem().getCount()));
        });
    }
    // ── Buff27：获得抗性提升1 ──────────────────────────────
    private static int buff27Ticker = 0;

    private static void tickBuff27(MinecraftServer server) {
        if (++buff27Ticker < 40) return;
        buff27Ticker = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!GameManager.hasEffect(player.getUUID(), "buff27")) continue;
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 0, false, false));
        }
    }

    // ── Buff29：空手右键村民进行搜刮 ──────────────────────────────
    private static final Map<UUID, Long> BUFF29_CD = new HashMap<>();

    @SubscribeEvent
    public static void onInteractVillagerBuff29(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!GameManager.hasEffect(player.getUUID(), "buff29")) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!player.getMainHandItem().isEmpty()) return;
        if (!(event.getTarget() instanceof Villager villager)) return;

        long now = System.currentTimeMillis();
        if (now - BUFF29_CD.getOrDefault(player.getUUID(), 0L) < 10_000L) return;
        BUFF29_CD.put(player.getUUID(), now);
        event.setCanceled(true);

        ServerLevel level = (ServerLevel) player.level();
        var rng = player.getRandom();
        // 搜刮绿宝石,面包以及铁锭
        player.getInventory().add(new ItemStack(Items.EMERALD, 1 + rng.nextInt(3)));
        if (rng.nextFloat() < 0.4f) player.getInventory().add(new ItemStack(Items.BREAD, 1 + rng.nextInt(4)));
        if (rng.nextFloat() < 0.2f) player.getInventory().add(new ItemStack(Items.IRON_INGOT, 1 + rng.nextInt(2)));
        if (rng.nextFloat() < 0.1f) player.getInventory().add(new ItemStack(Items.BLAZE_ROD, 1 + rng.nextInt(2)));

        level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                villager.getX(), villager.getEyeY(), villager.getZ(), 5, 0.3, 0.3, 0.3, 0.02);
    }

    // ── Buff33：吸血 ────────────────────────────
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) return;
        if (!GameManager.hasEffect(player.getUUID(), "buff33")) return;

        float healAmount = event.getNewDamage() * 0.2f;
        if (healAmount > 0 && player.getHealth() > 0)
            player.heal(healAmount);
    }

    // ── Buff34：受到伤害后恢复饱食度和饱和度 ────────────────────────────
    @SubscribeEvent
    private static void Buff34(LivingDamageEvent.Post event){
        if (event.getEntity() instanceof Player player) {
            if (!GameManager.hasEffect(player.getUUID(), "buff34")) return;
            if (player.level().isClientSide()) return;
            float damage = event.getNewDamage();
            player.getFoodData().eat((int) (damage * 0.5), (float) (damage * 0.5));
        }
    }
/*    private static final ResourceLocation SCALE_ID_2 = ResourceLocation.fromNamespaceAndPath("select", "buff35_scale");
    // ── Buff35：根据玩家饱腹度对玩家碰撞箱内的生物造成伤害 ────────────────────────────
    private static void tickBuff35(MinecraftServer server){
        for (Player player : server.getPlayerList().getPlayers()) {
            if (!GameManager.hasEffect(player.getUUID(), "buff35")) continue;
            var attr = player.getAttribute(Attributes.SCALE);
            if (attr == null) return;
            if (!attr.hasModifier(SCALE_ID_2))
                attr.addPermanentModifier(new AttributeModifier(SCALE_ID_2, 1 + player.getFoodData().getSaturationLevel(), AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
            List<LivingEntity> livingEntities = player.level().getEntitiesOfClass(
                    LivingEntity.class,
                    player.getBoundingBox()
            );
            for (LivingEntity entity : livingEntities) {
                if (entity == player) continue;
                if (entity.isAlive())
                    entity.hurt(player.damageSources().playerAttack(player),  player.getFoodData().getSaturationLevel());
            }
        }
    }*/

    // ── deBuff2：死后尸体爆炸 ──────────────────────────────
    @SubscribeEvent
    public static void onDeBuff2(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!GameManager.hasEffect(player.getUUID(), "debuff2")) return;
        if (player.isDeadOrDying()) player.level().explode(player, player.getX(), player.getY(), player.getZ(), 5, Level.ExplosionInteraction.MOB);
    }

    // ── deBuff3：玩家周围的动物变成兔子 ──────────────────────────────
    private static void tickDeBuff3(MinecraftServer server) {
        for (Player player : server.getPlayerList().getPlayers()) {
            if (!GameManager.hasEffect(player.getUUID(), "debuff3")) return;
            LivingEntity[] entitys = player.level().getEntitiesOfClass(LivingEntity.class,
                    player.getHitbox().inflate(8)).toArray(new LivingEntity[0]);
            for(LivingEntity e : entitys) {
                if (!(e instanceof Animal) || e instanceof Rabbit || e instanceof Hoglin) continue;
                Rabbit rabbit = EntityType.RABBIT.create(player.level());
                if (rabbit == null) continue;
                rabbit.moveTo(e.getX(), e.getY(), e.getZ());
                e.level().addFreshEntity(rabbit);
                e.discard();
            }
        }
    }

    // ── deBuff4：射中自己的弹射物会产生爆炸 ──────────────────────────────
    @SubscribeEvent
    public static void onDeBuff2(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!GameManager.hasEffect(player.getUUID(), "debuff4")) return;
        var src = event.getSource();
        if (src.getDirectEntity() instanceof Projectile)
            player.level().explode(src.getEntity(), player.getX(), player.getY(), player.getZ(), 1, Level.ExplosionInteraction.MOB);
    }

    // ── deBuff8：所乘的船会持续下沉 ──────────────────────────────
    // 在实体 tick 之后覆盖船的 y 速度，抵消原版浮力使其稳定下沉。
    @SubscribeEvent
    public static void onBoatTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.vehicle.Boat boat)) return;
        if (boat.level().isClientSide) return;
        boolean sink = boat.getPassengers().stream().anyMatch(
                e -> e instanceof ServerPlayer p && GameManager.hasEffect(p.getUUID(), "debuff8"));
        if (!sink) return;
        var motion = boat.getDeltaMovement();
        // 始终给一个向下的速度，水的阻力会让它平滑下沉而非瞬移
        boat.setDeltaMovement(motion.x, Math.min(motion.y, -0.18), motion.z);
        boat.resetFallDistance();
    }

    // ── deBuff10：无法使用工作台 ──────────────────────────────
    @SubscribeEvent
    public static void onUseCraftingTable(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!GameManager.hasEffect(player.getUUID(), "debuff10")) return;
        if (!event.getLevel().getBlockState(event.getPos()).is(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE)) return;
        event.setCanceled(true);
        player.displayClientMessage(Component.literal("你无法使用工作台").withStyle(net.minecraft.ChatFormatting.RED), true);
    }

    // ── deBuff11：只能在水下呼吸──────────────────────────────
    @SubscribeEvent
    public static void onDebuff11Breathe(LivingBreatheEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!GameManager.hasEffect(player.getUUID(), "debuff11")) return;
        if (player.isCreative() || player.isSpectator()) return;

        boolean headInWater = player.isEyeInFluid(FluidTags.WATER);
        event.setCanBreathe(headInWater);
    }

    // ── deBuff14：每隔段时间飞起来 ──────────────────────────────
    private static int debuff14Ticker = 0;
    private static void tickDeBuff14(MinecraftServer server) {
        if (++debuff14Ticker < 600) return;
        debuff14Ticker = 0;
        for (Player player : server.getPlayerList().getPlayers()) {
            if (!GameManager.hasEffect(player.getUUID(), "debuff14")) continue;
            player.setDeltaMovement(player.getDeltaMovement().add(0.0, 1.0, 0.0));
            player.hurtMarked = true;
        }
    }

    // ── debuff12：砍原木必须持斧头 ──────────────────────────────────────────
    @SubscribeEvent
    public static void onDebuff12BreakLog(net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        if (!GameManager.hasEffect(player.getUUID(), "debuff12")) return;
        if (!event.getState().is(BlockTags.LOGS)) return;
        if (player.getMainHandItem().is(net.minecraft.tags.ItemTags.AXES)) return;
        event.setCanceled(true);
        player.displayClientMessage(Component.literal("你需要拿着斧头才能砍树！").withStyle(net.minecraft.ChatFormatting.RED), true);
    }

    // ── debuff16：在水里难以行动 ───────────────────────────────────────────────
    private static void tickDebuff16(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!GameManager.hasEffect(player.getUUID(), "debuff16")) continue;
            if (player.isSpectator() || player.isCreative()) continue;
            if (!player.isInWater()) continue;
            if (player.isSwimming()) {
                player.setSwimming(false);
            }
            Vec3 motion = player.getDeltaMovement();
            if (motion.y > 0) {
                motion = new Vec3(motion.x, 0, motion.z);
            } else if (motion.y > -0.02) {
                motion = new Vec3(motion.x, -0.01, motion.z);
            }
            player.setDeltaMovement(motion);
            player.hurtMarked = true;
        }
    }

    // ── debuff20：中立生物主动攻击玩家 ─────────────────────────────────────
    private static int debuff20Ticker = 0;
    private static void tickDebuff20(MinecraftServer server) {
        if (++debuff20Ticker < 40) return;
        debuff20Ticker = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!GameManager.hasEffect(player.getUUID(), "debuff20")) continue;
            if (!(player.level() instanceof ServerLevel level)) continue;
            level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class,
                    player.getBoundingBox().inflate(20),
                    e -> e instanceof net.minecraft.world.entity.NeutralMob && e.getTarget() == null)
                    .forEach(mob -> mob.setTarget(player));
        }
    }

    // ── debuff21：受到的伤害翻倍 ────────────────────────────────────────────
    @SubscribeEvent
    public static void onDebuff21Damage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!GameManager.hasEffect(player.getUUID(), "debuff21")) return;
        event.setAmount(event.getAmount() * 2);
    }

    // ── debuff22：火焰无法自然熄灭 ─────────────────────────────────────────
    private static void tickDebuff22(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!GameManager.hasEffect(player.getUUID(), "debuff22")) continue;
            // 只要玩家正在燃烧（tick > 0），持续刷新计时，防止自然熄灭；入水不受影响
            if (player.getRemainingFireTicks() > 0)
                player.setRemainingFireTicks(40);
        }
    }

    // ── debuff23：使用床必定爆炸（模拟地狱睡觉效果） ──────────────────────────
    @SubscribeEvent
    public static void onDebuff23Bed(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!GameManager.hasEffect(player.getUUID(), "debuff23")) return;
        var level = event.getLevel();
        var pos = event.getPos();
        var state = level.getBlockState(pos);
        if (!state.is(net.minecraft.tags.BlockTags.BEDS)) return;
        event.setCanceled(true);

        if (state.getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
            var facing = state.getValue(net.minecraft.world.level.block.HorizontalDirectionalBlock.FACING);
            var part = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.BED_PART);
            // FOOT 朝 facing 找 HEAD，HEAD 朝反方向找 FOOT
            var otherPos = part == net.minecraft.world.level.block.state.properties.BedPart.FOOT
                    ? pos.relative(facing) : pos.relative(facing.getOpposite());
            level.removeBlock(pos, false);
            if (level.getBlockState(otherPos).is(net.minecraft.tags.BlockTags.BEDS)) {
                level.removeBlock(otherPos, false);
            }
        }
        var center = pos.getCenter();
        level.explode(null, level.damageSources().badRespawnPointExplosion(center),
                null, center, 5.0f, true, Level.ExplosionInteraction.BLOCK);
    }

    // ── debuff24：只能靠牛肉回复饱食度 ────────────────────────────────────────
    @SubscribeEvent
    public static void onDebuff24EatStart(LivingEntityUseItemEvent.Start event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!GameManager.hasEffect(player.getUUID(), "debuff24")) return;

        ItemStack item = event.getItem();
        if (item.get(net.minecraft.core.component.DataComponents.FOOD) == null) return;
        if (item.is(Items.BEEF) || item.is(Items.COOKED_BEEF)) return;
        event.setCanceled(true);
        player.displayClientMessage(
                Component.literal("你只能靠牛肉恢复饱食度！").withStyle(ChatFormatting.RED),
                true
        );
    }

    // ── debuff25：每 30s 随机播放一个 haha 音效 ────────────────────────────
    private static int debuff25Ticker = 0;
    private static final java.util.Random DEBUFF25_RNG = new java.util.Random();

    private static void tickDebuff25(MinecraftServer server) {
        if (++debuff25Ticker < 400) return;
        Random random = new Random();
        debuff25Ticker = random.nextInt(0,300);
        if (ModSounds.HAHA.isEmpty()) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!GameManager.hasEffect(player.getUUID(), "debuff25")) continue;
            if (!(player.level() instanceof ServerLevel)) continue;
            var sound = ModSounds.HAHA.get(DEBUFF25_RNG.nextInt(ModSounds.HAHA.size()));
            // 仅向该玩家播放，定位在其当前位置
            player.connection.send(new ClientboundSoundPacket(
                    sound,
                    SoundSource.MASTER,
                    player.getX(), player.getY(), player.getZ(),
                    1.0f,
                    0.8f + DEBUFF25_RNG.nextFloat() * 0.4f,
                    0
            ));
        }
    }

    // ── debuff26：附近 32 格内有玩家时，双方背景乐被替换为随机 music，各自 60s CD ──
    private static final int DEBUFF26_RANGE = 32;
    private static final int DEBUFF26_CD_TICKS = 1200;     // 60s
    private static final int DEBUFF26_MUSIC_TICKS = 1200;  // 替换乐持续 60s
    private static final Map<UUID, Long> DEBUFF26_CD = new HashMap<>();
    private static final java.util.Random DEBUFF26_RNG = new java.util.Random();
    private static final String[] DEBUFF26_MUSIC_IDS =
            { "music.spectre", "music.all_falls_down", "music.alone", "music.faded" };

    private static void tickDebuff26(MinecraftServer server) {
        var players = server.getPlayerList().getPlayers();
        long now = server.getTickCount();
        double rangeSq = (double) DEBUFF26_RANGE * DEBUFF26_RANGE;
        for (ServerPlayer self : players) {
            if (!GameManager.hasEffect(self.getUUID(), "debuff26")) continue;
            // 寻找一名同维度、32格内、且不在CD的其他玩家
            ServerPlayer other = null;
            for (ServerPlayer p : players) {
                if (p == self) continue;
                if (p.level() != self.level()) continue;
                if (p.distanceToSqr(self) > rangeSq) continue;
                if (isOnCd(p.getUUID(), now)) continue;  // other 必须不在 CD
                other = p;
                break;
            }
            if (other == null) continue;

            // 随机音乐 ID
            String musicId = DEBUFF26_MUSIC_IDS[DEBUFF26_RNG.nextInt(DEBUFF26_MUSIC_IDS.length)];

            // 给 other 播放并设置 CD（other 一定不在 CD）
            ReplaceMusicPacket packetOther = new ReplaceMusicPacket(musicId, DEBUFF26_MUSIC_TICKS);
            PacketDistributor.sendToPlayer(other, packetOther);
            DEBUFF26_CD.put(other.getUUID(), now);

            // 检查 self 是否在 CD
            if (!isOnCd(self.getUUID(), now)) {
                // self 不在 CD，也给 self 播放并设置 CD
                ReplaceMusicPacket packetSelf = new ReplaceMusicPacket(musicId, DEBUFF26_MUSIC_TICKS);
                PacketDistributor.sendToPlayer(self, packetSelf);
                DEBUFF26_CD.put(self.getUUID(), now);
                // 仅当 self 也被播放时，才发送旋转包
                SpinCameraPacket spin = new SpinCameraPacket();
                PacketDistributor.sendToPlayer(self, spin);
            }
            // 注意：如果 self 已在 CD 中，则不会发送音乐包和旋转包，只影响 other
        }
    }

    private static boolean isOnCd(UUID id, long now) {
        Long last = DEBUFF26_CD.get(id);
        return last != null && now - last < DEBUFF26_CD_TICKS;
    }

    // ── debuff27：持续饥饿效果 ──────────────────────────────────────────────
    private static void tickDebuff27(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!GameManager.hasEffect(player.getUUID(), "debuff27")) continue;
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 40, 0, false, false));
        }
    }

    // ── debuff28：物品持续获得消失诅咒 ─────────────────────────────────────
    private static int debuff28Ticker = 0;
    private static void tickDebuff28(MinecraftServer server) {
        if (++debuff28Ticker < 40) return;
        debuff28Ticker = 0;
        var enchantReg = server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var vanishing = enchantReg.getOrThrow(Enchantments.VANISHING_CURSE);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!GameManager.hasEffect(player.getUUID(), "debuff28")) continue;
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

    // ── debuff29：持续向玩家发送乱码消息 ───────────────────────────────────
    private static int debuff29Ticker = 0;
    private static final java.util.Random DEBUFF29_RNG = new java.util.Random();
    private static final String GARBLE_POOL =
            "!@#$%^&*()_+=[]{}|;':\",./<>?`~\\█▓▒░▀▄▌▐■□▪▫♦♣♥♠♀♂☺☻♪♫☼►◄↕↑↓→←∟↔▲▼⌂Ç";

    private static void tickDebuff29(MinecraftServer server) {
        if (++debuff29Ticker < 2) return;
        debuff29Ticker = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!GameManager.hasEffect(player.getUUID(), "debuff29")) continue;
            int len = 8 + DEBUFF29_RNG.nextInt(16);
            var sb = new StringBuilder(len);
            for (int i = 0; i < len; i++) sb.append(GARBLE_POOL.charAt(DEBUFF29_RNG.nextInt(GARBLE_POOL.length())));
            player.sendSystemMessage(Component.literal(sb.toString()));
        }
    }

    // ── debuff30：头顶密集灰色粒子 + 周身下雨粒子 ─────────────────────────
    private static int debuff30Ticker = 0;
    private static final DustParticleOptions GRAY_DUST =
            new DustParticleOptions(new Vector3f(0.45f, 0.45f, 0.45f), 0.8f);

    private static void tickDebuff30(MinecraftServer server) {
        if (++debuff30Ticker < 3) return;
        debuff30Ticker = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!GameManager.hasEffect(player.getUUID(), "debuff30")) continue;
            if (!(player.level() instanceof ServerLevel level)) continue;
            double x = player.getX(), y = player.getY(), z = player.getZ();
            double topY = y + player.getBbHeight() + 1.2;
            // 头顶密集灰色尘粒
            level.sendParticles(GRAY_DUST, x, topY, z, 300, 1.35, 0.25, 1.35, 0.01);
            // 周身下雨粒子（散布在以玩家为中心的3×3区域）
            RandomSource rng = player.getRandom();
            for (int i = 0; i < 100; i++) {
                double rx = x + (rng.nextDouble() * 4 - 2);
                double ry = topY - (rng.nextDouble() * 4);
                double rz = z + (rng.nextDouble() * 4 - 2);
                level.sendParticles(ParticleTypes.RAIN, rx, ry, rz, 1, 0, 0, 0, 0);
            }
        }
    }

    // ── debuff33：无法进入过黑的地方 ─────────────────────────
    private static final Map<UUID, Long> DEBUFF33_COOLDOWN = new HashMap<>();
    private static final int DEBUFF33_SEARCH_RADIUS = 32;
    private static final int DEBUFF33_COOLDOWN_TICKS = 100;

    private static void tickDebuff33(MinecraftServer server) {
        long now = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!GameManager.hasEffect(player.getUUID(), "debuff33")) continue;
            if (DEBUFF33_COOLDOWN.containsKey(player.getUUID())) {
                if (now - DEBUFF33_COOLDOWN.get(player.getUUID()) < DEBUFF33_COOLDOWN_TICKS) continue;
                DEBUFF33_COOLDOWN.remove(player.getUUID());
            }
            ServerLevel level = (ServerLevel) player.level();
            BlockPos pos = player.blockPosition();
            int totalLight = level.getMaxLocalRawBrightness(pos);
            if (totalLight > 0) continue;
            BlockPos target = findRandomSafePos(level, pos, DEBUFF33_SEARCH_RADIUS);
            if (target == null) continue;
            player.teleportTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
            level.playSound(null, target, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
            spawnTeleportParticles(level, target);
            DEBUFF33_COOLDOWN.put(player.getUUID(), now);
        }
    }

    private static BlockPos findRandomSafePos(ServerLevel level, BlockPos center, int radius) {
        Random random = new Random();
        for (int attempt = 0; attempt < 50; attempt++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            int x = center.getX() + dx;
            int z = center.getZ() + dz;
            int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
            if (topY < level.getMinBuildHeight() + 2) continue;
            BlockPos candidate = new BlockPos(x, topY, z);
            if (isSpaceAbove(level, candidate, 3)) {
                return candidate;
            }
        }
        return null;
    }
    private static boolean isSpaceAbove(ServerLevel level, BlockPos pos, int neededSpace) {
        for (int i = 1; i <= neededSpace; i++) {
            if (!level.isEmptyBlock(pos.above(i))) return false;
        }
        return true;
    }
    private static void spawnTeleportParticles(ServerLevel level, BlockPos pos) {
        Random random = new Random();
        for (int i = 0; i < 64; i++) {
            double angle = random.nextDouble() * 2 * Math.PI;
            double radius = 0.5 + random.nextDouble() * 1.5;
            double xOff = Math.cos(angle) * radius;
            double zOff = Math.sin(angle) * radius;
            double yOff = random.nextDouble() - 0.5;
            level.sendParticles(
                    ParticleTypes.PORTAL,
                    pos.getX() + 0.5 + xOff,
                    pos.getY() + 0.5 + yOff,
                    pos.getZ() + 0.5 + zOff,
                    0, 0, 0, 0, 0.5
            );
        }
    }

    private static final ResourceLocation SCALE_ID = ResourceLocation.fromNamespaceAndPath("select", "debuff34_scale");
    // ── debuff34：小僵尸，大麻烦！ ─────────────────────────
    private static void tickDeBuff34(MinecraftServer server) {
        for (Player player : server.getPlayerList().getPlayers()) {
            if (!GameManager.hasEffect(player.getUUID(), "debuff34")) return;
            Monster[] mobs = player.level().getEntitiesOfClass(Monster.class,
                    player.getHitbox().inflate(32)).toArray(new Monster[0]);
            for(Monster e : mobs) {
                var attr = e.getAttribute(Attributes.SCALE);
                if (attr == null) return;
                if (!attr.hasModifier(SCALE_ID))
                    attr.addPermanentModifier(new AttributeModifier(SCALE_ID, -0.75, AttributeModifier.Operation.ADD_VALUE));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20000, 4, false, true));
            }
        }
    }

    // ── deBuff35：玩家周围的兔子变成杀手兔 ──────────────────────────────
    private static void tickDeBuff35(MinecraftServer server) {
        for (Player player : server.getPlayerList().getPlayers()) {
            if (!GameManager.hasEffect(player.getUUID(), "debuff35")) continue;
            List<Rabbit> rabbits = player.level().getEntitiesOfClass(
                    Rabbit.class,
                    player.getBoundingBox().inflate(8)
            );
            for (Rabbit rabbit : rabbits) {
                if (rabbit.isAlive() && rabbit.getVariant() != Rabbit.Variant.EVIL)
                    rabbit.setVariant(Rabbit.Variant.EVIL);
            }
        }
    }
}

