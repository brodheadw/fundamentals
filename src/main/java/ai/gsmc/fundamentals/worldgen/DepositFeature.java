package ai.gsmc.fundamentals.worldgen;

import ai.gsmc.fundamentals.registry.OreBlock;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * An ore deposit: ore distributed through a body of rock — either a host rock of its own or the
 * stone already there — in one of the shapes ore really takes. Vanilla's blob-shaped {@code ore} feature can't express any of them.
 *
 * <ul>
 *   <li>{@code bed} — a gently dipping lens (sedimentary layers, layered intrusions)</li>
 *   <li>{@code plug} — a steep pipe or stock (porphyries, carbonatites)</li>
 *   <li>{@code vein} — a thin steep sheet along a fracture (hydrothermal veins, dykes)</li>
 *   <li>{@code blanket} — a layer following the land surface (laterites, gossan caps)</li>
 * </ul>
 *
 * <p>Everything stays within 15 blocks of the origin horizontally, so a deposit never writes
 * outside the 3x3 chunks a feature is allowed to touch.
 */
public class DepositFeature extends Feature<DepositFeature.Config> {

    public static final DepositFeature INSTANCE = new DepositFeature();

    private static final int REACH = 15;

    public enum Shape implements StringRepresentable {
        BED, PLUG, VEIN, BLANKET;

        public static final Codec<Shape> CODEC = StringRepresentable.fromEnum(Shape::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    /** How an ore is spread through the host rock. */
    public enum Style implements StringRepresentable {
        /** Isolated grains scattered evenly. */
        DISSEMINATED,
        /** Irregular masses a few blocks across. */
        POCKETS,
        /** Continuous thin layers following the body's dip. */
        SEAMS,
        /** Pockets confined to the upper part of the body (supergene enrichment). */
        TOP;

        public static final Codec<Style> CODEC = StringRepresentable.fromEnum(Style::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public record Range(int min, int max) {
        public static final Range NONE = new Range(0, 0);
        public static final Codec<Range> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("min").forGetter(Range::min),
                Codec.INT.fieldOf("max").forGetter(Range::max)).apply(i, Range::new));

        int sample(RandomSource random) {
            return min + random.nextInt(max - min + 1);
        }
    }

    /**
     * @param deepslate the block used where the ore replaces deepslate, if it has such a twin
     * @param fraction  share of the body's blocks that are this ore
     */
    public record Ore(BlockState state, Optional<BlockState> deepslate, float fraction, Style style) {
        public static final Codec<Ore> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockState.CODEC.fieldOf("state").forGetter(Ore::state),
                BlockState.CODEC.optionalFieldOf("deepslate_state").forGetter(Ore::deepslate),
                Codec.FLOAT.fieldOf("fraction").forGetter(Ore::fraction),
                Style.CODEC.fieldOf("style").forGetter(Ore::style)).apply(i, Ore::new));
    }

    /**
     * @param host        rock the whole body is turned into; empty leaves the existing rock, so
     *                    the ore sits directly in stone or deepslate
     * @param radius      horizontal half-extent (half the strike length, for a vein)
     * @param thickness   bed/blanket thickness, or vein width
     * @param height      vertical extent of a plug or vein; unused otherwise
     * @param replaceable blocks the deposit may replace
     */
    public record Config(Shape shape, Optional<BlockState> host, List<Ore> ores, Range radius, Range thickness,
                         Range height, TagKey<Block> replaceable) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(i -> i.group(
                Shape.CODEC.fieldOf("shape").forGetter(Config::shape),
                BlockState.CODEC.optionalFieldOf("host").forGetter(Config::host),
                Ore.CODEC.listOf().fieldOf("ores").forGetter(Config::ores),
                Range.CODEC.fieldOf("radius").forGetter(Config::radius),
                Range.CODEC.fieldOf("thickness").forGetter(Config::thickness),
                Range.CODEC.optionalFieldOf("height", Range.NONE).forGetter(Config::height),
                TagKey.codec(Registries.BLOCK).fieldOf("replaceable").forGetter(Config::replaceable)
        ).apply(i, Config::new));
    }

    public DepositFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<Config> context) {
        Body body = new Body(context.level(), context.config(), context.origin(),
                context.level().getSeed() ^ context.origin().asLong() * 0x9E3779B97F4A7C15L);
        RandomSource random = context.random();
        int radius = Math.min(REACH, context.config().radius().sample(random));
        int thickness = context.config().thickness().sample(random);
        int height = context.config().height().sample(random);
        switch (context.config().shape()) {
            case BED -> bed(body, random, radius, thickness);
            case PLUG -> plug(body, random, radius, height);
            case VEIN -> vein(body, random, radius, thickness, height);
            case BLANKET -> blanket(body, radius, thickness);
        }
        return body.placed > 0;
    }

    private static void bed(Body body, RandomSource random, int radius, int thickness) {
        double dipX = (random.nextDouble() - 0.5) * 0.4;
        double dipZ = (random.nextDouble() - 0.5) * 0.4;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) / (radius * body.edge(dx, dz));
                if (d >= 1) continue;
                double centre = dipX * dx + dipZ * dz;
                double half = Math.max(0.6, thickness / 2.0 * Math.sqrt(1 - d * d));  // thins to the edge
                for (int dy = (int) Math.floor(centre - half); dy <= Math.ceil(centre + half); dy++) {
                    body.put(dx, dy, dz, dy - centre, (dy - centre) / half, Math.max(d, Math.abs(dy - centre) / half * 0.8));
                }
            }
        }
    }

    private static void plug(Body body, RandomSource random, int radius, int height) {
        double leanX = (random.nextDouble() - 0.5) * 0.2;
        double leanZ = (random.nextDouble() - 0.5) * 0.2;
        for (int dy = 0; dy < height; dy++) {
            double t = (dy + 0.5) / height;
            double r = radius * Math.pow(Math.sin(Math.PI * t), 0.35) * (0.75 + 0.5 * body.noise(0, dy * 0.15, 40));
            double cx = leanX * (dy - height / 2.0), cz = leanZ * (dy - height / 2.0);
            for (int dx = -REACH; dx <= REACH; dx++) {
                for (int dz = -REACH; dz <= REACH; dz++) {
                    double d = Math.sqrt((dx - cx) * (dx - cx) + (dz - cz) * (dz - cz));
                    double rim = r * body.edge(dx, dz);
                    if (d < rim) {
                        body.put(dx, dy - height / 2, dz, dy, t * 2 - 1, d / rim);
                    }
                }
            }
        }
    }

    private static void vein(Body body, RandomSource random, int radius, int thickness, int height) {
        double strike = random.nextDouble() * Math.PI;
        double nx = Math.cos(strike), nz = Math.sin(strike);
        double dip = (random.nextDouble() - 0.5) * 0.7;  // sideways shift per block of depth: steep, not vertical
        double halfHeight = height / 2.0;
        for (int dx = -REACH; dx <= REACH; dx++) {
            for (int dz = -REACH; dz <= REACH; dz++) {
                double along = -dx * nz + dz * nx;
                if (Math.abs(along) >= radius) continue;
                for (int dy = (int) -halfHeight; dy <= halfHeight; dy++) {
                    double across = dx * nx + dz * nz + dy * dip;
                    double taper = Math.sqrt(1 - Math.pow(along / radius, 2)) * Math.sqrt(1 - Math.pow(dy / (halfHeight + 1), 2));
                    // Veins pinch and swell along their length.
                    double half = thickness / 2.0 * taper * (0.6 + 0.9 * body.noise(along * 0.2, dy * 0.2, 80));
                    if (half > 0.3 && Math.abs(across) <= Math.max(half, 0.5)) {
                        body.put(dx, dy, dz, dy, dy / (halfHeight + 1),
                                Math.max(Math.abs(along) / radius, Math.abs(dy) / (halfHeight + 1)));
                    }
                }
            }
        }
    }

    private static void blanket(Body body, int radius, int thickness) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz) / (radius * body.edge(dx, dz));
                if (d >= 1) continue;
                int x = body.origin.getX() + dx, z = body.origin.getZ() + dz;
                int surface = body.level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                if (!body.level.getFluidState(pos.set(x, surface + 1, z)).isEmpty()) continue;  // not under water
                int depth = (int) Math.ceil(thickness * Math.sqrt(1 - d * d));
                for (int k = 0; k < depth; k++) {
                    // Starts one block down, leaving the topsoil in place.
                    body.put(dx, surface - 1 - k - body.origin.getY(), dz, -k, 1 - 2.0 * k / depth, d);
                }
            }
        }
    }

    /** One deposit being placed: knows which block goes at each position inside the body. */
    private static final class Body {
        final WorldGenLevel level;
        final Config config;
        final BlockPos origin;
        final long seed;
        final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int placed;

        Body(WorldGenLevel level, Config config, BlockPos origin, long seed) {
            this.level = level;
            this.config = config;
            this.origin = origin;
            this.seed = seed;
        }

        /** Wobble for the body's outline, 0.8..1.2, so no deposit is a clean circle. */
        double edge(int dx, int dz) {
            return 0.8 + 0.4 * noise(dx * 0.22, dz * 0.22, 0);
        }

        double noise(double a, double b, int salt) {
            return Noise.value(seed + salt, a, b, 0);
        }

        /**
         * @param layer    height within the body in blocks, following its dip (selects seams)
         * @param vertical -1 at the bottom of the body to +1 at the top
         * @param edge     0 at the heart of the body to 1 at its rim; ore thins out toward the rim
         */
        void put(int dx, int dy, int dz, double layer, double vertical, double edge) {
            if (Math.abs(dx) > REACH || Math.abs(dz) > REACH) return;
            pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
            if (level.isOutsideBuildHeight(pos)) return;
            BlockState existing = level.getBlockState(pos);
            if (!existing.is(config.replaceable())) return;
            BlockState state = stateAt(layer, vertical, edge, existing.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES));
            if (state != null) {
                level.setBlock(pos, state, Block.UPDATE_CLIENTS);
                placed++;
            }
        }

        /** The block for this spot, or null to leave the existing rock alone. */
        private BlockState stateAt(double layer, double vertical, double edge, boolean inDeepslate) {
            // Rich in the middle, a scatter at the rim: no deposit ends in a clean wall of ore.
            float richness = (float) (1 - 0.85 * edge * edge);
            for (int i = 0; i < config.ores().size(); i++) {
                Ore ore = config.ores().get(i);
                long salt = seed + 1000L * (i + 1);
                boolean here = switch (ore.style()) {
                    case DISSEMINATED -> Noise.hash(salt, pos.getX(), pos.getY(), pos.getZ()) < ore.fraction() * richness;
                    case POCKETS -> pocket(salt, ore.fraction() * richness);
                    case SEAMS -> Noise.hash(salt, (int) Math.floor(layer), 0, 0) < ore.fraction();
                    case TOP -> vertical > 0.35 && pocket(salt, Math.min(0.9F, ore.fraction() * 3 * richness));
                };
                if (here) {
                    BlockState state = inDeepslate ? ore.deepslate().orElse(ore.state()) : ore.state();
                    return state.hasProperty(OreBlock.GRADE)
                            ? state.setValue(OreBlock.GRADE, OreBlock.Grade.at(edge)) : state;
                }
            }
            return config.host().orElse(null);
        }

        private boolean pocket(long salt, float fraction) {
            return Noise.value(salt, pos.getX() * 0.3, pos.getY() * 0.3, pos.getZ() * 0.3) > Noise.threshold(fraction);
        }
    }

    /** Small deterministic value noise; worldgen must give the same answer for the same seed. */
    private static final class Noise {
        private static final double[] SORTED = new double[8192];

        static {
            for (int i = 0; i < SORTED.length; i++) {
                SORTED[i] = value(12345, i * 0.731, i * 0.377, i * 0.519);
            }
            Arrays.sort(SORTED);
        }

        /** The noise level that a {@code fraction} share of space exceeds. */
        static double threshold(float fraction) {
            int index = (int) ((1 - fraction) * (SORTED.length - 1));
            return SORTED[Math.max(0, Math.min(SORTED.length - 1, index))];
        }

        static double hash(long seed, int x, int y, int z) {
            long h = seed;
            h = (h ^ x * 0x9E3779B97F4A7C15L) * 0xBF58476D1CE4E5B9L;
            h = (Long.rotateLeft(h, 27) ^ y * 0xC2B2AE3D27D4EB4FL) * 0x94D049BB133111EBL;
            h = (Long.rotateLeft(h, 31) ^ z * 0x165667B19E3779F9L) * 0xD6E8FEB86659FD93L;
            h ^= h >>> 32;
            return (h >>> 11) * 0x1.0p-53;
        }

        static double value(long seed, double x, double y, double z) {
            int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y), z0 = (int) Math.floor(z);
            double fx = smooth(x - x0), fy = smooth(y - y0), fz = smooth(z - z0);
            double result = 0;
            for (int i = 0; i < 8; i++) {
                int ix = i & 1, iy = (i >> 1) & 1, iz = (i >> 2) & 1;
                double weight = (ix == 1 ? fx : 1 - fx) * (iy == 1 ? fy : 1 - fy) * (iz == 1 ? fz : 1 - fz);
                result += weight * hash(seed, x0 + ix, y0 + iy, z0 + iz);
            }
            return result;
        }

        private static double smooth(double t) {
            return t * t * (3 - 2 * t);
        }
    }
}
