package ai.gsmc.fundamentals.air;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.heat.Dial;
import ai.gsmc.fundamentals.heat.ThermometerBlock;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.wildspell.fundamental.api.air.AirField;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Reads the air's electric field where it stands. Reading a dust storm's field, with no thunder overhead, is the finding that the sand is charged. */
public class FieldMillBlockEntity extends BlockEntity implements IHaveGoggleInformation, Dial {

    public static final int SAMPLE = 20;
    /** Players this close see the reading, and share the finding. */
    private static final double WITNESS = 16;

    private double voltsPerMetre;
    private int cooldown;
    private float dial, prevDial;

    public FieldMillBlockEntity(BlockPos pos, BlockState state) {
        super(FieldMill.entity(), pos, state);
    }

    @Override
    public Direction facing() {
        return getBlockState().getValue(ThermometerBlock.FACING);
    }

    @Override
    public float dial(float partialTick) {
        return prevDial + (dial - prevDial) * partialTick;
    }

    public double voltsPerMetre() {
        return voltsPerMetre;
    }

    public int signal() {
        return FieldMill.signal(voltsPerMetre);
    }

    /** One reading: called every {@link #SAMPLE} ticks, and directly by tests. */
    public void sample(Level level) {
        int before = signal();
        voltsPerMetre = AirField.at(level, worldPosition);
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        if (signal() != before) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        if (voltsPerMetre >= FieldMill.DUST_STORM && !level.isThundering() && level instanceof ServerLevel server) {
            var finding = server.getServer().getAdvancements().get(Fundamentals.id("triboelectric"));
            if (finding != null) {
                for (ServerPlayer player : server.players()) {
                    if (player.blockPosition().closerThan(worldPosition, WITNESS)) player.getAdvancements().award(finding, "reading");
                }
            }
        }
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, FieldMillBlockEntity mill) {
        if (mill.cooldown-- > 0) return;
        mill.cooldown = SAMPLE;
        mill.sample(level);
    }

    static void clientTick(Level level, BlockPos pos, BlockState state, FieldMillBlockEntity mill) {
        mill.prevDial = mill.dial;
        mill.dial += ((float) FieldMill.fraction(mill.voltsPerMetre) - mill.dial) * 0.125F;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        String key = "goggles.fundamentals.field_mill.";
        tooltip.add(indent(getBlockState().getBlock().getName().withStyle(ChatFormatting.GRAY)));
        tooltip.add(indent(voltsPerMetre >= 1000 ? Component.translatable(key + "kilovolts", String.format("%,.1f", voltsPerMetre / 1000))
                : Component.translatable(key + "volts", String.format("%,.0f", voltsPerMetre))));
        String reading = voltsPerMetre >= FieldMill.DUST_STORM ? "storm" : voltsPerMetre > AirField.RAIN ? "charged"
                : voltsPerMetre > 0 ? "fair" : "screened";
        tooltip.add(indent(Component.translatable(key + reading).withStyle(reading.equals("storm") ? ChatFormatting.GOLD : ChatFormatting.DARK_GRAY)));
        return true;
    }

    private static Component indent(Component text) {
        return Component.literal("    ").append(text);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putDouble("volts_per_metre", voltsPerMetre);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        voltsPerMetre = tag.getDouble("volts_per_metre");
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
