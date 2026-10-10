package ai.gsmc.fundamentals.titanium;

import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlock;
import com.simibubi.create.content.fluids.pipes.valve.FluidValveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class TitaniumValveBlock extends FluidValveBlock {

    public TitaniumValveBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends FluidValveBlockEntity> getBlockEntityType() {
        return Titanium.valveEntity();
    }
}
