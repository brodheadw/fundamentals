package ai.gsmc.fundamentals.titanium;

import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import javax.annotation.Nullable;

public class TitaniumTankBlockEntity extends FluidTankBlockEntity {

    public TitaniumTankBlockEntity(BlockPos pos, BlockState state) {
        super(Titanium.tankEntity(), pos, state);
    }

    public IFluidHandler handler(@Nullable Direction side) {
        return fluidCapability;
    }
}
