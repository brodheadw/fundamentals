package ai.gsmc.fundamentals.titanium;

import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class TitaniumPumpBlock extends PumpBlock {

    public TitaniumPumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends PumpBlockEntity> getBlockEntityType() {
        return Titanium.pumpEntity();
    }
}
