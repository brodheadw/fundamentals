package ai.gsmc.fundamentals.worldgen;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.registry.OreBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Vanilla's giant deep ore veins are hardcoded, not data: the iron one places deepslate iron ore
 * and raw iron blocks. With vanilla iron ore gone (PLAN §2.4) that vein becomes magnetite — ore
 * through the vein, rich "core" ore where vanilla put raw blocks. The copper vein is left alone:
 * vanilla copper ore is native copper.
 *
 * <p>The swap happens on first use rather than when the vein type is created, because on
 * NeoForge that is before any mod block is registered.
 */
public final class MagnetiteVeins {

    /** Implemented by the mixin on vanilla's vein type. */
    public interface Vein {
        void fundamentals$setBlocks(BlockState ore, BlockState rich);
    }

    /** Vanilla's iron vein type, captured by the mixin. */
    public static volatile Vein iron;

    private static volatile boolean applied;

    private MagnetiteVeins() {}

    public static void apply() {
        if (applied || iron == null) {
            return;
        }
        BlockState magnetite = BuiltInRegistries.BLOCK
                .get(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "magnetite_ore"))
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
