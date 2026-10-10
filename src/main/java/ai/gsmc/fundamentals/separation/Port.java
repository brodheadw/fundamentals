package ai.gsmc.fundamentals.separation;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import javax.annotation.Nullable;

record Port(FluidTank tank, @Nullable Boolean organic) implements IFluidHandler {

    private boolean accepts(FluidStack stack) {
        if (organic == null) {
            return false;
        }
        Reagents.Kind kind = Separation.kind(stack.getFluid());
        return organic ? kind == Reagents.Kind.ORGANIC : kind == Reagents.Kind.LIQUOR || kind == Reagents.Kind.ACID || kind == Reagents.Kind.CRUDE;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int index) {
        return tank.getFluid();
    }

    @Override
    public int getTankCapacity(int index) {
        return tank.getCapacity();
    }

    @Override
    public boolean isFluidValid(int index, FluidStack stack) {
        return accepts(stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return accepts(resource) ? tank.fill(resource, action) : 0;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return tank.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return tank.drain(maxDrain, action);
    }
}
