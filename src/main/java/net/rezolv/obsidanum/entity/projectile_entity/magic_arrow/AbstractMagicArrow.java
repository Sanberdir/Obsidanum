package net.rezolv.obsidanum.entity.projectile_entity.magic_arrow;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public abstract class AbstractMagicArrow extends Projectile {
    // Константы для самонаведения
    private static final double HOMING_RADIUS = 20.0D;
    private static final double HOMING_SPEED_FACTOR = 0.002D;
    private static final double VERTICAL_OFFSET = 0.5D;
    private static final double ARROW_BASE_DAMAGE = 2.0F;

    private static final EntityDataAccessor<Byte> ID_FLAGS = SynchedEntityData.defineId(AbstractMagicArrow.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Byte> PIERCE_LEVEL = SynchedEntityData.defineId(AbstractMagicArrow.class, EntityDataSerializers.BYTE);
    private static final int FLAG_CRIT = 1;
    private static final int FLAG_NOPHYSICS = 2;
    private static final int FLAG_CROSSBOW = 4;

    @Nullable
    private BlockState lastState;
    protected boolean inGround;
    protected int inGroundTime;
    public Pickup pickup;
    public int shakeTime;
    private int life;
    private double baseDamage;
    private int knockback;
    private SoundEvent soundEvent;
    @Nullable
    private IntOpenHashSet piercingIgnoreEntityIds;
    @Nullable
    private List<Entity> piercedAndKilledEntities;
    private final IntOpenHashSet ignoredEntities;

    @Nullable
    private LivingEntity homingTarget;

    protected AbstractMagicArrow(EntityType<? extends AbstractMagicArrow> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.pickup = Pickup.DISALLOWED;
        this.baseDamage = ARROW_BASE_DAMAGE;
        this.soundEvent = this.getDefaultHitGroundSoundEvent();
        this.ignoredEntities = new IntOpenHashSet();
    }

    protected AbstractMagicArrow(EntityType<? extends AbstractMagicArrow> pEntityType, double pX, double pY, double pZ, Level pLevel) {
        this(pEntityType, pLevel);
        this.setPos(pX, pY, pZ);
    }

    protected AbstractMagicArrow(EntityType<? extends AbstractMagicArrow> pEntityType, LivingEntity pShooter, Level pLevel) {
        this(pEntityType, pShooter.getX(), pShooter.getEyeY() - 0.1F, pShooter.getZ(), pLevel);
        this.setOwner(pShooter);
        if (pShooter instanceof Player) {
            this.pickup = Pickup.ALLOWED;
        }

        // Начальное смещение вверх
        Vec3 motion = this.getDeltaMovement();
        this.setDeltaMovement(motion.x, motion.y + VERTICAL_OFFSET, motion.z);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ID_FLAGS, (byte)0);
        this.entityData.define(PIERCE_LEVEL, (byte)0);
    }

    @Nullable
    private LivingEntity findHomingTarget() {
        if (this.level().isClientSide || this.getOwner() == null) return null;

        return this.level().getEntitiesOfClass(
                        LivingEntity.class,
                        this.getBoundingBox().inflate(HOMING_RADIUS),
                        e -> e != this.getOwner() &&
                                e.isAlive() &&
                                !e.isAlliedTo(this.getOwner()) &&
                                this.canHitEntity(e)
                ).stream()
                .min((e1, e2) -> Float.compare(
                        (float) this.distanceToSqr(e1),
                        (float) this.distanceToSqr(e2)
                )).orElse(null);
    }

    private void updateHoming() {
        if (this.homingTarget == null || !this.homingTarget.isAlive()) {
            this.homingTarget = findHomingTarget();
        }

        if (this.homingTarget != null) {
            Vec3 targetPos = this.homingTarget.position().add(0, this.homingTarget.getBbHeight() * 0.5, 0);
            Vec3 currentPos = this.position();
            Vec3 direction = targetPos.subtract(currentPos).normalize();

            double speed = this.getDeltaMovement().length();
            double boostedSpeed = speed * (1.0 + HOMING_SPEED_FACTOR);

            this.setDeltaMovement(direction.scale(boostedSpeed));

            double horizontalDist = direction.horizontalDistance();
            this.setYRot((float)(Mth.atan2(direction.x, direction.z) * (180F / (float)Math.PI)));
            this.setXRot((float)(Mth.atan2(direction.y, horizontalDist) * (180F / (float)Math.PI)));
            this.yRotO = this.getYRot();
        }
    }

    @Override
    public void tick() {
        if (!this.inGround && !this.isNoPhysics()) {
            this.updateHoming();
        }

        super.tick();

        boolean flag = this.isNoPhysics();
        Vec3 vec3 = this.getDeltaMovement();

        if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
            double d0 = vec3.horizontalDistance();
            this.setYRot((float)(Mth.atan2(vec3.x, vec3.z) * (180F / (float)Math.PI)));
            this.setXRot((float)(Mth.atan2(vec3.y, d0) * (180F / (float)Math.PI)));
            this.yRotO = this.getYRot();
            this.xRotO = this.getXRot();
        }

        BlockPos blockpos = this.blockPosition();
        BlockState blockstate = this.level().getBlockState(blockpos);

        if (!blockstate.isAir() && !flag) {
            VoxelShape voxelshape = blockstate.getCollisionShape(this.level(), blockpos);
            if (!voxelshape.isEmpty()) {
                Vec3 vec31 = this.position();
                for(AABB aabb : voxelshape.toAabbs()) {
                    if (aabb.move(blockpos).contains(vec31)) {
                        this.inGround = true;
                        break;
                    }
                }
            }
        }

        if (this.shakeTime > 0) {
            --this.shakeTime;
        }

        if (this.isInWaterOrRain() || blockstate.is(Blocks.POWDER_SNOW)) {
            this.clearFire();
        }


            this.inGroundTime = 0;
            Vec3 vec32 = this.position();
            Vec3 vec33 = vec32.add(vec3);
            HitResult hitresult = this.level().clip(new ClipContext(vec32, vec33, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));

            if (hitresult.getType() != HitResult.Type.MISS) {
                vec33 = hitresult.getLocation();
            }

            while(!this.isRemoved()) {
                EntityHitResult entityhitresult = this.findHitEntity(vec32, vec33);
                if (entityhitresult != null) {
                    hitresult = entityhitresult;
                }

                if (hitresult != null && hitresult.getType() == HitResult.Type.ENTITY) {
                    Entity entity = ((EntityHitResult)hitresult).getEntity();
                    Entity entity1 = this.getOwner();
                    if (entity instanceof Player && entity1 instanceof Player && !((Player)entity1).canHarmPlayer((Player)entity)) {
                        hitresult = null;
                        entityhitresult = null;
                    }
                }

                if (hitresult != null && hitresult.getType() != HitResult.Type.MISS && !flag) {
                    this.onHit(hitresult);
                    this.hasImpulse = true;
                }

                if (entityhitresult == null || this.getPierceLevel() <= 0) {
                    break;
                }
                hitresult = null;
            }

            if (this.isRemoved()) return;

            vec3 = this.getDeltaMovement();
            if (this.isCritArrow()) {
                for(int i = 0; i < 4; ++i) {
                    this.level().addParticle(ParticleTypes.CRIT,
                            this.getX() + vec3.x * i / 4.0F,
                            this.getY() + vec3.y * i / 4.0F,
                            this.getZ() + vec3.z * i / 4.0F,
                            -vec3.x, -vec3.y + 0.2, -vec3.z);
                }
            }

            double d7 = this.getX() + vec3.x;
            double d2 = this.getY() + vec3.y;
            double d3 = this.getZ() + vec3.z;
            double d4 = vec3.horizontalDistance();

            this.setYRot((float)(Mth.atan2(vec3.x, vec3.z) * (180F / (float)Math.PI)));
            this.setXRot((float)(Mth.atan2(vec3.y, d4) * (180F / (float)Math.PI)));
            this.setXRot(lerpRotation(this.xRotO, this.getXRot()));
            this.setYRot(lerpRotation(this.yRotO, this.getYRot()));

            this.setDeltaMovement(vec3.scale(0.99F));
            if (!this.isNoGravity() && !flag) {
                Vec3 vec34 = this.getDeltaMovement();
                this.setDeltaMovement(vec34.x, vec34.y - 0.05F, vec34.z);
            }

            this.setPos(d7, d2, d3);
            this.checkInsideBlocks();

    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        this.homingTarget = null;
        super.onHitEntity(pResult);

        Entity entity = pResult.getEntity();
        float f = (float)this.getDeltaMovement().length();
        int i = Mth.ceil(Mth.clamp(f * this.baseDamage, 0.0F, Integer.MAX_VALUE));

        if (this.getPierceLevel() > 0) {
            if (this.piercingIgnoreEntityIds == null) {
                this.piercingIgnoreEntityIds = new IntOpenHashSet(5);
            }
            if (this.piercedAndKilledEntities == null) {
                this.piercedAndKilledEntities = Lists.newArrayListWithCapacity(5);
            }
            if (this.piercingIgnoreEntityIds.size() >= this.getPierceLevel() + 1) {
                this.discard();
                return;
            }
            this.piercingIgnoreEntityIds.add(entity.getId());
        }

        if (this.isCritArrow()) {
            long j = this.random.nextInt(i / 2 + 2);
            i = (int)Math.min(j + i, 2147483647L);
        }

        Entity entity1 = this.getOwner();
        DamageSource damagesource = entity1 == null
                ? this.damageSources().indirectMagic(this, this.getOwner())
                : this.damageSources().indirectMagic(this, this.getOwner());

        if (entity1 instanceof LivingEntity) {
            ((LivingEntity)entity1).setLastHurtMob(entity);
        }

        boolean flag = entity.getType() == EntityType.ENDERMAN;
        int k = entity.getRemainingFireTicks();

        if (this.isOnFire() && !flag) {
            entity.setSecondsOnFire(5);
        }

        if (entity.hurt(damagesource, i)) {
            if (flag) return;

            if (entity instanceof LivingEntity livingentity) {
                if (!this.level().isClientSide && this.getPierceLevel() <= 0) {
                    livingentity.setArrowCount(livingentity.getArrowCount() + 1);
                }

                if (this.knockback > 0) {
                    double d0 = Math.max(0.0F, 1.0F - livingentity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
                    Vec3 vec3 = this.getDeltaMovement().multiply(1.0F, 0.0F, 1.0F).normalize().scale(this.knockback * 0.6 * d0);
                    if (vec3.lengthSqr() > 0.0F) {
                        livingentity.push(vec3.x, 0.1, vec3.z);
                    }
                }

                if (!this.level().isClientSide && entity1 instanceof LivingEntity) {
                    EnchantmentHelper.doPostHurtEffects(livingentity, entity1);
                    EnchantmentHelper.doPostDamageEffects((LivingEntity)entity1, livingentity);
                }

                this.doPostHurtEffects(livingentity);

                if (entity1 != null && livingentity != entity1 && livingentity instanceof Player && entity1 instanceof ServerPlayer) {
                    ((ServerPlayer)entity1).connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.ARROW_HIT_PLAYER, 0.0F));
                }

                if (!entity.isAlive() && this.piercedAndKilledEntities != null) {
                    this.piercedAndKilledEntities.add(livingentity);
                }

                if (!this.level().isClientSide && entity1 instanceof ServerPlayer serverplayer) {
                    if (this.piercedAndKilledEntities != null && this.shotFromCrossbow()) {
                        CriteriaTriggers.KILLED_BY_CROSSBOW.trigger(serverplayer, this.piercedAndKilledEntities);
                    } else if (!entity.isAlive() && this.shotFromCrossbow()) {
                        CriteriaTriggers.KILLED_BY_CROSSBOW.trigger(serverplayer, Arrays.asList(entity));
                    }
                }
            }

            if (this.getPierceLevel() <= 0) {
                this.discard();
            }
        } else {
            entity.setRemainingFireTicks(k);
            this.setDeltaMovement(this.getDeltaMovement().scale(-0.1));
            this.setYRot(this.getYRot() + 180.0F);
            this.yRotO += 180.0F;

            if (!this.level().isClientSide && this.getDeltaMovement().lengthSqr() < 1.0E-7) {
                if (this.pickup == Pickup.ALLOWED) {
                    this.spawnAtLocation(this.getPickupItem(), 0.1F);
                }
                this.discard();
            }
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
        this.homingTarget = null;
        this.lastState = this.level().getBlockState(pResult.getBlockPos());
        super.onHitBlock(pResult);

        Vec3 vec3 = pResult.getLocation().subtract(this.getX(), this.getY(), this.getZ());
        this.setDeltaMovement(vec3);
        Vec3 vec31 = vec3.normalize().scale(0.05F);
        this.setPosRaw(this.getX() - vec31.x, this.getY() - vec31.y, this.getZ() - vec31.z);

        this.inGround = true;
        this.shakeTime = 7;
        this.setCritArrow(false);
        this.setPierceLevel((byte)0);
        this.setShotFromCrossbow(false);
    }

    // Остальные методы без изменений
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_THROW;
    }

    protected final SoundEvent getHitGroundSoundEvent() {
        return this.soundEvent;
    }

    protected void doPostHurtEffects(LivingEntity pTarget) {}

    @Nullable
    protected EntityHitResult findHitEntity(Vec3 pStartVec, Vec3 pEndVec) {
        return ProjectileUtil.getEntityHitResult(this.level(), this, pStartVec, pEndVec,
                this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0F), this::canHitEntity);
    }

    protected boolean canHitEntity(Entity pEntity) {
        return super.canHitEntity(pEntity) &&
                (this.piercingIgnoreEntityIds == null || !this.piercingIgnoreEntityIds.contains(pEntity.getId())) &&
                !this.ignoredEntities.contains(pEntity.getId());
    }

    public void addAdditionalSaveData(CompoundTag pCompound) {
        super.addAdditionalSaveData(pCompound);
        pCompound.putShort("life", (short)this.life);
        if (this.lastState != null) {
            pCompound.put("inBlockState", NbtUtils.writeBlockState(this.lastState));
        }
        pCompound.putByte("shake", (byte)this.shakeTime);
        pCompound.putBoolean("inGround", this.inGround);
        pCompound.putByte("pickup", (byte)this.pickup.ordinal());
        pCompound.putDouble("damage", this.baseDamage);
        pCompound.putBoolean("crit", this.isCritArrow());
        pCompound.putByte("PierceLevel", this.getPierceLevel());
        pCompound.putString("SoundEvent", BuiltInRegistries.SOUND_EVENT.getKey(this.soundEvent).toString());
        pCompound.putBoolean("ShotFromCrossbow", this.shotFromCrossbow());
    }

    public void readAdditionalSaveData(CompoundTag pCompound) {
        super.readAdditionalSaveData(pCompound);
        this.life = pCompound.getShort("life");
        if (pCompound.contains("inBlockState", 10)) {
            this.lastState = NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), pCompound.getCompound("inBlockState"));
        }
        this.shakeTime = pCompound.getByte("shake") & 255;
        this.inGround = pCompound.getBoolean("inGround");
        if (pCompound.contains("damage", 99)) {
            this.baseDamage = pCompound.getDouble("damage");
        }
        this.pickup = Pickup.byOrdinal(pCompound.getByte("pickup"));
        this.setCritArrow(pCompound.getBoolean("crit"));
        this.setPierceLevel(pCompound.getByte("PierceLevel"));
        if (pCompound.contains("SoundEvent", 8)) {
            this.soundEvent = BuiltInRegistries.SOUND_EVENT.getOptional(new ResourceLocation(pCompound.getString("SoundEvent")))
                    .orElse(this.getDefaultHitGroundSoundEvent());
        }
        this.setShotFromCrossbow(pCompound.getBoolean("ShotFromCrossbow"));
    }

    public void setOwner(@Nullable Entity pEntity) {
        super.setOwner(pEntity);
        if (pEntity instanceof Player player) {
            this.pickup = player.getAbilities().instabuild ? Pickup.CREATIVE_ONLY : Pickup.ALLOWED;
        }
    }

    public void playerTouch(Player pEntity) {
        if (!this.level().isClientSide && (this.inGround || this.isNoPhysics()) && this.shakeTime <= 0 && this.tryPickup(pEntity)) {
            pEntity.take(this, 1);
            this.discard();
        }
    }

    protected boolean tryPickup(Player pPlayer) {
        return switch (this.pickup) {
            case ALLOWED -> pPlayer.getInventory().add(this.getPickupItem());
            case CREATIVE_ONLY -> pPlayer.getAbilities().instabuild;
            default -> false;
        };
    }

    protected abstract ItemStack getPickupItem();

    protected Entity.MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }

    public void setBaseDamage(double pBaseDamage) {
        this.baseDamage = pBaseDamage;
    }

    public double getBaseDamage() {
        return this.baseDamage;
    }

    public void setKnockback(int pKnockback) {
        this.knockback = pKnockback;
    }

    public int getKnockback() {
        return this.knockback;
    }

    public boolean isAttackable() {
        return false;
    }

    protected float getEyeHeight(Pose pPose, EntityDimensions pSize) {
        return 0.13F;
    }

    public void setCritArrow(boolean pCritArrow) {
        this.setFlag(FLAG_CRIT, pCritArrow);
    }

    public void setPierceLevel(byte pPierceLevel) {
        this.entityData.set(PIERCE_LEVEL, pPierceLevel);
    }

    private void setFlag(int pId, boolean pValue) {
        byte b0 = this.entityData.get(ID_FLAGS);
        this.entityData.set(ID_FLAGS, (byte)(pValue ? b0 | pId : b0 & ~pId));
    }

    public boolean isCritArrow() {
        return (this.entityData.get(ID_FLAGS) & FLAG_CRIT) != 0;
    }

    public boolean shotFromCrossbow() {
        return (this.entityData.get(ID_FLAGS) & FLAG_CROSSBOW) != 0;
    }

    public byte getPierceLevel() {
        return this.entityData.get(PIERCE_LEVEL);
    }

    public void setEnchantmentEffectsFromEntity(LivingEntity pShooter, float pVelocity) {
        int i = EnchantmentHelper.getEnchantmentLevel(Enchantments.POWER_ARROWS, pShooter);
        int j = EnchantmentHelper.getEnchantmentLevel(Enchantments.PUNCH_ARROWS, pShooter);

        this.setBaseDamage(pVelocity * 2.0F + this.random.triangle(this.level().getDifficulty().getId() * 0.11, 0.57425));

        if (i > 0) {
            this.setBaseDamage(this.getBaseDamage() + i * 0.5F + 0.5F);
        }
        if (j > 0) {
            this.setKnockback(j);
        }
        if (EnchantmentHelper.getEnchantmentLevel(Enchantments.FLAMING_ARROWS, pShooter) > 0) {
            this.setSecondsOnFire(100);
        }
    }

    protected float getWaterInertia() {
        return 0.6F;
    }

    public void setNoPhysics(boolean pNoPhysics) {
        this.noPhysics = pNoPhysics;
        this.setFlag(FLAG_NOPHYSICS, pNoPhysics);
    }

    public boolean isNoPhysics() {
        return !this.level().isClientSide ? this.noPhysics : (this.entityData.get(ID_FLAGS) & FLAG_NOPHYSICS) != 0;
    }

    public void setShotFromCrossbow(boolean pShotFromCrossbow) {
        this.setFlag(FLAG_CROSSBOW, pShotFromCrossbow);
    }

    public enum Pickup {
        DISALLOWED,
        ALLOWED,
        CREATIVE_ONLY;

        public static Pickup byOrdinal(int pOrdinal) {
            return pOrdinal >= 0 && pOrdinal < values().length ? values()[pOrdinal] : DISALLOWED;
        }
    }
}