package ai.gsmc.fundamentals.magnet;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class MagnetRebuildRecipe extends CustomRecipe {

    public static final TagKey<Item> MACHINES = TagKey.create(Registries.ITEM, Fundamentals.id("magnet_machines"));

    public MagnetRebuildRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !assemble(input, level.registryAccess()).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack machine = ItemStack.EMPTY;
        MagnetGrade grade = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (stack.is(MACHINES) && machine.isEmpty()) {
                machine = stack;
            } else if (Magnets.grade(stack) != null && grade == null) {
                grade = Magnets.grade(stack);
            } else {
                return ItemStack.EMPTY;
            }
        }
        if (machine.isEmpty() || grade == null) {
            return ItemStack.EMPTY;
        }
        ItemStack rebuilt = machine.copyWithCount(1);
        rebuilt.set(Magnets.CHARGE, new MagnetCharge(grade, 1));
        return rebuilt;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Magnets.REBUILD;
    }
}
