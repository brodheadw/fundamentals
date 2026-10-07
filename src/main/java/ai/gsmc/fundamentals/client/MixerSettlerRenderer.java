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
 * Draws a stage's fluids from its controller: the two phases in the settling bay, the organic floating on
 * the aqueous, and the churn in the mixing trough behind the weir. Coordinates are
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
        if (!stage.isController()) {
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
        // the trough: the back row, or the back five pixels of the only row, behind a weir
        float wellBack = 1 - PX, wellFront = oneRow ? 11 * PX : PX;
        float bayBack = oneRow ? 10 * PX : 0, bayFront = -(l - 1) + PX;
        float weir = oneRow ? 12 * PX : h == 1 ? brim : 1 + 2 * PX;

        FluidStack aqueous = stage.aqueous();
        FluidStack organic = stage.organic();
        // Each phase gets half the depth of the bay; the organic sits on whatever aqueous there is.
        float depth = brim - FLOOR;
        float aqueousTop = Math.min(brim, FLOOR + depth * 0.5F * aqueous.getAmount() / stage.capacity());
        float organicTop = Math.min(brim, aqueousTop + depth * 0.5F * organic.getAmount() / stage.capacity());
        if (!aqueous.isEmpty()) {
            FLUIDS.renderFluidBox(aqueous, PX, FLOOR, bayFront, right, aqueousTop, bayBack, buffer, ms, light, false, false);
        }
        if (!organic.isEmpty()) {
            FLUIDS.renderFluidBox(organic, PX, aqueousTop, bayFront, right, organicTop, bayBack, buffer, ms, light, false, false);
        }
        // The trough holds the emulsion: the aqueous colour, as high as both phases together, heaving a
        // little while it is stirred; it cannot show above the weir until the bay is that full.
        FluidStack churn = aqueous.isEmpty() ? organic : aqueous;
        if (!churn.isEmpty()) {
            float churnTop = stage.stirring() ? organicTop + PX : organicTop;
            churnTop = organicTop > weir ? churnTop : Math.min(churnTop, weir - PX);
            FLUIDS.renderFluidBox(churn, PX, FLOOR, wellFront, right, churnTop, wellBack, buffer, ms, light, false, false);
        }
        ms.popPose();
    }
}
