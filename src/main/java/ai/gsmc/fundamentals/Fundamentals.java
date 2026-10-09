package ai.gsmc.fundamentals;

import ai.gsmc.fundamentals.client.GrindingAnimation;
import ai.gsmc.fundamentals.client.SeparationClient;
import ai.gsmc.fundamentals.elements.PeriodicTable;
import ai.gsmc.fundamentals.ironworking.IronWorking;
import ai.gsmc.fundamentals.loot.AddToChests;
import ai.gsmc.fundamentals.loot.ScarceInChests;
import ai.gsmc.fundamentals.loot.SwapDrop;
import ai.gsmc.fundamentals.power.Electricity;
import ai.gsmc.fundamentals.registry.FundamentalsContent;
import ai.gsmc.fundamentals.registry.HandTools;
import ai.gsmc.fundamentals.registry.MaterialItems;
import ai.gsmc.fundamentals.registry.OreBlocks;
import ai.gsmc.fundamentals.separation.MagnetomigrationCellBlockEntity;
import ai.gsmc.fundamentals.separation.MixerSettlerBlockEntity;
import ai.gsmc.fundamentals.separation.PlasticTankBlockEntity;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import ai.gsmc.fundamentals.heat.Heat;
import ai.gsmc.fundamentals.separation.Hazards;
import ai.gsmc.fundamentals.uses.PlatinumMetals;
import ai.gsmc.fundamentals.uses.Uses;
import net.neoforged.neoforge.common.NeoForge;
import ai.gsmc.fundamentals.separation.Separation;
import ai.gsmc.fundamentals.worldgen.DepositFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Fundamentals.MOD_ID)
public class Fundamentals {

    public static final String MOD_ID = "fundamentals";
    public static final Logger LOGGER = LoggerFactory.getLogger("Fundamentals");

    private static final ResourceLocation MINERALS_TAB = ResourceLocation.fromNamespaceAndPath(MOD_ID, "minerals");
    private static final ResourceLocation MATERIALS_TAB = ResourceLocation.fromNamespaceAndPath(MOD_ID, "materials");
    private static final ResourceLocation DEPOSIT = ResourceLocation.fromNamespaceAndPath(MOD_ID, "deposit");

    public Fundamentals(IEventBus modBus) {
        LOGGER.info("Fundamentals initializing");
        FundamentalsContent.registerAll();
        modBus.addListener(RegisterEvent.class, event -> {
            event.register(Registries.BLOCK, helper -> {
                OreBlocks.registerBlocks(helper::register);
                IronWorking.registerBlocks(helper::register);
                MaterialItems.registerBlocks(helper::register);
                Electricity.registerBlocks(helper::register);
                Separation.registerBlocks(helper::register);
                Uses.registerBlocks(helper::register);
            });
            event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
                IronWorking.registerBlockEntities(helper::register);
                Electricity.registerBlockEntities(helper::register);
                Separation.registerBlockEntities(helper::register);
            });
            event.register(Registries.RECIPE_TYPE, helper -> {
                IronWorking.registerRecipeTypes(helper::register);
                Separation.registerRecipeTypes(helper::register);
            });
            event.register(Registries.RECIPE_SERIALIZER, helper -> {
                IronWorking.registerRecipeSerializers(helper::register);
                Separation.registerRecipeSerializers(helper::register);
            });
            event.register(NeoForgeRegistries.Keys.FLUID_TYPES, helper -> Separation.registerFluidTypes(helper::register));
            event.register(Registries.FLUID, helper -> Separation.registerFluids(helper::register));
            event.register(Registries.ITEM, helper -> {
                OreBlocks.registerItems(helper::register);
                IronWorking.registerItems(helper::register);
                HandTools.registerItems(helper::register);
                MaterialItems.registerItems(helper::register);
                Electricity.registerItems(helper::register);
                Separation.registerItems(helper::register);
                Uses.registerItems(helper::register);
                PlatinumMetals.registerItems(helper::register);
                PeriodicTable.registerItems(helper::register);
            });
            event.register(Registries.FEATURE, helper -> helper.register(DEPOSIT, DepositFeature.INSTANCE));
            event.register(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, helper -> {
                helper.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, "scarce_in_chests"), ScarceInChests.CODEC);
                helper.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, "swap_drop"), SwapDrop.CODEC);
                helper.register(ResourceLocation.fromNamespaceAndPath(MOD_ID, "add_to_chests"), AddToChests.CODEC);
            });
            event.register(Registries.CREATIVE_MODE_TAB, helper -> {
                helper.register(MINERALS_TAB, mineralsTab());
                helper.register(MATERIALS_TAB, materialsTab());
            });
        });
        NeoForge.EVENT_BUS.addListener(Hazards::onPlayerTick);
        modBus.addListener(Heat::registerDataMaps);
        NeoForge.EVENT_BUS.addListener(Heat::registerCommands);
        modBus.addListener(RegisterCapabilitiesEvent.class, event -> {
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, Separation.mixerSettlerEntity(), MixerSettlerBlockEntity::handler);
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, Separation.magnetomigrationCellEntity(), MagnetomigrationCellBlockEntity::handler);
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, Separation.plasticTankEntity(), PlasticTankBlockEntity::handler);
        });
        modBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(() ->
                MountedFluidStorageType.REGISTRY.register(Separation.plasticTank(), AllMountedStorageTypes.FLUID_TANK.get())));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            GrindingAnimation.register(modBus);
            SeparationClient.register(modBus);
        }
    }

    private static CreativeModeTab mineralsTab() {
        return CreativeModeTab.builder().title(Component.translatable("itemGroup.fundamentals.minerals"))
                .icon(() -> new ItemStack(OreBlocks.items().iterator().next()))
                .displayItems((parameters, output) -> {
                    IronWorking.items().forEach(output::accept);
                    Separation.items().forEach(output::accept);
                    Uses.items().forEach(output::accept);
                    PlatinumMetals.items().forEach(output::accept);
                    HandTools.items().forEach(output::accept);
                    Electricity.items().forEach(output::accept);
                    OreBlocks.rawItems().forEach(output::accept);
                    OreBlocks.items().forEach(output::accept);
                })
                .build();
    }

    private static CreativeModeTab materialsTab() {
        return CreativeModeTab.builder().title(Component.translatable("itemGroup.fundamentals.materials"))
                .icon(() -> new ItemStack(MaterialItems.items().get(ResourceLocation.fromNamespaceAndPath(MOD_ID, "neodymium_ingot"))))
                .displayItems((parameters, output) -> MaterialItems.items().values().forEach(output::accept))
                .build();
    }
}
