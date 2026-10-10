package ai.gsmc.fundamentals.separation;

import net.minecraft.network.chat.Component;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Battery(List<MixerSettlerBlockEntity> stages) {

    public static Battery of(MixerSettlerBlockEntity casing) {
        MixerSettlerBlockEntity head = casing.stage();
        for (MixerSettlerBlockEntity back; (back = head.nextStage(false)) != null; ) {
            head = back;
        }
        List<MixerSettlerBlockEntity> stages = new ArrayList<>();
        for (MixerSettlerBlockEntity stage = head; stage != null; stage = stage.nextStage(true)) {
            stages.add(stage);
        }
        return new Battery(stages);
    }

    public MixerSettlerBlockEntity head() { return stages.getFirst(); }
    public MixerSettlerBlockEntity tail() { return stages.getLast(); }
    public int size() { return stages.size(); }

    public boolean isSwitchedOn() { return head().hasSignal(); }

    static int wastePerCut(MixerSettlerBlockEntity head) { return Math.max(1, head.batch() / 5); }

    public int equilibration() { return MixerSettlerBlockEntity.EQUILIBRATION_PER_STAGE * size(); }

    public Optional<Stall> stall() {
        MixerSettlerBlockEntity head = head(), tail = tail();
        FluidStack feed = head.aqueous.getFluid();
        int batch = head.batch();
        if (feed.getAmount() < batch) {
            return Optional.of(new Stall("idle"));
        }
        Optional<SeparationRecipe> found = SeparationRecipe.forLiquor(head.getLevel(), Separation.clarified(feed.getFluid()));
        if (found.isEmpty()) {
            return Optional.of(new Stall("no_cut", feed.getHoverName()));
        }
        SeparationRecipe cut = found.get();
        if (size() < cut.stages()) {
            return Optional.of(new Stall("short", feed.getHoverName(), cut.stages(), size()));
        }
        if (!stages.stream().allMatch(MixerSettlerBlockEntity::isStirred)) {
            return Optional.of(new Stall("mixer"));
        }
        if (stages.stream().anyMatch(MixerSettlerBlockEntity::isOverStirred)) {
            return Optional.of(new Stall("emulsion"));
        }
        if (!isSwitchedOn()) {
            return Optional.of(new Stall("lever"));
        }
        if (stages.stream().anyMatch(s -> Separation.kind(s.organic.getFluid().getFluid()) == Reagents.Kind.FOULED)) {
            return Optional.of(new Stall("crud"));
        }
        if (!stages.stream().allMatch(s -> s.organic.getFluid().is(cut.organic()) && s.organic.getFluidAmount() >= batch)) {
            return Optional.of(new Stall("organic", name(cut.organic())));
        }
        if (Separation.reagent(tail.aqueous.getFluid().getFluid()) != cut.strip() || tail.aqueous.getFluidAmount() < batch) {
            return Optional.of(new Stall("strip", name(cut.strip())));
        }
        int light = cut.lightOf(batch), heavy = cut.heavyOf(batch);
        if (head.out.fill(new FluidStack(cut.light(), light), IFluidHandler.FluidAction.SIMULATE) < light
                || tail.out.fill(new FluidStack(cut.heavy(), heavy), IFluidHandler.FluidAction.SIMULATE) < heavy) {
            return Optional.of(new Stall("full"));
        }
        if (head.waste.fill(new FluidStack(Separation.fluid("spent_liquor"), wastePerCut(head)), IFluidHandler.FluidAction.SIMULATE) < wastePerCut(head)) {
            return Optional.of(new Stall("waste"));
        }
        return Optional.empty();
    }

    void runCut() {
        MixerSettlerBlockEntity head = head(), tail = tail();
        FluidStack feed = head.aqueous.getFluid();
        SeparationRecipe cut = SeparationRecipe.forLiquor(head.getLevel(), Separation.clarified(feed.getFluid())).orElseThrow();
        int batch = head.batch();
        head.organic.drain(Math.max(1, batch / 50), IFluidHandler.FluidAction.EXECUTE);
        if (Separation.kind(feed.getFluid()) == Reagents.Kind.CRUDE && ++head.crud >= 3) {
            head.crud = 0;
            for (MixerSettlerBlockEntity stage : stages) {
                stage.organic.setFluid(new FluidStack(Separation.fouled(stage.organic.getFluid().getFluid()), stage.organic.getFluidAmount()));
            }
        }
        head.aqueous.drain(batch, IFluidHandler.FluidAction.EXECUTE);
        tail.aqueous.drain(batch, IFluidHandler.FluidAction.EXECUTE);
        head.out.fill(new FluidStack(cut.light(), cut.lightOf(batch)), IFluidHandler.FluidAction.EXECUTE);
        tail.out.fill(new FluidStack(cut.heavy(), cut.heavyOf(batch)), IFluidHandler.FluidAction.EXECUTE);
        head.waste.fill(new FluidStack(Separation.fluid("spent_liquor"), wastePerCut(head)), IFluidHandler.FluidAction.EXECUTE);
        int strip = size() * 2 / 5;
        for (int i = 0; i < size(); i++) {
            MixerSettlerBlockEntity stage = stages.get(i);
            if (i > 0 && i < size() - 1) {
                Fluid shown = i < size() - strip ? cut.light() : cut.heavy();
                if (!stage.aqueous.getFluid().is(shown)) {
                    stage.aqueous.setFluid(FluidStack.EMPTY);
                }
                int room = stage.phaseCapacity() - stage.aqueous.getFluidAmount();
                if (room > 0) {
                    stage.aqueous.fill(new FluidStack(shown, Math.min(stage.capacity() / 10, room)), IFluidHandler.FluidAction.EXECUTE);
                }
            }
            stage.stirring = MixerSettlerBlockEntity.STIR_TICKS;
            stage.dirty = true;
        }
    }

    static Component name(Fluid fluid) {
        return new FluidStack(fluid, 1).getHoverName();
    }
}
