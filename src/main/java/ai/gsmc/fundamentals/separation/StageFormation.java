package ai.gsmc.fundamentals.separation;

import ai.gsmc.fundamentals.separation.MixerSettlerBlock.Rows;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class StageFormation {

    private StageFormation() {}

    static void merge(MixerSettlerBlockEntity casing) {
        int best = casing.volume();
        BlockPos bestOrigin = null;
        int[] bestDims = null;
        for (int w = 1; w <= MixerSettlerBlock.MAX_ACROSS; w++) {
            for (int l = 1; l <= MixerSettlerBlock.MAX_ALONG; l++) {
                for (int h = 1; h <= MixerSettlerBlock.MAX_TALL; h++) {
                    if (w * l * h <= best) {
                        continue;
                    }
                    for (int a = 0; a < w; a++) {
                        for (int l0 = 0; l0 < l; l0++) {
                            for (int u = 0; u < h; u++) {
                                BlockPos origin = casing.getBlockPos().relative(casing.right(), -a).relative(casing.facing(), -l0).below(u);
                                if (fits(casing, origin, w, l, h)) {
                                    best = w * l * h;
                                    bestOrigin = origin;
                                    bestDims = new int[] {w, l, h};
                                }
                            }
                        }
                    }
                }
            }
        }
        if (bestOrigin != null) {
            form(casing, bestOrigin, bestDims[0], bestDims[1], bestDims[2]);
        }
    }

    private static boolean fits(MixerSettlerBlockEntity casing, BlockPos origin, int w, int l, int h) {
        for (int a = 0; a < w; a++) {
            for (int l0 = 0; l0 < l; l0++) {
                for (int u = 0; u < h; u++) {
                    MixerSettlerBlockEntity cell = casing.casingAt(casing.cell(origin, a, l0, u));
                    if (cell == null) {
                        return false;
                    }
                    MixerSettlerBlockEntity stage = cell.stage();
                    BlockPos far = stage.cell(stage.getBlockPos(), stage.across - 1, stage.along - 1, stage.tall - 1);
                    if (!inside(casing, origin, w, l, h, stage.getBlockPos()) || !inside(casing, origin, w, l, h, far)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean inside(MixerSettlerBlockEntity casing, BlockPos origin, int w, int l, int h, BlockPos pos) {
        BlockPos rel = pos.subtract(origin);
        Direction right = casing.right(), facing = casing.facing();
        int a = rel.getX() * right.getStepX() + rel.getZ() * right.getStepZ();
        int l0 = rel.getX() * facing.getStepX() + rel.getZ() * facing.getStepZ();
        return a >= 0 && a < w && l0 >= 0 && l0 < l && rel.getY() >= 0 && rel.getY() < h;
    }

    private static void form(MixerSettlerBlockEntity casing, BlockPos origin, int w, int l, int h) {
        Level level = casing.getLevel();
        MixerSettlerBlockEntity.formations++;
        Set<MixerSettlerBlockEntity> absorbed = new LinkedHashSet<>();
        List<FluidStack> organics = new ArrayList<>();
        List<FluidStack> aqueouses = new ArrayList<>();
        List<FluidStack> outs = new ArrayList<>();
        List<FluidStack> wastes = new ArrayList<>();
        for (int a = 0; a < w; a++) {
            for (int l0 = 0; l0 < l; l0++) {
                for (int u = 0; u < h; u++) {
                    MixerSettlerBlockEntity stage = casing.casingAt(casing.cell(origin, a, l0, u)).stage();
                    if (absorbed.add(stage)) {
                        organics.add(stage.organic.getFluid().copy());
                        aqueouses.add(stage.aqueous.getFluid().copy());
                        outs.add(stage.out.getFluid().copy());
                        wastes.add(stage.waste.getFluid().copy());
                    }
                }
            }
        }
        MixerSettlerBlockEntity head = casing.casingAt(origin);
        for (int a = 0; a < w; a++) {
            for (int l0 = 0; l0 < l; l0++) {
                for (int u = 0; u < h; u++) {
                    MixerSettlerBlockEntity cell = casing.casingAt(casing.cell(origin, a, l0, u));
                    cell.controller = origin;
                    cell.across = w;
                    cell.along = l;
                    cell.tall = h;
                    cell.empty();
                    cell.resize();
                    cell.setChanged();
                    BlockState state = cell.getBlockState()
                            .setValue(MixerSettlerBlock.LEFT, a > 0).setValue(MixerSettlerBlock.RIGHT, a < w - 1)
                            .setValue(MixerSettlerBlock.BACK, l0 > 0).setValue(MixerSettlerBlock.FRONT, l0 < l - 1)
                            .setValue(MixerSettlerBlock.BELOW, u > 0).setValue(MixerSettlerBlock.ABOVE, u < h - 1)
                            .setValue(MixerSettlerBlock.ROWS, l == 1 ? Rows.SINGLE : l0 == 0 ? Rows.WELL : Rows.BAY)
                            .setValue(MixerSettlerBlock.OPEN, l0 == 0 && u == h - 1 && a == w / 2)
                            .setValue(MixerSettlerBlock.WINDOW, a == w / 2 || l0 == l / 2);
                    level.setBlock(cell.getBlockPos(), state, Block.UPDATE_ALL);
                }
            }
        }
        for (FluidStack stack : organics) head.organic.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        for (FluidStack stack : aqueouses) head.aqueous.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        for (FluidStack stack : outs) head.out.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        for (FluidStack stack : wastes) head.waste.fill(stack, IFluidHandler.FluidAction.EXECUTE);
        head.dirty = true;
    }

    static void dissolve(MixerSettlerBlockEntity casing) {
        Level level = casing.getLevel();
        MixerSettlerBlockEntity.formations++;
        MixerSettlerBlockEntity stage = casing.stage();
        BlockPos origin = stage.getBlockPos();
        int w = stage.across, l = stage.along, h = stage.tall;
        for (int a = 0; a < w; a++) {
            for (int l0 = 0; l0 < l; l0++) {
                for (int u = 0; u < h; u++) {
                    BlockPos at = casing.cell(origin, a, l0, u);
                    MixerSettlerBlockEntity cell = casing.casingAt(at);
                    if (cell == null || cell == casing) {
                        continue;
                    }
                    cell.becomeSingle();
                    cell.empty();
                    level.setBlock(at, Separation.mixerSettler().defaultBlockState().setValue(MixerSettlerBlock.FACING, casing.facing()), Block.UPDATE_ALL);
                    level.scheduleTick(at, Separation.mixerSettler(), 1);
                }
            }
        }
    }
}
