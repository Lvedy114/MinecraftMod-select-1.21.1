package com.lvedy.select.special.select;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 所有选项效果的注册中心。
 * 提供全部效果列表、按 ID 查找，以及随机生成两个「buff+debuff 组合」选项。
 *
 * 新增效果步骤：
 *   1. 在 Buffs.java 或 Debuffs.java 中添加新的静态内部类
 *   2. 在下方 static 块中调用 register(new Buffs.BuffX()) 或 register(new Debuffs.DebuffX())
 */
public class SelectEffects {

    private static final List<SelectEffect> ALL = new ArrayList<>();
    private static final List<SelectEffect> BUFFS = new ArrayList<>();
    private static final List<SelectEffect> DEBUFFS = new ArrayList<>();
    private static final Map<String, SelectEffect> BY_ID = new HashMap<>();

    static {
        // ── 增益效果 ────────────────────────────────────────────────
        register(new Buffs.Buff1());
        register(new Buffs.Buff2());
        register(new Buffs.Buff3());
        register(new Buffs.Buff4());
        register(new Buffs.Buff5());
        register(new Buffs.Buff6());
        register(new Buffs.Buff7());
        register(new Buffs.Buff8());
        register(new Buffs.Buff9());
        register(new Buffs.Buff10());
        register(new Buffs.Buff11());
        register(new Buffs.Buff12());
        register(new Buffs.Buff13());
        register(new Buffs.Buff14());
        register(new Buffs.Buff15());
        register(new Buffs.Buff16());
        register(new Buffs.Buff17());
        register(new Buffs.Buff18());
        register(new Buffs.Buff19());
        register(new Buffs.Buff20());
        register(new Buffs.Buff21());
        register(new Buffs.Buff22());
        register(new Buffs.Buff23());
        register(new Buffs.Buff24());
        register(new Buffs.Buff25());
        register(new Buffs.Buff26());
        register(new Buffs.Buff27());
        register(new Buffs.Buff28());
        register(new Buffs.Buff29());
        register(new Buffs.Buff30());
        register(new Buffs.Buff31());
        register(new Buffs.Buff32());
        register(new Buffs.Buff33());
        register(new Buffs.Buff34());
        //register(new Buffs.Buff35());


        // ── 负面效果 ────────────────────────────────────────────────
        register(new Debuffs.Debuff1());
        register(new Debuffs.Debuff2());
        register(new Debuffs.Debuff3());
        register(new Debuffs.Debuff4());
        register(new Debuffs.Debuff5());
        register(new Debuffs.Debuff6());
        register(new Debuffs.Debuff7());
        register(new Debuffs.Debuff8());
        register(new Debuffs.Debuff9());
        register(new Debuffs.Debuff10());
        register(new Debuffs.Debuff11());
        register(new Debuffs.Debuff12());
        register(new Debuffs.Debuff13());
        register(new Debuffs.Debuff14());
        register(new Debuffs.Debuff15());
        register(new Debuffs.Debuff16());
        register(new Debuffs.Debuff17());
        register(new Debuffs.Debuff18());
        register(new Debuffs.Debuff19());
        register(new Debuffs.Debuff20());
        register(new Debuffs.Debuff21());
        register(new Debuffs.Debuff22());
        register(new Debuffs.Debuff23());
        register(new Debuffs.Debuff24());
        register(new Debuffs.Debuff25());
        register(new Debuffs.Debuff26());
        register(new Debuffs.Debuff27());
        register(new Debuffs.Debuff28());
        register(new Debuffs.Debuff29());
        register(new Debuffs.Debuff30());
        register(new Debuffs.DeBuff31());
        register(new Debuffs.DeBuff32());
        register(new Debuffs.DeBuff33());
        register(new Debuffs.DeBuff34());
        register(new Debuffs.DeBuff35());
    }

    private static void register(SelectEffect effect) {
        ALL.add(effect);
        BY_ID.put(effect.getId(), effect);
        if (effect.isBuff()) {
            BUFFS.add(effect);
        } else {
            DEBUFFS.add(effect);
        }
    }

    public static List<SelectEffect> all() {
        return Collections.unmodifiableList(ALL);
    }

    public static SelectEffect byId(String id) {
        return BY_ID.get(id);
    }

    /**
     * 随机生成两个选项，每个选项由一个 buff 和一个 debuff 组成。
     * 两个选项使用互不相同的 buff 和互不相同的 debuff。
     * <p>
     * visible=true 的效果为「非一次性」效果，玩家已拥有后不再出现在候选池中；
     * visible=false 的效果为「一次性」效果，每次可反复出现。
     *
     * @param owned 该玩家当前已拥有的全部效果
     */
    public static SelectOption[] pickTwoOptions(Collection<SelectEffect> owned) {
        Set<String> ownedNonDisposable = new HashSet<>();
        for (SelectEffect e : owned) {
            if (e.isVisible()) ownedNonDisposable.add(e.getId());
        }

        List<SelectEffect> buffPool = new ArrayList<>();
        for (SelectEffect e : BUFFS) {
            if (!e.isVisible() || !ownedNonDisposable.contains(e.getId())) buffPool.add(e);
        }
        List<SelectEffect> debuffPool = new ArrayList<>();
        for (SelectEffect e : DEBUFFS) {
            if (!e.isVisible() || !ownedNonDisposable.contains(e.getId())) debuffPool.add(e);
        }

        Collections.shuffle(buffPool, ThreadLocalRandom.current());
        Collections.shuffle(debuffPool, ThreadLocalRandom.current());

        SelectOption optionA = new SelectOption(buffPool.get(0), debuffPool.get(0));
        SelectOption optionB = new SelectOption(buffPool.get(1), debuffPool.get(1));
        return new SelectOption[]{ optionA, optionB };
    }
}
