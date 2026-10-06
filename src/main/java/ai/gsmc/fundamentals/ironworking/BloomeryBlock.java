package ai.gsmc.fundamentals.ironworking;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A bloomery: the clay shaft furnace of antiquity and the Middle Ages, where charcoal reduces an
 * oxide ore by hand. For iron it never gets hot enough to melt the metal, so out comes a solid,
 * spongy <em>bloom</em> to be hammered into wrought iron; copper and lead do melt and come out as
 * metal. What goes in and what comes out is data: see {@link BloomeryRecipe}.
 *
 * <p>Worked by hand, with no GUI: put in iron ore and at least as much charcoal, light it with a
 * torch (or flint and steel), wait, then take out the blooms and slag with an empty hand.
 */
public class BloomeryBlock extends BaseEntityBlock {

    public static final MapCodec<BloomeryBlock> CODEC = simpleCodec(BloomeryBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public BloomeryBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    /** A footing, a tapering shaft, a throat: the shape of the model. */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(1, 0, 1, 15, 2, 15), Block.box(2, 2, 2, 14, 10, 14), Block.box(3, 10, 3, 13, 16, 13));

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BloomeryBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || !state.getValue(LIT) ? null
                : createTickerHelper(type, IronWorking.bloomeryEntity(), BloomeryBlockEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BloomeryBlockEntity bloomery)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        boolean used;
        if (BloomeryBlockEntity.recipeFor(level, stack).isPresent()) {
            used = bloomery.addOre(stack);
        } else if (stack.is(Items.CHARCOAL)) {
            // Charcoal only: mineral coal's sulfur makes iron brittle, which is why smiths avoided it.
            used = bloomery.addCharcoal();
        } else if (stack.is(Items.TORCH) || stack.is(Items.FLINT_AND_STEEL) || stack.is(Items.FIRE_CHARGE)) {
            // A torch has to work: flint and steel needs iron, and this is how you get your first.
            used = bloomery.ignite();
            if (used && !level.isClientSide) {
                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (stack.is(Items.FLINT_AND_STEEL)) {
                    stack.hurtAndBreak(1, player, net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
                } else {
                    stack.consume(1, player);
                }
            }
            return used ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        } else {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!used) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            stack.consume(1, player);
            level.playSound(null, pos, SoundEvents.GRAVEL_PLACE, SoundSource.BLOCKS, 0.8F, 0.9F);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof BloomeryBlockEntity bloomery && bloomery.hasProducts()) {
            if (!level.isClientSide) {
                bloomery.takeProducts(pos.relative(state.getValue(FACING)));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof BloomeryBlockEntity bloomery) {
            bloomery.dropContents();
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
        level.addParticle(ParticleTypes.LARGE_SMOKE, x + (random.nextDouble() - 0.5) * 0.3, y, z + (random.nextDouble() - 0.5) * 0.3, 0, 0.04, 0);
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.FLAME, x, y - 0.1, z, 0, 0.02, 0);
        }
        if (random.nextInt(12) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.BLASTFURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.8F, 1.0F, false);
        }
    }
}
