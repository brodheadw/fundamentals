package ai.gsmc.fundamentals.oxidation;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;

import java.util.List;

/**
 * A steel canister flushed with argon, holding one stack sealed away from the air. Right-click a stack onto it (or it onto a
 * stack) to seal it in; right-click it empty-handed to open it, which lets the argon go and leaves an empty canister. What is
 * sealed inside never ages: the ageing pass only ever looks at the canister.
 */
public class CanisterItem extends Item {

    public CanisterItem(Properties properties) {
        super(properties);
    }

    public static ItemStack contents(ItemStack canister) {
        return canister.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyOne();
    }

    public static void seal(ItemStack canister, ItemStack stack) {
        canister.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(List.of(stack)));
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack canister, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY) {
            return false;
        }
        ItemStack inside = contents(canister);
        if (inside.isEmpty() && slot.hasItem()) {
            ItemStack taken = slot.safeTake(slot.getItem().getCount(), slot.getItem().getCount(), player);
            if (!taken.isEmpty()) {
                seal(canister, taken);
            }
            return true;
        }
        if (!inside.isEmpty() && !slot.hasItem() && slot.mayPlace(inside)) {
            slot.safeInsert(inside);
            player.containerMenu.setCarried(new ItemStack(Oxidation.canister()));
            return true;
        }
        return false;
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack canister, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess carried) {
        if (action != ClickAction.SECONDARY || !slot.allowModification(player)) {
            return false;
        }
        ItemStack inside = contents(canister);
        if (inside.isEmpty() && !other.isEmpty()) {
            seal(canister, other.copy());
            carried.set(ItemStack.EMPTY);
            return true;
        }
        if (!inside.isEmpty() && other.isEmpty()) {
            carried.set(inside);
            slot.set(new ItemStack(Oxidation.canister()));
            return true;
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack canister, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ItemStack inside = contents(canister);
        tooltip.add((inside.isEmpty() ? Component.translatable("item.fundamentals.argon_canister.empty")
                : Component.translatable("item.fundamentals.argon_canister.holds", inside.getCount(), inside.getHoverName())).withStyle(ChatFormatting.GRAY));
    }
}
