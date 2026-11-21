package ai.gsmc.fundamentals.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * What the instruments can pick up from afar: the ground shock of a blast, the radio crack of a lightning stroke, and the
 * electromagnetic pulse of a fission bomb. Each is logged where and when it happened; an instrument works out what reached it.
 */
public final class Disturbances {

    public enum Kind { QUAKE, SFERIC, PULSE }

    /** {@code size} is a quake's magnitude, a stroke's or pulse's nothing. */
    public record Event(Kind kind, BlockPos pos, double size, long time) {}

    /** A minute: as far back as a seismometer's last event is kept. */
    public static final long KEEP = 1200;
    /** An ordinary block of TNT, power 4, taken as ten kilograms; a blast's yield goes as the cube of its power. */
    static final double TNT_BLOCK_TONNES = 0.01;
    /** The share of a surface blast's energy that goes into the ground as seismic waves. */
    static final double COUPLING = 0.01;
    // shortcut: kept in memory, so a restart forgets the last minute; persist with SavedData if readings must survive one
    private static final Map<ResourceKey<Level>, List<Event>> RECENT = new HashMap<>();

    private Disturbances() {}

    public static void add(ServerLevel level, Kind kind, BlockPos pos, double size) {
        List<Event> events = RECENT.computeIfAbsent(level.dimension(), key -> new ArrayList<>());
        long now = level.getGameTime();
        events.removeIf(event -> now - event.time() > KEEP);
        events.add(new Event(kind, pos.immutable(), size, now));
    }

    /** Events of a kind no older than {@code ticks}. */
    public static List<Event> since(ServerLevel level, Kind kind, long ticks) {
        long now = level.getGameTime();
        return RECENT.getOrDefault(level.dimension(), List.of()).stream()
                .filter(event -> event.kind() == kind && now - event.time() <= ticks).toList();
    }

    /** Gutenberg and Richter: log10 E = 1.5 M + 4.8, with E the seismic energy in joules. */
    public static double magnitude(double tonnesTnt) {
        return (Math.log10(Math.max(tonnesTnt, 1.0E-9) * 4.184E9 * COUPLING) - 4.8) / 1.5;
    }

    /** A fission bomb's yield, scaled from Little Boy's fifteen kilotons at a fireball of 24 blocks. */
    public static double kilotons(int radius) {
        return 15.0 * Math.pow(radius / 24.0, 3);
    }

    public static void nuclear(ServerLevel level, BlockPos centre, int radius) {
        add(level, Kind.QUAKE, centre, magnitude(kilotons(radius) * 1000));
        add(level, Kind.PULSE, centre, 0);
    }

    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        double power = event.getExplosion().radius();
        add(level, Kind.QUAKE, BlockPos.containing(event.getExplosion().center()), magnitude(TNT_BLOCK_TONNES * Math.pow(power / 4.0, 3)));
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getEntity() instanceof LightningBolt bolt && event.getLevel() instanceof ServerLevel level) {
            add(level, Kind.SFERIC, bolt.blockPosition(), 0);
        }
    }
}
