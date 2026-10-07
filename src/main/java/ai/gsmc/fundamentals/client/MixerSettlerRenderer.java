package ai.gsmc.fundamentals.client;

import ai.gsmc.fundamentals.separation.MixerSettlerBlockEntity;
import ai.gsmc.fundamentals.separation.VatGeometry;
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
 * bay, the organic floating on the aqueous, and the emulsion in the mixing trough behind the weir. Coordinates
 * are the stage's own, with the controller's block at the origin and the stage running right (+x) and
 * forward (-z) from it; the back row (z from 0 to 1) is the trough. Every height and inset is
 * {@link VatGeometry}'s, the same numbers the models were cut from.
 */
public class MixerSettlerRenderer implements BlockEntityRenderer<MixerSettlerBlockEntity> {

    @SuppressWarnings("unchecked")
    private static final FluidRenderHelper<FluidStack> FLUIDS = (FluidRenderHelper<FluidStack>) CatnipServices.FLUID_RENDERER;

    /** Fluid faces sit this far off the wall and weir planes so they never fight them. */
    private static final float EPS = 0.004F;

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

        VatGeometry vat = VatGeometry.get();
        int w = stage.across(), l = stage.along(), h = stage.tall();
        float in = vat.wallIn() + EPS;
        float left = in, right = w - in;
        float floor = vat.floorY();
        float brim = vat.brim(h);
        float weir = vat.weir(h);
        // the trough is the back row behind the weir, which stands on the row's front seam; the bay is everything ahead
        float wellBack = 1 - in, wellFront = in;
        float bayBack = -EPS, bayFront = -(l - 1) + in;

        FluidStack aqueous = stage.aqueous();
        FluidStack organic = stage.organic();
        float aqueousTop = vat.aqueousTop(h, stage.aqueousFill());
        float organicTop = vat.surface(h, stage.aqueousFill(), stage.organicFill());

        // The bay: the two settled phases, the organic floating on the aqueous.
        if (!aqueous.isEmpty()) {
            FLUIDS.renderFluidBox(aqueous, left, floor, bayFront, right, aqueousTop, bayBack, buffer, ms, light, false, false);
        }
        if (!organic.isEmpty() && organicTop > aqueousTop) {
            FLUIDS.renderFluidBox(organic, left, aqueousTop + EPS / 2, bayFront, right, organicTop, bayBack, buffer, ms, light, false, false);
        }

        // The trough: while the battery runs the mixer keeps the two phases beaten into an emulsion, drawn as
        // the aqueous column with bands of organic through it, up to the weir's lip; with the lever off they
        // settle into layers like the bay.
        float churnTop = Math.min(weir - VatGeometry.PX, organicTop);
        boolean mixing = stage.isStirred() && stage.battery().isSwitchedOn();
        if (mixing && !aqueous.isEmpty() && !organic.isEmpty()) {
            FLUIDS.renderFluidBox(aqueous, left, floor, wellFront, right, churnTop, wellBack, buffer, ms, light, false, false);
            float band = (churnTop - floor) / 6;
            for (int i = 0; i < 3; i++) {
                float y0 = floor + band * (2 * i + 1);
                FLUIDS.renderFluidBox(organic, left + VatGeometry.PX, y0, wellFront + VatGeometry.PX, right - VatGeometry.PX,
                        Math.min(churnTop - EPS / 2, y0 + band * 0.5F), wellBack - VatGeometry.PX, buffer, ms, light, false, false);
            }
        } else {
            float settledAqueous = Math.min(churnTop, aqueousTop);
            if (!aqueous.isEmpty()) {
                FLUIDS.renderFluidBox(aqueous, left, floor, wellFront, right, settledAqueous, wellBack, buffer, ms, light, false, false);
            }
            if (!organic.isEmpty() && churnTop > settledAqueous) {
                FLUIDS.renderFluidBox(organic, left, settledAqueous + EPS / 2, wellFront, right, churnTop, wellBack, buffer, ms, light, false, false);
            }
        }
        ms.popPose();
    }
}
