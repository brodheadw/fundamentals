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

    private static final ResourceLocation FACE = Fundamentals.id("block/blast_door"), EDGE = Fundamentals.id("block/blast_door_edge");

    private GlassClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> event.registerBlockEntityRenderer(Glass.blastDoorEntity(), context -> new DoorRenderer()));
        modBus.addListener(FMLClientSetupEvent.class, event -> event.enqueueWork(() -> ItemProperties.register(Glass.riotShield(), ResourceLocation.withDefaultNamespace("blocking"),
                (stack, level, entity, seed) -> entity != null && entity.isUsingItem() && entity.getUseItem() == stack ? 1.0F : 0.0F)));
    }

    /** Draws the two leaves of a door from its controller, each a block thick and a block and a half wide, slid apart by how open it is. */
    private static class DoorRenderer implements BlockEntityRenderer<BlastDoorBlockEntity> {

        @Override
        public void render(BlastDoorBlockEntity door, float partialTicks, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            var atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
            TextureAtlasSprite face = atlas.apply(FACE), edge = atlas.apply(EDGE);
            Direction facing = door.getBlockState().getValue(BlastDoorBlock.FACING);
            int lit = LevelRenderer.getLightColor(door.getLevel(), door.getBlockPos().relative(facing).above());
            float slide = 1.5F * door.open(partialTicks);
            VertexConsumer out = buffers.getBuffer(Sheets.translucentCullBlockSheet());
            pose.pushPose();
            pose.translate(0.5, 0, 0.5);
            pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));
            pose.translate(-0.5, 0, -0.5);
            // in this frame the door spans x -1..2 (its width), y 0..3, z 0..1
            leaf(pose, out, face, edge, -1 - slide, 0.5F - slide, 0, lit);
            leaf(pose, out, face, edge, 0.5F + slide, 2 + slide, 0.5F, lit);
            pose.popPose();
        }

        private static void leaf(PoseStack pose, VertexConsumer out, TextureAtlasSprite face, TextureAtlasSprite edge, float x0, float x1, float u0, int light) {
            float u1 = u0 + 0.5F;
            // front (+z) and back (-z): the face texture spans the whole closed door, so each leaf takes its half
            quad(pose, out, face, x0, 0, 1, x1, 0, 1, x1, 3, 1, x0, 3, 1, u0, 1, u0 + 0.5F, 0, 0, 0, 1, light);
            quad(pose, out, face, x1, 0, 0, x0, 0, 0, x0, 3, 0, x1, 3, 0, 1 - u1, 1, 1 - u0, 0, 0, 0, -1, light);
            // the two side edges, top and bottom in plain steel
            quad(pose, out, edge, x0, 0, 0, x0, 0, 1, x0, 3, 1, x0, 3, 0, 0, 1, 1, 0, -1, 0, 0, light);
            quad(pose, out, edge, x1, 0, 1, x1, 0, 0, x1, 3, 0, x1, 3, 1, 0, 1, 1, 0, 1, 0, 0, light);
            quad(pose, out, edge, x0, 3, 1, x1, 3, 1, x1, 3, 0, x0, 3, 0, 0, 1, 1, 0, 0, 1, 0, light);
            quad(pose, out, edge, x0, 0, 0, x1, 0, 0, x1, 0, 1, x0, 0, 1, 0, 1, 1, 0, 0, -1, 0, light);
        }

        /** A quad from bottom-left counter-clockwise, its texture spanning u0..u1 and v1 (bottom) to v0 (top) of the sprite. */
        private static void quad(PoseStack pose, VertexConsumer out, TextureAtlasSprite sprite,
                                 float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
                                 float u0, float v1, float u1, float v0, float nx, float ny, float nz, int light) {
            PoseStack.Pose last = pose.last();
            float su0 = sprite.getU(u0), su1 = sprite.getU(u1), sv0 = sprite.getV(v0), sv1 = sprite.getV(v1);
            out.addVertex(last, ax, ay, az).setColor(-1).setUv(su0, sv1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, ny, nz);
            out.addVertex(last, bx, by, bz).setColor(-1).setUv(su1, sv1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, ny, nz);
            out.addVertex(last, cx, cy, cz).setColor(-1).setUv(su1, sv0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, ny, nz);
            out.addVertex(last, dx, dy, dz).setColor(-1).setUv(su0, sv0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, ny, nz);
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
