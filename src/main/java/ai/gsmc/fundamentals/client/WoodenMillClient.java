package ai.gsmc.fundamentals.client;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.mill.WoodenKineticBlockEntity;
import ai.gsmc.fundamentals.mill.WoodenMill;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** The oak shaft and cogs turn as Create's do: upright partial models spun about their axis, or the block model without Flywheel. */
public final class WoodenMillClient {

    // partials are named before models load, so Flywheel bakes them
    private static final PartialModel SHAFT = PartialModel.of(Fundamentals.id("block/wooden_shaft"));
    private static final PartialModel COGWHEEL = PartialModel.of(Fundamentals.id("block/wooden_cogwheel"));
    private static final PartialModel LARGE_COGWHEEL = PartialModel.of(Fundamentals.id("block/wooden_large_cogwheel"));

    private WoodenMillClient() {}

    private static PartialModel partial(BlockState state) {
        return state.is(WoodenMill.largeCogwheel()) ? LARGE_COGWHEEL : state.is(WoodenMill.cogwheel()) ? COGWHEEL : SHAFT;
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(() ->
                SimpleBlockEntityVisualizer.builder(WoodenMill.entity())
                        .factory((context, be, partialTick) -> new SingleAxisRotatingVisual<WoodenKineticBlockEntity>(context, be, partialTick,
                                Models.partial(partial(be.getBlockState()))))
                        .skipVanillaRender(be -> true).apply()));
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class,
                event -> event.registerBlockEntityRenderer(WoodenMill.entity(), KineticBlockEntityRenderer::new));
    }
}
