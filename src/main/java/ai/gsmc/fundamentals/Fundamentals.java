package ai.gsmc.fundamentals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.gsmc.fundamentals.registry.OreBlocks;
import ai.gsmc.fundamentals.worldgen.DepositFeature;
import ai.gsmc.fundamentals.worldgen.OreSpawns;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

//? if fabric {
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
//?}

//? if neoforge {
/*import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.RegisterEvent;
*///?}

/**
 * Common entrypoint for Fundamentals.
 *
 * Loader-specific wiring is selected at build time by Stonecutter comments
 * ({@code //? if fabric} / {@code //? if neoforge}). Shared logic lives in
 * {@link #init()} so every loader runs the same code path.
 */
//? if neoforge {
/*@Mod(Fundamentals.MOD_ID)
*///?}
public class Fundamentals /*? if fabric {*/ implements ModInitializer /*?}*/ {

    public static final String MOD_ID = "fundamentals";
    public static final Logger LOGGER = LoggerFactory.getLogger("Fundamentals");

    //? if fabric {
    @Override
    public void onInitialize() {
        init();
        OreBlocks.registerBlocks((id, block) -> Registry.register(BuiltInRegistries.BLOCK, id, block));
        OreBlocks.registerItems((id, item) -> Registry.register(BuiltInRegistries.ITEM, id, item));
        Registry.register(BuiltInRegistries.FEATURE, DEPOSIT, DepositFeature.INSTANCE);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MINERALS_TAB, mineralsTab(FabricItemGroup.builder()));
        OreSpawns.apply();
    }
    //?}

    //? if neoforge {
    /*public Fundamentals(IEventBus modBus) {
        init();
        modBus.addListener(RegisterEvent.class, event -> {
            event.register(Registries.BLOCK, helper -> OreBlocks.registerBlocks(helper::register));
            event.register(Registries.ITEM, helper -> OreBlocks.registerItems(helper::register));
            event.register(Registries.FEATURE, helper -> helper.register(DEPOSIT, DepositFeature.INSTANCE));
            event.register(Registries.CREATIVE_MODE_TAB,
                    helper -> helper.register(MINERALS_TAB, mineralsTab(CreativeModeTab.builder())));
        });
    }
    *///?}

    private static final ResourceLocation MINERALS_TAB = ResourceLocation.fromNamespaceAndPath(MOD_ID, "minerals");
    private static final ResourceLocation DEPOSIT = ResourceLocation.fromNamespaceAndPath(MOD_ID, "deposit");

    private static CreativeModeTab mineralsTab(CreativeModeTab.Builder builder) {
        return builder.title(Component.translatable("itemGroup.fundamentals.minerals"))
                .icon(() -> new ItemStack(OreBlocks.items().iterator().next()))
                .displayItems((parameters, output) -> OreBlocks.items().forEach(output::accept))
                .build();
    }

    /** Loader-agnostic setup. Register content, materials, and processing here. */
    public static void init() {
        LOGGER.info("Fundamentals initializing ({} loader)", loaderName());
        ai.gsmc.fundamentals.registry.FundamentalsContent.registerAll();
    }

    private static String loaderName() {
        //? if fabric {
        return "Fabric";
        //?}
        //? if neoforge {
        /*return "NeoForge";
        *///?}
    }
}
