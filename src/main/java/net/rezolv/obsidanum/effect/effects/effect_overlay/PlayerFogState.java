package net.rezolv.obsidanum.effect.effects.effect_overlay;

public class PlayerFogState {
    public float fogExposureTime = 0;
    public boolean isInFog = false;
    public int appliedEffectLevel = 0;
    public long lastFogExitTime = -1; // Время (в тиках), когда игрок вышел из тумана (-1 = не выходил)

    public static final float MAX_EXPOSURE_TIME_LVL1 = 8.0f * 20;  // 8 секунд
    public static final float MAX_EXPOSURE_TIME_LVL2 = 16.0f * 20; // 16 секунд
    public static final int EFFECT_DURATION = 20 * 20;             // 20 секунд
}
