package ai.gsmc.fundamentals.client;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.kiln.Kiln;
import ai.gsmc.fundamentals.kiln.KilnDrumBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The kiln drum turns as a shaft does: an upright partial spun about the drum's axis. */
public final class KilnClient {

    private static final PartialModel DRUM = PartialModel.of(Fundamentals.id("block/kiln_drum"));

    private KilnClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(() ->
                SimpleBlockEntityVisualizer.builder(Kiln.drumEntity())
                        .factory((context, be, partialTick) -> new SingleAxisRotatingVisual<KilnDrumBlockEntity>(context, be, partialTick, Models.partial(DRUM)))
                        .skipVanillaRender(be -> true).apply()));
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class,
                event -> event.registerBlockEntityRenderer(Kiln.drumEntity(), KineticBlockEntityRenderer::new));
    }
}
