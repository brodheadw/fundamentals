package ai.gsmc.fundamentals.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * A hand tool used as an ingredient (the smithing hammer, the mortar and pestle): it stays in the
 * crafting grid and wears by one use each time.
 */
public class HandToolItem extends Item {

    public HandToolItem(Properties properties) {
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
