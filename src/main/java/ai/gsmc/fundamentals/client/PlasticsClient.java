package ai.gsmc.fundamentals.client;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.plastics.DyedPlasticPipeBlock;
import ai.gsmc.fundamentals.plastics.Plastics;
import ai.gsmc.fundamentals.separation.PlasticTankBlock;
import ai.gsmc.fundamentals.separation.Separation;
import com.drmangotea.tfmg.content.decoration.pipes.TFMGPipeAttachmentModel;
import com.drmangotea.tfmg.content.decoration.pipes.TFMGPipes;
import com.simibubi.create.CreateClient;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

import javax.annotation.Nullable;
import java.util.List;

/** The dyes on plastic: a tint over the natural sheets, drawn cutout so the tinted plastic is opaque. */
public final class PlasticsClient {

    static final ChunkRenderTypeSet TRANSLUCENT = ChunkRenderTypeSet.of(RenderType.translucent());
    static final ChunkRenderTypeSet CUTOUT = ChunkRenderTypeSet.of(RenderType.cutout());

    private PlasticsClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(FMLClientSetupEvent.class, event -> CreateClient.MODEL_SWAPPER.getCustomBlockModels()
                .register(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "dyed_plastic_pipe"), DyedPipeModel::new));
        modBus.addListener(RegisterColorHandlersEvent.Block.class, event -> {
            event.register((state, level, pos, tint) -> colour(state.getValue(PlasticTankBlock.COLOR).dye), Separation.plasticTank());
            event.register((state, level, pos, tint) -> colour(state.getValue(DyedPlasticPipeBlock.COLOR)), Plastics.dyedPipe());
        });
    }

    private static int colour(@Nullable DyeColor dye) {
        return dye == null ? -1 : dye.getTextureDiffuseColor();
    }

    /** The same quads, on tint index 0. */
    static List<BakedQuad> tinted(List<BakedQuad> quads) {
        return quads.stream().map(q -> new BakedQuad(q.getVertices(), 0, q.getDirection(), q.getSprite(), q.isShade(), q.hasAmbientOcclusion())).toList();
    }

    /** The Factory Must Grow's plastic pipe model, rims and all, tinted and drawn opaque. */
    private static class DyedPipeModel extends BakedModelWrapper<BakedModel> {

        DyedPipeModel(BakedModel model) {
            super(new TFMGPipeAttachmentModel(model, true, TFMGPipes.PipeMaterial.PLASTIC));
        }

        @Override
        public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
            return tinted(super.getQuads(state, side, rand, data, renderType));
        }

        @Override
        public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
            return CUTOUT;
        }
    }
}
