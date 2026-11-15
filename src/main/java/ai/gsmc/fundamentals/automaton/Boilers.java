package ai.gsmc.fundamentals.automaton;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

/** A Create boiler raises steam when its tank holds water over heat; an automaton fills its own small boiler from one. */
public final class Boilers {

    private Boilers() {}

    /** Whether the tank block at pos belongs to a boiler that is raising steam right now. */
    public static boolean steaming(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof FluidTankBlockEntity tank)) return false;
        FluidTankBlockEntity controller = tank.getControllerBE();
        if (controller == null) return false;
        var boiler = controller.boiler;
        return boiler.waterSupply > 0 && (boiler.activeHeat > 0 || boiler.passiveHeat);
    }

    /** The nearest steaming boiler tank within radius of from, or null. */
    public static BlockPos nearest(Level level, BlockPos from, int radius) {
        BlockPos best = null;
        double bestDistance = (double) radius * radius;
        int minChunkX = (from.getX() - radius) >> 4, maxChunkX = (from.getX() + radius) >> 4;
        int minChunkZ = (from.getZ() - radius) >> 4, maxChunkZ = (from.getZ() + radius) >> 4;
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!level.hasChunk(cx, cz)) continue;
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!(be instanceof FluidTankBlockEntity)) continue;
                    double d = be.getBlockPos().distSqr(from);
                    if (d < bestDistance && steaming(level, be.getBlockPos())) {
                        bestDistance = d;
                        best = be.getBlockPos();
                    }
                }
            }
        }
        return best;
    }
}
