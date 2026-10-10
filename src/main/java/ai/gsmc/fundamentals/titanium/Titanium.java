package ai.gsmc.fundamentals.titanium;

import ai.gsmc.fundamentals.Fundamentals;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlockEntity;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import com.simibubi.create.content.fluids.tank.FluidTankItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

import java.util.List;
import java.util.function.BiConsumer;

public final class Titanium {

    private static Block pipe, pump, valve, tank;
    private static BlockEntityType<FluidPipeBlockEntity> pipeEntity;
    private static BlockEntityType<PumpBlockEntity> pumpEntity;
    private static BlockEntityType<FluidValveBlockEntity> valveEntity;
    private static BlockEntityType<TitaniumTankBlockEntity> tankEntity;
    private static List<Item> items = List.of();

    private Titanium() {}

    public static Block pipe() { return pipe; }
    public static Block pump() { return pump; }
    public static Block valve() { return valve; }
    public static Block tank() { return tank; }
    public static BlockEntityType<FluidPipeBlockEntity> pipeEntity() { return pipeEntity; }
    public static BlockEntityType<PumpBlockEntity> pumpEntity() { return pumpEntity; }
    public static BlockEntityType<FluidValveBlockEntity> valveEntity() { return valveEntity; }
    public static BlockEntityType<TitaniumTankBlockEntity> tankEntity() { return tankEntity; }
    public static List<Item> items() { return items; }

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_LIGHT_GRAY);
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        registry.accept(Fundamentals.id("titanium_pipe"), pipe = new TitaniumPipeBlock(metal().forceSolidOff()));
        registry.accept(Fundamentals.id("titanium_mechanical_pump"), pump = new TitaniumPumpBlock(metal()));
        registry.accept(Fundamentals.id("titanium_fluid_valve"), valve = new TitaniumValveBlock(metal().noOcclusion()));
        registry.accept(Fundamentals.id("titanium_fluid_tank"), tank = new TitaniumTankBlock(metal().noOcclusion().isRedstoneConductor((state, level, pos) -> true)));
    }

    public static void registerBlockEntities(BiConsumer<ResourceLocation, BlockEntityType<?>> registry) {
        pipeEntity = BlockEntityType.Builder.of((pos, state) -> new FluidPipeBlockEntity(pipeEntity, pos, state), pipe).build(null);
        pumpEntity = BlockEntityType.Builder.of((pos, state) -> new PumpBlockEntity(pumpEntity, pos, state), pump).build(null);
        valveEntity = BlockEntityType.Builder.of((pos, state) -> new FluidValveBlockEntity(valveEntity, pos, state), valve).build(null);
        tankEntity = BlockEntityType.Builder.of(TitaniumTankBlockEntity::new, tank).build(null);
        registry.accept(Fundamentals.id("titanium_pipe"), pipeEntity);
        registry.accept(Fundamentals.id("titanium_mechanical_pump"), pumpEntity);
        registry.accept(Fundamentals.id("titanium_fluid_valve"), valveEntity);
        registry.accept(Fundamentals.id("titanium_fluid_tank"), tankEntity);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        Item pipeItem = new BlockItem(pipe, new Item.Properties()), pumpItem = new BlockItem(pump, new Item.Properties());
        Item valveItem = new BlockItem(valve, new Item.Properties()), tankItem = new FluidTankItem(tank, new Item.Properties());
        registry.accept(Fundamentals.id("titanium_pipe"), pipeItem);
        registry.accept(Fundamentals.id("titanium_mechanical_pump"), pumpItem);
        registry.accept(Fundamentals.id("titanium_fluid_valve"), valveItem);
        registry.accept(Fundamentals.id("titanium_fluid_tank"), tankItem);
        items = List.of(pipeItem, pumpItem, valveItem, tankItem);
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, tankEntity, TitaniumTankBlockEntity::handler);
    }

    public static void commonSetup() {
        BlockStressValues.IMPACTS.register(pump, () -> 4.0);
        MountedFluidStorageType.REGISTRY.register(tank, AllMountedStorageTypes.FLUID_TANK.get());
    }
}
