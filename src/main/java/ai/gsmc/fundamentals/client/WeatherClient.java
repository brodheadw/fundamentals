package ai.gsmc.fundamentals.client;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.survey.Survey;
import ai.gsmc.fundamentals.survey.WeatherRadarBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

/** The weather radar's turning dish and the weather balloon in flight. */
public final class WeatherClient {

    private static final ModelResourceLocation DISH = ModelResourceLocation.standalone(Fundamentals.id("block/weather_radar_dish"));
    /** Six turns a minute, as a weather radar sweeps. */
    private static final float DEGREES_PER_TICK = 1.8F;

    private WeatherClient() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ModelEvent.RegisterAdditional.class, event -> event.register(DISH));
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
            event.registerBlockEntityRenderer(Survey.weatherRadarEntity(), context -> new Dish());
            event.registerEntityRenderer(Survey.weatherBalloonEntity(), context -> new ThrownItemRenderer<>(context, 3.0F, false));
        });
    }

    static final class Dish implements BlockEntityRenderer<WeatherRadarBlockEntity> {
        @Override
        public void render(WeatherRadarBlockEntity radar, float partialTick, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
            long time = radar.getLevel() == null ? 0 : radar.getLevel().getGameTime();
            ms.pushPose();
            ms.translate(0.5, 0, 0.5);
            ms.mulPose(Axis.YP.rotationDegrees(-((time % 200) + partialTick) * DEGREES_PER_TICK));
            ms.translate(-0.5, 0, -0.5);
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(ms.last(), buffer.getBuffer(RenderType.solid()), null,
                    Minecraft.getInstance().getModelManager().getModel(DISH), 1, 1, 1, light, overlay, ModelData.EMPTY, RenderType.solid());
            ms.popPose();
        }
    }
}
