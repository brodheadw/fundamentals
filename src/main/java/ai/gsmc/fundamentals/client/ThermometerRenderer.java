package ai.gsmc.fundamentals.client;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.heat.ThermometerBlockEntity;
import ai.gsmc.fundamentals.heat.Thermometers;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Swings a thermometer's needle over its dial. The needle is its own model, drawn facing north with its pivot at the centre
 * of the dial, and turned here to the way the gauge faces; it sweeps 240 degrees, bottom left to bottom right.
 */
public class ThermometerRenderer implements BlockEntityRenderer<ThermometerBlockEntity> {

    private static final ModelResourceLocation NEEDLE = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "block/thermometer_needle"));
    private static final float SWEEP = 240;

    public static void register(IEventBus modBus) {
        modBus.addListener(ModelEvent.RegisterAdditional.class, event -> event.register(NEEDLE));
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class,
                event -> event.registerBlockEntityRenderer(Thermometers.entity(), context -> new ThermometerRenderer()));
    }

    @Override
    public void render(ThermometerBlockEntity gauge, float partialTick, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        ms.pushPose();
        ms.translate(0.5, 0.5, 0.5);
        switch (gauge.facing()) {
            case EAST -> ms.mulPose(Axis.YP.rotationDegrees(-90));
            case SOUTH -> ms.mulPose(Axis.YP.rotationDegrees(180));
            case WEST -> ms.mulPose(Axis.YP.rotationDegrees(90));
            case UP -> ms.mulPose(Axis.XP.rotationDegrees(90));
            case DOWN -> ms.mulPose(Axis.XP.rotationDegrees(-90));
            default -> {}
        }
        ms.mulPose(Axis.ZP.rotationDegrees(SWEEP * (gauge.dial(partialTick) - 0.5F)));
        ms.translate(-0.5, -0.5, -0.5);
        BakedModel needle = Minecraft.getInstance().getModelManager().getModel(NEEDLE);
        Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(ms.last(), buffer.getBuffer(RenderType.solid()), null, needle,
                1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.solid());
        ms.popPose();
    }
}
