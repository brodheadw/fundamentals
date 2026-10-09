package ai.gsmc.fundamentals.plastics;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.separation.PlasticTankBlock;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Polymers: the Ziegler-Natta catalyst The Factory Must Grow's olefins now polymerise over, PVC from ethylene and chlorine, plastic
 * blocks in the sixteen dyes, and dyeing: a dye on a plastic tank colours the whole tank, and on a plastic pipe makes it a
 * {@link DyedPlasticPipeBlock}. The recipes are written by tools/build_uses_data.py.
 */
public final class Plastics {

    private static final ResourceLocation PLASTIC_PIPE = ResourceLocation.parse("tfmg:plastic_pipe");
    private static final Map<DyeColor, Block> BLOCKS = new EnumMap<>(DyeColor.class);
    private static final List<Item> ITEMS = new ArrayList<>();
    private static Block dyedPipe;
    private static BlockEntityType<FluidPipeBlockEntity> dyedPipeEntity;

    private Plastics() {}

    public static Block dyedPipe() { return dyedPipe; }
    public static BlockEntityType<FluidPipeBlockEntity> dyedPipeEntity() { return dyedPipeEntity; }
    public static List<Item> items() { return ITEMS; }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        for (DyeColor dye : DyeColor.values()) {
            Block block = new Block(BlockBehaviour.Properties.of().mapColor(dye.getMapColor()).requiresCorrectToolForDrops()
                    .strength(1.5F, 6.0F).sound(SoundType.STONE));
            BLOCKS.put(dye, block);
            registry.accept(id(dye.getSerializedName() + "_plastic_block"), block);
        }
        dyedPipe = new DyedPlasticPipeBlock(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).forceSolidOn().strength(1.0F).sound(SoundType.STONE));
        registry.accept(id("dyed_plastic_pipe"), dyedPipe);
    }

    public static void registerBlockEntities(BiConsumer<ResourceLocation, BlockEntityType<?>> registry) {
        dyedPipeEntity = BlockEntityType.Builder.of((pos, state) -> new FluidPipeBlockEntity(dyedPipeEntity, pos, state), dyedPipe).build(null);
        registry.accept(id("dyed_plastic_pipe"), dyedPipeEntity);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        for (String name : new String[] {"ziegler_natta_catalyst", "pvc_resin", "pvc_sheet"}) {
            Item item = new Item(new Item.Properties());
            ITEMS.add(item);
            registry.accept(id(name), item);
        }
        BLOCKS.forEach((dye, block) -> {
            Item item = new BlockItem(block, new Item.Properties());
            ITEMS.add(item);
            registry.accept(id(dye.getSerializedName() + "_plastic_block"), item);
        });
    }

    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        Level level = event.getLevel();
        if (!(stack.getItem() instanceof DyeItem dye) || !dyeable(level.getBlockState(event.getPos()))) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (!level.isClientSide && dye(level, event.getPos(), dye.getDyeColor())) {
            stack.consume(1, event.getEntity());
            level.playSound(null, event.getPos(), SoundEvents.DYE_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    public static boolean dyeable(BlockState state) {
        return state.getBlock() instanceof PlasticTankBlock || state.getBlock() instanceof DyedPlasticPipeBlock
                || BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(PLASTIC_PIPE);
    }

    /** Colours the plastic at {@code pos}: a whole tank, or one pipe. False if it was that colour already. */
    public static boolean dye(Level level, BlockPos pos, DyeColor dye) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof PlasticTankBlock) {
            FluidTankBlockEntity tank = level.getBlockEntity(pos) instanceof FluidTankBlockEntity here ? here.getControllerBE() : null;
            BlockPos origin = tank == null ? pos : tank.getBlockPos();
            int width = tank == null ? 1 : tank.getWidth(), height = tank == null ? 1 : tank.getHeight();
            boolean changed = false;
            for (BlockPos at : BlockPos.betweenClosed(origin, origin.offset(width - 1, height - 1, width - 1))) {
                BlockState part = level.getBlockState(at);
                if (part.getBlock() instanceof PlasticTankBlock && part.getValue(PlasticTankBlock.COLOR) != Pigment.of(dye)) {
                    level.setBlockAndUpdate(at, part.setValue(PlasticTankBlock.COLOR, Pigment.of(dye)));
                    changed = true;
                }
            }
            return changed;
        }
        if (state.getBlock() instanceof DyedPlasticPipeBlock) {
            if (state.getValue(DyedPlasticPipeBlock.COLOR) == dye) {
                return false;
            }
            level.setBlockAndUpdate(pos, state.setValue(DyedPlasticPipeBlock.COLOR, dye));
            return true;
        }
        if (!BuiltInRegistries.BLOCK.getKey(state.getBlock()).equals(PLASTIC_PIPE)) {
            return false;
        }
        BlockState dyed = dyedPipe.defaultBlockState().setValue(DyedPlasticPipeBlock.COLOR, dye);
        for (Property<?> property : state.getProperties()) {
            dyed = copy(state, dyed, property);
        }
        FluidTransportBehaviour.cacheFlows(level, pos);
        level.setBlockAndUpdate(pos, dyed);
        FluidTransportBehaviour.loadFlows(level, pos);
        return true;
    }

    private static <T extends Comparable<T>> BlockState copy(BlockState from, BlockState to, Property<T> property) {
        return to.hasProperty(property) ? to.setValue(property, from.getValue(property)) : to;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, path);
    }
}
