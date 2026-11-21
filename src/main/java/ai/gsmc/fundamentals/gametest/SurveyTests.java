package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.bombs.Fallout;
import ai.gsmc.fundamentals.heat.ThermometerBlock;
import ai.gsmc.fundamentals.survey.Disturbances;
import ai.gsmc.fundamentals.survey.GeigerCounterItem;
import ai.gsmc.fundamentals.survey.RadioTelescopeBlockEntity;
import ai.gsmc.fundamentals.survey.SeismometerBlockEntity;
import ai.gsmc.fundamentals.survey.Survey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class SurveyTests {

    @GameTest(template = "empty", batch = "survey")
    public void theGeigerCounterFindsTheSourceAndTheFallout(GameTestHelper helper) {
        var level = helper.getLevel();
        Vec3 tube = Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 1, 0)));
        Vec3 above = Vec3.atCenterOf(helper.absolutePos(new BlockPos(2, 2, 2)));
        // neighbouring tests may leave sources nearby, so measure what each spot already reads
        double quiet = GeigerCounterItem.countsPerMinute(level, tube), quietAbove = GeigerCounterItem.countsPerMinute(level, above);
        BlockPos store = new BlockPos(2, 1, 2);
        helper.setBlock(store, Blocks.CHEST);
        ((ChestBlockEntity) helper.getBlockEntity(store)).setItem(0, new ItemStack(BuiltInRegistries.ITEM.get(Fundamentals.id("polonium"))));
        double near = GeigerCounterItem.countsPerMinute(level, tube) - quiet;
        double nearer = GeigerCounterItem.countsPerMinute(level, above) - quietAbove;
        helper.assertTrue(near > 100 && nearer > near * 4, "the polonium should count, more as the tube nears it: " + near + ", " + nearer);
        Fallout.get(level).add(helper.absolutePos(new BlockPos(0, 1, 0)), 4, 200);
        helper.assertTrue(GeigerCounterItem.countsPerMinute(level, tube) > quiet + near + 10000, "fresh fallout should roar");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "survey")
    public void theSeismometerGivesAQuakesMagnitudeAndDistance(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, Survey.seismometer().defaultBlockState().setValue(ThermometerBlock.FACING, Direction.UP));
        SeismometerBlockEntity seismometer = helper.getBlockEntity(at);
        BlockPos station = helper.absolutePos(at);
        Disturbances.add(level, Disturbances.Kind.QUAKE, station.offset(5000, 0, 0), Disturbances.magnitude(0.01));
        seismometer.sample(level);
        helper.assertTrue(seismometer.signal() == 0, "a stick of TNT five thousand blocks off is lost in the noise");
        double tnt = Disturbances.magnitude(Disturbances.kilotons(24) * 1000);
        Disturbances.add(level, Disturbances.Kind.QUAKE, station.offset(300, 0, 400), tnt);
        seismometer.sample(level);
        helper.assertTrue(seismometer.signal() > 0 && Math.abs(seismometer.distance() - 500) < 1 && Math.abs(seismometer.magnitude() - tnt) < 1e-9,
                "a fission blast 500 blocks off should read with its distance and magnitude: " + seismometer.signal() + ", " + seismometer.distance());
        helper.assertTrue(tnt > 4 && tnt < 5.5, "fifteen kilotons should read like Hiroshima, about magnitude 4.5: " + tnt);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "survey_sky")
    public void theDishSeesTheSunAtNoonAndAPulseOverEverything(GameTestHelper helper) {
        var level = helper.getLevel();
        long time = level.getDayTime();
        BlockPos up = new BlockPos(0, 1, 0), north = new BlockPos(2, 1, 2);
        helper.setBlock(up, Survey.radioTelescope().defaultBlockState().setValue(ThermometerBlock.FACING, Direction.UP));
        helper.setBlock(north, Survey.radioTelescope().defaultBlockState().setValue(ThermometerBlock.FACING, Direction.NORTH));
        RadioTelescopeBlockEntity zenith = helper.getBlockEntity(up), tilted = helper.getBlockEntity(north);
        try {
            level.setDayTime(6000);
            zenith.sample(level);
            tilted.sample(level);
            helper.assertTrue(zenith.source().equals("sun") && zenith.kelvin() > 1.0E4, "the Sun is overhead at noon: " + zenith.source() + " " + zenith.kelvin());
            helper.assertTrue(!tilted.source().equals("sun"), "a dish tilted north misses the noon Sun");
            level.setDayTime(18000);
            zenith.sample(level);
            helper.assertTrue(zenith.kelvin() < 200, "the midnight sky is cold: " + zenith.kelvin());
            Disturbances.nuclear(level, helper.absolutePos(up).offset(3000, 0, 0), 24);
            zenith.sample(level);
            helper.assertTrue(zenith.source().equals("pulse") && zenith.signal() == 15, "a blast three thousand blocks off floods the dish");
        } finally {
            level.setDayTime(time);
        }
        helper.succeed();
    }
}
