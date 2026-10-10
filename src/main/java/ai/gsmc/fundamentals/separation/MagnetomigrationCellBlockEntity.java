package ai.gsmc.fundamentals.separation;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;

/**
 * One cell of a magnetomigration line. The head cell holds the feed, piped into its back; the tail holds the two streams
 * the line parts it into, drained from its sides: the right, where the magnet is, gives what the magnets draw, the left
 * the rest. The head runs the line, and reads its temperature every {@link #SAMPLE} ticks: the colder the liquor, the more
 * magnetic its ions and the sooner a batch is parted.
 */
public class MagnetomigrationCellBlockEntity extends BlockEntity implements IHaveGoggleInformation {

    public static final int CAPACITY = 1000;
    public static final int BATCH = 50;
    public static final int PERIOD = 100;
    public static final int SAMPLE = 40;

    final Tank feed = new Tank(stack -> Separation.kind(stack.getFluid()) == Reagents.Kind.LIQUOR);
    final Tank near = new Tank(stack -> true);
    final Tank far = new Tank(stack -> true);
    int cooldown;
    private double celsius = Double.NaN;
    private boolean dirty;

    public MagnetomigrationCellBlockEntity(BlockPos pos, BlockState state) {
        super(Separation.magnetomigrationCellEntity(), pos, state);
    }

    class Tank extends FluidTank {
        Tank(Predicate<FluidStack> valid) {
            super(CAPACITY, valid);
        }

        @Override
        protected void onContentsChanged() {
            setChanged();
            dirty = true;
        }
    }

    public Direction facing() {
        return getBlockState().getValue(MagnetomigrationCellBlock.FACING);
    }

    /** The same-facing cell ahead of this one, or behind it. */
    @Nullable
    MagnetomigrationCellBlockEntity next(boolean ahead) {
        BlockPos at = worldPosition.relative(facing(), ahead ? 1 : -1);
        return level.getBlockEntity(at) instanceof MagnetomigrationCellBlockEntity cell && cell.facing() == facing() ? cell : null;
    }

    public MagnetomigrationLine line() {
        return MagnetomigrationLine.of(this);
    }

    /** The feed into the back of the head cell, and the two streams out of the tail's sides: magnet side right, the rest left. */
    @Nullable
    public IFluidHandler handler(@Nullable Direction side) {
        if (side == null) {
            return null;
        }
        if (side == facing().getOpposite() && next(false) == null) {
            return feed;
        }
        if (next(true) == null) {
            if (side == facing().getClockWise()) {
                return new Port(near, null);
            }
            if (side == facing().getCounterClockWise()) {
                return new Port(far, null);
            }
        }
        return null;
    }

    /** The liquor's temperature, °C, as the head last read it; a cell not yet read is taken at 20. */
    public double celsius() {
        return Double.isNaN(celsius) ? Paramagnetism.ROOM_KELVIN - 273.15 : celsius;
    }

    static void serverTick(Level level, BlockPos pos, BlockState state, MagnetomigrationCellBlockEntity cell) {
        if (cell.next(false) == null) {
            if (Double.isNaN(cell.celsius) || Math.floorMod(level.getGameTime() + pos.asLong(), SAMPLE) == 0) {
                double now = Heat.at(level, pos);
                if (Double.isNaN(cell.celsius) || Math.abs(now - cell.celsius) >= 0.5) {
                    cell.celsius = now;
                    cell.setChanged();
                    cell.dirty = true;
                }
            }
            MagnetomigrationLine line = cell.line();
            if (line.stall().isEmpty()) {
                if (++cell.cooldown >= line.period()) {
                    cell.cooldown = 0;
                    line.run();
                }
            } else {
                cell.cooldown = 0;
            }
        }
        if (cell.dirty && level.getGameTime() % 10 == 0) {
            cell.dirty = false;
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        MagnetomigrationLine line = line();
        MagnetomigrationCellBlockEntity head = line.head(), tail = line.tail();
        tooltip.add(indent(Component.translatable("goggles.fundamentals.magnetomigration_cell.line", line.size(), BATCH)));
        Optional<MagnetomigrationLine.Stall> stall = line.stall();
        Optional<MagneticRecipe> cut = MagneticRecipe.forLiquor(level, head.feed.getFluid().getFluid());
        if (stall.isEmpty()) {
            MagneticRecipe c = cut.orElseThrow();
            tooltip.add(indent(Component.translatable("goggles.fundamentals.magnetomigration_cell.ready", head.feed.getFluid().getHoverName(),
                    c.of(c.attracted(), BATCH), Battery.name(c.attracted()), c.of(c.repelled(), BATCH), Battery.name(c.repelled()))));
        } else {
            tooltip.add(indent(Component.translatable("goggles.fundamentals.magnetomigration_cell." + stall.get().key(), stall.get().args())));
        }
        cut.ifPresent(c -> tooltip.add(indent(Component.translatable("goggles.fundamentals.magnetomigration_cell.passes", c.passes(), line.size()))));
        cut.ifPresent(c -> tooltip.add(indent(Component.translatable("goggles.fundamentals.magnetomigration_cell.heat", String.format("%.0f", head.celsius()),
                String.format("%.0f", 100 * c.contrast(head.celsius())), String.format("%.1f", line.period() / 20.0)))));
        tooltip.add(indent(Component.translatable("goggles.fundamentals.magnetomigration_cell.feed", held(head.feed))));
        tooltip.add(indent(Component.translatable("goggles.fundamentals.magnetomigration_cell.outlets", held(tail.near), held(tail.far))));
        return true;
    }

    private static Component held(FluidTank tank) {
        FluidStack stack = tank.getFluid();
        if (stack.isEmpty()) {
            return Component.translatable("goggles.fundamentals.mixer_settler.nothing");
        }
        return Component.literal(String.format("%,d mB ", stack.getAmount())).append(stack.getHoverName())
                .withStyle(Style.EMPTY.withColor(Separation.tint(stack.getFluid())));
    }

    private static Component indent(Component text) {
        return Component.literal("    ").append(text);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("feed", feed.getFluid().saveOptional(registries));
        tag.put("near", near.getFluid().saveOptional(registries));
        tag.put("far", far.getFluid().saveOptional(registries));
        tag.putInt("cooldown", cooldown);
        if (!Double.isNaN(celsius)) {
            tag.putDouble("celsius", celsius);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        feed.setFluid(FluidStack.parseOptional(registries, tag.getCompound("feed")));
        near.setFluid(FluidStack.parseOptional(registries, tag.getCompound("near")));
        far.setFluid(FluidStack.parseOptional(registries, tag.getCompound("far")));
        cooldown = tag.getInt("cooldown");
        celsius = tag.contains("celsius") ? tag.getDouble("celsius") : Double.NaN;
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
