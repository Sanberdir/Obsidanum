package net.rezolv.obsidanum.block.custom;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public class UrnUtils {
    public static List<BlockPos> getAllConnectedParts(Level level, BlockPos pos, BlockState state) {
        BlockPos mainPos = findMainPart(pos, state, level);
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        Direction right = facing.getClockWise();

        List<BlockPos> positions = new ArrayList<>();

        for (int dy = 0; dy < 2; dy++) {
            for (int dz = 0; dz < 2; dz++) {
                for (int dx = 0; dx < 2; dx++) {
                    BlockPos partPos = mainPos
                            .relative(right, dx)
                            .relative(facing.getOpposite(), dz)
                            .above(dy);

                    if (level.getBlockState(partPos).getBlock() instanceof LargeUrnPart
                            || level.getBlockState(partPos).getBlock() instanceof LargeUrnMain) {
                        positions.add(partPos);
                    }
                }
            }
        }

        return positions;
    }

    public static BlockPos findMainPart(BlockPos pos, BlockState state, Level level) {
        if (state.getBlock() instanceof LargeUrnMain) return pos;

        Part part = state.getValue(LargeUrnPart.PART);
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        Direction right = facing.getClockWise();

        return pos
                .relative(right.getOpposite(), part.getX())
                .relative(facing, part.getZ()) // возвращаемся «вперёд» к главному
                .below(part.getY());
    }

    public static BlockPos calculatePosition(BlockPos mainPos, Direction facing, int dx, int dy, int dz) {
        Direction right = facing.getClockWise();
        return mainPos
                .relative(right, dx)
                .relative(facing.getOpposite(), dz)
                .above(dy);
    }
}
