package ai.gsmc.fundamentals.bombs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Ground a fission bomb has dusted with its products, and how long it stays hot. Fallout's activity falls off fast, so the dose
 * is heaviest in the first day and lightest in the third. With Create: Nuclear it gives that mod's radiation, which its
 * anti-radiation suit keeps out; without it, the sickness alone.
 */
public final class Fallout extends SavedData {

    public static final long TICKS = 72000L, DIRTY_TICKS = 24000L;
    private static final String NAME = "fundamentals_fallout";
    private static final ResourceLocation RADIATION = ResourceLocation.fromNamespaceAndPath("createnuclear", "radiation");

    record Zone(BlockPos centre, int radius, long from, long until) {}

    private final List<Zone> zones = new ArrayList<>();

    public static Fallout get(ServerLevel level) {
        Fallout fallout = level.getDataStorage().computeIfAbsent(new Factory<>(Fallout::new, Fallout::load), NAME);
        fallout.now = level.getGameTime();
        return fallout;
    }

    private static Fallout load(CompoundTag tag, HolderLookup.Provider registries) {
        Fallout fallout = new Fallout();
        for (Tag entry : tag.getList("zones", Tag.TAG_COMPOUND)) {
            CompoundTag zone = (CompoundTag) entry;
            fallout.zones.add(new Zone(BlockPos.of(zone.getLong("centre")), zone.getInt("radius"), zone.getLong("from"), zone.getLong("until")));
        }
        return fallout;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        for (Zone zone : zones) {
            CompoundTag entry = new CompoundTag();
            entry.putLong("centre", zone.centre().asLong());
            entry.putInt("radius", zone.radius());
            entry.putLong("from", zone.from());
            entry.putLong("until", zone.until());
            list.add(entry);
        }
        tag.put("zones", list);
        return tag;
    }

    private long now;

    public void add(BlockPos centre, int radius, long ticks) {
        zones.add(new Zone(centre.immutable(), radius, now, now + ticks));
        setDirty();
    }

    public boolean hot(BlockPos pos) {
        return zones.stream().anyMatch(zone -> zone.until() > now && zone.centre().distSqr(pos) <= (double) zone.radius() * zone.radius());
    }

    /** How much of its activity the hottest fallout here has left, 1 when fresh to 0 when gone. */
    public double activity(BlockPos pos) {
        return zones.stream().filter(zone -> zone.until() > now && zone.centre().distSqr(pos) <= (double) zone.radius() * zone.radius())
                .mapToDouble(zone -> (double) (zone.until() - now) / (zone.until() - zone.from())).max().orElse(0);
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Fallout fallout = get(level);
        if (fallout.zones.isEmpty() || fallout.now % 20 != 0) return;
        if (fallout.zones.removeIf(zone -> zone.until() <= fallout.now)) fallout.setDirty();
        Optional<net.minecraft.core.Holder.Reference<MobEffect>> radiation = BuiltInRegistries.MOB_EFFECT.getHolder(RADIATION);
        for (Zone zone : fallout.zones) {
            double left = (double) (zone.until() - fallout.now) / (zone.until() - zone.from());
            int strength = left > 2 / 3.0 ? 2 : left > 1 / 3.0 ? 1 : 0;
            double r2 = (double) zone.radius() * zone.radius();
            for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(zone.centre()).inflate(zone.radius()),
                    entity -> entity.blockPosition().distSqr(zone.centre()) <= r2)) {
                if (radiation.isPresent()) {
                    entity.addEffect(new MobEffectInstance(radiation.get(), 200, strength));
                } else {
                    entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, strength));
                    entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, strength));
                    if (strength > 0) entity.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                }
            }
        }
    }
}
