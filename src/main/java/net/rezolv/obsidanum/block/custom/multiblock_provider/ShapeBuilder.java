/*
 * Copyright (c) 2023 Skyler James
 * Permission is granted to use, modify, and redistribute this software, in parts or in whole,
 * under the GNU LGPLv3 license (https://www.gnu.org/licenses/lgpl-3.0.en.html)
 */

package net.rezolv.obsidanum.block.custom.multiblock_provider;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.Function;

/**
 * Implementation of {@code Function<.BlockState, VoxelShape>}
 * that is used to create voxel shapes to be cached separately
 * @see Function
 * @see BlockState
 * @see VoxelShape
 */
@FunctionalInterface
public interface ShapeBuilder extends Function<BlockState, VoxelShape> {}
