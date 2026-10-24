package ai.gsmc.fundamentals.client;

import ai.gsmc.fundamentals.compute.Compute;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

public final class ComputeClient {
    private static final ResourceLocation STILL = ResourceLocation.withDefaultNamespace("block/water_still");
    private static final ResourceLocation FLOW = ResourceLocation.withDefaultNamespace("block/water_flow");

    private ComputeClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterClientExtensionsEvent.class, event -> Compute.FLUID_TINTS.forEach((fluid, tint) -> event.registerFluidType(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOW;
            }

            @Override
            public int getTintColor() {
                return 0xF0000000 | tint;
            }
        }, Compute.FLUID_TYPE.get(fluid))));
    }
}
