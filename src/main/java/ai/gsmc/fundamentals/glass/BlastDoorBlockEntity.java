package ai.gsmc.fundamentals.glass;

import com.drmangotea.tfmg.content.electricity.base.ElectricBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;

/** The door's drive: open while the grid feeds it, drawing a motor's load as the leaves move; shut by its accumulator when the grid is cut. */
public class BlastDoorBlockEntity extends ElectricBlockEntity implements com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation {

    /** Ticks for the leaves to travel: three seconds each way. */
    public static final int TRAVEL = 60;
    public static final int MIN_VOLTS = 50;
    /** Ohms: a motor's load (about 600 W at 120 V) while the leaves run, a latch's hold once they are open. */
    public static final float RUNNING = 24, HOLDING = 480;

    private int progress, lastProgress;
    private boolean opening;

    public BlastDoorBlockEntity(BlockPos pos, BlockState state) {
        super(Glass.blastDoorEntity(), pos, state);
    }

    public boolean live() {
        return canWork() && getData().getVoltage() >= MIN_VOLTS;
    }

    public boolean moving() {
        return opening ? progress < TRAVEL : progress > 0;
    }

    public float open(float partialTicks) {
        return (lastProgress + (progress - lastProgress) * partialTicks) / TRAVEL;
    }

    public boolean opening() {
        return opening;
    }

    @Override
    public float resistance() {
        return opening && progress < TRAVEL ? RUNNING : HOLDING;
    }

    @Override
    public void tick() {
        super.tick();
        boolean wasRunning = opening && progress < TRAVEL;
        lastProgress = progress;
        if (!level.isClientSide) {
            boolean want = live();
            if (want != opening) {
                opening = want;
                if (!opening) setOpen(false);
                level.playSound(null, worldPosition, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 1.0F, 0.5F);
                sendData();
            }
        }
        progress = Math.clamp(progress + (opening ? 1 : -1), 0, TRAVEL);
        if (!level.isClientSide && opening && progress == TRAVEL && !getBlockState().getValue(BlastDoorBlock.OPEN)) setOpen(true);
        if (wasRunning != (opening && progress < TRAVEL)) updateNextTick();
    }

    @Override
    public boolean addToGoggleTooltip(java.util.List<net.minecraft.network.chat.Component> tooltip, boolean isPlayerSneaking) {
        int volts = getData().getVoltage();
        ai.gsmc.fundamentals.goggles.Readout.status(tooltip, getBlockState().getBlock().getName(), moving() ? (opening ? "opening" : "closing") : progress == TRAVEL ? "open" : "shut");
        ai.gsmc.fundamentals.goggles.Readout.bar(tooltip, "voltage", volts, 2 * MIN_VOLTS, volts >= MIN_VOLTS, volts + " V");
        ai.gsmc.fundamentals.goggles.Readout.bar(tooltip, "travel", progress, TRAVEL, true, 100 * progress / TRAVEL + "% open");
        if (!live()) ai.gsmc.fundamentals.goggles.Readout.needs(tooltip, "volts_door", MIN_VOLTS);
        return true;
    }

    /** Opens or shuts the nine parts' collision together. */
    private void setOpen(boolean open) {
        BlockState state = getBlockState();
        for (int part = 0; part < 9; part++) {
            BlockPos at = BlastDoorBlock.partPos(worldPosition, state.getValue(BlastDoorBlock.FACING), part);
            BlockState there = level.getBlockState(at);
            if (there.is(Glass.blastDoor()) && there.getValue(BlastDoorBlock.OPEN) != open) {
                level.setBlock(at, there.setValue(BlastDoorBlock.OPEN, open), 3);
            }
        }
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.putInt("Progress", progress);
        tag.putBoolean("Opening", opening);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        progress = tag.getInt("Progress");
        opening = tag.getBoolean("Opening");
        if (!clientPacket) lastProgress = progress;
    }
}
