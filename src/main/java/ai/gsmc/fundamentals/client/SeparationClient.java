package ai.gsmc.fundamentals.client;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.client.ponder.FundamentalsPonderPlugin;
import ai.gsmc.fundamentals.separation.Acids;
import net.createmod.ponder.foundation.PonderIndex;
import ai.gsmc.fundamentals.separation.MixerSettlerBlockEntity;
import ai.gsmc.fundamentals.separation.Reagents;
import ai.gsmc.fundamentals.separation.Separation;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.fluids.tank.FluidTankRenderer;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.HorizontalCTBehaviour;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

public final class SeparationClient {

    private static final ResourceLocation STILL = Fundamentals.id("block/fluid/liquor_still");
    private static final ResourceLocation FLOW = Fundamentals.id("block/fluid/liquor_flow");

    private SeparationClient() {}

    public static void register(IEventBus modBus) {
        PonderIndex.addPlugin(new FundamentalsPonderPlugin());
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
            event.registerBlockEntityRenderer(Separation.mixerSettlerEntity(), context -> new MixerSettlerRenderer());
            event.registerBlockEntityRenderer(Separation.plasticTankEntity(), FluidTankRenderer::new);
        });
        modBus.addListener(FMLClientSetupEvent.class, event -> Acids.all().values().forEach(acid -> {
            ItemBlockRenderTypes.setRenderLayer(acid.source, RenderType.translucent());
            ItemBlockRenderTypes.setRenderLayer(acid.flowing, RenderType.translucent());
        }));
        modBus.addListener(FMLClientSetupEvent.class, event -> {
            CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(Fundamentals.id("mixer_settler"), model -> new CTModel(model, new StageWalls()));
            CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(Fundamentals.id("plastic_fluid_tank"), model -> new TankModel(model, "plastic_fluid_tank"));
        });
        modBus.addListener(RegisterClientExtensionsEvent.class, event -> {
            for (Reagents.Reagent reagent : Reagents.ALL) {
                if (reagent.kind() == Reagents.Kind.WATER) {
                    event.registerFluidType(new LikeWater(), Separation.fluidTypes().get(reagent.id()));
                    continue;
                }
                int alpha = reagent.kind() == Reagents.Kind.ORGANIC ? 0xF0 : 0xC8;
                int tint = alpha << 24 | reagent.tint();
                event.registerFluidType(new IClientFluidTypeExtensions() {
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
                        return tint;
                    }
                }, Separation.fluidTypes().get(reagent.id()));
            }
        });
    }

    private static class LikeWater implements IClientFluidTypeExtensions {
        private static IClientFluidTypeExtensions water() {
            return IClientFluidTypeExtensions.of(NeoForgeMod.WATER_TYPE.value());
        }

        @Override
        public ResourceLocation getStillTexture() {
            return water().getStillTexture();
        }

        @Override
        public ResourceLocation getFlowingTexture() {
            return water().getFlowingTexture();
        }

        @Override
        public ResourceLocation getOverlayTexture() {
            return water().getOverlayTexture();
        }

        @Override
        public ResourceLocation getRenderOverlayTexture(Minecraft minecraft) {
            return water().getRenderOverlayTexture(minecraft);
        }

        @Override
        public int getTintColor() {
            return water().getTintColor();
        }

        @Override
        public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
            return water().getTintColor(state, getter, pos);
        }
    }

    private static class StageWalls extends HorizontalCTBehaviour {
        StageWalls() {
            super(CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, Fundamentals.id("block/mixer_settler_side"), Fundamentals.id("block/mixer_settler_side_connected")),
                    CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, Fundamentals.id("block/mixer_settler_top"), Fundamentals.id("block/mixer_settler_top_connected")));
        }

        @Override
        public boolean connectsTo(BlockState state, BlockState other, BlockAndTintGetter reader, BlockPos pos, BlockPos otherPos, Direction face) {
            return super.connectsTo(state, other, reader, pos, otherPos, face)
                    && reader.getBlockEntity(pos) instanceof MixerSettlerBlockEntity mine
                    && reader.getBlockEntity(otherPos) instanceof MixerSettlerBlockEntity theirs
                    && mine.controllerPos().equals(theirs.controllerPos());
        }
    }

}
