package net.rezolv.obsidanum.item.custom;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class VelnariumSword extends SwordItem {
    private static final String NBT_KEY = "VelnariumUUID";
    private final double extraDamage;

    public VelnariumSword(Tier tier,
                          int attackDamageModifier,
                          float attackSpeedModifier,
                          Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
        this.extraDamage = attackDamageModifier;
    }

    @Override
    public void onCraftedBy(ItemStack stack, Level world, Player player) {
        super.onCraftedBy(stack, world, player);
        // При создании предмета задаём уникальный UUID в NBT
        if (!stack.hasTag() || !stack.getTag().hasUUID(NBT_KEY)) {
            stack.getOrCreateTag().putUUID(NBT_KEY, UUID.randomUUID());
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, world, entity, slot, selected);

        if (!stack.hasTag() || !stack.getTag().hasUUID(NBT_KEY)) {
            stack.getOrCreateTag().putUUID(NBT_KEY, UUID.randomUUID());
        }

        // 💡 Важно: обновляем флаг "InNether", чтобы getAttributeModifiers знал, где мы
        stack.getOrCreateTag().putBoolean("InNether", world.dimension() == Level.NETHER);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> base = super.getDefaultAttributeModifiers(slot);

        if (slot == EquipmentSlot.MAINHAND) {
            UUID uuid = stack.getTag() != null && stack.getTag().hasUUID(NBT_KEY)
                    ? stack.getTag().getUUID(NBT_KEY)
                    : UUID.randomUUID();
            if (stack.getTag() == null || !stack.getTag().hasUUID(NBT_KEY)) {
                stack.getOrCreateTag().putUUID(NBT_KEY, uuid);
            }

            // Копируем базу без лишнего модификатора урона
            Multimap<Attribute, AttributeModifier> map = HashMultimap.create(base);

            // ✅ Добавляем ТОЛЬКО бонус в Незере
            if (stack.getOrCreateTag().getBoolean("InNether")) {
                map.put(
                        Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(
                                UUID.nameUUIDFromBytes((uuid.toString() + "nether_bonus").getBytes()),
                                "Velnarium sword Nether bonus",
                                4.0,
                                AttributeModifier.Operation.ADDITION
                        )
                );
            }

            return map;
        }

        return base;
    }


    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (isNetherEntity(target) && attacker instanceof Player player) {
            target.hurt(player.damageSources().playerAttack(player), 4.0f);
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public void appendHoverText(ItemStack itemstack, Level world, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, world, list, flag);
        if (Screen.hasShiftDown()) {
            list.add(Component.translatable("obsidanum.press_shift2").withStyle(ChatFormatting.DARK_GRAY));
            if (world.dimension() == Level.NETHER) {
                list.add(Component.translatable("item.velnarium_sword.description.in_nether").withStyle(ChatFormatting.RED));
            } else {
                list.add(Component.translatable("item.velnarium_sword.description.not_in_nether").withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            list.add(Component.translatable("obsidanum.press_shift").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private boolean isNetherEntity(LivingEntity entity) {
        return NETHER_ENTITIES.contains(entity.getType());
    }
    private static final Set<EntityType<?>> NETHER_ENTITIES = Set.of(
            EntityType.BLAZE,
            EntityType.GHAST,
            EntityType.MAGMA_CUBE,
            EntityType.WITHER_SKELETON,
            EntityType.ZOMBIFIED_PIGLIN,
            EntityType.PIGLIN,
            EntityType.PIGLIN_BRUTE,
            EntityType.HOGLIN,
            EntityType.ZOGLIN,
            EntityType.STRIDER,
            EntityType.WITHER  // Бонус по Боссу тоже звучит эпично
    );
}
