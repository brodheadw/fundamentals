package ai.gsmc.fundamentals.bombs;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A fission bomb going off, spread over ticks so the server keeps its feet: the flash and the blast at once, then the fireball
 * hollows its crater a slice at a time and fuses the sand of its floor to trinitite, the thermal pulse scorches a ring three
 * times wider, the cloud climbs for fifteen seconds, and what it lifted comes down as fallout over three days.
 */
public final class Detonation {

    /** Blocks the fireball takes in one tick. */
    static final int PER_TICK = 12000;
    static final int CLOUD_TICKS = 300;
    private static final int FLAGS = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
    // shortcut: a blast still carving when the server stops is not resumed; save it with Fallout if that matters
    private static final List<Blast> ACTIVE = new ArrayList<>();

    private Detonation() {}

    static final class Blast {
        final ServerLevel level;
        final BlockPos centre;
        final int radius, depth;
        int carved, scorched, age;

        Blast(ServerLevel level, BlockPos centre, int radius) {
            this.level = level;
            this.centre = centre;
            this.radius = radius;
            this.depth = radius / 2;
        }

        int width() { return 2 * radius + 1; }
        int height() { return radius + depth + 1; }
        int volume() { return width() * width() * height(); }
        int scorchWidth() { return 2 * scorch() + 1; }
        int scorch() { return radius * 3; }
        boolean done() { return carved >= volume() && scorched >= scorchWidth() * scorchWidth() && age >= CLOUD_TICKS; }
    }

    public static void start(ServerLevel level, BlockPos centre, int radius) {
        Blast blast = new Blast(level, centre, radius);
        flashAndBlast(blast);
        Fallout.get(level).add(centre, radius * 3, Fallout.TICKS);
        ai.gsmc.fundamentals.survey.Disturbances.nuclear(level, centre, radius);
        ACTIVE.add(blast);
    }

    public static boolean active() {
        return !ACTIVE.isEmpty();
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        for (Iterator<Blast> it = ACTIVE.iterator(); it.hasNext(); ) {
            Blast blast = it.next();
            step(blast);
            if (blast.done()) it.remove();
        }
    }

    static void step(Blast blast) {
        if (blast.carved < blast.volume()) carve(blast);
        else if (blast.scorched < blast.scorchWidth() * blast.scorchWidth()) scorch(blast);
        cloud(blast);
        blast.age++;
    }

    private static void flashAndBlast(Blast blast) {
        ServerLevel level = blast.level;
        Vec3 at = Vec3.atCenterOf(blast.centre);
        int r = blast.radius;
        level.playSound(null, blast.centre, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 48F, 0.4F);
        level.playSound(null, blast.centre, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 48F, 0.5F);
        level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 40, r / 3.0, r / 3.0, r / 3.0, 0);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 30, r / 3.0, r / 4.0, r / 3.0, 0);
        for (ServerPlayer player : level.players()) {
            double d = player.position().distanceTo(at);
            if (d < r * 10 && seen(level, at, player.getEyePosition())) {
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, d < r * 4 ? 200 : 80));
            }
        }
        for (LivingEntity entity : level.getEntitiesOfClass(LivingEntity.class, new AABB(blast.centre).inflate(r * 4))) {
            double d = entity.position().distanceTo(at);
            boolean exposed = seen(level, at, entity.getEyePosition());
            if (d < r) {
                entity.hurt(level.damageSources().explosion(null, null), 10000F);
                continue;
            }
            double falloff = 1 - (d - r) / (r * 3.0);
            if (falloff <= 0) continue;
            entity.hurt(level.damageSources().explosion(null, null), (float) (80 * falloff * (exposed ? 1 : 0.4)));
            if (exposed) entity.igniteForSeconds((float) (20 * falloff));
            Vec3 away = entity.position().subtract(at).normalize().scale(3 * falloff);
            entity.push(away.x, 0.6 * falloff, away.z);
            entity.hurtMarked = true;
        }
    }

    static boolean seen(ServerLevel level, Vec3 from, Vec3 to) {
        return level.clip(new ClipContext(from, to, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()))
                .getType() == HitResult.Type.MISS;
    }

    /** Inside an ellipsoid as wide as the fireball and half as deep below, its rim roughened. */
    static boolean inFireball(Blast blast, int dx, int dy, int dz) {
        double rim = blast.radius + (Mth.murmurHash3Mixer(dx * 31 + dz * 17) & 3) * 0.5 - 0.75;
        double vertical = dy < 0 ? blast.depth * rim / blast.radius : rim;
        return (dx * dx + dz * dz) / (rim * rim) + (double) (dy * dy) / (vertical * vertical) <= 1.0;
    }

    private static void carve(Blast blast) {
        ServerLevel level = blast.level;
        int w = blast.width(), r = blast.radius;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int end = Math.min(blast.volume(), blast.carved + PER_TICK);
        for (int i = blast.carved; i < end; i++) {
            int dx = i % w - r, dz = (i / w) % w - r, dy = i / (w * w) - blast.depth;
            if (!inFireball(blast, dx, dy, dz)) continue;
            pos.setWithOffset(blast.centre, dx, dy, dz);
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (state.isAir() || state.getDestroySpeed(level, pos) < 0) continue;
            // vaporised: a chest's contents go with it rather than spilling across the crater
            if (level.getBlockEntity(pos) instanceof Container container) container.clearContent();
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
        }
        blast.carved = end;
        if (end >= blast.volume()) glass(blast);
    }

    /** The crater's floor: sand fused green, the rest glowing here and there. */
    private static void glass(Blast blast) {
        ServerLevel level = blast.level;
        int r = blast.radius;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                if (dx * dx + dz * dz > r * r) continue;
                pos.setWithOffset(blast.centre, dx, 0, dz);
                if (!level.isLoaded(pos)) continue;
                for (int down = 0; down <= blast.depth + 3 && level.getBlockState(pos).isAir(); down++) pos.move(0, -1, 0);
                BlockState floor = level.getBlockState(pos);
                if (floor.isAir() || floor.getDestroySpeed(level, pos) < 0) continue;
                if (floor.is(BlockTags.SAND) || floor.is(Blocks.SANDSTONE) || floor.is(Blocks.RED_SANDSTONE)) {
                    level.setBlock(pos, Blocks.GREEN_STAINED_GLASS.defaultBlockState(), FLAGS);
                } else if (level.random.nextInt(8) == 0) {
                    level.setBlock(pos, Blocks.MAGMA_BLOCK.defaultBlockState(), FLAGS);
                }
            }
        }
    }

    /** The thermal pulse beyond the fireball: leaves and glass gone, grass seared, fires set. */
    private static void scorch(Blast blast) {
        ServerLevel level = blast.level;
        int w = blast.scorchWidth(), s = blast.scorch(), r = blast.radius;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int end = Math.min(w * w, blast.scorched + PER_TICK / 3);
        for (int i = blast.scorched; i < end; i++) {
            int dx = i % w - s, dz = i / w - s, d2 = dx * dx + dz * dz;
            if (d2 > s * s || d2 <= r * r) continue;
            pos.set(blast.centre.getX() + dx, 0, blast.centre.getZ() + dz);
            if (!level.isLoaded(pos)) continue;
            pos.setY(level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ()) - 1);
            BlockState top = level.getBlockState(pos);
            double near = 1 - Math.sqrt(d2) / s;
            if (top.is(BlockTags.LEAVES) || top.is(Blocks.SNOW) || top.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_BLOCKS)
                    || top.is(net.neoforged.neoforge.common.Tags.Blocks.GLASS_PANES)) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), FLAGS);
                pos.move(0, -1, 0);
                top = level.getBlockState(pos);
            }
            if (top.is(Blocks.GRASS_BLOCK) || top.is(Blocks.PODZOL) || top.is(Blocks.MYCELIUM)) {
                level.setBlock(pos, Blocks.COARSE_DIRT.defaultBlockState(), FLAGS);
            }
            BlockPos above = pos.above();
            if (level.random.nextDouble() < 0.25 * near && level.getBlockState(above).canBeReplaced()) {
                level.setBlock(above, Blocks.FIRE.defaultBlockState(), Block.UPDATE_ALL);
            }
        }
        blast.scorched = end;
    }

    /** The column rises and spreads its cap, a little more each tick, for every player near enough to see it. */
    private static void cloud(Blast blast) {
        if (blast.age >= CLOUD_TICKS || blast.age % 2 != 0) return;
        ServerLevel level = blast.level;
        int r = blast.radius;
        double x = blast.centre.getX() + 0.5, y = blast.centre.getY(), z = blast.centre.getZ() + 0.5;
        double rise = Math.min(1.0, blast.age / 120.0), top = y + r * 4 * rise;
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(x, y, z) > 768 * 768) continue;
            send(level, player, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, y + (top - y) / 2, z, 12, r / 5.0, (top - y) / 3, r / 5.0);
            send(level, player, ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, x, top, z, 30, r * 1.2 * rise, r / 3.0, r * 1.2 * rise);
            if (blast.age < 100) send(level, player, ParticleTypes.FLAME, x, y + 2, z, 20, r / 2.0, r / 4.0, r / 2.0);
            if (blast.age < 60) send(level, player, ParticleTypes.LAVA, x, top, z, 10, r / 2.0, r / 3.0, r / 2.0);
        }
    }

    private static void send(ServerLevel level, ServerPlayer player, ParticleOptions particle, double x, double y, double z, int count, double dx, double dy, double dz) {
        level.sendParticles(player, particle, true, x, y, z, count, dx, dy, dz, 0.01);
    }
}
