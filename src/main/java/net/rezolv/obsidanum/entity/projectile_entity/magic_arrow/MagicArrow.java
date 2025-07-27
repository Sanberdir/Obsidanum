package net.rezolv.obsidanum.entity.projectile_entity.magic_arrow;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraftforge.network.PlayMessages;
import net.rezolv.obsidanum.entity.ModItemEntities;
import net.rezolv.obsidanum.item.ItemsObs;

public class MagicArrow extends AbstractMagicArrow {

    // Конструктор для создания стрелы по типу сущности и миру
    public MagicArrow(EntityType<? extends MagicArrow> entityType, Level world) {
        super(entityType, world);
    }

    // Конструктор для создания стрелы, выпущенной живым существом
    public MagicArrow(Level world, LivingEntity shooter) {
        super(ModItemEntities.MAGIC_ARROW_ENTITY.get(), shooter, world);
    }

    // Конструктор для создания стрелы в конкретных координатах
    public MagicArrow(Level world, double x, double y, double z) {
        super(ModItemEntities.MAGIC_ARROW_ENTITY.get(), x, y, z, world);
    }

    // Конструктор для спавна стрелы через сетевой пакет
    public MagicArrow(PlayMessages.SpawnEntity spawnPacket, Level world) {
        this(ModItemEntities.MAGIC_ARROW_ENTITY.get(), world);
    }

    @Override
    protected ItemStack getPickupItem() {
        return new ItemStack(ItemsObs.MAGIC_ARROW.get());
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        // Здесь можно добавить дополнительную логику при попадании в блок
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        // Здесь можно добавить дополнительную логику при попадании в сущность
    }
}