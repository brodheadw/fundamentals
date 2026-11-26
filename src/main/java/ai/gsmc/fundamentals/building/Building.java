package ai.gsmc.fundamentals.building;

import ai.gsmc.fundamentals.Fundamentals;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.api.behaviour.movement.MovementBehaviour;
import com.simibubi.create.content.contraptions.behaviour.DoorMovingInteraction;
import com.simibubi.create.content.contraptions.behaviour.TrapdoorMovingInteraction;
import com.simibubi.create.content.decoration.TrainTrapdoorBlock;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorBlock;
import com.simibubi.create.content.decoration.slidingDoor.SlidingDoorMovementBehaviour;
import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** Create-style sliding doors and framed hatches in sheet metal, and metal roofing: tinplate and corrugated galvanized iron, which the noon sun makes too hot to stand on. */
public final class Building {

    /** Metals the doors and trapdoors come in; copper and iron are vanilla's. */
    public static final List<String> METALS = List.of("brass", "bronze", "tin", "silver", "gold", "stainless_steel", "galvanized_steel", "titanium");
    /** The metal whose door and hatch are blast-rated: they open only on a redstone signal, never by hand or for a passing train. */
    public static final String BLAST = "titanium";
    public static final List<String> ROOFING = List.of("tin_roofing", "corrugated_iron");
    /** How far over the air a sunlit roof runs at noon in a clear sky: bare sheet metal in summer sun reaches 60-70 °C. */
    public static final double SUN_EXCESS = 40.0;
    /** Sun strength above which a roof burns bare feet: the middle of the day. */
    public static final double SCORCHING = 0.8;

    private static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    private static final List<Item> ITEMS = new ArrayList<>();

    private Building() {}

    public static Block block(String id) { return BLOCKS.get(id); }
    public static List<Item> items() { return ITEMS; }

    /** 0 at night, under cover or in rain, 1 at clear noon. */
    public static double sun(Level level, BlockPos roof) {
        if (!level.dimensionType().hasSkyLight() || level.isRaining() || !level.canSeeSky(roof.above())) return 0;
        double day = (level.getDayTime() % 24000) / 24000.0;
        return Math.max(0, Math.cos((day - 0.25) * 2 * Math.PI));
    }

    public static void bake(ServerLevel level, BlockPos pos) {
        double sun = sun(level, pos);
        // a random tick comes about once a minute a block; the warmth lasts past the next
        if (sun > 0) Heat.boost(level, pos, SUN_EXCESS * sun, 2, 1800);
    }

    public static void scorch(Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living) || entity.isSteppingCarefully()
                || !living.getItemBySlot(EquipmentSlot.FEET).isEmpty() || sun(level, pos) < SCORCHING) return;
        entity.hurt(level.damageSources().hotFloor(), 1.0F);
    }

    private static BlockBehaviour.Properties sheet() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(3.0F, 6.0F).randomTicks();
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        for (String metal : METALS) {
            boolean blast = metal.equals(BLAST);
            block(registry, metal + "_door", blast ? new BlastSlidingDoor(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_DOOR).strength(25.0F, 1200.0F))
                    : SlidingDoorBlock.metal(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_DOOR), false));
            block(registry, metal + "_trapdoor", blast ? new BlastHatch(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_TRAPDOOR).strength(25.0F, 1200.0F))
                    : TrainTrapdoorBlock.metal(BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_TRAPDOOR)));
        }
        for (String roof : ROOFING) {
            Roof base = new Roof(sheet());
            block(registry, roof, base);
            block(registry, roof + "_stairs", new RoofStairs(base.defaultBlockState(), sheet()));
            block(registry, roof + "_slab", new RoofSlab(sheet()));
        }
    }

    /** Create's own sliding-door block entity and renderer drive the doors; this lets that type sit on them. */
    public static void adoptDoors(BlockEntityTypeAddBlocksEvent event) {
        event.modify(AllBlockEntityTypes.SLIDING_DOOR.get(), METALS.stream().map(metal -> block(metal + "_door")).toArray(Block[]::new));
    }

    /** Trains and contraptions open them as they do Create's own, all but the blast door and hatch, which answer only to a signal. */
    public static void commonSetup() {
        for (String metal : METALS) {
            if (metal.equals(BLAST)) continue;
            MovementBehaviour.REGISTRY.register(block(metal + "_door"), new SlidingDoorMovementBehaviour());
            MovingInteractionBehaviour.REGISTRY.register(block(metal + "_door"), new DoorMovingInteraction());
            MovingInteractionBehaviour.REGISTRY.register(block(metal + "_trapdoor"), new TrapdoorMovingInteraction());
        }
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        BLOCKS.forEach((id, block) -> {
            Item item = block instanceof DoorBlock ? new DoubleHighBlockItem(block, new Item.Properties()) : new BlockItem(block, new Item.Properties());
            ITEMS.add(item);
            registry.accept(Fundamentals.id(id), item);
        });
    }

    private static void block(BiConsumer<ResourceLocation, Block> registry, String id, Block block) {
        BLOCKS.put(id, block);
        registry.accept(Fundamentals.id(id), block);
    }

    private static void signalOnly(Level level, Player player) {
        if (!level.isClientSide) player.displayClientMessage(Component.translatable("block.fundamentals.blast.signal_only"), true);
    }

    public static class BlastSlidingDoor extends SlidingDoorBlock {
        public BlastSlidingDoor(Properties properties) { super(properties, SlidingDoorBlock.TRAIN_SET_TYPE.get(), false); }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            signalOnly(level, player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        // villagers and Create's station door control open doors through here
        @Override
        public void setOpen(@Nullable Entity entity, Level level, BlockState state, BlockPos pos, boolean open) {}
    }

    public static class BlastHatch extends TrainTrapdoorBlock {
        public BlastHatch(Properties properties) { super(SlidingDoorBlock.TRAIN_SET_TYPE.get(), properties); }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            signalOnly(level, player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
    }

    public static class Roof extends Block {
        public Roof(Properties properties) { super(properties); }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { bake(level, pos); }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            scorch(level, pos, entity);
            super.stepOn(level, pos, state, entity);
        }
    }

    public static class RoofStairs extends StairBlock {
        public RoofStairs(BlockState base, Properties properties) { super(base, properties); }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { bake(level, pos); }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            scorch(level, pos, entity);
            super.stepOn(level, pos, state, entity);
        }
    }

    public static class RoofSlab extends SlabBlock {
        public RoofSlab(Properties properties) { super(properties); }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) { bake(level, pos); }

        @Override
        public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
            scorch(level, pos, entity);
            super.stepOn(level, pos, state, entity);
        }
    }
}
