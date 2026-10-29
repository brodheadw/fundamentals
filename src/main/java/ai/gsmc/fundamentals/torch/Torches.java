package ai.gsmc.fundamentals.torch;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.BiConsumer;

/**
 * Torches no longer burn down. The burnt-out stubs of 0.19.4 are kept so worlds and inventories that hold them still load: a stub
 * in the world lights itself again on its next random tick, and one in hand is re-dipped into a torch with a coal.
 */
public final class Torches {

    private static Block burnt;
    private static Block burntWall;
    private static Item burntItem;

    private Torches() {}

    public static Block burnt() { return burnt; }
    public static Block burntWall() { return burntWall; }
    public static Item burntItem() { return burntItem; }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        BlockBehaviour.Properties stub = BlockBehaviour.Properties.of().noCollission().instabreak().sound(SoundType.WOOD)
                .pushReaction(PushReaction.DESTROY).randomTicks();
        registry.accept(Fundamentals.id("burnt_torch"), burnt = new BurntTorch(stub));
        registry.accept(Fundamentals.id("burnt_wall_torch"), burntWall = new BurntWallTorch(stub.dropsLike(burnt)));
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(Fundamentals.id("burnt_torch"), burntItem = new StandingAndWallBlockItem(burnt, burntWall, new Item.Properties(), Direction.DOWN));
    }

    static class BurntTorch extends TorchBlock {
        BurntTorch(Properties properties) {
            super(ParticleTypes.SMOKE, properties);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {}

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            level.setBlockAndUpdate(pos, Blocks.TORCH.defaultBlockState());
        }
    }

    static class BurntWallTorch extends WallTorchBlock {
        BurntWallTorch(Properties properties) {
            super(ParticleTypes.SMOKE, properties);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {}

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            level.setBlockAndUpdate(pos, Blocks.WALL_TORCH.defaultBlockState().setValue(FACING, state.getValue(FACING)));
        }
    }
}
