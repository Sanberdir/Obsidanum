package net.rezolv.obsidanum.entity.mutated_gart;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Snowball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rezolv.obsidanum.entity.mutated_gart.ai.ConditionalMeleeAttackGoal;
import net.rezolv.obsidanum.entity.mutated_gart.ai.ConditionalMoveGoal;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.RenderUtils;

public class MutatedGart extends Monster implements GeoEntity {
    // Анимации
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation PUNCH_ANIM = RawAnimation.begin().thenPlay("punch");
    private static final RawAnimation MAGIC_PUNCH_ANIM = RawAnimation.begin().thenPlay("magic_punch");
    public static final EntityDataAccessor<Integer> ATTACK_TIMER = SynchedEntityData.defineId(MutatedGart.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private ServerBossEvent bossInfo;
    private LivingEntity currentAttackTarget; // Храним цель для удара

    public MutatedGart(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }


    // Добавление игрока в полосу здоровья босса
    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        if (this.bossInfo == null) {
            this.bossInfo = new ServerBossEvent(this.getDisplayName(), ServerBossEvent.BossBarColor.YELLOW, ServerBossEvent.BossBarOverlay.PROGRESS);
        }
        this.bossInfo.addPlayer(player);
    }

    // Удаление игрока из полосы здоровья босса
    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        if (this.bossInfo != null) {
            this.bossInfo.removePlayer(player);
        }
    }

    // Иммунитет к огню
    @Override
    public boolean fireImmune() {
        return true;
    }

    // Обновление полосы здоровья босса
    @Override
    public void customServerAiStep() {
        super.customServerAiStep();
        if (this.bossInfo == null) {
            this.bossInfo = new ServerBossEvent(this.getDisplayName(), ServerBossEvent.BossBarColor.YELLOW, ServerBossEvent.BossBarOverlay.PROGRESS);
        }
        this.bossInfo.setProgress(this.getHealth() / this.getMaxHealth());
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        // Проверяем, не идет ли уже анимация удара
        if (this.entityData.get(ATTACK_TIMER) > 0) {
            return false;
        }

        if (!this.level().isClientSide) {
            // Сохраняем цель атаки
            this.currentAttackTarget = target instanceof LivingEntity ? (LivingEntity) target : null;
            // Устанавливаем таймер на 70 тиков (20 анимация + 20 пауза)
            this.entityData.set(ATTACK_TIMER, 35);
        }

        this.swing(InteractionHand.MAIN_HAND);
        return true;
    }

    // Регистрация целей AI
    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new ConditionalMoveGoal(this));

        this.goalSelector.addGoal(2, new ConditionalMeleeAttackGoal(this, 1.0D, true));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, IronGolem.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Warden.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Villager.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, WitherBoss.class, true));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 15.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 1.2D));
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 1.0));
    }

    // Создание атрибутов моба
    public static AttributeSupplier.Builder createAttributes() {
        return createMobAttributes()
                .add(Attributes.MAX_HEALTH, 200)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.ARMOR_TOUGHNESS, 0.8D)
                .add(Attributes.FOLLOW_RANGE, 20)
                .add(Attributes.ATTACK_DAMAGE, 12)
                .add(Attributes.ATTACK_KNOCKBACK, 3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.1)
                .add(Attributes.ARMOR, 0.0);
    }

    // Выпадение опыта при смерти
    @Override
    protected void dropExperience() {
        super.dropExperience();
        this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY(), this.getZ(), 500));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllerRegistrar) {
        controllerRegistrar.add(new AnimationController<>(this, "controller", 5, this::animate));
    }

    // ИСПРАВЛЕННАЯ ЛОГИКА АНИМАЦИЙ
    private PlayState animate(AnimationState state) {
        int timer = this.entityData.get(ATTACK_TIMER);
        boolean magic = this.entityData.get(MAGIC_ATTACK);
        if (magic && timer > 20) {
            state.getController().setAnimation(MAGIC_PUNCH_ANIM); // проигрываем анимацию magic_punch
            return PlayState.CONTINUE;
        }
        if (!magic && timer > 20) {
            state.getController().setAnimation(PUNCH_ANIM);       // проигрываем обычный punch
            return PlayState.CONTINUE;
        }
        // Когда нет атаки – стандартные анимации ходьбы/стояния
        if (state.isMoving()) {
            state.getController().setAnimation(WALK_ANIM);
        } else {
            state.getController().setAnimation(IDLE_ANIM);
        }
        return PlayState.CONTINUE;
    }
    public static final EntityDataAccessor<Boolean> MAGIC_ATTACK = SynchedEntityData.defineId(MutatedGart.class, EntityDataSerializers.BOOLEAN);

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACK_TIMER, 0);
        this.entityData.define(MAGIC_ATTACK, false);
    }
    private void spawnSnowballAtTarget() {
        if (this.currentAttackTarget == null || !this.currentAttackTarget.isAlive()) return;

        Snowball snowball = new Snowball(this.level(), this);

        double dx = this.currentAttackTarget.getX() - this.getX();
        double dy = this.currentAttackTarget.getY(0.5D) - snowball.getY(); // немного ниже глаз
        double dz = this.currentAttackTarget.getZ() - this.getZ();

        float velocity = 1.2F;       // скорость полета
        float inaccuracy = 0.2F;     // разброс (можно 0 для точности)

        snowball.shoot(dx, dy, dz, velocity, inaccuracy);

        this.level().addFreshEntity(snowball);
        this.playSound(SoundEvents.SNOWBALL_THROW, 1.0F, 1.0F);
    }
    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide) {
            LivingEntity target = this.getTarget();
            // Если есть цель и она далеко
            if (target != null && target.isAlive() && this.distanceTo(target) > 5.0F) {
                // Если сейчас нет активной атаки
                if (this.entityData.get(ATTACK_TIMER) == 0) {
                    this.currentAttackTarget = target;
                    this.entityData.set(MAGIC_ATTACK, true);      // флаг дальнего удара
                    this.entityData.set(ATTACK_TIMER, 50);       // устанавливаем больший таймер
                    this.swing(InteractionHand.MAIN_HAND);       // анимация взмаха рукой
                }
            }
            if (this.entityData.get(MAGIC_ATTACK) && this.currentAttackTarget != null) {
                this.getLookControl().setLookAt(
                        this.currentAttackTarget.getX(),
                        this.currentAttackTarget.getEyeY(),
                        this.currentAttackTarget.getZ(),
                        10.0F,  // maxYawChange — скорость поворота по горизонтали
                        10.0F   // maxPitchChange — по вертикали
                );
            }
            // Существующий код по таймеру атаки
            int timer = this.entityData.get(ATTACK_TIMER);
            if (timer > 0) {
                int newTimer = timer - 1;
                this.entityData.set(ATTACK_TIMER, newTimer);
                // На тике 20 кидаем снежок (поскольку установили 50, через 30 тиков от начала)
                if (newTimer == 20 && this.entityData.get(MAGIC_ATTACK)) {
                    spawnSnowballAtTarget();
                    // Можно сбросить флаг после броска (или оставить, чтобы анимация нормально завершилась)
                    this.entityData.set(MAGIC_ATTACK, false);
                }
                // Обычный урон в ближнем бою, если это не магическая атака
                if (newTimer == 20 && !this.entityData.get(MAGIC_ATTACK)
                        && this.currentAttackTarget != null && this.distanceTo(currentAttackTarget) < 4.0F) {
                    super.doHurtTarget(this.currentAttackTarget);
                    this.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
                }
            }
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object o) {
        return RenderUtils.getCurrentTick();
    }
}