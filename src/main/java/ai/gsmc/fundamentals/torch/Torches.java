package ai.gsmc.fundamentals.torch;

import ai.gsmc.fundamentals.Fundamentals;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Torches burn down. A torch in the world goes out about eight days after it was lit and leaves a charred stub, which flint and
 * steel or a fire charge relights, or a lump of coal re-dips into a fresh torch. The day each torch goes out is kept on its chunk;
 * a torch with none recorded, from before this or from a structure, is counted as lit when first noticed.
 */
public final class Torches {

    static final long DAY = 24000;
    // a pitch torch burns an hour or so; at the game's pace that is stretched to about a week, give or take two days
    static final long LIFE = 8 * DAY;
    static final long SPREAD = 2 * DAY;

    /** Each torch's day of going out, by its packed position. */
    public static final AttachmentType<Map<String, Long>> OUT_AT = AttachmentType.<Map<String, Long>>builder(() -> new HashMap<>())
            .serialize(Codec.unboundedMap(Codec.STRING, Codec.LONG).xmap(HashMap::new, map -> map)).build();

    private static Block burnt;
    private static Block burntWall;
    private static Item burntItem;

    private Torches() {}

    public static Block burnt() { return burnt; }
    public static Block burntWall() { return burntWall; }
    public static Item burntItem() { return burntItem; }

    public static void registerAttachments(BiConsumer<ResourceLocation, AttachmentType<?>> registry) {
        registry.accept(Fundamentals.id("torches_out_at"), OUT_AT);
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        BlockBehaviour.Properties stub = BlockBehaviour.Properties.of().noCollission().instabreak().sound(SoundType.WOOD).pushReaction(PushReaction.DESTROY);
        registry.accept(Fundamentals.id("burnt_torch"), burnt = new BurntTorch(stub));
        registry.accept(Fundamentals.id("burnt_wall_torch"), burntWall = new BurntWallTorch(stub.dropsLike(burnt)));
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(Fundamentals.id("burnt_torch"), burntItem = new StandingAndWallBlockItem(burnt, burntWall, new Item.Properties(), Direction.DOWN));
    }

    /** Only the plain torch burns down; soul fire, redstone and other mods' lights are left alone. */
    public static boolean burns(BlockState state) {
        return state.is(Blocks.TORCH) || state.is(Blocks.WALL_TORCH);
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level && burns(event.getPlacedBlock())) {
            lit(level, event.getPos());
        }
    }

    /** Called on each random tick of a torch: notes when an unrecorded torch goes out, and puts out one whose day has come. */
    public static void tick(BlockState state, ServerLevel level, BlockPos pos) {
        Long out = outAt(level, pos);
        long now = level.getDayTime();
        // none recorded, or a record the clock has been set back past
        if (out == null || out - now > LIFE + SPREAD) {
            lit(level, pos);
        } else if (now >= out) {
            putOut(state, level, pos);
        }
    }

    public static Long outAt(ServerLevel level, BlockPos pos) {
        return level.getChunkAt(pos).getData(OUT_AT).get(key(pos));
    }

    public static void lit(ServerLevel level, BlockPos pos) {
        RandomSource random = level.getRandom();
        record(level, pos, level.getDayTime() + LIFE + random.nextLong() % (SPREAD + 1));
    }

    static void record(ServerLevel level, BlockPos pos, Long out) {
        LevelChunk chunk = level.getChunkAt(pos);
        Map<String, Long> map = chunk.getData(OUT_AT);
        if (out == null) {
            map.remove(key(pos));
        } else {
            map.put(key(pos), out);
        }
        chunk.setUnsaved(true);
    }

    static void putOut(BlockState state, ServerLevel level, BlockPos pos) {
        BlockState stub = state.is(Blocks.WALL_TORCH)
                ? burntWall.defaultBlockState().setValue(WallTorchBlock.FACING, state.getValue(WallTorchBlock.FACING))
                : burnt.defaultBlockState();
        level.setBlockAndUpdate(pos, stub);
        record(level, pos, null);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3F, 1.6F);
        level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 4, 0.05, 0.05, 0.05, 0.01);
    }

    private static String key(BlockPos pos) {
        return Long.toString(pos.asLong());
    }

    // flint and steel or a fire charge lights the stub again; it is a fresh torch with a fresh week
    static ItemInteractionResult relight(ItemStack stack, BlockState lit, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level instanceof ServerLevel server) {
            level.setBlockAndUpdate(pos, lit);
            lit(server, pos);
            level.playSound(null, pos, stack.is(Items.FIRE_CHARGE) ? SoundEvents.FIRECHARGE_USE : SoundEvents.FLINTANDSTEEL_USE,
                    SoundSource.BLOCKS, 1.0F, 1.0F);
            if (stack.is(Items.FIRE_CHARGE)) {
                stack.consume(1, player);
            } else {
                stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    static class BurntTorch extends TorchBlock {
        BurntTorch(Properties properties) {
            super(ParticleTypes.SMOKE, properties);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {}

        @Override
        protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return relight(stack, Blocks.TORCH.defaultBlockState(), level, pos, player, hand);
        }
    }

    static class BurntWallTorch extends WallTorchBlock {
        BurntWallTorch(Properties properties) {
            super(ParticleTypes.SMOKE, properties);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {}

        @Override
        protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return relight(stack, Blocks.WALL_TORCH.defaultBlockState().setValue(FACING, state.getValue(FACING)), level, pos, player, hand);
        }
    }
}
