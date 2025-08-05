package net.rezolv.obsidanum.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;
import net.rezolv.obsidanum.item.ItemsObs;
import net.rezolv.obsidanum.item.custom.EnchantedScroll;

import java.util.List;
import java.util.stream.Collectors;

public class ScrollShelf extends Block {

    public static final BooleanProperty HAS_SCROLL     = BooleanProperty.create("has_scroll");
    public static final BooleanProperty SCROLL_PRESENT = BooleanProperty.create("scroll_present");

    // Количество чар в одном свитке: от 1 до 3
    private static final int MIN_COUNT = 1;
    private static final int MAX_COUNT = 3;

    // Кешируем все зачарования (отфильтровав проклятия, если не нужны)
    private static final List<Enchantment> ALL_ENCHANTMENTS =
            ForgeRegistries.ENCHANTMENTS.getValues()
                    .stream()
                    .filter(e -> !e.isCurse())
                    .collect(Collectors.toList());

    public ScrollShelf(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(HAS_SCROLL, false)
                .setValue(SCROLL_PRESENT, false));
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide() && state.getValue(SCROLL_PRESENT)) {
            if (!player.getItemInHand(hand).isEmpty()) {
                return InteractionResult.PASS;
            }

            // Убираем свиток из полки
            level.setBlock(pos, state.setValue(SCROLL_PRESENT, false), 3);

            // Генерируем случайный свиток (25% шанс для каждого типа)
            ItemStack stack;
            int roll = level.random.nextInt(10); // 0-3

            switch (roll) {
                case 0 -> stack = new ItemStack(ItemsObs.ANCIENT_SCROLL.get());
                case 1 -> {
                    stack = new ItemStack(ItemsObs.ENCHANTED_SCROLL.get());
                    addRandomEnchantments(stack, level.random);
                }
                case 2 -> stack = new ItemStack(ItemsObs.UN_ORDER_REPAIR_SCROLL.get());
                case 3 -> stack = new ItemStack(ItemsObs.UN_ORDER_DESTRUCTION_SCROLL.get());
                case 4 -> stack = new ItemStack(ItemsObs.UN_ORDER_SCROLL.get());
                case 5 -> stack = new ItemStack(ItemsObs.UN_NETHER_SCROLL.get());
                case 6 -> stack = new ItemStack(ItemsObs.UN_CATACOMBS_SCROLL.get());
                case 7 -> stack = new ItemStack(ItemsObs.UN_ORDER_SCROLL_UP.get());
                case 8 -> stack = new ItemStack(ItemsObs.UN_NETHER_SCROLL_UP.get());
                case 9 -> stack = new ItemStack(ItemsObs.UN_CATACOMBS_SCROLL_UP.get());
                default -> stack = ItemStack.EMPTY; // На всякий случай
            }

            // Отдаём игроку
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }

            player.swing(hand);
            level.playSound(null, pos,
                    SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.5F, 1.0F);

            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** Добавляем в scroll от 1 до 3 случайных чар из всего реестра */
    private void addRandomEnchantments(ItemStack scroll, RandomSource random) {
        int count = MIN_COUNT + random.nextInt(MAX_COUNT - MIN_COUNT + 1);
        for (int i = 0; i < count; i++) {
            // Берём случайное зачарование
            Enchantment enchantment = ALL_ENCHANTMENTS.get(random.nextInt(ALL_ENCHANTMENTS.size()));

            // Диапазон уровней у чарки
            int enchMin = enchantment.getMinLevel();   // обычно ≥1
            int enchMax = enchantment.getMaxLevel();   // может быть 1–5

            // Рандомим уровень в этом диапазоне
            int level = enchMin + random.nextInt(enchMax - enchMin + 1);

            // Записываем в ваш собственный тег StoredEnchantments
            EnchantedScroll.addEnchantment(scroll, new EnchantmentInstance(enchantment, level));
        }
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
                        BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(state.getBlock()) && !level.isClientSide()) {
            BlockState newState = state
                    .setValue(HAS_SCROLL, false)
                    .setValue(SCROLL_PRESENT, false);

            Player player = level.getNearestPlayer(
                    pos.getX(), pos.getY(), pos.getZ(), 10, null
            );
            // 90% шанс свитка при креативе
            if (player != null && player.isCreative()
                    && level.random.nextFloat() < 0.1f) {
                newState = newState
                        .setValue(HAS_SCROLL, true)
                        .setValue(SCROLL_PRESENT, true);
            }

            if (!newState.equals(state)) {
                level.setBlock(pos, newState, 3);
            }
        }
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder
    ) {
        builder.add(HAS_SCROLL, SCROLL_PRESENT);
    }

    @Override
    public float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos) {
        return 2;
    }
}
