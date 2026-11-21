package ai.gsmc.fundamentals.survey;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The satellites in orbit over the Overworld: whose each is, its name, and the ground it was released over. An Earth-observation
 * satellite's orbit carries it back over the same strip of ground pass after pass, so it can only image within {@link #VIEW}
 * blocks of where it went up.
 */
public final class Satellites extends SavedData {

    /** Ad Astra's Earth orbit, the only place a satellite can be let go. */
    public static final String ORBIT = "ad_astra:earth_orbit";
    public static final int VIEW = 4096;
    private static final String NAME = "fundamentals_satellites";

    public record Satellite(int id, UUID owner, String name, int x, int z) {
        public Component label() {
            return name.isEmpty() ? Component.translatable("satellite.fundamentals.default", id) : Component.literal(name);
        }

        public boolean sees(int tx, int tz) {
            long dx = tx - x, dz = tz - z;
            return dx * dx + dz * dz <= (long) VIEW * VIEW;
        }
    }

    private final List<Satellite> satellites = new ArrayList<>();

    public static Satellites get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(new Factory<>(Satellites::new, Satellites::load), NAME);
    }

    private static Satellites load(CompoundTag tag, HolderLookup.Provider registries) {
        Satellites orbit = new Satellites();
        for (Tag entry : tag.getList("satellites", Tag.TAG_COMPOUND)) {
            CompoundTag s = (CompoundTag) entry;
            orbit.satellites.add(new Satellite(s.getInt("id"), s.getUUID("owner"), s.getString("name"), s.getInt("x"), s.getInt("z")));
        }
        return orbit;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Satellite satellite : satellites) {
            CompoundTag s = new CompoundTag();
            s.putInt("id", satellite.id());
            s.putUUID("owner", satellite.owner());
            s.putString("name", satellite.name());
            s.putInt("x", satellite.x());
            s.putInt("z", satellite.z());
            list.add(s);
        }
        tag.put("satellites", list);
        return tag;
    }

    public Satellite launch(UUID owner, String name, int x, int z) {
        Satellite satellite = new Satellite(satellites.isEmpty() ? 1 : satellites.getLast().id() + 1, owner, name, x, z);
        satellites.add(satellite);
        setDirty();
        return satellite;
    }

    public List<Satellite> owned(UUID owner) {
        return satellites.stream().filter(s -> s.owner().equals(owner)).toList();
    }

    public Optional<Satellite> byId(int id) {
        return satellites.stream().filter(s -> s.id() == id).findFirst();
    }
}
