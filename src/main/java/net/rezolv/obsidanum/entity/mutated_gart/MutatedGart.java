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
import net.minecraft.world.level.Level;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animatable.instance.SingletonAnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.animation.AnimationState; // Исправленный импорт
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.RenderUtils;

public class MutatedGart extends Monster implements GeoEntity {
    // Анимации
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation PUNCH_ANIM = RawAnimation.begin().thenPlay("punch");
    private static final RawAnimation MAGIC_PUNCH_ANIM = RawAnimation.begin().thenPlay("magic_punch");
    private static final EntityDataAccessor<Integer> ATTACK_TIMER = SynchedEntityData.defineId(MutatedGart.class, EntityDataSerializers.INT);

    private final AnimatableInstanceCache cache = new SingletonAnimatableInstanceCache(this);
    private ServerBossEvent bossInfo;

    public MutatedGart(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACK_TIMER, 0);
    }
    // Обновление AI
    @Override
    public void aiStep() {
        super.aiStep();
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
            this.entityData.set(ATTACK_TIMER, 20);
            this.setLastHurtByMob((LivingEntity) target);
        }

        this.swing(InteractionHand.MAIN_HAND);
        return true;
    }
    // Регистрация целей AI (только пассивное поведение)
    @Override
    protected void registerGoals() {
        super.registerGoals();
        // Добавляем цель для ближнего боя
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true)); // Приоритет 2 - высокий

        // Существующие цели
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal(this, Player.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, IronGolem.class, true));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal(this, Warden.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal(this, Villager.class, true));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal(this, WitherBoss.class, true));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 15.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 1.2D));
        this.goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, 1.0));
    }

    // Создание атрибутов моба (сохранены оригинальные характеристики)
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

    // Исправленный метод без дженерика
    private PlayState animate(AnimationState state) {
        int timer = this.entityData.get(ATTACK_TIMER);
        if (timer > 0) {
            state.getController().setAnimation(PUNCH_ANIM);
            return PlayState.CONTINUE;
        }

        // Стандартные анимации
        if (state.isMoving()) {
            state.getController().setAnimation(WALK_ANIM);
        } else {
            state.getController().setAnimation(IDLE_ANIM);
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide) {
            int timer = this.entityData.get(ATTACK_TIMER);

            if (timer > 0) {
                this.entityData.set(ATTACK_TIMER, timer - 1);

                // Когда таймер дойдёт до 0 — наносим урон
                if (timer == 1) {
                    Entity target = this.getLastHurtByMob();
                    if (target != null && this.distanceTo(target) < 4.0F && target.isAlive()) {
                        // Урон и звук
                        boolean result = super.doHurtTarget(target);
                        if (result) {
                            this.playSound(SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
                        }
                    }
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