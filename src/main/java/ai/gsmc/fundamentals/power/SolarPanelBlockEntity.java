package ai.gsmc.fundamentals.power;

import com.drmangotea.tfmg.content.electricity.base.ElectricBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;

public class SolarPanelBlockEntity extends ElectricBlockEntity implements com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation {

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

    @Override
    public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip, boolean isPlayerSneaking) {
        int light = sunlight();
        ai.gsmc.fundamentals.goggles.Readout.status(tooltip, getBlockState().getBlock().getName(), light > 0 ? "generating" : "dark");
        ai.gsmc.fundamentals.goggles.Readout.bar(tooltip, "sunlight", light, 15, light > 0, light + " / 15");
        ai.gsmc.fundamentals.goggles.Readout.bar(tooltip, "output", WATTS * light / 15.0, WATTS, light > 0, (WATTS * light / 15) + " W at " + (light > 0 ? VOLTS : 0) + " V");
        if (light == 0) ai.gsmc.fundamentals.goggles.Readout.needs(tooltip, "sky");
        return true;
    }

    @Override
    public int voltageGeneration() {
        return sun > 0 ? VOLTS : 0;
    }

    @Override
    public int powerGeneration() {
        return WATTS * sun / 15;
    }
}
