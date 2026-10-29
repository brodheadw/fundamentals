package ai.gsmc.fundamentals.mill;

import ai.gsmc.fundamentals.Fundamentals;
import com.simibubi.create.content.kinetics.simpleRelays.CogwheelBlockItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * The millwright's gearing, before iron: an oak shaft and oak cogwheels with pegged hornbeam teeth, as water and wind mills were
 * built for two thousand years. They need no metal but give way under a heavy load or a fast spin, where they snap to sticks.
 */
public final class WoodenMill {

    /** Past these the oak gives: a wooden shaft or cog snaps above 64 RPM, or with more than 1,024 stress units on its network. */
    public static final float MOST_RPM = 64, MOST_STRESS = 1024;

    private static Block shaft, cogwheel, largeCogwheel;
    private static BlockEntityType<WoodenKineticBlockEntity> entity;
    private static List<Item> items;

    private WoodenMill() {}

    public static Block shaft() { return shaft; }
    public static Block cogwheel() { return cogwheel; }
    public static Block largeCogwheel() { return largeCogwheel; }
    public static BlockEntityType<WoodenKineticBlockEntity> entity() { return entity; }
    public static List<Item> items() { return items; }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        BlockBehaviour.Properties oak = BlockBehaviour.Properties.ofFullCopy(Blocks.STRIPPED_OAK_LOG).noOcclusion();
        registry.accept(Fundamentals.id("wooden_shaft"), shaft = new WoodenShaftBlock(oak));
        registry.accept(Fundamentals.id("wooden_cogwheel"), cogwheel = new WoodenCogwheelBlock(false, oak));
        registry.accept(Fundamentals.id("wooden_large_cogwheel"), largeCogwheel = new WoodenCogwheelBlock(true, oak));
    }

    public static void registerBlockEntities(BiConsumer<ResourceLocation, BlockEntityType<?>> registry) {
        entity = BlockEntityType.Builder.of((pos, state) -> new WoodenKineticBlockEntity(entity, pos, state), shaft, cogwheel, largeCogwheel).build(null);
        registry.accept(Fundamentals.id("wooden_mill"), entity);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        Item.Properties properties = new Item.Properties();
        Item shaftItem = new BlockItem(shaft, properties) {
            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                limits(tooltip);
            }
        };
        Item cogItem = new CogwheelBlockItem((WoodenCogwheelBlock) cogwheel, properties) {
            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                limits(tooltip);
            }
        };
        Item largeCogItem = new CogwheelBlockItem((WoodenCogwheelBlock) largeCogwheel, properties) {
            @Override
            public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
                limits(tooltip);
            }
        };
        registry.accept(Fundamentals.id("wooden_shaft"), shaftItem);
        registry.accept(Fundamentals.id("wooden_cogwheel"), cogItem);
        registry.accept(Fundamentals.id("wooden_large_cogwheel"), largeCogItem);
        items = List.of(shaftItem, cogItem, largeCogItem);
    }

    private static void limits(List<Component> tooltip) {
        tooltip.add(Component.translatable("tooltip.fundamentals.wooden_mill", (int) MOST_RPM, (int) MOST_STRESS).withStyle(ChatFormatting.GRAY));
    }
}
