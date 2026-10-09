package ai.gsmc.fundamentals.oxidation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/** The air a metal sits in: dry (desert, the Nether), ordinary, damp (touching water, a humid climate, rain on it) or salt (by the sea). */
public enum Moisture {
    DRY, AIR, WET, SALT;

    public static Moisture at(Level level, BlockPos pos, boolean rainReaches) {
        Holder<Biome> biome = level.getBiome(pos);
        boolean marine = biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH);
        boolean water = level.getFluidState(pos).is(FluidTags.WATER);
        for (Direction side : Direction.values()) {
            water |= level.getFluidState(pos.relative(side)).is(FluidTags.WATER);
        }
        if (water || rainReaches && level.isRainingAt(pos.above())) {
            return marine ? SALT : WET;
        }
        if (marine) {
            return SALT;
        }
        if (!biome.value().hasPrecipitation() || level.dimensionType().ultraWarm()) {
            return DRY;
        }
        return biome.value().getModifiedClimateSettings().downfall() >= 0.8F ? WET : AIR;
    }
}
