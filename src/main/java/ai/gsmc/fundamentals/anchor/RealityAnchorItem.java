package ai.gsmc.fundamentals.anchor;

import com.wildspell.fundamental.api.chaos.Chaos;
import com.wildspell.fundamental.api.field.Anchors;
import com.wildspell.fundamental.api.field.Field;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * A Scranton-style reality anchor: switched on and carried, it holds the place round its bearer at baseline, so the field
 * there reads still and empty and nothing that works through it can work, the bearer's own magic included. It runs on its
 * cell, a charge a second, and harder against a strong field, as an anchor strains against what pushes on it. A fresh
 * lead-acid cell at an anvil recharges it.
 */
public class RealityAnchorItem extends Item {
    /** Seconds of running a full cell gives in still air: an hour. */
    public static final int CHARGE = 3600;
    public static final double REACH = 8.0;
    /** Charge a second spent against a field at full strength, beyond the second's own. */
    public static final float STRAIN = 4.0F;
    /** Chaos it drains from its place a second. */
    public static final float STILLS = 5.0F;
    private static final String ON = "anchor_on";
    private static final ResourceLocation CELL = ResourceLocation.fromNamespaceAndPath("fundamentals", "lead_acid_cell");

    public RealityAnchorItem(Properties properties) {
        super(properties);
    }

    public static boolean on(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBoolean(ON);
    }

    public static void set(ItemStack stack, boolean on) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(ON, on));
    }

    public static boolean spent(ItemStack stack) {
        return stack.getDamageValue() >= stack.getMaxDamage() - 1;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            boolean now = !on(stack) && !spent(stack);
            set(stack, now);
            player.displayClientMessage(Component.translatable(spent(stack) ? "item.fundamentals.reality_anchor.spent"
                    : now ? "item.fundamentals.reality_anchor.on" : "item.fundamentals.reality_anchor.off"), true);
            level.playSound(null, player.blockPosition(), now ? SoundEvents.BEACON_ACTIVATE : SoundEvents.BEACON_DEACTIVATE, SoundSource.PLAYERS, 0.6F, 1.4F);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server) || !on(stack) || server.getGameTime() % 20 != 0) return;
        if (spent(stack)) {
            set(stack, false);
            return;
        }
        hold(server, entity, stack);
    }

    /** One second of holding: the place at baseline, its chaos drained, the cell spent by the push against it. */
    public static void hold(ServerLevel level, Entity bearer, ItemStack stack) {
        var at = bearer.blockPosition();
        float push = Field.equilibrium(level, at).intensity();
        Anchors.hold(level, at, REACH, 30);
        Chaos.take(level, at, STILLS);
        int spend = 1 + Math.round(push * STRAIN);
        stack.setDamageValue(Math.min(stack.getMaxDamage() - 1, stack.getDamageValue() + spend));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return on(stack);
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack repair) {
        return BuiltInRegistries.ITEM.getKey(repair.getItem()).equals(CELL);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        int left = stack.getMaxDamage() - 1 - stack.getDamageValue();
        tooltip.add(Component.translatable(on(stack) ? "item.fundamentals.reality_anchor.tooltip.on" : "item.fundamentals.reality_anchor.tooltip.off")
                .withStyle(on(stack) ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.fundamentals.reality_anchor.tooltip.charge", left / 60, left % 60).withStyle(ChatFormatting.GRAY));
    }
}
