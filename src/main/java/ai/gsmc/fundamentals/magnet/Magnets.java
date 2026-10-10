package ai.gsmc.fundamentals.magnet;

import ai.gsmc.fundamentals.Fundamentals;
import com.drmangotea.tfmg.content.electricity.generators.large_generator.RotorBlockEntity;
import com.drmangotea.tfmg.registry.TFMGBlockEntities;
import com.simibubi.create.api.event.BlockEntityBehaviourEvent;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public final class Magnets {

    public static final DataComponentType<MagnetCharge> CHARGE = DataComponentType.<MagnetCharge>builder()
            .persistent(MagnetCharge.CODEC).networkSynchronized(MagnetCharge.STREAM_CODEC).build();
    public static final RecipeSerializer<MagnetRebuildRecipe> REBUILD = new SimpleCraftingRecipeSerializer<>(MagnetRebuildRecipe::new);
    private static final ResourceLocation TFMG_MAGNET = ResourceLocation.fromNamespaceAndPath("tfmg", "magnet");

    private static final Map<MagnetGrade, Item> MAGNETS = new EnumMap<>(MagnetGrade.class);

    private Magnets() {}

    public static Item magnet(MagnetGrade grade) {
        return MAGNETS.get(grade);
    }

    public static List<Item> items() {
        return List.copyOf(MAGNETS.values());
    }

    public static MagnetGrade grade(ItemStack stack) {
        for (Map.Entry<MagnetGrade, Item> magnet : MAGNETS.entrySet()) {
            if (stack.is(magnet.getValue())) {
                return magnet.getKey();
            }
        }
        return stack.is(BuiltInRegistries.ITEM.get(TFMG_MAGNET)) ? MagnetGrade.DY_NDFEB : null;
    }

    public static Component name(MagnetGrade grade) {
        return Component.translatable("item.fundamentals." + grade.magnet());
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        for (MagnetGrade grade : MagnetGrade.values()) {
            Item item = new Item(new Item.Properties());
            MAGNETS.put(grade, item);
            registry.accept(Fundamentals.id(grade.magnet()), item);
        }
    }

    public static void registerComponents(BiConsumer<ResourceLocation, DataComponentType<?>> registry) {
        registry.accept(Fundamentals.id("magnet"), CHARGE);
    }

    public static void registerRecipeSerializers(BiConsumer<ResourceLocation, RecipeSerializer<?>> registry) {
        registry.accept(Fundamentals.id("magnet_rebuild"), REBUILD);
    }

    public static void attach(BlockEntityBehaviourEvent event) {
        event.forType(TFMGBlockEntities.ELECTRIC_MOTOR.get(), be -> event.attach(new MagnetBehaviour(be, be::updateGeneratedRotation)));
        event.forType(TFMGBlockEntities.GENERATOR.get(), be -> event.attach(new MagnetBehaviour(be, be::updateNextTick)));
        event.forType(TFMGBlockEntities.ELECTRIC_PUMP.get(), be -> event.attach(new MagnetBehaviour(be, be::updatePressureChange)));
        event.forType(TFMGBlockEntities.VOLTMETER.get(), be -> event.attach(new MagnetBehaviour(be, () -> {})));
        event.forType(TFMGBlockEntities.STATOR.get(), be -> event.attach(new MagnetBehaviour(be, () -> {
            if (be.rotor != null && be.getLevel().getBlockEntity(be.rotor) instanceof RotorBlockEntity rotor) {
                rotor.updateNextTick();
            }
        })));
    }

    public static void onTooltip(ItemTooltipEvent event) {
        MagnetCharge charge = event.getItemStack().get(CHARGE);
        if (charge == null || event.getToolTip().isEmpty()) {
            return;
        }
        MagnetGrade grade = charge.grade();
        event.getToolTip().add(1, Component.translatable("tooltip.fundamentals.magnet.grade", name(grade), grade.maxOperating).withStyle(ChatFormatting.GRAY));
        if (charge.demagnetised()) {
            event.getToolTip().add(2, Component.translatable("tooltip.fundamentals.magnet.demagnetised").withStyle(ChatFormatting.RED));
        } else if (charge.field() < 1) {
            event.getToolTip().add(2, Component.translatable("tooltip.fundamentals.magnet.field", Math.round(charge.field() * 100)).withStyle(ChatFormatting.GOLD));
        }
    }

}
