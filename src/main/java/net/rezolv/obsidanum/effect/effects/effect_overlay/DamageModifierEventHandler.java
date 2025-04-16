package net.rezolv.obsidanum.effect.effects.effect_overlay;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.effect.EffectsObs;
@Mod.EventBusSubscriber(modid = Obsidanum.MOD_ID)
public class DamageModifierEventHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player player) {
            // Проверяем, есть ли у игрока нужный эффект (например, "Confusion")
            if (player.hasEffect(EffectsObs.FLASH.get())) {
                float originalDamage = event.getAmount();
                float bonusDamage = originalDamage * 0.3f; // +20% к урону
                event.setAmount(originalDamage + bonusDamage);
            }
        }
    }
}