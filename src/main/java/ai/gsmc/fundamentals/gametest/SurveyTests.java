package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.bombs.Fallout;
import ai.gsmc.fundamentals.heat.ThermometerBlock;
import ai.gsmc.fundamentals.registry.OreBlock;
import ai.gsmc.fundamentals.survey.AerialCameraItem;
import ai.gsmc.fundamentals.survey.Disturbances;
import ai.gsmc.fundamentals.survey.GeigerCounterItem;
import ai.gsmc.fundamentals.survey.GroundStationBlockEntity;
import ai.gsmc.fundamentals.survey.HygrometerBlockEntity;
import ai.gsmc.fundamentals.survey.MagnetometerItem;
import ai.gsmc.fundamentals.survey.RadioTelescopeBlockEntity;
import ai.gsmc.fundamentals.survey.SatelliteItem;
import ai.gsmc.fundamentals.survey.Satellites;
import ai.gsmc.fundamentals.survey.SeismometerBlockEntity;
import ai.gsmc.fundamentals.survey.Survey;
import ai.gsmc.fundamentals.survey.WeatherBalloon;
import ai.gsmc.fundamentals.survey.WeatherRadarBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.ServerLevelData;
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

    @GameTest(template = "empty", batch = "survey")
    public void theHygrometerReadsDampByWaterAndNearDryInADesert(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, Survey.hygrometer().defaultBlockState().setValue(ThermometerBlock.FACING, Direction.UP));
        HygrometerBlockEntity hygrometer = helper.getBlockEntity(at);
        hygrometer.sample(level);
        double air = hygrometer.humidity();
        helper.setBlock(at.east(), Blocks.WATER);
        hygrometer.sample(level);
        helper.assertTrue(hygrometer.humidity() > air || air == 100, "open water should wet the air: " + air + " to " + hygrometer.humidity());
        double desert = HygrometerBlockEntity.relativeHumidity(0, 40, false, false), jungle = HygrometerBlockEntity.relativeHumidity(0.9, 28, false, false);
        helper.assertTrue(desert < 5 && jungle > 60, "a desert's air is near dry, a jungle's damp: " + desert + ", " + jungle);
        helper.assertTrue(HygrometerBlockEntity.relativeHumidity(0, 40, false, true) >= 95, "rain saturates the air");
        helper.succeed();
    }

    // with the barograph, which also sets the world's weather; in a batch of its own ahead of the automata, their climb test flaked
    @GameTest(template = "empty", batch = "barometer")
    public void theRadarSeesRainOnlyWhileItRains(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        ServerLevelData data = (ServerLevelData) level.getLevelData();
        int clear = data.getClearWeatherTime(), rain = data.getRainTime(), thunder = data.getThunderTime();
        boolean raining = data.isRaining(), thundering = data.isThundering();
        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, Survey.weatherRadar().defaultBlockState());
        WeatherRadarBlockEntity radar = helper.getBlockEntity(at);
        try {
            level.setWeatherParameters(6000, 0, false, false);
            level.setRainLevel(0);
            radar.sample(level);
            helper.assertTrue(radar.echo().equals("clear") && radar.signal() == 0, "no echoes under a clear sky");
            level.setWeatherParameters(0, 6000, true, true);
            // the rain fades in over a second or so; a test cannot wait for it with the weather set
            level.setRainLevel(1);
            radar.sample(level);
            helper.assertTrue(!radar.echo().equals("clear") && radar.signal() > 0 && radar.cover() > 0,
                    "rain falling round about should echo: " + radar.echo() + " " + radar.signal());
            helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(at)).getAnalogOutputSignal(level, helper.absolutePos(at)) == radar.signal(),
                    "a comparator reads the echo");
        } finally {
            level.setWeatherParameters(clear, rain, raining, thundering);
            data.setThunderTime(thunder);
            level.setRainLevel(raining ? 1 : 0);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "survey", timeoutTicks = 200)
    public void theWeatherBalloonRadiosLowerPressureAsItClimbs(GameTestHelper helper) {
        helper.assertTrue(new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("tfmg:hydrogen_bucket"))).getCraftingRemainingItem().is(Items.BUCKET),
                "crafting a balloon gives back the hydrogen's bucket");
        // let go above the test's barrier roof, which would snag it
        BlockPos launch = helper.absolutePos(new BlockPos(1, 12, 1));
        WeatherBalloon balloon = new WeatherBalloon(helper.getLevel(), launch.getX() + 0.5, launch.getY(), launch.getZ() + 0.5, null);
        helper.getLevel().addFreshEntity(balloon);
        double[] ground = {0};
        helper.startSequence()
                .thenWaitUntil(() -> helper.assertTrue(balloon.reports() >= 1, "the sonde reports at launch"))
                .thenExecute(() -> ground[0] = balloon.hectopascals())
                .thenWaitUntil(() -> helper.assertTrue(balloon.reports() >= 2, "and again sixteen blocks up"))
                .thenExecute(() -> {
                    helper.assertTrue(balloon.hectopascals() < ground[0] - 1, "the pressure falls as it climbs: " + ground[0] + " to " + balloon.hectopascals());
                    balloon.discard();
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", batch = "survey")
    public void theAerialCameraPrintsAMapOfTheGroundAndItsBiomes(GameTestHelper helper) {
        ItemStack photo = AerialCameraItem.photograph(helper.getLevel(), helper.absolutePos(new BlockPos(1, 1, 1)));
        var data = MapItem.getSavedData(photo, helper.getLevel());
        int seen = 0;
        for (byte colour : data.colors) if (colour != 0) seen++;
        helper.assertTrue(photo.is(Items.FILLED_MAP) && seen > 128 * 128 * 9 / 10, "the frame should cover the map: " + seen);
        helper.assertTrue(photo.get(DataComponents.LORE).lines().size() == 9, "the margin lists the ground below and eight bearings");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "survey")
    public void theMagnetometerPointsToMagnetite(GameTestHelper helper) {
        var level = helper.getLevel();
        Vec3 probe = Vec3.atCenterOf(helper.absolutePos(new BlockPos(0, 1, 0)));
        MagnetometerItem.Reading quiet = MagnetometerItem.read(level, probe);
        var ore = BuiltInRegistries.BLOCK.get(Fundamentals.id("magnetite_ore")).defaultBlockState().setValue(OreBlock.GRADE, OreBlock.Grade.CORE);
        for (int z = 0; z < 3; z++) helper.setBlock(new BlockPos(5, 1, z), ore);
        MagnetometerItem.Reading near = MagnetometerItem.read(level, probe);
        Vec3 pull = near.toward().subtract(quiet.toward());
        helper.assertTrue(near.anomaly() - quiet.anomaly() > 100, "three blocks of magnetite five blocks off should read: " + (near.anomaly() - quiet.anomaly()));
        helper.assertTrue(pull.x > 0 && Math.abs(pull.x) > Math.abs(pull.z), "and point east toward it: " + pull);
        helper.succeed();
    }

    /** A station at (1, 1, 1) tasking a satellite let go over it by a fresh player. */
    private static GroundStationBlockEntity station(GameTestHelper helper, Player player) {
        BlockPos at = new BlockPos(1, 1, 1);
        helper.setBlock(at, Survey.groundStation().defaultBlockState());
        BlockPos pos = helper.absolutePos(at);
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        SatelliteItem.deploy(helper.getLevel().getServer(), player, new ItemStack(Survey.satellite()));
        GroundStationBlockEntity station = helper.getBlockEntity(at);
        helper.assertTrue(station.cycle(helper.getLevel(), player).isPresent(), "the station tasks the player's satellite");
        return station;
    }

    @GameTest(template = "empty", batch = "survey")
    public void aSatelliteGoesIntoOrbitOverWhereItIsLetGo(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        player.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        ItemStack stack = new ItemStack(Survey.satellite());
        Satellites.Satellite satellite = SatelliteItem.deploy(helper.getLevel().getServer(), player, stack);
        var owned = Satellites.get(helper.getLevel().getServer()).owned(player.getUUID());
        helper.assertTrue(stack.isEmpty() && owned.size() == 1 && owned.getFirst().equals(satellite), "the satellite leaves the hand for orbit");
        helper.assertTrue(satellite.x() == pos.getX() && satellite.z() == pos.getZ(), "over where it was let go: " + satellite);
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "survey")
    public void theGroundStationImagesItsTargetAtItsZoomAndOnlyInView(GameTestHelper helper) {
        GroundStationBlockEntity station = station(helper, helper.makeMockPlayer(GameType.SURVIVAL));
        station.zoom();
        station.zoom();
        int x = station.targetX() + 100, z = station.targetZ() - 50;
        station.target(x, z);
        ItemStack frame = station.image(helper.getLevel());
        var data = MapItem.getSavedData(frame, helper.getLevel());
        helper.assertTrue(frame.is(Items.FILLED_MAP) && data != null, "a frame comes down as a map: " + station.status());
        helper.assertTrue(data.centerX == x && data.centerZ == z && data.scale == 2 && data.locked,
                "centred on the target at the zoom, locked: " + data.centerX + ", " + data.centerZ + " scale " + data.scale);
        station.target(x + Satellites.VIEW + 1, z);
        helper.assertTrue(station.image(helper.getLevel()).isEmpty() && station.status().equals("view"), "beyond its view it refuses");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "survey")
    public void recordingPrintsFramesIntoTheChestBesideItFromItsPaper(GameTestHelper helper) {
        GroundStationBlockEntity station = station(helper, helper.makeMockPlayer(GameType.SURVIVAL));
        station.cycleRecording();
        BlockPos store = new BlockPos(2, 1, 1);
        helper.setBlock(store, Blocks.CHEST);
        ChestBlockEntity chest = helper.getBlockEntity(store);
        chest.setItem(0, new ItemStack(Items.PAPER));
        helper.assertTrue(station.record(helper.getLevel()), "a frame is recorded: " + station.status());
        int maps = 0, paper = 0;
        for (int i = 0; i < chest.getContainerSize(); i++) {
            if (chest.getItem(i).is(Items.FILLED_MAP)) maps++;
            paper += chest.getItem(i).is(Items.PAPER) ? chest.getItem(i).getCount() : 0;
        }
        helper.assertTrue(maps == 1 && paper == 0, "the frame goes in the chest and its paper is used: " + maps + " maps, " + paper + " paper");
        helper.assertTrue(!station.record(helper.getLevel()) && station.interval() == 0, "out of paper, recording stops");
        helper.succeed();
    }
}
