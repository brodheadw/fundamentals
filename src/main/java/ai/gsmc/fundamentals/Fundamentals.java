package ai.gsmc.fundamentals;

import ai.gsmc.fundamentals.registry.FundamentalsContent;
import ai.gsmc.fundamentals.registry.OreBlocks;
import ai.gsmc.fundamentals.worldgen.DepositFeature;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Entrypoint for Fundamentals, a Create add-on for NeoForge. */
@Mod(Fundamentals.MOD_ID)
public class Fundamentals {

    public static final String MOD_ID = "fundamentals";
    public static final Logger LOGGER = LoggerFactory.getLogger("Fundamentals");

    private static final ResourceLocation MINERALS_TAB = ResourceLocation.fromNamespaceAndPath(MOD_ID, "minerals");
    private static final ResourceLocation DEPOSIT = ResourceLocation.fromNamespaceAndPath(MOD_ID, "deposit");

    public Fundamentals(IEventBus modBus) {
        LOGGER.info("Fundamentals initializing");
        FundamentalsContent.registerAll();
        modBus.addListener(RegisterEvent.class, event -> {
            event.register(Registries.BLOCK, helper -> OreBlocks.registerBlocks(helper::register));
            event.register(Registries.ITEM, helper -> OreBlocks.registerItems(helper::register));
            event.register(Registries.FEATURE, helper -> helper.register(DEPOSIT, DepositFeature.INSTANCE));
            event.register(Registries.CREATIVE_MODE_TAB, helper -> helper.register(MINERALS_TAB, mineralsTab()));
        });
    }

    private static CreativeModeTab mineralsTab() {
        return CreativeModeTab.builder().title(Component.translatable("itemGroup.fundamentals.minerals"))
                .icon(() -> new ItemStack(OreBlocks.items().iterator().next()))
                .displayItems((parameters, output) -> OreBlocks.items().forEach(output::accept))
                .build();
    }
}
