package ai.gsmc.fundamentals.compute;

import com.drmangotea.tfmg.content.electricity.base.ElectricBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** A grid-tied power supply: the computers beside it run while its side of the grid can carry them. */
public class PowerSupplyBlockEntity extends ElectricBlockEntity implements com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation {

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
    public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip, boolean isPlayerSneaking) {
        int volts = getData().getVoltage();
        ai.gsmc.fundamentals.goggles.Readout.status(tooltip, getBlockState().getBlock().getName(), live() ? "powered" : "unpowered");
        ai.gsmc.fundamentals.goggles.Readout.bar(tooltip, "voltage", volts, 2 * Power.MIN_VOLTS, volts >= Power.MIN_VOLTS, volts + " V");
        if (!live()) ai.gsmc.fundamentals.goggles.Readout.needs(tooltip, "volts", Power.MIN_VOLTS);
        return true;
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
