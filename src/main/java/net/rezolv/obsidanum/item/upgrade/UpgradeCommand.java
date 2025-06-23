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

public class UpgradeCommand {

    public static final SuggestionProvider<CommandSourceStack> ADD_SUGGEST = (ctx, builder) -> {
        try {
            Player player = ctx.getSource().getPlayerOrException();
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof IUpgradeableItem) {
                IUpgradeableItem upgradable = (IUpgradeableItem) stack.getItem();
                ObsidanumToolUpgrades current = upgradable.getUpgrade(stack);
                for (ObsidanumToolUpgrades upg : ObsidanumToolUpgrades.values()) {
                    if (current == null || upg != current) builder.suggest(upg.getName());
                }
            }
        } catch (CommandSyntaxException ignored) {
        }
        return builder.buildFuture();
    };

    public static final SuggestionProvider<CommandSourceStack> REMOVE_SUGGEST = (ctx, builder) -> {
        try {
            Player player = ctx.getSource().getPlayerOrException();
            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof IUpgradeableItem) {
                IUpgradeableItem upgradable = (IUpgradeableItem) stack.getItem();
                ObsidanumToolUpgrades current = upgradable.getUpgrade(stack);
                if (current != null) builder.suggest(current.getName());
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
                                        // Добавляем необязательный аргумент уровня (от 1 и выше)
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                .executes(ctx -> executeAdd(ctx,
                                                        StringArgumentType.getString(ctx, "upgrade"),
                                                        IntegerArgumentType.getInteger(ctx, "level")))
                                        )
                                        .executes(ctx -> executeAdd(ctx, StringArgumentType.getString(ctx, "upgrade"), 1)) // уровень по умолчанию 1
                                )
                        )
                        .then(Commands.literal("remove")
                                .then(Commands.argument("upgrade", StringArgumentType.word())
                                        .suggests(REMOVE_SUGGEST)
                                        .executes(ctx -> executeRemove(ctx, StringArgumentType.getString(ctx, "upgrade")))
                        )
                )
        );
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
            player.displayClientMessage(Component.literal("This item cannot be upgraded!"), false);
            return Command.SINGLE_SUCCESS;
        }

        ObsidanumToolUpgrades upgrade = findUpgradeByName(upgradeName);
        if (upgrade == null) {
            player.displayClientMessage(Component.literal("Unknown upgrade: " + upgradeName), false);
            return Command.SINGLE_SUCCESS;
        }

        // 1) Получаем максимальный уровень из библиотеки
        int maxLevel = UpgradeLibrary.getMaxLevel(upgrade);
        if (level < 1 || level > maxLevel) {
            player.displayClientMessage(
                    Component.literal("Invalid level: " + level + ". Must be between 1 and " + maxLevel + ".")
                            .withStyle(ChatFormatting.RED),
                    false
            );
            return Command.SINGLE_SUCCESS;
        }

        IUpgradeableItem upgradable = (IUpgradeableItem) stack.getItem();

        // 2) Устанавливаем апгрейд с проверенным уровнем
        upgradable.setUpgrade(stack, upgrade, level);

        player.displayClientMessage(
                Component.literal("Upgrade " + upgradeName + " added with level " + level + ".")
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
            player.displayClientMessage(Component.literal("This item cannot be upgraded!"), false);
            return Command.SINGLE_SUCCESS;
        }

        ObsidanumToolUpgrades upgrade = findUpgradeByName(upgradeName);
        if (upgrade == null) {
            player.displayClientMessage(Component.literal("Unknown upgrade: " + upgradeName), false);
            return Command.SINGLE_SUCCESS;
        }

        IUpgradeableItem upgradable = (IUpgradeableItem) stack.getItem();
        if (upgrade.equals(upgradable.getUpgrade(stack))) {
            upgradable.removeUpgrade(stack);
            player.displayClientMessage(Component.literal("Upgrade " + upgradeName + " removed."), false);
        } else {
            player.displayClientMessage(Component.literal("This upgrade is not present on the item."), false);
        }

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
