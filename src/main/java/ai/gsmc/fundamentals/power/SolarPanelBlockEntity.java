package ai.gsmc.fundamentals.power;

import com.drmangotea.tfmg.content.electricity.base.ElectricBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

public class SolarPanelBlockEntity extends ElectricBlockEntity {

    public static final int VOLTS = 120;
    public static final int WATTS = 200;

    private int sun;

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(Electricity.solarPanelEntity(), pos, state);
    }

    private int sunlight() {
        BlockPos above = worldPosition.above();
        return level.dimensionType().hasSkyLight() && level.canSeeSky(above)
                ? Math.max(0, level.getBrightness(LightLayer.SKY, above) - level.getSkyDarken()) : 0;
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (!level.isClientSide && sunlight() != sun) {
            sun = sunlight();
            updateNextTick();
        }
    }

    // A cell holds its voltage as the light fades; what falls off is the power behind it.
    @Override
    public int voltageGeneration() {
        return sun > 0 ? VOLTS : 0;
    }

    @Override
    public int powerGeneration() {
        return WATTS * sun / 15;
    }
}
