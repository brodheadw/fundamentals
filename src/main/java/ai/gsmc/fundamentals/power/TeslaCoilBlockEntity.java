package ai.gsmc.fundamentals.power;

import com.drmangotea.tfmg.content.electricity.base.ElectricBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The coil draws from the grid while it charges its top-load, a light load once it is full, and empties it into an enemy
 * as an arc. A full charge is a second of a kilowatt at the grid's voltage; below 100 V the gap will not break down.
 */
public class TeslaCoilBlockEntity extends ElectricBlockEntity implements com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation {
    public static final int MIN_VOLTS = 100, CHARGE_TICKS = 20;
    public static final double REACH = 8;
    public static final float DAMAGE = 6F;
    /** Ohms: charging (about a kilowatt at 120 V), and the trickle of a full coil holding its charge. */
    public static final float CHARGING = 14, HOLDING = 1200;

    private int charge;

    public TeslaCoilBlockEntity(BlockPos pos, BlockState state) {
        super(Electricity.teslaCoilEntity(), pos, state);
    }

    public boolean live() {
        return canWork() && getData().getVoltage() >= MIN_VOLTS;
    }

    public boolean charged() {
        return charge >= CHARGE_TICKS;
    }

    public void fill() {
        charge = CHARGE_TICKS;
    }

    @Override
    public float resistance() {
        return charged() ? HOLDING : CHARGING;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level instanceof ServerLevel server)) return;
        boolean wasCharged = charged();
        if (live() && !charged()) charge++;
        if (charged()) arc(server);
        if (wasCharged != charged()) {
            updateNextTick();
            sendData();
        }
    }

    @Override
    public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip, boolean isPlayerSneaking) {
        int volts = getData().getVoltage();
        ai.gsmc.fundamentals.goggles.Readout.status(tooltip, getBlockState().getBlock().getName(), !live() ? "unpowered" : charged() ? "charged" : "charging");
        ai.gsmc.fundamentals.goggles.Readout.bar(tooltip, "voltage", volts, 2 * MIN_VOLTS, volts >= MIN_VOLTS, volts + " V");
        ai.gsmc.fundamentals.goggles.Readout.bar(tooltip, "charge", charge, CHARGE_TICKS, charged(), 100 * charge / CHARGE_TICKS + "%");
        if (!live()) ai.gsmc.fundamentals.goggles.Readout.needs(tooltip, "volts", MIN_VOLTS);
        else if (charged()) ai.gsmc.fundamentals.goggles.Readout.needs(tooltip, "enemy", (int) REACH);
        return true;
    }

    /** Empties a full charge into the nearest enemy in reach; true if it struck. */
    public boolean arc(ServerLevel server) {
        if (!charged()) return false;
        Vec3 top = Vec3.atBottomCenterOf(worldPosition).add(0, 1.0, 0);
        LivingEntity target = server.getNearestEntity(server.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition).inflate(REACH), e -> e instanceof Enemy && e.isAlive()),
                TargetingConditions.forNonCombat().ignoreLineOfSight(), null, top.x, top.y, top.z);
        if (target == null) return false;
        charge = 0;
        target.hurt(server.damageSources().lightningBolt(), DAMAGE);
        Vec3 to = target.position().add(0, target.getBbHeight() / 2, 0);
        for (int i = 0; i <= 12; i++) {
            Vec3 at = top.lerp(to, i / 12.0).add(server.random.nextGaussian() * 0.12, server.random.nextGaussian() * 0.12, server.random.nextGaussian() * 0.12);
            server.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 2, 0, 0, 0, 0.0);
        }
        server.playSound(null, worldPosition, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.BLOCKS, 0.25F, 2.0F);
        return true;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Charge", charge);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        charge = tag.getInt("Charge");
    }
}
