package net.rezolv.obsidanum.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class PranaCrystallEntity extends BlockEntity {
    private final float rotSpeed;
    private final RandomSource randomSource;
    private final int lights;
    private final float rotOffset;
    private final Vec3 randomOffset;


    public PranaCrystallEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.PRANA_CRYSTALL.get(), pPos, pBlockState);
        randomSource = RandomSource.create(pPos.asLong());
        rotSpeed = (float) ((randomSource.nextFloat() * 0.5F + 1F) * randomSource.nextGaussian());
        lights = randomSource.nextInt(5) + 4;
        rotOffset = randomSource.nextFloat() * 360;
        randomOffset = new Vec3(randomSource.nextFloat() - 0.5F, randomSource.nextFloat() - 0.5F, randomSource.nextFloat() - 0.5F);

    }



    @OnlyIn(Dist.CLIENT)
    public AABB getRenderBoundingBox() {
        BlockPos pos = this.getBlockPos();
        return new AABB(pos.offset(-4, -4, -4), pos.offset(5, 5, 5));
    }

    public RandomSource getRandom() {
        return randomSource;
    }

    public int getLights() {
        return lights;
    }

    public float getRotOffset() {
        return rotOffset;
    }

    public float getRotSpeed() {
        return rotSpeed;
    }

    public Vec3 getRandomOffset() {
        return randomOffset;
    }

    public float calculateShineScale(Vec3 from) {
        double maxDist = 200;
        double dist = Math.min(from.distanceTo(Vec3.atCenterOf(this.getBlockPos())), maxDist);
        float f = (float) Math.pow(Math.sin(dist / maxDist * Math.PI), 0.5F);
        return f * 3F;
    }
}
