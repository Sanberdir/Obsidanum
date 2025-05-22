package net.rezolv.obsidanum.effect.effects.effect_overlay;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.rezolv.obsidanum.Obsidanum;
import net.rezolv.obsidanum.effect.EffectsObs;
import net.rezolv.obsidanum.item.item_entity.pot_grenade.fog.PotGrenadeFog;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = Obsidanum.MOD_ID)
public class FogEffectHandler {
    private static final Map<UUID, PlayerFogState> SERVER_PLAYER_STATES = new HashMap<>();

    // Константы для времени
    private static final int FIRST_EFFECT_DELAY = 5 * 20;  // 5 секунд в тиках
    private static final int SECOND_EFFECT_DELAY = 10 * 20; // 10 секунд в тиках
    private static final int EFFECT_DURATION = 20 * 20;     // 20 секунд в тиках
    private static final int FIRST_EFFECT_LEVEL = 0;        // Уровень 1 (нумерация с 0)
    private static final int SECOND_EFFECT_LEVEL = 1;       // Уровень 2

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer)) return;

        ServerPlayer player = (ServerPlayer) event.player;
        UUID playerId = player.getUUID();
        boolean inFog = isPlayerInFog(player);

        PlayerFogState state = SERVER_PLAYER_STATES.computeIfAbsent(playerId, k -> new PlayerFogState());

        if (inFog) {
            state.ticksInFog++;

            // Проверяем, прошло ли 10 секунд для эффекта уровня 2
            if (state.ticksInFog >= SECOND_EFFECT_DELAY && !state.secondEffectGiven) {
                player.addEffect(new MobEffectInstance(
                        EffectsObs.FLASH.get(),
                        EFFECT_DURATION,
                        SECOND_EFFECT_LEVEL,
                        false, // не частицы
                        true // видно в инвентаре
                ));
                state.secondEffectGiven = true;
                state.firstEffectGiven = true; // Чтобы не давать первый эффект, если уже дали второй
            }
            // Проверяем, прошло ли 5 секунд для эффекта уровня 1 (если еще не давали второй эффект)
            else if (state.ticksInFog >= FIRST_EFFECT_DELAY && !state.firstEffectGiven) {
                player.addEffect(new MobEffectInstance(
                        EffectsObs.FLASH.get(),
                        EFFECT_DURATION,
                        FIRST_EFFECT_LEVEL,
                        false,
                        true
                ));
                state.firstEffectGiven = true;
            }
        } else {
            // Сбрасываем состояние, если игрок вышел из тумана
            state.reset();
        }
    }

    private static boolean isPlayerInFog(ServerPlayer player) {
        return !player.level().getEntitiesOfClass(PotGrenadeFog.class, player.getBoundingBox())
                .isEmpty();
    }

    // Класс для хранения состояния игрока
    private static class PlayerFogState {
        int ticksInFog = 0;
        boolean firstEffectGiven = false;
        boolean secondEffectGiven = false;

        void reset() {
            ticksInFog = 0;
            firstEffectGiven = false;
            secondEffectGiven = false;
        }
    }
}