package ai.gsmc.fundamentals.separation;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.heat.Heat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The acids exist in the world, not only in pipes: each has a flowing form, a block and a bucket, hurts whatever
 * stands in it, and eats the blocks it really attacks, listed in the block tag {@code fundamentals:dissolves/<acid>}
 * (hydrochloric acid takes carbonates, hydrofluoric glass and silica, nitric copper and iron, aqua regia gold as well; phosphoric acid only
 * stings). Bromine is no acid but lives with them: it burns, fumes and poisons like hydrofluoric, and eats copper, iron and aluminium, and
 * polyethylene too. A block being eaten cracks like one being mined, over five seconds, then fizzes away, and the acid
 * that ate it is spent.
 */
public final class Acids {

    /** Ticks between bites, and bites to eat a block through at 20 °C: reactions go twice as fast for every ten degrees. */
    private static final int BITE = 20;
    private static final int BITES = 5;

    static int bitesAt(Level level, BlockPos pos) {
        return (int) Math.round(Math.max(2, Math.min(10, BITES * Math.pow(2, (20 - Heat.at(level, pos)) / 10))));
    }

    public static final class Acid {
        public final String id;
        public final float damage;
        public final boolean poisons;
        /** Hydrochloric, hydrofluoric and nitric acid, aqua regia and bromine fume in the open: standing near them bare hurts. */
        public final boolean fumes;
        public final TagKey<Block> dissolves;
        public final FlowingFluid source;
        public final FlowingFluid flowing;
        public final AcidBlock block;
        public final Item bucket;

        private Acid(String id, float damage, boolean poisons, boolean fumes) {
            this.id = id;
            this.damage = damage;
            this.poisons = poisons;
            this.fumes = fumes;
            this.dissolves = BlockTags.create(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "dissolves/" + id));
            FlowingFluid[] pair = new FlowingFluid[2];
            Block[] block = new Block[1];
            Item[] bucket = new Item[1];
            BaseFlowingFluid.Properties properties = new BaseFlowingFluid.Properties(() -> Separation.fluidTypes().get(id), () -> pair[0], () -> pair[1])
                    .block(() -> (LiquidBlock) block[0]).bucket(() -> bucket[0]).levelDecreasePerBlock(2).slopeFindDistance(3);
            pair[0] = source = new BaseFlowingFluid.Source(properties);
            pair[1] = flowing = new BaseFlowingFluid.Flowing(properties);
            block[0] = this.block = new AcidBlock(this, BlockBehaviour.Properties.ofFullCopy(Blocks.WATER));
            bucket[0] = this.bucket = new BucketItem(source, new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1));
        }
    }

    private static Map<String, Acid> all;

    private Acids() {}

    /** Every acid in the reagent table, built once; the registries take the pieces from here. */
    public static Map<String, Acid> all() {
        if (all == null) {
            all = new LinkedHashMap<>();
            for (Reagents.Reagent reagent : Reagents.ALL) {
                if (reagent.kind() == Reagents.Kind.ACID) {
                    boolean toxic = reagent.id().equals("hydrofluoric_acid") || reagent.id().equals("bromine");
                    float damage = toxic ? 2.0F : reagent.id().equals("phosphoric_acid") ? 0.5F : 1.0F;
                    all.put(reagent.id(), new Acid(reagent.id(), damage, toxic, !reagent.id().equals("phosphoric_acid")));
                }
            }
        }
        return all;
    }

    public static boolean isAcid(Fluid fluid) {
        return all().values().stream().anyMatch(a -> a.source == fluid || a.flowing == fluid);
    }

    public static class AcidBlock extends LiquidBlock {
        /** How far each block being eaten has got; server-side and transient, which is fine: a reload just starts the bite over. */
        private static final Map<BlockPos, Integer> EATEN = new HashMap<>();
        private final Acid acid;

        AcidBlock(Acid acid, Properties properties) {
            super(acid.source, properties);
            this.acid = acid;
        }

        public Acid acid() { return acid; }

        @Override
        protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
            super.onPlace(state, level, pos, oldState, movedByPiston);
            level.scheduleTick(pos, this, BITE);
        }

        @Override
        protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!level.getBlockState(pos).is(this)) {
                return;
            }
            if (acid.fumes) {
                Hazards.fume(level, pos, acid);
            }
            for (Direction side : new Direction[] {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                BlockPos at = pos.relative(side);
                BlockState target = level.getBlockState(at);
                if (!target.is(acid.dissolves)) {
                    continue;
                }
                int bites = EATEN.merge(at.immutable(), 1, Integer::sum);
                int needed = bitesAt(level, at);
                level.destroyBlockProgress(at.hashCode(), at, Math.min(9, bites * 10 / needed - 1));
                level.sendParticles(ParticleTypes.BUBBLE_POP, at.getX() + 0.5 - side.getStepX() * 0.6, at.getY() + 0.5 - side.getStepY() * 0.6 + 0.3,
                        at.getZ() + 0.5 - side.getStepZ() * 0.6, 4, 0.3, 0.1, 0.3, 0.0);
                if (bites == 1) {
                    level.playSound(null, at, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1.6F);
                }
                if (bites >= needed) {
                    EATEN.remove(at);
                    level.destroyBlockProgress(at.hashCode(), at, -1);
                    level.destroyBlock(at, false);
                    level.sendParticles(ParticleTypes.CLOUD, at.getX() + 0.5, at.getY() + 0.6, at.getZ() + 0.5, 10, 0.3, 0.2, 0.3, 0.02);
                    level.playSound(null, at, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 0.6F, 1.2F);
                    // the acid that did the eating is used up
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    return;
                }
            }
            level.scheduleTick(pos, this, BITE);
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            if (level.isClientSide || !(entity instanceof LivingEntity living)) {
                return;
            }
            living.hurt(level.damageSources().magic(), acid.damage);
            if (acid.poisons) {
                living.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
            }
        }
    }
}
