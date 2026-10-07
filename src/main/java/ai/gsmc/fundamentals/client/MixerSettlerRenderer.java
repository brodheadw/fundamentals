package ai.gsmc.fundamentals.client;

import ai.gsmc.fundamentals.separation.MixerSettlerBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.createmod.catnip.platform.CatnipServices;
import net.createmod.catnip.render.FluidRenderHelper;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;
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
                float y0 = FLOOR + band * (2 * i + 1) + (stage.getLevel().getGameTime() % 10 < 5 ? band * 0.3F : 0);
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
        if (stage.isStirred()) {
            shaft(stage, partialTick, ms, buffer, light, overlay, w / 2 + 0.5F, oneRow ? 13 * PX : 0.5F, h, churnTop);
        }
        ms.popPose();
    }

    /** Create's mixer hangs above the vat; this is its shaft reaching down into the mixing box, and the whisk on it. */
    private static void shaft(MixerSettlerBlockEntity stage, float partialTick, PoseStack ms, MultiBufferSource buffer, int light, int overlay,
                              float x, float z, float top, float liquid) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.parse("create:block/mixer_head"));
        VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
        float angle = (stage.getLevel().getGameTime() + partialTick) * 12F;
        float bottom = FLOOR + 2 * PX;
        ms.pushPose();
        ms.translate(x, 0, z);
        ms.mulPose(Axis.YP.rotationDegrees(angle));
        Matrix4f m = ms.last().pose();
        float u0 = sprite.getU(12 / 16F), u1 = sprite.getU(14 / 16F), v0 = sprite.getV(0), v1 = sprite.getV(12 / 16F);
        column(vc, m, PX, bottom, top, u0, u1, v0, v1, light, overlay);
        float b0 = sprite.getU(0), b1 = sprite.getU(11 / 16F), bv0 = sprite.getV(0), bv1 = sprite.getV(10 / 16F);
        float reach = 5 * PX;
        float bladeTop = Math.min(liquid, bottom + 10 * PX);
        pane(vc, m, reach, bottom, bladeTop, b0, b1, bv0, bv1, light, overlay, false);
        pane(vc, m, reach, bottom, bladeTop, b0, b1, bv0, bv1, light, overlay, true);
        ms.popPose();
    }

    private static void column(VertexConsumer vc, Matrix4f m, float half, float y0, float y1, float u0, float u1, float v0, float v1, int light, int overlay) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            float nx = d.getStepX(), nz = d.getStepZ();
            float ax = d.getStepZ() * half, az = -d.getStepX() * half;
            float cx = nx * half, cz = nz * half;
            vertex(vc, m, cx - ax, y1, cz - az, u0, v0, light, overlay, nx, nz);
            vertex(vc, m, cx + ax, y1, cz + az, u1, v0, light, overlay, nx, nz);
            vertex(vc, m, cx + ax, y0, cz + az, u1, v1, light, overlay, nx, nz);
            vertex(vc, m, cx - ax, y0, cz - az, u0, v1, light, overlay, nx, nz);
        }
    }

    private static void pane(VertexConsumer vc, Matrix4f m, float reach, float y0, float y1, float u0, float u1, float v0, float v1, int light, int overlay, boolean turned) {
        float ax0 = turned ? 0 : -reach, az0 = turned ? -reach : 0, ax1 = turned ? 0 : reach, az1 = turned ? reach : 0;
        float nx = turned ? 1 : 0, nz = turned ? 0 : 1;
        for (int side = 0; side < 2; side++) {
            float sx = side == 0 ? nx : -nx, sz = side == 0 ? nz : -nz;
            vertex(vc, m, ax0, y1, az0, u0, v0, light, overlay, sx, sz);
            vertex(vc, m, ax1, y1, az1, u1, v0, light, overlay, sx, sz);
            vertex(vc, m, ax1, y0, az1, u1, v1, light, overlay, sx, sz);
            vertex(vc, m, ax0, y0, az0, u0, v1, light, overlay, sx, sz);
            float t = ax0; ax0 = ax1; ax1 = t;
            t = az0; az0 = az1; az1 = t;
        }
    }

    private static void vertex(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int light, int overlay, float nx, float nz) {
        vc.addVertex(m, x, y, z).setColor(255, 255, 255, 255).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(nx, 0, nz);
    }
}
