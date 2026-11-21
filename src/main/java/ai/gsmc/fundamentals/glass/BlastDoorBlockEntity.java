package ai.gsmc.fundamentals.glass;

import com.drmangotea.tfmg.content.electricity.base.ElectricBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

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
            // a warning horn sounds while the leaves run
            if (moving() && progress % 20 == 0) level.playSound(null, worldPosition.above(2), SoundEvents.NOTE_BLOCK_DIDGERIDOO.value(), SoundSource.BLOCKS, 1.5F, 0.6F);
            if (!opening && progress > 0) clear();
        }
        int before = progress;
        progress = Math.clamp(progress + (opening ? 1 : -1), 0, TRAVEL);
        if (!level.isClientSide && before != progress && (progress == 0 || progress == TRAVEL)) {
            level.playSound(null, worldPosition.above(), SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.8F, 0.4F);
        }
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

    /** Shuts no one in: whoever the closing leaves would take is put out of the doorway on the side they are nearer. */
    private void clear() {
        Direction facing = getBlockState().getValue(BlastDoorBlock.FACING);
        AABB doorway = new AABB(BlastDoorBlock.partPos(worldPosition, facing, 0)).minmax(new AABB(BlastDoorBlock.partPos(worldPosition, facing, 8)));
        float open = open(1);
        for (Entity entity : level.getEntities((Entity) null, doorway, entity -> !entity.isSpectator() && (entity.isPushable() || entity instanceof Player))) {
            boolean caught = false;
            for (int part = 0; part < 9 && !caught; part++) {
                BlockPos at = BlastDoorBlock.partPos(worldPosition, facing, part);
                for (AABB box : BlastDoorBlock.shape(facing, part, open).toAabbs()) caught |= box.move(at).intersects(entity.getBoundingBox());
            }
            if (!caught) continue;
            Direction axis = facing;
            double plane = Vec3.atCenterOf(worldPosition).get(axis.getAxis()), along = entity.position().get(axis.getAxis()) - plane;
            Direction out = along * axis.getAxisDirection().getStep() >= 0 ? axis : axis.getOpposite();
            double clear = 0.5 + entity.getBbWidth() / 2 + 0.05;
            Vec3 to = entity.position().with(axis.getAxis(), plane + out.getAxisDirection().getStep() * clear);
            entity.teleportTo(to.x, to.y, to.z);
        }
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
