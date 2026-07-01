package com.lvedy.select.special.select;

/**
 * 一个选项，由一个增益效果和一个负面效果组合而成。
 * 选项上方文本框显示 buff 描述，下方文本框显示 debuff 描述。
 */
public record SelectOption(SelectEffect buff, SelectEffect debuff) {
}
