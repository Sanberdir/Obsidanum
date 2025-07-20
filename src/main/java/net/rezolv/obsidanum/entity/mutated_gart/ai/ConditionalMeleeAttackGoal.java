package net.rezolv.obsidanum.entity.mutated_gart.ai;

import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.rezolv.obsidanum.entity.mutated_gart.MutatedGart;

public class ConditionalMeleeAttackGoal extends MeleeAttackGoal {
    private final MutatedGart gart;

    public ConditionalMeleeAttackGoal(MutatedGart gart, double speedModifier, boolean followContinuously) {
        super(gart, speedModifier, followContinuously);
        this.gart = gart;
    }

    @Override
    public boolean canUse() {
        // Запрещаем ближнюю атаку, если:
        // 1. Цель далеко
        // 2. Идет магическая атака
        return super.canUse()
                && !isMagicAttacking()
                && isTargetInMeleeRange();
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse()
                && !isMagicAttacking()
                && isTargetInMeleeRange();
    }

    private boolean isMagicAttacking() {
        return gart.getEntityData().get(MutatedGart.ATTACK_TIMER) > 0
                && gart.getEntityData().get(MutatedGart.MAGIC_ATTACK);
    }

    private boolean isTargetInMeleeRange() {
        if (gart.getTarget() == null) return false;
        return gart.distanceTo(gart.getTarget()) <= 6.0F; // допустим, до 6 блоков подбегает
    }
}
