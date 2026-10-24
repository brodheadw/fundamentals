package ai.gsmc.fundamentals.content.nuclear;

import ai.gsmc.fundamentals.uses.Uses;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/** A burnt-out reactor rod comes back as spent fuel, which only cools under water: the spent fuel pool. */
public final class SpentFuel {

    /** A day in the pool, the game's stand-in for the years real fuel spends there. */
    public static final int COOL_TICKS = 24000;
    private static final String POOLED = "fundamentals:pooled";

    private SpentFuel() {}

    /**
     * Called by the reactor as it burns a fuel rod. The rod inputs take only fresh rods, so the spent one goes into
     * whatever container sits on top of the controller, or out loose on top of it.
     */
    public static void discharge(Level level, BlockPos controller) {
        ItemStack spent = new ItemStack(Uses.item("spent_fuel_rod"));
        IItemHandler above = level.getCapability(Capabilities.ItemHandler.BLOCK, controller.above(), Direction.DOWN);
        if (above != null) spent = ItemHandlerHelper.insertItemStacked(above, spent, false);
        if (!spent.isEmpty()) Block.popResource(level, controller.above(), spent);
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof ItemEntity entity && !entity.level().isClientSide && entity.getItem().is(Uses.item("spent_fuel_rod"))) {
            pool(entity, 1);
        }
    }

    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        String key = stack.is(Uses.item("spent_fuel_rod")) ? "spent_fuel_rod" : stack.is(Uses.item("waste_canister")) ? "waste_canister" : null;
        if (key != null && !event.getToolTip().isEmpty()) {
            event.getToolTip().add(1, Component.translatable("tooltip.fundamentals.nuclear." + key).withStyle(ChatFormatting.GRAY));
        }
    }

    /** Counts the time a spent rod lies under water; out of the water the count waits. */
    public static void pool(ItemEntity entity, int ticks) {
        if (!entity.isInWater()) return;
        entity.setUnlimitedLifetime();
        // the blue of Cherenkov light, as real spent fuel glows in its pool
        if (entity.level() instanceof ServerLevel level && entity.tickCount % 10 == 0) {
            level.sendParticles(ParticleTypes.GLOW, entity.getX(), entity.getY() + 0.2, entity.getZ(), 2, 0.15, 0.1, 0.15, 0.01);
        }
        CompoundTag data = entity.getPersistentData();
        int pooled = data.getInt(POOLED) + ticks;
        if (pooled < COOL_TICKS) {
            data.putInt(POOLED, pooled);
            return;
        }
        data.remove(POOLED);
        entity.setItem(new ItemStack(Uses.item("cooled_spent_fuel"), entity.getItem().getCount()));
    }
}
