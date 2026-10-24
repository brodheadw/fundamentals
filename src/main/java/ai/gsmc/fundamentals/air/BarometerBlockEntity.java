package ai.gsmc.fundamentals.air;

import ai.gsmc.fundamentals.heat.Dial;
import ai.gsmc.fundamentals.heat.ThermometerBlock;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.wildspell.fundamental.api.air.Pressure;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.List;

/** Reads the air pressure where it hangs and keeps an hour of its own readings, so its trend is measured, not told. */
public class BarometerBlockEntity extends BlockEntity implements IHaveGoggleInformation, Dial {

    public static final int SAMPLE = 100;
    /** Samples that span an in-game hour. */
    public static final int HOUR = Pressure.HOUR / SAMPLE + 1;
    /** Falls faster than this read as weather on its way. */
    public static final double FALLING = 0.3, STORM = 1.5;

    private final ArrayDeque<Double> history = new ArrayDeque<>();
    private double hectopascals = Pressure.SEA_LEVEL;
    private double perHour;
    private int cooldown;
    private float dial, prevDial;

    public BarometerBlockEntity(BlockPos pos, BlockState state) {
        super(Barometers.entity(), pos, state);
    }

    public Barometer kind() {
        return ((BarometerBlock) getBlockState().getBlock()).kind();
    }

    @Override
    public Direction facing() {
        return getBlockState().getValue(ThermometerBlock.FACING);
    }

    @Override
    public float dial(float partialTick) {
        return prevDial + (dial - prevDial) * partialTick;
    }

    public double hectopascals() {
        return hectopascals;
    }

    /** Change per hour over the readings held; zero until two are recorded. */
    public double perHour() {
        return perHour;
    }

    public int signal() {
        return history.isEmpty() ? 0 : kind().signal(hectopascals, perHour);
    }

    /** One reading: called every {@link #SAMPLE} ticks, and directly by tests. */
    public void sample(Level level) {
        int before = signal();
        hectopascals = Pressure.at(level, worldPosition);
        history.addLast(hectopascals);
        while (history.size() > HOUR) history.removeFirst();
        perHour = history.size() < 2 ? 0 : (history.peekLast() - history.peekFirst()) * Pressure.HOUR / (double) ((history.size() - 1) * SAMPLE);
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        if (signal() != before) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, BarometerBlockEntity gauge) {
        if (gauge.cooldown-- > 0) return;
        gauge.cooldown = SAMPLE;
        gauge.sample(level);
    }

    static void clientTick(Level level, BlockPos pos, BlockState state, BarometerBlockEntity gauge) {
        gauge.prevDial = gauge.dial;
        gauge.dial += ((float) Barometer.fraction(gauge.hectopascals) - gauge.dial) * 0.125F;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        String key = "goggles.fundamentals.barometer.";
        tooltip.add(indent(getBlockState().getBlock().getName().withStyle(ChatFormatting.GRAY)));
        tooltip.add(indent(Component.translatable(key + "reading", String.format("%,.1f", hectopascals),
                String.format("%,.0f", hectopascals * 0.750062))));
        String trend = history.size() < 2 ? "settling" : perHour <= -STORM ? "storm" : perHour <= -FALLING ? "falling"
                : perHour >= FALLING ? "rising" : "steady";
        tooltip.add(indent(Component.translatable(key + trend, String.format("%.1f", Math.abs(perHour)))
                .withStyle(trend.equals("storm") ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY)));
        return true;
    }

    private static Component indent(Component text) {
        return Component.literal("    ").append(text);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("hectopascals", hectopascals);
        tag.putDouble("per_hour", perHour);
        tag.putLongArray("history", history.stream().mapToLong(Double::doubleToLongBits).toArray());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        hectopascals = tag.contains("hectopascals") ? tag.getDouble("hectopascals") : Pressure.SEA_LEVEL;
        perHour = tag.getDouble("per_hour");
        history.clear();
        for (long bits : tag.getLongArray("history")) history.addLast(Double.longBitsToDouble(bits));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
