package ai.gsmc.fundamentals.client;

import ai.gsmc.fundamentals.Fundamentals;
import com.simibubi.create.api.connectivity.ConnectivityHandler;
import com.simibubi.create.content.fluids.tank.FluidTankCTBehaviour;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTModel;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import net.createmod.catnip.data.Iterate;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import java.util.ArrayList;
import java.util.List;

/** Create's fluid tank model with our sheets: connected textures across the multiblock, and the walls between two blocks of one
 * tank left out. Create's own is private to its two tanks. */
class PlasticTankModel extends CTModel {

    private static final ModelProperty<boolean[]> JOINED = new ModelProperty<>();

    PlasticTankModel(BakedModel model) {
        super(model, new FluidTankCTBehaviour(shift("plastic_fluid_tank"), shift("plastic_fluid_tank_top"), shift("plastic_fluid_tank_inner")));
    }

    private static CTSpriteShiftEntry shift(String name) {
        return CTSpriteShifter.getCT(AllCTTypes.RECTANGLE, ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "block/" + name),
                ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "block/" + name + "_connected"));
    }

    @Override
    protected ModelData.Builder gatherModelData(ModelData.Builder builder, BlockAndTintGetter world, BlockPos pos, BlockState state, ModelData blockEntityData) {
        super.gatherModelData(builder, world, pos, state, blockEntityData);
        boolean[] joined = new boolean[4];
        for (Direction d : Iterate.horizontalDirections) {
            joined[d.get2DDataValue()] = ConnectivityHandler.isConnected(world, pos, pos.relative(d));
        }
        return builder.with(JOINED, joined);
    }

    @Override
    public List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData, RenderType renderType) {
        if (side != null) {
            return List.of();
        }
        boolean[] joined = extraData.get(JOINED);
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction d : Iterate.directions) {
            if (joined == null || d.getAxis().isVertical() || !joined[d.get2DDataValue()]) {
                quads.addAll(super.getQuads(state, d, rand, extraData, renderType));
            }
        }
        quads.addAll(super.getQuads(state, null, rand, extraData, renderType));
        return quads;
    }
}
