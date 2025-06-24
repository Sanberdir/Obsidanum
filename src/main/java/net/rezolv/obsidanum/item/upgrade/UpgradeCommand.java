package net.rezolv.obsidanum.item.upgrade;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Map;

public class UpgradeCommand {

    public static final SuggestionProvider<CommandSourceStack> ADD_SUGGEST = (ctx, builder) -> {
        // Предлагаем все возможные улучшения
        for (ObsidanumToolUpgrades upg : ObsidanumToolUpgrades.values()) {
            builder.suggest(upg.getName());
        }
        return builder.buildFuture();
    };

    public static final SuggestionProvider<CommandSourceStack> REMOVE_SUGGEST = (ctx, builder) -> {
        try {
            Player player = ctx.getSource().getPlayerOrException();
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof IUpgradeableItem) {
                IUpgradeableItem upgradable = (IUpgradeableItem) stack.getItem();
                Map<ObsidanumToolUpgrades, Integer> upgrades = upgradable.getUpgrades(stack);
                for (ObsidanumToolUpgrades upg : upgrades.keySet()) {
                    builder.suggest(upg.getName());
                }
            }
        } catch (CommandSyntaxException ignored) {
        }
        return builder.buildFuture();
    };

    public static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("tool_upgrade")
                        .then(Commands.literal("add")
                                .then(Commands.argument("upgrade", StringArgumentType.word())
                                        .suggests(ADD_SUGGEST)
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                .executes(ctx -> executeAdd(ctx,
                                                        StringArgumentType.getString(ctx, "upgrade"),
                                                        IntegerArgumentType.getInteger(ctx, "level")))
                                        )
                                        .executes(ctx -> executeAdd(ctx, StringArgumentType.getString(ctx, "upgrade"), 1))
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("upgrade", StringArgumentType.word())
                                        .suggests(REMOVE_SUGGEST)
                                        .executes(ctx -> executeRemove(ctx, StringArgumentType.getString(ctx, "upgrade")))
                                )
                                .then(Commands.literal("all") // Новая команда для удаления всех
                                        .executes(UpgradeCommand::executeRemoveAll)
                                )
                        ));
    }


    private static int executeAdd(CommandContext<CommandSourceStack> ctx, String upgradeName, int level) {
        Player player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
        ItemStack stack = player.getMainHandItem();

        if (!(stack.getItem() instanceof IUpgradeableItem)) {
            player.displayClientMessage(Component.literal("Этот предмет нельзя улучшить!"), false);
            return Command.SINGLE_SUCCESS;
        }

        ObsidanumToolUpgrades upgrade = findUpgradeByName(upgradeName);
        if (upgrade == null) {
            player.displayClientMessage(Component.literal("Неизвестное улучшение: " + upgradeName), false);
            return Command.SINGLE_SUCCESS;
        }

        int maxLevel = UpgradeLibrary.getMaxLevel(upgrade);
        if (level < 1 || level > maxLevel) {
            player.displayClientMessage(
                    Component.literal("Недопустимый уровень: " + level + ". Должен быть между 1 и " + maxLevel + ".")
                            .withStyle(ChatFormatting.RED),
                    false
            );
            return Command.SINGLE_SUCCESS;
        }

        IUpgradeableItem upgradable = (IUpgradeableItem) stack.getItem();
        upgradable.addUpgrade(stack, upgrade, level);

        player.displayClientMessage(
                Component.literal("Добавлено улучшение: " + upgradeName + " (уровень " + level + ")")
                        .withStyle(ChatFormatting.GOLD),
                false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int executeRemove(CommandContext<CommandSourceStack> ctx, String upgradeName) {
        Player player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
        ItemStack stack = player.getMainHandItem();

        if (!(stack.getItem() instanceof IUpgradeableItem)) {
            player.displayClientMessage(Component.literal("Этот предмет нельзя улучшить!"), false);
            return Command.SINGLE_SUCCESS;
        }

        ObsidanumToolUpgrades upgrade = findUpgradeByName(upgradeName);
        if (upgrade == null) {
            player.displayClientMessage(Component.literal("Неизвестное улучшение: " + upgradeName), false);
            return Command.SINGLE_SUCCESS;
        }

        IUpgradeableItem upgradable = (IUpgradeableItem) stack.getItem();
        upgradable.removeUpgrade(stack, upgrade);

        player.displayClientMessage(
                Component.literal("Удалено улучшение: " + upgradeName)
                        .withStyle(ChatFormatting.GOLD),
                false
        );
        return Command.SINGLE_SUCCESS;
    }

    private static int executeRemoveAll(CommandContext<CommandSourceStack> ctx) {
        Player player;
        try {
            player = ctx.getSource().getPlayerOrException();
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
        ItemStack stack = player.getMainHandItem();

        if (!(stack.getItem() instanceof IUpgradeableItem)) {
            player.displayClientMessage(Component.literal("Этот предмет нельзя улучшить!"), false);
            return Command.SINGLE_SUCCESS;
        }

        IUpgradeableItem upgradable = (IUpgradeableItem) stack.getItem();

        // Получаем все улучшения для отображения в сообщении
        Map<ObsidanumToolUpgrades, Integer> upgrades = upgradable.getUpgrades(stack);

        if (upgrades.isEmpty()) {
            player.displayClientMessage(
                    Component.literal("На предмете нет улучшений")
                            .withStyle(ChatFormatting.YELLOW),
                    false
            );
            return Command.SINGLE_SUCCESS;
        }

        // Удаляем все улучшения
        upgradable.removeAllUpgrades(stack);

        player.displayClientMessage(
                Component.literal("Все улучшения удалены (" + upgrades.size() + " шт.)")
                        .withStyle(ChatFormatting.GOLD),
                false
        );
        return Command.SINGLE_SUCCESS;
    }


    private static ObsidanumToolUpgrades findUpgradeByName(String name) {
        for (ObsidanumToolUpgrades upg : ObsidanumToolUpgrades.values()) {
            if (upg.getName().equalsIgnoreCase(name)) {
                return upg;
            }
        }
        return null;
    }
}
