package ai.gsmc.fundamentals.separation;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Magnetomigration cells standing end to end, from the head forward. Each cell is a pass: the liquor runs along the line
 * and its paramagnetic ions drift a little further into the stream along the magnets at every one. A line at least as
 * long as its cut's passes parts each batch into the cut's two products, in the proportion the battery would. What each cell
 * draws aside goes with the products' difference in susceptibility, so a warm line holds each batch as much longer as its
 * temperature weakens that.
 */
public record MagnetomigrationLine(List<MagnetomigrationCellBlockEntity> cells) {

    /** Why the line is not parting: a lang key under goggles.fundamentals.magnetomigration_cell and its arguments. */
    public record Stall(String key, Object... args) {}

    public static MagnetomigrationLine of(MagnetomigrationCellBlockEntity cell) {
        MagnetomigrationCellBlockEntity head = cell;
        for (MagnetomigrationCellBlockEntity back; (back = head.next(false)) != null; ) {
            head = back;
        }
        List<MagnetomigrationCellBlockEntity> cells = new ArrayList<>();
        for (MagnetomigrationCellBlockEntity at = head; at != null; at = at.next(true)) {
            cells.add(at);
        }
        return new MagnetomigrationLine(cells);
    }

    public MagnetomigrationCellBlockEntity head() { return cells.getFirst(); }
    public MagnetomigrationCellBlockEntity tail() { return cells.getLast(); }
    public int size() { return cells.size(); }

    /** Ticks a batch takes at the head's temperature. */
    public int period() {
        double contrast = MagneticRecipe.forLiquor(head().getLevel(), head().feed.getFluid().getFluid()).map(cut -> cut.contrast(head().celsius())).orElse(1.0);
        return (int) Math.round(MagnetomigrationCellBlockEntity.PERIOD / contrast);
    }

    public Optional<Stall> stall() {
        FluidStack feed = head().feed.getFluid();
        int batch = MagnetomigrationCellBlockEntity.BATCH;
        if (feed.getAmount() < batch) {
            return Optional.of(new Stall("idle"));
        }
        Optional<MagneticRecipe> found = MagneticRecipe.forLiquor(head().getLevel(), feed.getFluid());
        if (found.isEmpty()) {
            return Optional.of(new Stall("no_cut", feed.getHoverName()));
        }
        MagneticRecipe cut = found.get();
        if (size() < cut.passes()) {
            return Optional.of(new Stall("short", feed.getHoverName(), cut.passes(), size()));
        }
        int near = cut.of(cut.attracted(), batch), far = cut.of(cut.repelled(), batch);
        if (tail().near.fill(new FluidStack(cut.attracted(), near), IFluidHandler.FluidAction.SIMULATE) < near
                || tail().far.fill(new FluidStack(cut.repelled(), far), IFluidHandler.FluidAction.SIMULATE) < far) {
            return Optional.of(new Stall("full"));
        }
        return Optional.empty();
    }

    /** One batch through the line; only called when {@link #stall()} is empty. */
    void run() {
        int batch = MagnetomigrationCellBlockEntity.BATCH;
        MagneticRecipe cut = MagneticRecipe.forLiquor(head().getLevel(), head().feed.getFluid().getFluid()).orElseThrow();
        head().feed.drain(batch, IFluidHandler.FluidAction.EXECUTE);
        tail().near.fill(new FluidStack(cut.attracted(), cut.of(cut.attracted(), batch)), IFluidHandler.FluidAction.EXECUTE);
        tail().far.fill(new FluidStack(cut.repelled(), cut.of(cut.repelled(), batch)), IFluidHandler.FluidAction.EXECUTE);
    }
}
