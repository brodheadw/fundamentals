package ai.gsmc.fundamentals.survey;

import com.wildspell.fundamental.api.air.Airflow;
import com.wildspell.fundamental.api.air.Pressure;
import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;
import java.util.UUID;

/**
 * A sounding balloon in flight. It climbs at a steady five metres a second and drifts with the wind; every sixteen blocks of
 * climb its radiosonde sends back the height, temperature, pressure and wind, until it bursts at the top of the sky or snags.
 */
public class WeatherBalloon extends Entity implements ItemSupplier {

    /** Five metres a second: what a sounding balloon is filled to climb at. */
    static final double ASCENT = 0.25;
    static final int REPORT = 16;
    /** Twenty minutes: a balloon lost from view is let go. */
    static final int LIFE = 24000;

    private UUID owner;
    private double launchY;
    private int reports;
    private double hectopascals;

    public WeatherBalloon(EntityType<? extends WeatherBalloon> type, Level level) {
        super(type, level);
        noPhysics = true;
    }

    public WeatherBalloon(Level level, double x, double y, double z, UUID owner) {
        this(Survey.weatherBalloonEntity(), level);
        setPos(x, y, z);
        this.owner = owner;
        launchY = y;
    }

    public int reports() { return reports; }
    public double hectopascals() { return hectopascals; }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel server) {
            Vec3 wind = Airflow.ambient(server, position());
            setDeltaMovement(wind.x, ASCENT, wind.z);
            BlockPos above = BlockPos.containing(getX(), getY() + getBbHeight() + ASCENT, getZ());
            if (!server.getBlockState(above).getCollisionShape(server, above).isEmpty()) {
                burst("snagged");
                return;
            }
            if (getY() >= server.getMaxBuildHeight() || tickCount > LIFE) {
                burst("burst");
                return;
            }
            if (getY() - launchY >= REPORT * reports) report();
        }
        setPos(position().add(getDeltaMovement()));
    }

    /** One line of the sounding, radioed to whoever let it go. */
    void report() {
        if (!(level() instanceof ServerLevel server)) return;
        reports++;
        BlockPos at = blockPosition();
        Vec3 wind = Airflow.ambient(server, position());
        double speed = wind.horizontalDistance() * 20;
        hectopascals = Pressure.at(server, at);
        tell(Component.translatable("message.fundamentals.weather_balloon.sounding", at.getY(),
                String.format(Locale.ROOT, "%.1f", Heat.ambient(server, at)), String.format(Locale.ROOT, "%,.1f", hectopascals),
                String.format(Locale.ROOT, "%.1f", speed), speed < 0.05 ? "-" : Survey.bearing(-wind.x, -wind.z)).withStyle(ChatFormatting.AQUA));
    }

    private void burst(String why) {
        ServerLevel server = (ServerLevel) level();
        server.sendParticles(ParticleTypes.POOF, getX(), getY() + getBbHeight() / 2, getZ(), 12, 0.4, 0.4, 0.4, 0.02);
        server.playSound(null, getX(), getY(), getZ(), SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, SoundSource.NEUTRAL, 3.0F, 0.5F);
        tell(Component.translatable("message.fundamentals.weather_balloon." + why, blockPosition().getY()).withStyle(ChatFormatting.GRAY));
        discard();
    }

    private void tell(Component message) {
        ServerPlayer player = owner == null || getServer() == null ? null : getServer().getPlayerList().getPlayer(owner);
        if (player != null) player.sendSystemMessage(message);
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(Survey.weatherBalloon());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("owner")) owner = tag.getUUID("owner");
        launchY = tag.getDouble("launch_y");
        reports = tag.getInt("reports");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (owner != null) tag.putUUID("owner", owner);
        tag.putDouble("launch_y", launchY);
        tag.putInt("reports", reports);
    }
}
