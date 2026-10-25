package ai.gsmc.fundamentals.cold;

import com.momosoftworks.coldsweat.common.item.FilledWaterskinItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Cold Sweat's filled waterskin behind a vacuum: it drinks, pours and warms the same, and loses its heat ten times slower. */
final class FilledFlask extends FilledWaterskinItem {

    // Cold Sweat's waterskin steps toward neutral every 5 ticks in the hand or hotbar, from 50 in about 2,500 ticks (two and a half game hours);
    // letting every tenth step through makes that 25,000, about a day, as a real flask keeps a drink hot overnight
    static final int SLOWER = 10;

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (entity.tickCount % (5 * SLOWER) == 0) {
            super.inventoryTick(stack, level, entity, slot, selected);
        }
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return super.getCraftingRemainingItem(stack).transmuteCopy(Cold.flask());
    }

    @Override
    public String getDescriptionId() {
        return getOrCreateDescriptionId();
    }
}
