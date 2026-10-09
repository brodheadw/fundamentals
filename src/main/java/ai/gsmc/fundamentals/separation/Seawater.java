package ai.gsmc.fundamentals.separation;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;

import javax.annotation.Nullable;

/**
 * Water is salt where it is the sea. A water source drawn from an ocean or a beach, by bucket, hose pulley or open pipe, comes up as
 * seawater; a river, a lake, a swamp or a cave gives fresh water as ever. Seawater is no block of its own: it looks and behaves as water does, and poured out it is water again.
 */
public final class Seawater {

    private Seawater() {}

    public static boolean sea(Holder<Biome> biome) {
        return biome.is(BiomeTags.IS_OCEAN) || biome.is(BiomeTags.IS_BEACH);
    }

    /** Whether the block at {@code pos} is a water source standing in the sea. */
    public static boolean seaSource(LevelReader level, BlockPos pos, BlockState state) {
        return state.is(Blocks.WATER) && state.getFluidState().isSource() && sea(level.getBiome(pos));
    }

    /** Water's own properties, as NeoForge gives vanilla water's type: it swims, drowns, douses fire and hydrates as water does. */
    public static FluidType type(String descriptionId) {
        return new FluidType(FluidType.Properties.create()
                .descriptionId(descriptionId)
                .fallDistanceModifier(0F)
                .canExtinguish(true)
                .canConvertToSource(true)
                .supportsBoating(true)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)
                .canHydrate(true));
    }

    public static Fluid fluid() {
        return Separation.fluid("seawater");
    }

    /** {@code drawn}, as seawater if it is water and was drawn from the sea. */
    public static FluidStack drawn(FluidStack drawn) {
        return drawn.is(Fluids.WATER) ? new FluidStack(fluid(), drawn.getAmount()) : drawn;
    }

    public static final class BucketOfSeawater extends BucketItem {

        public BucketOfSeawater(Fluid content, Properties properties) {
            super(content, properties);
        }

        @Override
        public boolean emptyContents(@Nullable Player player, Level level, BlockPos pos, @Nullable BlockHitResult hit, @Nullable ItemStack container) {
            return ((BucketItem) Items.WATER_BUCKET).emptyContents(player, level, pos, hit, null);
        }

        @Override
        protected boolean canBlockContainFluid(@Nullable Player player, Level level, BlockPos pos, BlockState state) {
            return state.getBlock() instanceof LiquidBlockContainer container && container.canPlaceLiquid(player, level, pos, state, Fluids.WATER);
        }
    }
}
