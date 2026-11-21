package ai.gsmc.fundamentals.survey;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.List;
import java.util.function.BiConsumer;

/**
 * Instruments for what happens out of sight: the Geiger counter, the seismometer and the radio telescope; and for the weather
 * and the land: the hair hygrometer, the weather radar, the weather balloon, the aerial camera and the magnetometer; and from orbit, the
 * Earth-observation satellite and the ground station that tasks it.
 */
public final class Survey {

    private static final String[] POINTS = {"N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE", "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"};

    private static Block seismometer, radioTelescope, hygrometer, weatherRadar, groundStation;
    private static BlockEntityType<SeismometerBlockEntity> seismometerEntity;
    private static BlockEntityType<RadioTelescopeBlockEntity> radioTelescopeEntity;
    private static BlockEntityType<HygrometerBlockEntity> hygrometerEntity;
    private static BlockEntityType<WeatherRadarBlockEntity> weatherRadarEntity;
    private static BlockEntityType<GroundStationBlockEntity> groundStationEntity;
    private static EntityType<WeatherBalloon> weatherBalloonEntity;
    private static Item geigerCounter, seismometerItem, radioTelescopeItem, hygrometerItem, weatherRadarItem, weatherBalloon, aerialCamera, magnetometer,
            satellite, groundStationItem;

    private Survey() {}

    public static Block seismometer() { return seismometer; }
    public static Block radioTelescope() { return radioTelescope; }
    public static Block hygrometer() { return hygrometer; }
    public static Block weatherRadar() { return weatherRadar; }
    public static Block groundStation() { return groundStation; }
    public static BlockEntityType<SeismometerBlockEntity> seismometerEntity() { return seismometerEntity; }
    public static BlockEntityType<RadioTelescopeBlockEntity> radioTelescopeEntity() { return radioTelescopeEntity; }
    public static BlockEntityType<HygrometerBlockEntity> hygrometerEntity() { return hygrometerEntity; }
    public static BlockEntityType<WeatherRadarBlockEntity> weatherRadarEntity() { return weatherRadarEntity; }
    public static BlockEntityType<GroundStationBlockEntity> groundStationEntity() { return groundStationEntity; }
    public static EntityType<WeatherBalloon> weatherBalloonEntity() { return weatherBalloonEntity; }
    public static Item weatherBalloon() { return weatherBalloon; }
    public static Item satellite() { return satellite; }
    public static List<Item> items() {
        return List.of(geigerCounter, seismometerItem, radioTelescopeItem, hygrometerItem, weatherRadarItem, weatherBalloon, aerialCamera, magnetometer,
                satellite, groundStationItem);
    }

    /** The compass point, of sixteen, from one place toward another: north is toward negative z. */
    public static String bearing(double dx, double dz) {
        double degrees = Math.toDegrees(Math.atan2(dx, -dz));
        return POINTS[(int) Math.floorMod(Math.round(degrees / 22.5), 16)];
    }

    public static void registerBlocks(BiConsumer<ResourceLocation, Block> registry) {
        seismometer = new SurveyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F).sound(SoundType.COPPER).noOcclusion(),
                Survey::seismometerEntity, SeismometerBlockEntity::new, true);
        radioTelescope = new SurveyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).noOcclusion(),
                Survey::radioTelescopeEntity, RadioTelescopeBlockEntity::new, false);
        hygrometer = new SurveyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(1.0F).sound(SoundType.COPPER).noOcclusion(),
                Survey::hygrometerEntity, HygrometerBlockEntity::new, true);
        weatherRadar = new SurveyBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).noOcclusion(),
                Survey::weatherRadarEntity, WeatherRadarBlockEntity::new, false);
        registry.accept(Fundamentals.id("seismometer"), seismometer);
        registry.accept(Fundamentals.id("radio_telescope"), radioTelescope);
        registry.accept(Fundamentals.id("hygrometer"), hygrometer);
        registry.accept(Fundamentals.id("weather_radar"), weatherRadar);
        groundStation = new GroundStationBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.0F).sound(SoundType.METAL).noOcclusion());
        registry.accept(Fundamentals.id("ground_station"), groundStation);
    }

    public static void registerBlockEntities(BiConsumer<ResourceLocation, BlockEntityType<?>> registry) {
        seismometerEntity = BlockEntityType.Builder.of(SeismometerBlockEntity::new, seismometer).build(null);
        radioTelescopeEntity = BlockEntityType.Builder.of(RadioTelescopeBlockEntity::new, radioTelescope).build(null);
        hygrometerEntity = BlockEntityType.Builder.of(HygrometerBlockEntity::new, hygrometer).build(null);
        weatherRadarEntity = BlockEntityType.Builder.of(WeatherRadarBlockEntity::new, weatherRadar).build(null);
        registry.accept(Fundamentals.id("seismometer"), seismometerEntity);
        registry.accept(Fundamentals.id("radio_telescope"), radioTelescopeEntity);
        registry.accept(Fundamentals.id("hygrometer"), hygrometerEntity);
        registry.accept(Fundamentals.id("weather_radar"), weatherRadarEntity);
        groundStationEntity = BlockEntityType.Builder.of(GroundStationBlockEntity::new, groundStation).build(null);
        registry.accept(Fundamentals.id("ground_station"), groundStationEntity);
    }

    public static void registerEntities(BiConsumer<ResourceLocation, EntityType<?>> registry) {
        weatherBalloonEntity = EntityType.Builder.<WeatherBalloon>of(WeatherBalloon::new, MobCategory.MISC).sized(0.8F, 1.2F)
                .clientTrackingRange(16).updateInterval(5).build(Fundamentals.id("weather_balloon").toString());
        registry.accept(Fundamentals.id("weather_balloon"), weatherBalloonEntity);
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        registry.accept(Fundamentals.id("geiger_counter"), geigerCounter = new GeigerCounterItem(new Item.Properties().stacksTo(1)));
        registry.accept(Fundamentals.id("seismometer"), seismometerItem = new BlockItem(seismometer, new Item.Properties()));
        registry.accept(Fundamentals.id("radio_telescope"), radioTelescopeItem = new BlockItem(radioTelescope, new Item.Properties()));
        registry.accept(Fundamentals.id("hygrometer"), hygrometerItem = new BlockItem(hygrometer, new Item.Properties()));
        registry.accept(Fundamentals.id("weather_radar"), weatherRadarItem = new BlockItem(weatherRadar, new Item.Properties()));
        registry.accept(Fundamentals.id("weather_balloon"), weatherBalloon = new WeatherBalloonItem(new Item.Properties().stacksTo(16)));
        registry.accept(Fundamentals.id("aerial_camera"), aerialCamera = new AerialCameraItem(new Item.Properties().stacksTo(1)));
        registry.accept(Fundamentals.id("magnetometer"), magnetometer = new MagnetometerItem(new Item.Properties().stacksTo(1)));
        registry.accept(Fundamentals.id("satellite"), satellite = new SatelliteItem(new Item.Properties().stacksTo(1)));
        registry.accept(Fundamentals.id("ground_station"), groundStationItem = new BlockItem(groundStation, new Item.Properties()));
    }
}
