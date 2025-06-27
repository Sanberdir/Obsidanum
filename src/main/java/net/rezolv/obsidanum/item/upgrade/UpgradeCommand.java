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
import java.util.Set;

public class UpgradeCommand {

    public static final SuggestionProvider<CommandSourceStack> ADD_SUGGEST = (ctx, builder) -> {
        try {
            Player player = ctx.getSource().getPlayerOrException();
            ItemStack stack = player.getMainHandItem();

            if (stack.getItem() instanceof IUpgradeableItem) {
                IUpgradeableItem upgradable = (IUpgradeableItem) stack.getItem();

                // Предлагаем только разрешенные улучшения
                for (ObsidanumToolUpgrades upg : ObsidanumToolUpgrades.values()) {
                    if (upgradable.isUpgradeAllowed(upg)) {
                        builder.suggest(upg.getName());
                    }
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
                                .then(Commands.literal("all")
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

        // Проверка, что предмет поддерживает улучшения
        if (!(stack.getItem() instanceof IUpgradeableItem)) {
            player.displayClientMessage(Component.translatable(TranslationKeysCommand.COMMAND_NOT_UPGRADEABLE), false);
            return Command.SINGLE_SUCCESS;
        }

        // Поиск улучшения по имени
        ObsidanumToolUpgrades upgrade = findUpgradeByName(upgradeName);
        if (upgrade == null) {
            player.displayClientMessage(Component.translatable(TranslationKeysCommand.COMMAND_UNKNOWN_UPGRADE, upgradeName), false);
            return Command.SINGLE_SUCCESS;
        }

        IUpgradeableItem upgradable = (IUpgradeableItem) stack.getItem();

        // Проверка, разрешено ли улучшение для этого предмета
        if (!upgradable.isUpgradeAllowed(upgrade)) {
            player.displayClientMessage(
                    Component.translatable(TranslationKeysCommand.COMMAND_UPGRADE_NOT_ALLOWED)
                            .withStyle(ChatFormatting.RED),
                    false
            );
            return Command.SINGLE_SUCCESS;
        }

        // Получаем текущие улучшения
        Map<ObsidanumToolUpgrades, Integer> currentUpgrades = upgradable.getUpgrades(stack);
        int currentLevel = currentUpgrades.getOrDefault(upgrade, 0);

        // Проверка конфликтов с другими улучшениями (только если добавляем новое улучшение)
        if (currentLevel == 0) {
            Set<ObsidanumToolUpgrades> exclusiveGroup = getExclusiveGroup(upgrade);

            if (exclusiveGroup != null) {
                for (ObsidanumToolUpgrades existingUpgrade : currentUpgrades.keySet()) {
                    // Проверяем только если это другое улучшение из той же группы
                    if (exclusiveGroup.contains(existingUpgrade) && existingUpgrade != upgrade) {
                        player.displayClientMessage(
                                Component.translatable(TranslationKeysCommand.COMMAND_UPGRADE_CONFLICT,
                                                upgradeName, existingUpgrade.getName())
                                        .withStyle(ChatFormatting.RED),
                                false
                        );
                        return Command.SINGLE_SUCCESS;
                    }
                }
            }
        }

        // Проверка допустимого уровня улучшения
        int maxLevel = UpgradeLibrary.getMaxLevel(upgrade);
        if (level < 1 || level > maxLevel) {
            player.displayClientMessage(
                    Component.translatable(TranslationKeysCommand.COMMAND_INVALID_LEVEL, level, 1, maxLevel)
                            .withStyle(ChatFormatting.RED),
                    false
            );
            return Command.SINGLE_SUCCESS;
        }

        // Проверка ограничения по количеству слотов
        int usedSlots = upgradable.getUsedSlots(stack);
        int newSlots = usedSlots - currentLevel + level;

        if (newSlots > IUpgradeableItem.MAX_UPGRADE_SLOTS) {
            player.displayClientMessage(
                    Component.translatable(TranslationKeysCommand.COMMAND_TOO_MANY_UPGRADES,
                                    IUpgradeableItem.MAX_UPGRADE_SLOTS)
                            .withStyle(ChatFormatting.RED),
                    false
            );
            return Command.SINGLE_SUCCESS;
        }

        // Добавление/обновление улучшения
        upgradable.addUpgrade(stack, upgrade, level);

        // Успешное добавление/обновление
        player.displayClientMessage(
                Component.translatable(TranslationKeysCommand.COMMAND_UPGRADE_ADDED, upgradeName, level)
                        .withStyle(ChatFormatting.GOLD),
                false
        );
        return Command.SINGLE_SUCCESS;
    }


    // Метод для получения группы взаимоисключающих улучшений
    private static Set<ObsidanumToolUpgrades> getExclusiveGroup(ObsidanumToolUpgrades upgrade) {
        // HARVESTER и ARCHAEOLOGIST - взаимоисключающие
        if (upgrade == ObsidanumToolUpgrades.HARVESTER || upgrade == ObsidanumToolUpgrades.ARCHAEOLOGIST) {
            return Set.of(ObsidanumToolUpgrades.HARVESTER, ObsidanumToolUpgrades.ARCHAEOLOGIST);
        }
        return null;
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
            player.displayClientMessage(Component.translatable(TranslationKeysCommand.COMMAND_NOT_UPGRADEABLE), false);
            return Command.SINGLE_SUCCESS;
        }

        ObsidanumToolUpgrades upgrade = findUpgradeByName(upgradeName);
        if (upgrade == null) {
            player.displayClientMessage(Component.translatable(TranslationKeysCommand.COMMAND_UNKNOWN_UPGRADE, upgradeName), false);
            return Command.SINGLE_SUCCESS;
        }

        IUpgradeableItem upgradable = (IUpgradeableItem) stack.getItem();
        upgradable.removeUpgrade(stack, upgrade);

        player.displayClientMessage(
                Component.translatable(TranslationKeysCommand.COMMAND_UPGRADE_REMOVED, upgradeName)
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
            player.displayClientMessage(Component.translatable(TranslationKeysCommand.COMMAND_NOT_UPGRADEABLE), false);
            return Command.SINGLE_SUCCESS;
        }

        IUpgradeableItem upgradable = (IUpgradeableItem) stack.getItem();
        Map<ObsidanumToolUpgrades, Integer> upgrades = upgradable.getUpgrades(stack);

        if (upgrades.isEmpty()) {
            player.displayClientMessage(
                    Component.translatable(TranslationKeysCommand.COMMAND_NO_UPGRADES)
                            .withStyle(ChatFormatting.YELLOW),
                    false
            );
            return Command.SINGLE_SUCCESS;
        }

        upgradable.removeAllUpgrades(stack);

        player.displayClientMessage(
                Component.translatable(TranslationKeysCommand.COMMAND_ALL_UPGRADES_REMOVED, upgrades.size())
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
