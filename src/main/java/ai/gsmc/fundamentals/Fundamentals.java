package ai.gsmc.fundamentals;

import ai.gsmc.fundamentals.client.GrindingAnimation;
import ai.gsmc.fundamentals.client.OxidationClient;
import ai.gsmc.fundamentals.client.PlasticsClient;
import ai.gsmc.fundamentals.client.SeparationClient;
import ai.gsmc.fundamentals.client.ThermometerRenderer;
import ai.gsmc.fundamentals.client.TitaniumClient;
import ai.gsmc.fundamentals.client.ComputeClient;
import ai.gsmc.fundamentals.compute.Compute;
import ai.gsmc.fundamentals.content.nuclear.Radiation;
import ai.gsmc.fundamentals.elements.PeriodicTable;
import ai.gsmc.fundamentals.ironworking.IronWorking;
import ai.gsmc.fundamentals.loot.AddToChests;
import ai.gsmc.fundamentals.magnet.Magnets;
import ai.gsmc.fundamentals.oxidation.InertDrumBlockEntity;
import ai.gsmc.fundamentals.oxidation.Oxidation;
import ai.gsmc.fundamentals.oxidation.Weathering;
import ai.gsmc.fundamentals.loot.ScarceInChests;
import ai.gsmc.fundamentals.loot.SwapDrop;
import ai.gsmc.fundamentals.plastics.Plastics;
import ai.gsmc.fundamentals.power.Electricity;
import ai.gsmc.fundamentals.registry.FundamentalsContent;
import ai.gsmc.fundamentals.registry.HandTools;
import ai.gsmc.fundamentals.registry.MaterialItems;
import ai.gsmc.fundamentals.registry.OreBlocks;
import ai.gsmc.fundamentals.separation.MagnetomigrationCellBlockEntity;
import ai.gsmc.fundamentals.separation.MixerSettlerBlockEntity;
import ai.gsmc.fundamentals.separation.PlasticTankBlockEntity;
import ai.gsmc.fundamentals.titanium.Titanium;
import com.simibubi.create.AllMountedStorageTypes;
import com.simibubi.create.api.contraption.storage.fluid.MountedFluidStorageType;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import ai.gsmc.fundamentals.computer.FundamentalsPeripherals;
import ai.gsmc.fundamentals.heat.Temperatures;
import ai.gsmc.fundamentals.heat.Thermometers;
import ai.gsmc.fundamentals.liquid.Liquids;
import ai.gsmc.fundamentals.separation.Hazards;
import ai.gsmc.fundamentals.uses.Fertiliser;
import ai.gsmc.fundamentals.uses.Uses;
import net.neoforged.neoforge.common.NeoForge;
import ai.gsmc.fundamentals.separation.Separation;
import ai.gsmc.fundamentals.worldgen.DepositFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import ai.gsmc.fundamentals.worldgen.OreData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(Fundamentals.MOD_ID)
public class Fundamentals {

    public static final String MOD_ID = "fundamentals";
    public static final Logger LOGGER = LoggerFactory.getLogger("Fundamentals");

    private static final ResourceLocation MINERALS_TAB = id("minerals");
    private static final ResourceLocation MATERIALS_TAB = id("materials");
    private static final ResourceLocation DEPOSIT = id("deposit");

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public Fundamentals(IEventBus modBus) {
        LOGGER.info("Fundamentals initializing");
        FundamentalsContent.registerAll();
        modBus.addListener(RegisterEvent.class, event -> {
            event.register(Registries.BLOCK, helper -> {
                OreBlocks.registerBlocks(helper::register);
                IronWorking.registerBlocks(helper::register);
                ai.gsmc.fundamentals.kiln.Kiln.registerBlocks(helper::register);
                MaterialItems.registerBlocks(helper::register);
                Electricity.registerBlocks(helper::register);
                Separation.registerBlocks(helper::register);
                Uses.registerBlocks(helper::register);
                ai.gsmc.fundamentals.bombs.Bombs.registerBlocks(helper::register);
                Plastics.registerBlocks(helper::register);
                Titanium.registerBlocks(helper::register);
                ai.gsmc.fundamentals.mill.WoodenMill.registerBlocks(helper::register);
                Thermometers.registerBlocks(helper::register);
                ai.gsmc.fundamentals.air.Barometers.registerBlocks(helper::register);
                ai.gsmc.fundamentals.air.FieldMill.registerBlocks(helper::register);
                ai.gsmc.fundamentals.survey.Survey.registerBlocks(helper::register);
                ai.gsmc.fundamentals.torch.Fixtures.registerBlocks(helper::register);
                Oxidation.registerBlocks(helper::register);
                Compute.registerBlocks(helper::register);
                ai.gsmc.fundamentals.glass.Glass.registerBlocks(helper::register);
                ai.gsmc.fundamentals.torch.Torches.aliasBlocks(event.getRegistry(Registries.BLOCK));
            });
            event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
                IronWorking.registerBlockEntities(helper::register);
                ai.gsmc.fundamentals.kiln.Kiln.registerBlockEntities(helper::register);
                Electricity.registerBlockEntities(helper::register);
                Separation.registerBlockEntities(helper::register);
                Plastics.registerBlockEntities(helper::register);
                Titanium.registerBlockEntities(helper::register);
                ai.gsmc.fundamentals.mill.WoodenMill.registerBlockEntities(helper::register);
                Thermometers.registerBlockEntities(helper::register);
                ai.gsmc.fundamentals.air.Barometers.registerBlockEntities(helper::register);
                ai.gsmc.fundamentals.air.FieldMill.registerBlockEntities(helper::register);
                ai.gsmc.fundamentals.survey.Survey.registerBlockEntities(helper::register);
                Oxidation.registerBlockEntities(helper::register);
                Compute.registerBlockEntities(helper::register);
                ai.gsmc.fundamentals.glass.Glass.registerBlockEntities(helper::register);
            });
            event.register(Registries.ENTITY_TYPE, helper -> {
                ai.gsmc.fundamentals.automaton.Automata.registerEntities(helper::register);
                ai.gsmc.fundamentals.bombs.Bombs.registerEntities(helper::register);
                ai.gsmc.fundamentals.survey.Survey.registerEntities(helper::register);
            });
            event.register(Registries.RECIPE_TYPE, helper -> {
                IronWorking.registerRecipeTypes(helper::register);
                ai.gsmc.fundamentals.kiln.Kiln.registerRecipeTypes(helper::register);
                Separation.registerRecipeTypes(helper::register);
            });
            event.register(Registries.RECIPE_SERIALIZER, helper -> {
                IronWorking.registerRecipeSerializers(helper::register);
                ai.gsmc.fundamentals.kiln.Kiln.registerRecipeSerializers(helper::register);
                Separation.registerRecipeSerializers(helper::register);
                Magnets.registerRecipeSerializers(helper::register);
            });
            event.register(NeoForgeRegistries.Keys.FLUID_TYPES, helper -> {
                Separation.registerFluidTypes(helper::register);
                Compute.registerFluidTypes(helper::register);
            });
            event.register(Registries.FLUID, helper -> {
                Separation.registerFluids(helper::register);
                Compute.registerFluids(helper::register);
            });
            if (event.getRegistryKey().equals(Registries.FLUID)) {
                event.getRegistry().addAlias(id("brine"), id("calcium_chloride_liquor"));
                if (ModList.get().isLoaded("createbigcannons")) {
                    event.getRegistry().addAlias(id("molten_pig_iron"), Separation.BIG_CANNONS_CAST_IRON);
                }
            }
            if (event.getRegistryKey().equals(Registries.ITEM)) {
                // ores that now drop a vanilla raw item keep old stacks of ours
                OreData.get().blocks().stream().filter(def -> def.raw() != null)
                        .forEach(def -> event.getRegistry().addAlias(id("raw_" + def.mineral()), ResourceLocation.parse(def.raw())));
            }
            event.register(Registries.ITEM, helper -> {
                OreBlocks.registerItems(helper::register);
                IronWorking.registerItems(helper::register);
                ai.gsmc.fundamentals.kiln.Kiln.registerItems(helper::register);
                ai.gsmc.fundamentals.anchor.Anchor.registerItems(helper::register);
                HandTools.registerItems(helper::register);
                MaterialItems.registerItems(helper::register);
                Electricity.registerItems(helper::register);
                Separation.registerItems(helper::register);
                Uses.registerItems(helper::register);
                ai.gsmc.fundamentals.uses.Arms.registerItems(helper::register);
                ai.gsmc.fundamentals.bombs.Bombs.registerItems(helper::register);
                ai.gsmc.fundamentals.automaton.Automata.registerItems(helper::register);
                Magnets.registerItems(helper::register);
                Plastics.registerItems(helper::register);
                Titanium.registerItems(helper::register);
                ai.gsmc.fundamentals.mill.WoodenMill.registerItems(helper::register);
                Thermometers.registerItems(helper::register);
                ai.gsmc.fundamentals.air.Barometers.registerItems(helper::register);
                ai.gsmc.fundamentals.air.FieldMill.registerItems(helper::register);
                ai.gsmc.fundamentals.survey.Survey.registerItems(helper::register);
                ai.gsmc.fundamentals.torch.Fixtures.registerItems(helper::register);
                Oxidation.registerItems(helper::register);
                PeriodicTable.registerItems(helper::register);
                Compute.registerItems(helper::register);
                ai.gsmc.fundamentals.cold.Parkas.registerItems(helper::register);
                ai.gsmc.fundamentals.cold.Cold.registerItems(helper::register);
                ai.gsmc.fundamentals.glass.Glass.registerItems(helper::register);
                ai.gsmc.fundamentals.torch.Torches.aliasItems(event.getRegistry(Registries.ITEM));
            });
            event.register(Registries.ARMOR_MATERIAL, helper -> ai.gsmc.fundamentals.cold.Parkas.registerArmorMaterials(helper::register));
            event.register(Registries.DATA_COMPONENT_TYPE, helper -> {
                Oxidation.registerComponents(helper::register);
                Magnets.registerComponents(helper::register);
            });
            event.register(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, helper -> {
                Oxidation.registerAttachments(helper::register);
            });
            event.register(Registries.FEATURE, helper -> helper.register(DEPOSIT, DepositFeature.INSTANCE));
            event.register(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, helper -> {
                helper.register(id("scarce_in_chests"), ScarceInChests.CODEC);
                helper.register(id("swap_drop"), SwapDrop.CODEC);
                helper.register(id("add_to_chests"), AddToChests.CODEC);
            });
            event.register(Registries.CREATIVE_MODE_TAB, helper -> {
                helper.register(MINERALS_TAB, mineralsTab());
                helper.register(MATERIALS_TAB, materialsTab());
            });
        });
        ai.gsmc.fundamentals.cold.Cold.register();
        ai.gsmc.fundamentals.automaton.Automata.register(modBus);
        NeoForge.EVENT_BUS.addListener(Hazards::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ai.gsmc.fundamentals.uses.FlintAndSteel::onUse);
        NeoForge.EVENT_BUS.addListener(Hazards::onServerTick);
        NeoForge.EVENT_BUS.addListener(Hazards::onBreak);
        NeoForge.EVENT_BUS.addListener(Hazards::onExplosion);
        NeoForge.EVENT_BUS.addListener(Fertiliser::onExplosion);
        NeoForge.EVENT_BUS.addListener(Fertiliser::onServerTick);
        NeoForge.EVENT_BUS.addListener(ai.gsmc.fundamentals.bombs.Detonation::onServerTick);
        NeoForge.EVENT_BUS.addListener(ai.gsmc.fundamentals.bombs.Fallout::onLevelTick);
        NeoForge.EVENT_BUS.addListener(ai.gsmc.fundamentals.survey.Disturbances::onExplosion);
        NeoForge.EVENT_BUS.addListener(ai.gsmc.fundamentals.survey.Disturbances::onJoin);
        NeoForge.EVENT_BUS.addListener(Plastics::onRightClick);
        NeoForge.EVENT_BUS.addListener(ai.gsmc.fundamentals.content.nuclear.SpentFuel::onEntityTick);
        NeoForge.EVENT_BUS.addListener(ai.gsmc.fundamentals.content.nuclear.SpentFuel::onTooltip);
        NeoForge.EVENT_BUS.addListener(Oxidation::onOpen);
        NeoForge.EVENT_BUS.addListener(Oxidation::onClose);
        NeoForge.EVENT_BUS.addListener(Oxidation::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(Oxidation::onPickup);
        NeoForge.EVENT_BUS.addListener(Oxidation::onTooltip);
        NeoForge.EVENT_BUS.addListener(Magnets::onTooltip);
        NeoForge.EVENT_BUS.addListener(ai.gsmc.fundamentals.magnet.MagnetLure::onJoin);
        NeoForge.EVENT_BUS.addListener(ai.gsmc.fundamentals.magnet.MagnetLure::onTooltip);
        NeoForge.EVENT_BUS.addListener(Magnets::attach);
        modBus.addListener(Oxidation::registerDataMaps);
        modBus.addListener(Temperatures::setup);
        modBus.addListener(Liquids::registerDataMaps);
        NeoForge.EVENT_BUS.addListener(Liquids::onDataMapsUpdated);
        NeoForge.EVENT_BUS.addListener(Temperatures::registerCommands);
        modBus.addListener(RegisterCapabilitiesEvent.class, event -> {
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, Separation.mixerSettlerEntity(), MixerSettlerBlockEntity::handler);
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, Separation.magnetomigrationCellEntity(), MagnetomigrationCellBlockEntity::handler);
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, Separation.plasticTankEntity(), PlasticTankBlockEntity::handler);
            event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, Oxidation.drumEntity(), InertDrumBlockEntity::handler);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Oxidation.drumEntity(), (drum, side) -> new InvWrapper(drum));
            event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new FluidBucketWrapper(stack), Separation.seawaterBucket());
            Titanium.registerCapabilities(event);
            event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ai.gsmc.fundamentals.kiln.Kiln.drumEntity(), ai.gsmc.fundamentals.kiln.KilnDrumBlockEntity::handler);
        });
        modBus.addListener(FMLCommonSetupEvent.class, event -> event.enqueueWork(() -> {
            MountedFluidStorageType.REGISTRY.register(Separation.plasticTank(), AllMountedStorageTypes.FLUID_TANK.get());
            Hazards.registerPipeEffects();
            Fertiliser.registerDispenserBehaviour();
            Titanium.commonSetup();
            ai.gsmc.fundamentals.kiln.Kiln.commonSetup();
            Separation.adoptCastIron();
            if (ModList.get().isLoaded("createnuclear")) {
                Radiation.register();
            }
            if (ModList.get().isLoaded("computercraft")) {
                FundamentalsPeripherals.register();
            }
        }));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            GrindingAnimation.register(modBus);
            SeparationClient.register(modBus);
            PlasticsClient.register(modBus);
            TitaniumClient.register(modBus);
            ai.gsmc.fundamentals.client.WoodenMillClient.register(modBus);
            ai.gsmc.fundamentals.client.KilnClient.register(modBus);
            OxidationClient.register(modBus);
            ThermometerRenderer.register(modBus);
            ComputeClient.register(modBus);
            ai.gsmc.fundamentals.client.GlassClient.register(modBus);
            ai.gsmc.fundamentals.client.AutomataClient.register(modBus);
            ai.gsmc.fundamentals.client.WeatherClient.register(modBus);
            modBus.addListener(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers.class,
                    event -> event.registerEntityRenderer(ai.gsmc.fundamentals.bombs.Bombs.charge(), net.minecraft.client.renderer.entity.TntRenderer::new));
        }
    }

    private static CreativeModeTab mineralsTab() {
        return CreativeModeTab.builder().title(Component.translatable("itemGroup.fundamentals.minerals"))
                .icon(() -> new ItemStack(OreBlocks.items().iterator().next()))
                .displayItems((parameters, output) -> {
                    IronWorking.items().forEach(output::accept);
                    ai.gsmc.fundamentals.kiln.Kiln.items().forEach(output::accept);
                    ai.gsmc.fundamentals.anchor.Anchor.items().forEach(output::accept);
                    Separation.items().forEach(output::accept);
                    Uses.items().forEach(output::accept);
                    ai.gsmc.fundamentals.uses.Arms.items().forEach(output::accept);
                    ai.gsmc.fundamentals.bombs.Bombs.items().forEach(output::accept);
                    ai.gsmc.fundamentals.automaton.Automata.items().forEach(output::accept);
                    Magnets.items().forEach(output::accept);
                    Plastics.items().forEach(output::accept);
                    Titanium.items().forEach(output::accept);
                    ai.gsmc.fundamentals.mill.WoodenMill.items().forEach(output::accept);
                    HandTools.items().forEach(output::accept);
                    Electricity.items().forEach(output::accept);
                    Thermometers.items().forEach(output::accept);
                    ai.gsmc.fundamentals.air.Barometers.items().forEach(output::accept);
                    output.accept(ai.gsmc.fundamentals.air.FieldMill.item());
                    ai.gsmc.fundamentals.survey.Survey.items().forEach(output::accept);
                    ai.gsmc.fundamentals.torch.Fixtures.items().forEach(output::accept);
                    Oxidation.items().forEach(output::accept);
                    Compute.items().forEach(output::accept);
                    ai.gsmc.fundamentals.cold.Parkas.items().forEach(output::accept);
                    ai.gsmc.fundamentals.cold.Cold.flasks().forEach(output::accept);
                    ai.gsmc.fundamentals.glass.Glass.items().forEach(output::accept);
                    OreBlocks.rawItems().forEach(output::accept);
                    OreBlocks.items().forEach(output::accept);
                })
                .build();
    }

    private static CreativeModeTab materialsTab() {
        return CreativeModeTab.builder().title(Component.translatable("itemGroup.fundamentals.materials"))
                .icon(() -> new ItemStack(MaterialItems.items().get(id("neodymium_ingot"))))
                .displayItems((parameters, output) -> {
                    MaterialItems.items().values().forEach(output::accept);
                    Weathering.items().forEach(output::accept);
                })
                .build();
    }
}
