package net.rezolv.obsidanum.entity.mutated_gart.ai;

import net.minecraft.world.entity.ai.goal.Goal;
import net.rezolv.obsidanum.entity.mutated_gart.MutatedGart;

import java.util.EnumSet;

public class ConditionalMoveGoal extends Goal {
    private final MutatedGart gart;

    public ConditionalMoveGoal(MutatedGart gart) {
        this.gart = gart;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        // Блокирует движение, если идет магическая атака
        return isMagicAttacking();
    }

    @Override
    public void start() {
        gart.getNavigation().stop(); // мгновенная остановка
    }

    private boolean isMagicAttacking() {
        return gart.getEntityData().get(MutatedGart.ATTACK_TIMER) > 0
                && gart.getEntityData().get(MutatedGart.MAGIC_ATTACK);
    }
}
