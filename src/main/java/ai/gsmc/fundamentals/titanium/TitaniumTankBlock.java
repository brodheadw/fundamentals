package ai.gsmc.fundamentals.titanium;

import com.simibubi.create.content.fluids.tank.FluidTankBlock;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class TitaniumTankBlock extends FluidTankBlock {

    public TitaniumTankBlock(Properties properties) {
        super(properties, false);
    }

    @Override
    public BlockEntityType<? extends FluidTankBlockEntity> getBlockEntityType() {
        return Titanium.tankEntity();
    }
}
