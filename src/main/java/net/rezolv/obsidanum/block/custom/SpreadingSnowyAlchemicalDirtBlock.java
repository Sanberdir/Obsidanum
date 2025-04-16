package net.rezolv.obsidanum.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LightEngine;
import net.rezolv.obsidanum.block.BlocksObs;

public class SpreadingSnowyAlchemicalDirtBlock extends SnowyAlchemicalDirtBlock {
    protected SpreadingSnowyAlchemicalDirtBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    // Проверяет, может ли блок оставаться "травой" (условия освещения и окружения)
    private static boolean canBeGrass(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);

        // Если сверху снег (1 слой), разрешить
        if (aboveState.is(Blocks.SNOW) && aboveState.getValue(SnowLayerBlock.LAYERS) == 1) {
            return true;
        }
        // Если жидкость (например, вода) сверху, запретить
        else if (aboveState.getFluidState().getAmount() == 8) {
            return false;
        }
        // Проверка уровня освещения
        else {
            // Вычисление уровня света, попадающего на блок
            int lightLevel = LightEngine.getLightBlockInto(
                    level, state, pos,
                    aboveState, abovePos, Direction.UP,
                    aboveState.getLightBlock(level, abovePos)
            );
            return lightLevel < level.getMaxLightLevel();
        }
    }

    // Проверяет возможность распространения на указанную позицию
    private static boolean canPropagate(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos abovePos = pos.above();
        // Должны выполняться условия для травы и отсутствия воды сверху
        return canBeGrass(state, level, pos) && !level.getFluidState(abovePos).is(FluidTags.WATER);
    }

    // Обработка случайных обновлений блока (вызывается периодически)
    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canBeGrass(state, level, pos)) {
            // Если условия не выполняются, превращаемся в грязь
            if (!level.isAreaLoaded(pos, 1)) return; // Проверка загрузки чанка
            level.setBlockAndUpdate(pos, BlocksObs.ALCHEMICAL_DIRT.get().defaultBlockState());
        } else {
            if (!level.isAreaLoaded(pos, 3)) return;

            // Распространение при достаточном освещении
            BlockState targetState = this.defaultBlockState();

            // 4 попытки распространения в случайные стороны
            for (int i = 0; i < 4; i++) {
                // Случайное смещение в пределах [-1,1] по X/Z и [-3,1] по Y
                BlockPos spreadPos = pos.offset(
                        random.nextInt(3) - 1, // -1, 0, +1
                        random.nextInt(5) - 3, // -3, -2, -1, 0, +1
                        random.nextInt(3) - 1
                );

                // Если найден блок грязи и можно распространиться
                if (level.getBlockState(spreadPos).is(BlocksObs.ALCHEMICAL_DIRT.get())
                        && canPropagate(targetState, level, spreadPos)) {
                    // Устанавливаем новый блок с учетом снега сверху
                    level.setBlockAndUpdate(spreadPos,
                            targetState.setValue(SNOWY,
                                    level.getBlockState(spreadPos.above()).is(Blocks.SNOW))
                    );
                }
            }
        }
    }
}
