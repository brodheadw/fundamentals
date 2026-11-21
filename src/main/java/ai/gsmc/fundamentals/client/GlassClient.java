package ai.gsmc.fundamentals.client;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.glass.BlastDoorBlock;
import ai.gsmc.fundamentals.glass.BlastDoorBlockEntity;
import ai.gsmc.fundamentals.glass.Glass;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public final class GlassClient {

    private static final ResourceLocation FACE = Fundamentals.id("block/blast_door"), EDGE = Fundamentals.id("block/blast_door_edge"),
            FRAME = Fundamentals.id("block/blast_door_frame"), LAMP = Fundamentals.id("block/blast_door_lamp");

    private GlassClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> event.registerBlockEntityRenderer(Glass.blastDoorEntity(), context -> new DoorRenderer()));
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(() -> ItemProperties.register(Glass.riotShield(), ResourceLocation.withDefaultNamespace("blocking"),
                (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F)));
    }

    /**
     * Draws a door from its controller: a hazard-striped frame, a lintel over the top and a trim down each jamb on both faces, and
     * two ribbed leaves that slide apart between the trims and into the wall. Amber lamps on the lintel flash while they run.
     */
    private static class DoorRenderer implements BlockEntityRenderer<BlastDoorBlockEntity> {

        private static final float T = 1 / 16F, TOP = 3 - (float) (1 - BlastDoorBlock.LINTEL), JAMB = (float) BlastDoorBlock.JAMB;
        private static final int AMBER = 0xFFFFA020, AMBER_OFF = 0xFF5A3A10;

        @Override
        public void render(BlastDoorBlockEntity door, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            var atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
            TextureAtlasSprite face = atlas.apply(FACE), edge = atlas.apply(EDGE), frame = atlas.apply(FRAME), lamp = atlas.apply(LAMP);
            Direction facing = door.getBlockState().getValue(BlastDoorBlock.FACING);
            int lit = LevelRenderer.getLightColor(door.getLevel(), door.getBlockPos().relative(facing).above());
            float slide = 1.5F * door.open(partialTicks);
            VertexConsumer out = buffers.getBuffer(Sheets.translucentCullBlockSheet());
            pose.pushPose();
            pose.translate(0.5, 0, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            pose.translate(-0.5, 0, -0.5);
            // in this frame the door spans x -1..2 (its width), y 0..3, z 0..1, its front at z = 1
            for (float[] leaf : new float[][]{{-1, 0.5F, -slide}, {0.5F, 2, slide}}) {
                float x0 = leaf[0] + leaf[2], x1 = leaf[1] + leaf[2], shift = leaf[2];
                box(pose, out, x0, 0, 2 * T, x1, TOP, 14 * T, face, shift, false, edge, -1, lit);
                for (float rib : new float[]{0.75F, 2.0F}) box(pose, out, x0 + T, rib, T, x1 - T, rib + 0.25F, 15 * T, face, shift, false, edge, -1, lit);
            }
            for (int x = -1; x < 2; x++) box(pose, out, x, TOP, 0, x + 1, 3, 1, frame, 0, true, frame, -1, lit);
            for (float[] side : new float[][]{{-1, -1 + JAMB}, {2 - JAMB, 2}}) {
                for (float y = 0; y < TOP; y += 1) {
                    float y1 = Math.min(y + 1, TOP);
                    box(pose, out, side[0], y, 0, side[1], y1, T, frame, 0, true, frame, -1, lit);
                    box(pose, out, side[0], y, 15 * T, side[1], y1, 1, frame, 0, true, frame, -1, lit);
                }
            }
            boolean flash = door.moving() && (door.getLevel().getGameTime() / 5) % 2 == 0;
            for (float x : new float[]{-0.8F, 1.55F}) {
                box(pose, out, x, TOP + 2 * T, 1, x + 0.25F, 3 - 2 * T, 1 + 2 * T, lamp, 0, true, lamp, flash ? AMBER : AMBER_OFF, flash ? LightTexture.FULL_BRIGHT : lit);
            }
            pose.popPose();
        }

        /**
         * A box. The front and back take {@code face} as if it spanned the whole closed door, {@code shift} blocks off where it is
         * drawn, so each leaf carries its own half; a {@code whole} piece, a block or less, takes the whole sprite. The other sides take {@code side}.
         */
        private static void box(PoseStack pose, VertexConsumer out, float x0, float y0, float z0, float x1, float y1, float z1,
                                TextureAtlasSprite face, float shift, boolean whole, TextureAtlasSprite side, int color, int light) {
            float fu0 = whole ? x0 - (float) Math.floor(x0) : (x0 - shift + 1) / 3, fu1 = whole ? fu0 + x1 - x0 : (x1 - shift + 1) / 3;
            float fv0 = whole ? 1 - (y1 - (float) Math.floor(y0)) : 1 - y1 / 3, fv1 = whole ? 1 - (y0 - (float) Math.floor(y0)) : 1 - y0 / 3;
            quad(pose, out, face, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, fu0, fv1, fu1, fv0, 0, 0, 1, color, light);
            quad(pose, out, face, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 1 - fu1, fv1, 1 - fu0, fv0, 0, 0, -1, color, light);
            quad(pose, out, side, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, z0, fv1, z1, fv0, -1, 0, 0, color, light);
            quad(pose, out, side, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1 - z1, fv1, 1 - z0, fv0, 1, 0, 0, color, light);
            quad(pose, out, side, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, fu0, z1, fu1, z0, 0, 1, 0, color, light);
            quad(pose, out, side, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, fu0, 1 - z0, fu1, 1 - z1, 0, -1, 0, color, light);
        }

        /** A quad from bottom-left counter-clockwise, its texture spanning u0..u1 and v1 (bottom) to v0 (top) of the sprite. */
        private static void quad(PoseStack pose, VertexConsumer out, TextureAtlasSprite sprite,
                                 float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
                                 float u0, float v1, float u1, float v0, float nx, float ny, float nz, int color, int light) {
            PoseStack.Pose last = pose.last();
            float su0 = sprite.getU(u0), su1 = sprite.getU(u1), sv0 = sprite.getV(v0), sv1 = sprite.getV(v1);
            out.addVertex(last, ax, ay, az).setColor(color).setUv(su0, sv1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, ny, nz);
            out.addVertex(last, bx, by, bz).setColor(color).setUv(su1, sv1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, ny, nz);
            out.addVertex(last, cx, cy, cz).setColor(color).setUv(su1, sv0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, ny, nz);
            out.addVertex(last, dx, dy, dz).setColor(color).setUv(su0, sv0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, ny, nz);
        }

        @Override
        public boolean shouldRenderOffScreen(BlastDoorBlockEntity door) {
            return true;
        }

        @Override
        public AABB getRenderBoundingBox(BlastDoorBlockEntity door) {
            return new AABB(door.getBlockPos()).inflate(3, 0, 3).expandTowards(0, 3, 0);
        }
    }
}
