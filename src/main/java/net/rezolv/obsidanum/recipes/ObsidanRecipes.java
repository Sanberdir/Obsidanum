package net.rezolv.obsidanum.recipes;

import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.rezolv.obsidanum.Obsidanum;

public class ObsidanRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Obsidanum.MOD_ID);

    public static final RegistryObject<RecipeSerializer<ForgeScrollNetherRecipe>> FORGE_SCROLL_NETHER_SERIALIZER =
            SERIALIZERS.register("forge_scroll_nether", () -> ForgeScrollNetherRecipe.Serializer.FORGE_SCROLL_NETHER);
    public static final RegistryObject<RecipeSerializer<ForgeScrollOrderRecipe>> FORGE_SCROLL_ORDER_SERIALIZER =
            SERIALIZERS.register("forge_scroll_order", () -> ForgeScrollOrderRecipe.Serializer.FORGE_SCROLL_ORDER);
    public static final RegistryObject<RecipeSerializer<ForgeScrollCatacombsRecipe>> FORGE_SCROLL_CATACOMBS_SERIALIZER =
            SERIALIZERS.register("forge_scroll_catacombs", () -> ForgeScrollCatacombsRecipe.Serializer.FORGE_SCROLL_CATACOMBS);

    public static final RegistryObject<RecipeSerializer<ForgeScrollCatacombsUpgradeRecipe>> FORGE_SCROLL_CATACOMBS_UP_SERIALIZER =
            SERIALIZERS.register("forge_scroll_catacombs_up", () -> ForgeScrollCatacombsUpgradeRecipe.Serializer.FORGE_SCROLL_CATACOMBS_UPGRADE);
    public static final RegistryObject<RecipeSerializer<ForgeScrollNetherUpgradeRecipe>> FORGE_SCROLL_NETHER_UP_SERIALIZER =
            SERIALIZERS.register("forge_scroll_nether_up", () -> ForgeScrollNetherUpgradeRecipe.Serializer.FORGE_SCROLL_NETHER_UPGRADE);
    public static final RegistryObject<RecipeSerializer<ForgeScrollOrderUpgradeRecipe>> FORGE_SCROLL_ORDER_UP_SERIALIZER =
            SERIALIZERS.register("forge_scroll_order_up", () -> ForgeScrollOrderUpgradeRecipe.Serializer.FORGE_SCROLL_ORDER_UPGRADE);

    public static final RegistryObject<RecipeSerializer<ForgeScrollOrderRepairRecipe>> FORGE_SCROLL_ORDER_REPAIR_SERIALIZER =
            SERIALIZERS.register("forge_scroll_order_repair", () -> ForgeScrollOrderRepairRecipe.Serializer.FORGE_SCROLL_ORDER_REPAIR);

    public static final RegistryObject<RecipeSerializer<ForgeScrollOrderDestructionRecipe>> FORGE_SCROLL_ORDER_DESTRUCTION_SERIALIZER =
            SERIALIZERS.register("forge_scroll_order_destruction", () -> ForgeScrollOrderDestructionRecipe.Serializer.FORGE_SCROLL_ORDER_DESTRUCTION);
}
