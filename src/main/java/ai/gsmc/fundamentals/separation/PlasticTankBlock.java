package ai.gsmc.fundamentals.separation;

import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Create's fluid tank in plastic, which the acids and liquors cannot touch: the same multiblock, windows and capacity, its own
 * block entity type so it joins only other plastic tanks. */
public class PlasticTankBlock extends FluidTankBlock {

    public PlasticTankBlock(Properties properties) {
        super(properties, false);
    }

    @Override
    public BlockEntityType<? extends FluidTankBlockEntity> getBlockEntityType() {
        return Separation.plasticTankEntity();
    }
}
