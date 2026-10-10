package ai.gsmc.fundamentals.worldgen;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.registry.OreBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;

public final class MagnetiteVeins {

    public interface Vein {
        void fundamentals$setBlocks(BlockState ore, BlockState rich);
    }

    public static volatile Vein iron;

    private static volatile boolean applied;

    private MagnetiteVeins() {}

    // The vein type is created before any mod block is registered, so this runs as terrain starts generating.
    public static void apply() {
        if (applied || iron == null) {
            return;
        }
        BlockState magnetite = BuiltInRegistries.BLOCK
                .get(Fundamentals.id("magnetite_ore"))
                .defaultBlockState();
        if (!magnetite.hasProperty(OreBlock.GRADE)) {
            return;
        }
        magnetite = magnetite.setValue(OreBlock.HOST, OreBlock.Host.DEEPSLATE);
        iron.fundamentals$setBlocks(magnetite.setValue(OreBlock.GRADE, OreBlock.Grade.EDGE),
                magnetite.setValue(OreBlock.GRADE, OreBlock.Grade.CORE));
        applied = true;
    }
}
