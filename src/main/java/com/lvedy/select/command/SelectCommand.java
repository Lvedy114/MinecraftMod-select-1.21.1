package com.lvedy.select.command;

import com.lvedy.select.config.Config;
import com.lvedy.select.game.GameManager;
import com.lvedy.select.network.NetworkHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class SelectCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("select")
                        .requires(source -> source.hasPermission(2))
                        // /select start
                        .then(Commands.literal("start").executes(ctx -> {
                            if (GameManager.isRunning()) {
                                ctx.getSource().sendFailure(Component.literal("游戏已经在运行中。"));
                                return 0;
                            }
                            GameManager.start();
                            ctx.getSource().sendSuccess(() -> Component.literal("游戏已开始：选项将按配置的间隔定期弹出。"), true);
                            return 1;
                        }))
                        // /select stop
                        .then(Commands.literal("stop").executes(ctx -> {
                            if (!GameManager.isRunning()) {
                                ctx.getSource().sendFailure(Component.literal("游戏当前并未运行。"));
                                return 0;
                            }
                            GameManager.stop();
                            ctx.getSource().sendSuccess(() -> Component.literal("游戏已暂停：计时与已有效果保留。"), true);
                            return 1;
                        }))
                        // /select clear
                        .then(Commands.literal("clear").executes(ctx -> {
                            GameManager.clear(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal("已清除所有玩家的效果并重置计时。"), true);
                            return 1;
                        }))
                        // /select give —— 打开给予效果测试界面
                        .then(Commands.literal("give").executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            NetworkHandler.sendOpenGiveScreen(player);
                            return 1;
                        }))
                        // /select reload —— 重新读取配置文件
                        .then(Commands.literal("reload").executes(ctx -> {
                            Config.reload();
                            ctx.getSource().sendSuccess(() -> Component.literal(
                                    "配置已重新读取。当前值 — 选择间隔：" + Config.SELECTION_INTERVAL.getAsInt()
                                    + " 秒，选择超时：" + Config.SELECTION_TIME_LIMIT.getAsInt() + " 秒"), true);
                            return 1;
                        }))
                        // /select config select <秒> / timeout <秒>
                        .then(Commands.literal("config")
                                .then(Commands.literal("select")
                                        .then(Commands.argument("秒", IntegerArgumentType.integer(10))
                                                .executes(ctx -> {
                                                    int val = IntegerArgumentType.getInteger(ctx, "秒");
                                                    Config.setSelectionInterval(val);
                                                    ctx.getSource().sendSuccess(() -> Component.literal("选择间隔已设置为 " + val + " 秒。"), true);
                                                    return 1;
                                                })))
                                .then(Commands.literal("timeout")
                                        .then(Commands.argument("秒", IntegerArgumentType.integer(5))
                                                .executes(ctx -> {
                                                    int val = IntegerArgumentType.getInteger(ctx, "秒");
                                                    Config.setSelectionTimeLimit(val);
                                                    ctx.getSource().sendSuccess(() -> Component.literal("选择超时已设置为 " + val + " 秒。"), true);
                                                    return 1;
                                                }))))
        );
    }
}

