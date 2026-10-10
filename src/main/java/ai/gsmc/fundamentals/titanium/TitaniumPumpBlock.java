package ai.gsmc.fundamentals.titanium;

import com.simibubi.create.content.fluids.pump.PumpBlock;
import com.simibubi.create.content.fluids.pump.PumpBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Create's mechanical pump with a titanium body. */
public class TitaniumPumpBlock extends PumpBlock {

    public TitaniumPumpBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntityType<? extends PumpBlockEntity> getBlockEntityType() {
        return Titanium.pumpEntity();
    }
}
