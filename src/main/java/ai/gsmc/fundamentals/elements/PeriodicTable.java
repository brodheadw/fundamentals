package ai.gsmc.fundamentals.elements;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CustomModelData;

import java.util.function.BiConsumer;

public final class PeriodicTable {

    public static final ResourceLocation ROOT = Fundamentals.id("elements/root");

    private static final int[] PERIOD_STARTS = {1, 3, 11, 19, 37, 55, 87, 119};

    private PeriodicTable() {}

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(Fundamentals.id("element"), new Item(new Item.Properties().stacksTo(1)));
    }

    public static boolean isTable(AdvancementNode node) {
        return node.root().holder().id().equals(ROOT);
    }

    public static void layout(AdvancementNode root) {
        if (!root.holder().id().equals(ROOT)) {
            return;
        }
        root.advancement().display().ifPresent(display -> display.setLocation(6.5F, 1.0F));
        for (AdvancementNode element : root.children()) {
            element.advancement().display().ifPresent(display -> {
                CustomModelData z = display.getIcon().get(DataComponents.CUSTOM_MODEL_DATA);
                if (z != null) {
                    float[] cell = cell(z.value());
                    display.setLocation(cell[0], cell[1]);
                }
            });
        }
    }

    public static float[] cell(int z) {
        int period = 0;
        while (z >= PERIOD_STARTS[period + 1]) {
            period++;
        }
        int i = z - PERIOD_STARTS[period];
        if (period == 0) {
            return new float[] {z == 1 ? 0 : 17, 0};
        }
        if (period <= 2) {
            return new float[] {i < 2 ? i : i + 10, period};
        }
        if (period <= 4) {
            return new float[] {i, period};
        }
        if (i >= 2 && i <= 16) {
            return new float[] {i, period + 2.5F};
        }
        return new float[] {i < 2 ? i : i - 14, period};
    }
}
