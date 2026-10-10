package ai.gsmc.fundamentals.separation;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record MagnetomigrationLine(List<MagnetomigrationCellBlockEntity> cells) {

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

    void run() {
        int batch = MagnetomigrationCellBlockEntity.BATCH;
        MagneticRecipe cut = MagneticRecipe.forLiquor(head().getLevel(), head().feed.getFluid().getFluid()).orElseThrow();
        head().feed.drain(batch, IFluidHandler.FluidAction.EXECUTE);
        tail().near.fill(new FluidStack(cut.attracted(), cut.of(cut.attracted(), batch)), IFluidHandler.FluidAction.EXECUTE);
        tail().far.fill(new FluidStack(cut.repelled(), cut.of(cut.repelled(), batch)), IFluidHandler.FluidAction.EXECUTE);
    }
}
