package ai.gsmc.fundamentals.torch;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Torches no longer burn down, and the burnt-out stubs of 0.19.4 are gone: a world or inventory that still holds one reads it as a
 * plain torch, a wall stub as a wall torch facing the same way.
 */
public final class Torches {

    private Torches() {}

    public static void aliasBlocks(Registry<Block> blocks) {
        blocks.addAlias(Fundamentals.id("burnt_torch"), ResourceLocation.withDefaultNamespace("torch"));
        blocks.addAlias(Fundamentals.id("burnt_wall_torch"), ResourceLocation.withDefaultNamespace("wall_torch"));
    }

    public static void aliasItems(Registry<Item> items) {
        items.addAlias(Fundamentals.id("burnt_torch"), ResourceLocation.withDefaultNamespace("torch"));
    }
}
