package ai.gsmc.fundamentals.client;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.titanium.Titanium;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.CreateClient;
import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.FluidTransportBehaviour.AttachmentTypes;
import com.simibubi.create.content.fluids.FluidTransportBehaviour.AttachmentTypes.ComponentPartials;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveRenderer;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveVisual;
import com.simibubi.create.content.fluids.pump.PumpRenderer;
import com.simibubi.create.content.fluids.tank.FluidTankRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.model.BakedModelWrapperWithData;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import dev.engine_room.flywheel.lib.visualization.SimpleBlockEntityVisualizer;
import net.createmod.catnip.data.Iterate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.common.util.TriState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** The titanium pipework drawn as Create draws its own: the rims and drains where a pipe meets something, the pump's cog, the
 * valve's pointer, the tank's fluid and connected walls. */
public final class TitaniumClient {

    // Partials must exist before models are registered, so they are made when this class loads, from the mod's constructor.
    private static final Map<ComponentPartials, Map<Direction, PartialModel>> RIMS = new EnumMap<>(ComponentPartials.class);

    static {
        for (ComponentPartials partial : ComponentPartials.values()) {
            Map<Direction, PartialModel> faces = new EnumMap<>(Direction.class);
            for (Direction d : Iterate.directions) {
                faces.put(d, PartialModel.of(id("block/titanium_pipe/" + partial.name().toLowerCase() + "/" + d.getSerializedName())));
            }
            RIMS.put(partial, faces);
        }
    }

    private TitaniumClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, event -> {
            for (String block : new String[] {"titanium_pipe", "titanium_mechanical_pump", "titanium_fluid_valve"}) {
                CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(id(block), Rims::new);
            }
            CreateClient.MODEL_SWAPPER.getCustomBlockModels().register(id("titanium_fluid_tank"), model -> new TankModel(model, "titanium_fluid_tank"));
            event.enqueueWork(() -> {
                SimpleBlockEntityVisualizer.builder(Titanium.pumpEntity()).factory(SingleAxisRotatingVisual.ofZ(AllPartialModels.MECHANICAL_PUMP_COG))
                        .skipVanillaRender(be -> true).apply();
                SimpleBlockEntityVisualizer.builder(Titanium.valveEntity()).factory(FluidValveVisual::new).skipVanillaRender(be -> true).apply();
            });
        });
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
            event.registerBlockEntityRenderer(Titanium.pumpEntity(), PumpRenderer::new);
            event.registerBlockEntityRenderer(Titanium.valveEntity(), FluidValveRenderer::new);
            event.registerBlockEntityRenderer(Titanium.tankEntity(), FluidTankRenderer::new);
        });
    }

    /** Create's PipeAttachmentModel with titanium rims for its copper ones; there is no casing to draw. */
    private static class Rims extends BakedModelWrapperWithData {

        private static final ModelProperty<Attached> ATTACHED = new ModelProperty<>();

        private record Attached(AttachmentTypes[] faces, BakedModel bracket) {}

        Rims(BakedModel model) {
            super(model);
        }

        @Override
        protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData blockEntityData) {
            AttachmentTypes[] faces = new AttachmentTypes[6];
            FluidTransportBehaviour transport = BlockEntityBehaviour.get(world, pos, FluidTransportBehaviour.TYPE);
            for (Direction d : Iterate.directions) {
                faces[d.get3DDataValue()] = transport == null ? AttachmentTypes.NONE : transport.getRenderedRimAttachment(world, pos, state, d);
            }
            BracketedBlockEntityBehaviour bracket = BlockEntityBehaviour.get(world, pos, BracketedBlockEntityBehaviour.TYPE);
            BlockState held = bracket == null ? null : bracket.getBracket();
            return builder.with(ATTACHED, new Attached(faces, held == null ? null : Minecraft.getInstance().getBlockRenderer().getBlockModel(held)));
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
            List<ChunkRenderTypeSet> sets = new ArrayList<>(List.of(super.getRenderTypes(state, rand, data)));
            Attached attached = data.get(ATTACHED);
            if (attached != null) {
                for (Direction d : Iterate.directions) {
                    for (ComponentPartials partial : attached.faces()[d.get3DDataValue()].partials) {
                        sets.add(RIMS.get(partial).get(d).get().getRenderTypes(state, rand, data));
                    }
                }
            }
            return ChunkRenderTypeSet.union(sets);
        }

        @Override
        public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData data, RenderType renderType) {
            List<BakedQuad> quads = super.getQuads(state, side, rand, data, renderType);
            Attached attached = data.get(ATTACHED);
            if (attached == null) {
                return quads;
            }
            quads = new ArrayList<>(quads);
            if (attached.bracket() != null) {
                quads.addAll(attached.bracket().getQuads(state, side, rand, data, renderType));
            }
            for (Direction d : Iterate.directions) {
                for (ComponentPartials partial : attached.faces()[d.get3DDataValue()].partials) {
                    quads.addAll(RIMS.get(partial).get(d).get().getQuads(state, side, rand, data, renderType));
                }
            }
            return quads;
        }

        @Override
        public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
            return TriState.TRUE;
        }
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, path);
    }
}
