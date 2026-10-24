package ai.gsmc.fundamentals.magnet;

import ai.gsmc.fundamentals.mixin.FishingHookAccessor;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.lang.reflect.Method;

/**
 * A magnet in the off hand rides Hybrid Aquatic's bobber as its lure (data/hybrid_aquatic/tags/item/lure_items) and
 * drags treasure up: luck in proportion to the grade's strength and the field the magnet has left.
 */
public final class MagnetLure {

    public static final int FULL_LUCK = 20;
    private static Method lure;

    private MagnetLure() {}

    public static int luck(ItemStack stack) {
        MagnetCharge charge = stack.get(Magnets.CHARGE);
        MagnetGrade grade = charge != null ? charge.grade() : Magnets.grade(stack);
        if (grade == null) return 0;
        return Math.round(FULL_LUCK * grade.strength * (charge != null ? charge.field() : 1));
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof FishingHook hook)) return;
        int luck = luck(lureOf(hook));
        if (luck > 0) ((FishingHookAccessor) hook).fundamentals$setLuck(((FishingHookAccessor) hook).fundamentals$luck() + luck);
    }

    // Hybrid Aquatic keeps the lure on its own mixin interface; read it without depending on the mod
    private static ItemStack lureOf(FishingHook hook) {
        try {
            if (lure == null) lure = hook.getClass().getMethod("getLureItem");
            return lure.invoke(hook) instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        } catch (ReflectiveOperationException e) {
            return ItemStack.EMPTY;
        }
    }

    public static void onTooltip(ItemTooltipEvent event) {
        int luck = luck(event.getItemStack());
        if (luck > 0 && ModList.get().isLoaded("hybrid_aquatic") && !event.getToolTip().isEmpty()) {
            event.getToolTip().add(Component.translatable("tooltip.fundamentals.magnet.lure", luck).withStyle(ChatFormatting.DARK_AQUA));
        }
    }
}
