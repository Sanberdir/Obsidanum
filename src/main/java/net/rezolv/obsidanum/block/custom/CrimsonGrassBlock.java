package net.rezolv.obsidanum.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.rezolv.obsidanum.block.BlocksObs;

public class CrimsonGrassBlock extends SpreadingSnowyAlchemicalDirtBlock  implements BonemealableBlock {
    public CrimsonGrassBlock(Properties properties) {
        super(properties);
    }
    // Проверка условий для применения костной муки
    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient) {
        // Костную муку можно использовать, если сверху есть воздух
        return level.getBlockState(pos.above()).isAir();
    }

    // Всегда успешное применение (если условия выше выполнены)
    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    // Действие при применении костной муки
    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        BlockPos topPos = pos.above();
        BlockState grassState = BlocksObs.CRIMSON_GRASS.get().defaultBlockState();

        // 128 попыток генерации травы вокруг
        for (int i = 0; i < 128; ++i) {
            // Случайное смещение позиции
            BlockPos currentPos = topPos.offset(
                    random.nextInt(7) - 3, // Смещение по X (-3, -2, -1, 0, 1, 2, 3)
                    random.nextInt(3) - 1, // Смещение по Y (-1, 0, 1)
                    random.nextInt(7) - 3  // Смещение по Z (-3, -2, -1, 0, 1, 2, 3)
            );

            // Проверка, что под текущей позицией находится наш блок
            if (!level.getBlockState(currentPos.below()).is(this)) {
                continue; // Пропуск попытки, если под позицией не наш блок
            }

            // Проверка, что текущая позиция свободна
            if (!level.getBlockState(currentPos).isAir()) {
                continue; // Пропуск попытки, если позиция занята
            }

            // Если на текущей позиции уже есть наша трава, усиливаем её с шансом 10%
            BlockState currentState = level.getBlockState(currentPos);
            if (currentState.is(grassState.getBlock()) && random.nextInt(10) == 0) {
                ((BonemealableBlock) grassState.getBlock()).performBonemeal(level, random, currentPos, currentState);
            }

            // Если позиция свободна, размещаем кастомную траву
            if (currentState.isAir()) {
                level.setBlock(currentPos, grassState, 3); // 3 — флаг для обновления блока
            }
        }
    }
}
