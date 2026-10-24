package ai.gsmc.fundamentals.content.nuclear;

import ai.gsmc.fundamentals.uses.Uses;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.List;

/** A burnt-out reactor rod comes back as spent fuel, which only cools under water: the spent fuel pool. */
public final class SpentFuel {

    /** A day in the pool, the game's stand-in for the years real fuel spends there. */
    public static final int COOL_TICKS = 24000;
    private static final String POOLED = "fundamentals:pooled";

    private SpentFuel() {}

    /** Called by the reactor as it burns a fuel rod: the spent rod goes back where the rods came from, or out on top. */
    public static void discharge(List<IItemHandler> handlers, Level level, BlockPos controller) {
        ItemStack spent = new ItemStack(Uses.item("spent_fuel_rod"));
        for (IItemHandler handler : handlers) {
            spent = ItemHandlerHelper.insertItemStacked(handler, spent, false);
            if (spent.isEmpty()) return;
        }
        Block.popResource(level, controller.above(), spent);
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        if (event.getEntity() instanceof ItemEntity entity && !entity.level().isClientSide && entity.getItem().is(Uses.item("spent_fuel_rod"))) {
            pool(entity, 1);
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
