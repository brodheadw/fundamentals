package ai.gsmc.fundamentals.client;

import ai.gsmc.fundamentals.oxidation.Oxidation;
import net.minecraft.client.renderer.item.ItemProperties;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

public final class OxidationClient {

    private OxidationClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(() ->
                ItemProperties.registerGeneric(Oxidation.STAGE_PROPERTY, (stack, level, entity, seed) -> Oxidation.stageProperty(stack))));
    }
}
