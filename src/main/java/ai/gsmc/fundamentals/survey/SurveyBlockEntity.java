package ai.gsmc.fundamentals.survey;

import ai.gsmc.fundamentals.heat.Dial;
import ai.gsmc.fundamentals.heat.ThermometerBlock;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** An instrument that samples once a second, shows it on goggles and a needle, and gives a comparator its place on the scale. */
public abstract class SurveyBlockEntity extends BlockEntity implements IHaveGoggleInformation, Dial {

    public static final int SAMPLE = 20;
    private int cooldown;
    private float dial, prevDial;

    protected SurveyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Takes a reading, storing whatever it shows. */
    protected abstract void read(ServerLevel level);

    /** Where the reading stands on the instrument's scale, 0 to 1. */
    public abstract double fraction();

    protected abstract void describe(List<Component> lines);

    protected abstract void save(CompoundTag tag);

    protected abstract void load(CompoundTag tag);

    public int signal() {
        return (int) Math.round(15 * fraction());
    }

    @Override
    public Direction facing() {
        return getBlockState().getValue(ThermometerBlock.FACING);
    }

    @Override
    public float dial(float partialTick) {
        return prevDial + (dial - prevDial) * partialTick;
    }

    /** One reading: called every {@link #SAMPLE} ticks, and directly by tests. */
    public void sample(ServerLevel level) {
        int before = signal();
        read(level);
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        if (signal() != before) level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, SurveyBlockEntity instrument) {
        if (instrument.cooldown-- > 0 || !(level instanceof ServerLevel server)) return;
        instrument.cooldown = SAMPLE;
        instrument.sample(server);
    }

    static void clientTick(Level level, BlockPos pos, BlockState state, SurveyBlockEntity instrument) {
        instrument.prevDial = instrument.dial;
        instrument.dial += ((float) instrument.fraction() - instrument.dial) * 0.125F;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        tooltip.add(Component.literal("    ").append(getBlockState().getBlock().getName().withStyle(ChatFormatting.GRAY)));
        List<Component> lines = new java.util.ArrayList<>();
        describe(lines);
        lines.forEach(line -> tooltip.add(Component.literal("    ").append(line)));
        return true;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        save(tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        load(tag);
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
