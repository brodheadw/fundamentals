package ai.gsmc.fundamentals.separation;

import ai.gsmc.fundamentals.plastics.Pigment;
import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** Create's fluid tank in plastic, which the acids and liquors cannot touch: the same multiblock, windows and capacity, its own
 * block entity type so it joins only other plastic tanks. Natural, it is milky and lets light through; dyed, it is opaque. */
public class PlasticTankBlock extends FluidTankBlock {

    public static final EnumProperty<Pigment> COLOR = EnumProperty.create("color", Pigment.class);

    public PlasticTankBlock(Properties properties) {
        super(properties, false);
        registerDefaultState(defaultBlockState().setValue(COLOR, Pigment.NONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COLOR);
    }

    @Override
    protected int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(COLOR) == Pigment.NONE ? 1 : 15;
    }

    @Override
    public BlockEntityType<? extends FluidTankBlockEntity> getBlockEntityType() {
        return Separation.plasticTankEntity();
    }
}
