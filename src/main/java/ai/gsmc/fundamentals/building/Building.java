package ai.gsmc.fundamentals.building;

import ai.gsmc.fundamentals.Fundamentals;
import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/** Sheet-metal doors and trapdoors, and metal roofing: tinplate and corrugated galvanized iron, which the noon sun makes too hot to stand on. */
public final class Building {

    /** Metals the doors and trapdoors come in; copper and iron are vanilla's. */
    public static final List<String> METALS = List.of("brass", "bronze", "tin", "silver", "gold", "stainless_steel", "galvanized_steel", "titanium");
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
            // hand-opened like copper's: they are household doors, not vault doors
            block(registry, metal + "_door", new DoorBlock(BlockSetType.COPPER, BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_DOOR)));
            block(registry, metal + "_trapdoor", new TrapDoorBlock(BlockSetType.COPPER, BlockBehaviour.Properties.ofFullCopy(Blocks.COPPER_TRAPDOOR)));
        }
        for (String roof : ROOFING) {
            Roof base = new Roof(sheet());
            block(registry, roof, base);
            block(registry, roof + "_stairs", new RoofStairs(base.defaultBlockState(), sheet()));
            block(registry, roof + "_slab", new RoofSlab(sheet()));
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
