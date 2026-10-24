package ai.gsmc.fundamentals.compute;

import com.drmangotea.tfmg.content.electricity.base.ElectricBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** A grid-tied power supply: the computers beside it run while its side of the grid can carry them. */
public class PowerSupplyBlockEntity extends ElectricBlockEntity {

    /** Ohms: about 150 W at the 120 V a small grid runs at. */
    public static final float RESISTANCE = 96;

    public PowerSupplyBlockEntity(BlockPos pos, BlockState state) {
        super(Compute.powerSupplyEntity(), pos, state);
    }

    @Override
    public float resistance() {
        return RESISTANCE;
    }

    public boolean live() {
        return canWork() && getData().getVoltage() >= Power.MIN_VOLTS;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level.isClientSide) return;
        boolean live = live();
        if (getBlockState().getValue(BlockStateProperties.POWERED) != live) {
            level.setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.POWERED, live), 3);
        }
    }
}
