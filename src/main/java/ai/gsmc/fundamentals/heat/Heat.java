package ai.gsmc.fundamentals.heat;

import ai.gsmc.fundamentals.Fundamentals;
import com.mojang.brigadier.Command;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Function;

/**
 * The temperature at a block, in °C, read on demand: the biome's climate mapped to degrees, cooled by altitude,
 * swinging with the day and the weather under open sky, plus every heat source within reach (lava, fire, burners,
 * lit furnaces, ice the other way) summed with falloff. No diffusion, nothing ticks; a reading is cached for a
 * second. Other mods add sources through the data map, scale them by state with {@link #registerScaler}, read the
 * field with {@link #at}, and push on it with {@link #boost}.
 */
public final class Heat {

    public static final DataMapType<Block, HeatSource> SOURCES = DataMapType.builder(
            ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "heat_source"), Registries.BLOCK, HeatSource.CODEC).build();

    /** °C = SCALE × biome temperature + OFFSET: tundra -5, taiga 1, plains 15, jungle 19, desert 45. */
    public static final double SCALE = 25, OFFSET = -5;
    /** Half the day-night swing under open sky, and what rain and thunder take off. */
    public static final double SWING = 5, RAIN = -3, THUNDER = -5;
    private static final int CACHE_TICKS = 20;
    private static final int MAX_REACH = 6;

    /** A state-dependent factor on a source's strength: a burner's level, a furnace's fire. */
    public interface Scaler extends Function<BlockState, Double> {}

    /** Code that adds to the field, for things that are not blocks. */
    public interface Provider {
        double celsiusAt(Level level, BlockPos pos);
    }

    private record Boost(BlockPos pos, double celsius, int reach, long until) {}

    private static final Map<Block, Scaler> SCALERS = new HashMap<>();
    private static final List<Provider> PROVIDERS = new ArrayList<>();
    private static final Map<Level, Map<Long, double[]>> CACHE = new WeakHashMap<>();
    private static final Map<Level, List<Boost>> BOOSTS = new WeakHashMap<>();

    private Heat() {}

    public static void registerDataMaps(RegisterDataMapTypesEvent event) {
        event.register(SOURCES);
        // the burner's level and a furnace's fire are blockstate, which a data map cannot see; kindled is 1,000 °C, seething 1,600
        registerScaler(com.simibubi.create.AllBlocks.BLAZE_BURNER.get(), state -> switch (BlazeBurnerBlock.getHeatLevelOf(state)) {
            case NONE -> 0.0; case SMOULDERING -> 0.25; case FADING -> 0.5; case KINDLED -> 1.0; case SEETHING -> 1.6; });
    }

    public static void registerScaler(Block block, Scaler scaler) { SCALERS.put(block, scaler); }
    public static void registerProvider(Provider provider) { PROVIDERS.add(provider); }

    /** A transient source: {@code celsius} at {@code pos}, falling off over {@code reach}, for {@code ticks}. Negative cools. */
    public static void boost(Level level, BlockPos pos, double celsius, int reach, int ticks) {
        BOOSTS.computeIfAbsent(level, l -> new ArrayList<>()).add(new Boost(pos.immutable(), celsius, reach, level.getGameTime() + ticks));
        CACHE.remove(level);
    }

    /** The temperature at {@code pos}, °C. */
    public static double at(Level level, BlockPos pos) {
        Map<Long, double[]> cache = CACHE.computeIfAbsent(level, l -> new HashMap<>());
        long key = pos.asLong();
        double[] hit = cache.get(key);
        long now = level.getGameTime();
        if (hit != null && now - (long) hit[0] < CACHE_TICKS) {
            return hit[1];
        }
        double value = ambient(level, pos) + sources(level, pos);
        if (cache.size() > 4096) {
            cache.clear();
        }
        cache.put(key, new double[] {now, value});
        return value;
    }

    public static double fahrenheit(double celsius) { return celsius * 9 / 5 + 32; }

    /** The climate: biome, altitude, time of day and weather, the last two only under open sky. */
    public static double ambient(Level level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        double t = biome.value().getBaseTemperature();
        // vanilla's own height rule for snow: about a degree of its scale per eight hundred blocks above 80, which is a fifth of a °C per 8 blocks
        double celsius = SCALE * t + OFFSET - Math.max(0, pos.getY() - 80) * 0.025;
        if (level.canSeeSky(pos) && level.dimensionType().hasSkyLight()) {
            double day = (level.getDayTime() % 24000) / 24000.0;
            celsius += SWING * Math.cos((day - 0.25) * 2 * Math.PI);
            if (level.isThundering()) {
                celsius += THUNDER;
            } else if (level.isRaining() && biome.value().hasPrecipitation()) {
                celsius += RAIN;
            }
        }
        return celsius;
    }

    /** Every source within reach, its own heat at its block and its outside heat falling off linearly to nothing at its reach, plus boosts and providers. */
    public static double sources(Level level, BlockPos at) {
        double sum = 0;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-MAX_REACH, -MAX_REACH, -MAX_REACH), at.offset(MAX_REACH, MAX_REACH, MAX_REACH))) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            HeatSource source = state.getBlockHolder().getData(SOURCES);
            if (source == null) {
                continue;
            }
            double scale = SCALERS.containsKey(state.getBlock()) ? SCALERS.get(state.getBlock()).apply(state)
                    : state.hasProperty(BlockStateProperties.LIT) && !state.getValue(BlockStateProperties.LIT) ? 0 : 1;
            if (scale == 0) {
                continue;
            }
            double distance = Math.sqrt(pos.distSqr(at));
            sum += contribution((distance == 0 ? source.celsius() : source.outside()) * scale, source.reach(), distance);
        }
        List<Boost> boosts = BOOSTS.get(level);
        if (boosts != null) {
            long now = level.getGameTime();
            boosts.removeIf(b -> b.until < now);
            for (Boost b : boosts) {
                sum += contribution(b.celsius, b.reach, Math.sqrt(b.pos.distSqr(at)));
            }
        }
        for (Provider provider : PROVIDERS) {
            sum += provider.celsiusAt(level, at);
        }
        return sum;
    }

    private static double contribution(double celsius, int reach, double distance) {
        return distance > reach ? 0 : celsius * (1 - distance / (reach + 1));
    }

    public static Component readout(Level level, BlockPos pos) {
        double c = at(level, pos);
        return Component.translatable("heat.fundamentals.readout", String.format("%.0f", c), String.format("%.0f", fahrenheit(c)));
    }

    /** {@code /heat}: the temperature where you stand. */
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("heat").executes(ctx -> {
            var source = ctx.getSource();
            source.sendSuccess(() -> readout(source.getLevel(), BlockPos.containing(source.getPosition())), false);
            return Command.SINGLE_SUCCESS;
        }));
    }
}
