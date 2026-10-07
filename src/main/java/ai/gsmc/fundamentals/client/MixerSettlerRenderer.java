package ai.gsmc.fundamentals.client;

import ai.gsmc.fundamentals.separation.MixerSettlerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.render.FluidRenderHelper;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws a stage's fluids from its controller, as seen from above in an open tank: the settled layers in the
 * bay, the organic floating on the aqueous, and the emulsion in the mixing trough behind the weir. Coordinates are
 * the stage's own, with the controller's block at the origin and the stage running right (+x) and
 * forward (-z) from it. A stage one row long keeps its trough at the back of that row.
 */
public class MixerSettlerRenderer implements BlockEntityRenderer<MixerSettlerBlockEntity> {

    @SuppressWarnings("unchecked")
    private static final FluidRenderHelper<FluidStack> FLUIDS = (FluidRenderHelper<FluidStack>) CatnipServices.FLUID_RENDERER;

    private static final float PX = 1 / 16F;
    private static final float FLOOR = PX;

    @Override
    public AABB getRenderBoundingBox(MixerSettlerBlockEntity casing) {
        return casing.isController() ? casing.stageBox().expandTowards(0, 1, 0) : BlockEntityRenderer.super.getRenderBoundingBox(casing);
    }

    @Override
    public void render(MixerSettlerBlockEntity stage, float partialTick, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (!stage.isController() || !stage.isStage()) {
            return;
        }
        ms.pushPose();
        ms.translate(0.5, 0, 0.5);
        ms.mulPose(Axis.YP.rotationDegrees(180 - stage.facing().toYRot()));
        ms.translate(-0.5, 0, -0.5);

        int w = stage.across(), l = stage.along(), h = stage.tall();
        boolean oneRow = l == 1;
        float right = w - PX;
        float brim = h - 2 * PX;
        // the trough: the back row, or the back five pixels of the only row, behind the weir
        float wellBack = 1 - PX, wellFront = oneRow ? 11 * PX : PX;
        float bayBack = oneRow ? 10 * PX : 0, bayFront = -(l - 1) + PX;
        float weir = oneRow ? 13 * PX : h == 1 ? brim : 1 + 3 * PX;

        // Seen from above: the two settled phases in the bay, the organic floating on the aqueous, and in the
        // trough the emulsion, the aqueous colour as high as both together, heaving while the mixer turns.
        FluidStack aqueous = stage.aqueous();
        FluidStack organic = stage.organic();
        float depth = brim - FLOOR;
        float aqueousTop = Math.min(brim, FLOOR + depth * 0.5F * aqueous.getAmount() / stage.capacity());
        float organicTop = Math.min(brim, aqueousTop + depth * 0.5F * organic.getAmount() / stage.capacity());
        if (!aqueous.isEmpty()) {
            FLUIDS.renderFluidBox(aqueous, PX, FLOOR, bayFront, right, aqueousTop, bayBack, buffer, ms, light, false, false);
        }
        if (!organic.isEmpty()) {
            FLUIDS.renderFluidBox(organic, PX, aqueousTop, bayFront, right, organicTop, bayBack, buffer, ms, light, false, false);
        }
        // The trough: while the plant runs the mixer keeps the two phases beaten into an emulsion, drawn as the
        // aqueous column with bands of organic through it; with the lever off they settle into layers like the bay.
        float churnTop = Math.min(weir - PX, organicTop);
        boolean mixing = stage.isStirred() && stage.isSwitchedOn();
        if (mixing && !aqueous.isEmpty() && !organic.isEmpty()) {
            FLUIDS.renderFluidBox(aqueous, PX, FLOOR, wellFront, right, churnTop, wellBack, buffer, ms, light, false, false);
            float band = (churnTop - FLOOR) / 6;
            for (int i = 0; i < 3; i++) {
                float y0 = FLOOR + band * (2 * i + 1);
                FLUIDS.renderFluidBox(organic, 2 * PX, y0, wellFront + PX, right - PX, Math.min(churnTop, y0 + band * 0.5F), wellBack - PX, buffer, ms, light, false, false);
            }
        } else {
            if (!aqueous.isEmpty()) {
                FLUIDS.renderFluidBox(aqueous, PX, FLOOR, wellFront, right, Math.min(churnTop, aqueousTop), wellBack, buffer, ms, light, false, false);
            }
            if (!organic.isEmpty() && churnTop > aqueousTop) {
                FLUIDS.renderFluidBox(organic, PX, Math.min(churnTop, aqueousTop), wellFront, right, churnTop, wellBack, buffer, ms, light, false, false);
            }
        }
        ms.popPose();
    }
}
