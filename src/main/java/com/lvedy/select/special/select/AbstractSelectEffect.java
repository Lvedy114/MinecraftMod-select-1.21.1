package com.lvedy.select.special.select;

/**
 * 效果基类，封装 id、显示名、描述、是否增益、是否显示等公共属性，
 * 子类只需在构造时传入这些值并实现 apply 逻辑。
 */
public abstract class AbstractSelectEffect implements SelectEffect {

    private final String id;
    private final String displayName;
    private final String description;
    private final boolean buff;
    private final boolean visible;

    protected AbstractSelectEffect(String id, String displayName, String description,
                                   boolean buff, boolean visible) {
        this.id = id;
        this.displayName = displayName;
        this.description = description;
        this.buff = buff;
        this.visible = visible;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isBuff() {
        return buff;
    }

    @Override
    public boolean isVisible() {
        return visible;
    }
}
