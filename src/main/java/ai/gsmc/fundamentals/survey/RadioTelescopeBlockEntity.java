package ai.gsmc.fundamentals.survey;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;

/**
 * A dish that gathers radio noise into a feed horn, read as the antenna temperature radio astronomers use: the temperature a
 * black body filling the beam would need to give the same power. The cold sky reads about ten kelvin, the ground about 290,
 * the quiet Sun tens of thousands, a nearby lightning stroke millions. Radio goes through cloud, so rain hides nothing.
 */
public class RadioTelescopeBlockEntity extends SurveyBlockEntity {

    public static final double SKY = 10, GALAXY = 40, GROUND = 290, RAIN = 30, STORM = 3000, SUN = 50000, STROKE = 1.0E6, PULSE = 1.0E9;
    /** How far from the dish's axis the Sun still falls in the beam. */
    static final double BEAM = Math.toRadians(30);
    static final double STROKE_RANGE = 2048, PULSE_RANGE = 8192;
    static final long BURST = 40;

    private double kelvin = SKY, away;
    private String source = "sky";

    public RadioTelescopeBlockEntity(BlockPos pos, BlockState state) {
        super(Survey.radioTelescopeEntity(), pos, state);
    }

    /** Straight up, or tilted halfway up the sky over the horizon it faces. */
    static Vec3 axis(Direction facing) {
        return facing == Direction.UP ? new Vec3(0, 1, 0) : new Vec3(facing.getStepX(), 1, facing.getStepZ()).normalize();
    }

    /** Where the Sun stands: rising in the east, overhead at noon, setting in the west. */
    static Vec3 sun(ServerLevel level) {
        double angle = 2 * Math.PI * level.getTimeOfDay(1F);
        return new Vec3(-Math.sin(angle), Math.cos(angle), 0);
    }

    @Override
    protected void read(ServerLevel level) {
        Direction facing = facing();
        away = 0;
        if (facing == Direction.DOWN || !level.canSeeSky(worldPosition.above())) {
            kelvin = GROUND;
            source = "ground";
        } else {
            Vec3 sun = sun(level);
            boolean day = sun.y > 0;
            kelvin = SKY;
            source = "sky";
            double strongest = 0;
            if (day && Math.acos(Math.clamp(sun.dot(axis(facing)), -1, 1)) < BEAM) {
                kelvin += SUN;
                strongest = SUN;
                source = "sun";
            } else if (!day) {
                kelvin += GALAXY;
                strongest = GALAXY;
                source = "galaxy";
            }
            if (level.isRaining()) kelvin += RAIN;
            if (level.isThundering()) {
                kelvin += STORM;
                if (STORM > strongest) { strongest = STORM; source = "storm"; }
            }
            for (Disturbances.Event stroke : Disturbances.since(level, Disturbances.Kind.SFERIC, BURST)) {
                double d = Math.sqrt(stroke.pos().distSqr(worldPosition));
                if (d > STROKE_RANGE) continue;
                double t = STROKE * Math.pow(100 / Math.max(d, 100), 2);
                kelvin += t;
                if (t > strongest) { strongest = t; source = "stroke"; away = d; }
            }
            for (Disturbances.Event pulse : Disturbances.since(level, Disturbances.Kind.PULSE, BURST)) {
                double d = Math.sqrt(pulse.pos().distSqr(worldPosition));
                if (d > PULSE_RANGE) continue;
                kelvin += PULSE;
                strongest = PULSE;
                source = "pulse";
                away = d;
            }
        }
    }

    public double kelvin() { return kelvin; }
    public String source() { return source; }

    /** Logarithmic: from the cold sky at nothing to a fission pulse at 15. */
    @Override
    public double fraction() {
        return Math.clamp((Math.log10(Math.max(kelvin, SKY)) - 1) / 8, 0, 1);
    }

    @Override
    protected void describe(List<Component> lines) {
        String key = "goggles.fundamentals.radio_telescope.";
        lines.add(Component.translatable(key + "kelvin", String.format(Locale.ROOT, kelvin >= 1.0E5 ? "%.1e" : "%,.0f", kelvin)));
        lines.add(Component.translatable(key + source, String.format(Locale.ROOT, "%,.0f", away))
                .withStyle(source.equals("pulse") || source.equals("stroke") ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY));
    }

    @Override
    protected void save(CompoundTag tag) {
        tag.putDouble("kelvin", kelvin);
        tag.putDouble("away", away);
        tag.putString("source", source);
    }

    @Override
    protected void load(CompoundTag tag) {
        kelvin = tag.contains("kelvin") ? tag.getDouble("kelvin") : SKY;
        away = tag.getDouble("away");
        source = tag.contains("source") ? tag.getString("source") : "sky";
    }
}
