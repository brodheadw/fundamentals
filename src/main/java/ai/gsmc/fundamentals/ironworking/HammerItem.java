package ai.gsmc.fundamentals.ironworking;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** A hammer used in a crafting recipe: it stays in the grid and wears by one use each time. */
public class HammerItem extends Item {

    public HammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack worn = stack.copy();
        worn.setDamageValue(worn.getDamageValue() + 1);
        return worn.getDamageValue() >= worn.getMaxDamage() ? ItemStack.EMPTY : worn;
    }
}
