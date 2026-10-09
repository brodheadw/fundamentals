package ai.gsmc.fundamentals.oxidation;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

import javax.annotation.Nullable;

/**
 * A metal block that weathers the way vanilla's copper does, a stage at a time on random ticks, held back by its neighbours,
 * with the stages, the honeycomb wax and the axe's scraping read from NeoForge's oxidizables and waxables data maps. Unlike
 * copper, how fast depends on the metal ({@code rate} times copper's) and the air round it. A block whose last stage has
 * crumbled to oxide cannot be scraped back.
 */
public class WeatheringMetalBlock extends Block implements WeatheringCopper {

    private final WeatherState age;
    private final Weathering.Family family;

    public WeatheringMetalBlock(WeatherState age, Weathering.Family family, Properties properties) {
        super(properties);
        this.age = age;
        this.family = family;
    }

    @Override
    public WeatherState getAge() {
        return age;
    }

    public Weathering.Family family() {
        return family;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return WeatheringCopper.getNext(state.getBlock()).isPresent();
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        double factor = family.rate() * switch (Moisture.at(level, pos, true)) {
            case DRY -> family.dry();
            case AIR -> 1;
            case WET -> family.wet();
            case SALT -> family.wet() * 2;
        };
        if (random.nextFloat() < 0.05688889F * factor) {
            getNextState(state, level, pos, random).ifPresent(next -> level.setBlockAndUpdate(pos, next));
        }
    }

    @Nullable
    @Override
    public BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility ability, boolean simulate) {
        if (ability == ItemAbilities.AXE_SCRAPE && age == WeatherState.OXIDIZED && family.crumbles()) {
            return null;
        }
        return super.getToolModifiedState(state, context, ability, simulate);
    }
}
