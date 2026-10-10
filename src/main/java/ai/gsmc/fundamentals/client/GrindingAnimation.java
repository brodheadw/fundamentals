package ai.gsmc.fundamentals.client;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.registry.MortarItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class GrindingAnimation {

    private static final ModelResourceLocation BOWL = ModelResourceLocation.standalone(
            Fundamentals.id("item/mortar_bowl"));
    private static final ModelResourceLocation PESTLE = ModelResourceLocation.standalone(
            Fundamentals.id("item/pestle"));

    private static final float FEED_TICKS = 8;

    private GrindingAnimation() {}

    public static void register(IEventBus modBus) {
        modBus.addListener(ModelEvent.RegisterAdditional.class, event -> {
            event.register(BOWL);
            event.register(PESTLE);
        });
        NeoForge.EVENT_BUS.addListener(RenderHandEvent.class, GrindingAnimation::render);
    }

    private static void render(RenderHandEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || !player.isUsingItem() || !(player.getUseItem().getItem() instanceof MortarItem)) {
            return;
        }
        float ticks = player.getTicksUsingItem() + event.getPartialTick();
        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == HumanoidArm.RIGHT ? 1 : -1;
        PoseStack pose = event.getPoseStack();
        ItemRenderer items = minecraft.getItemRenderer();
        ItemDisplayContext context = arm == HumanoidArm.RIGHT
                ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
        boolean left = arm == HumanoidArm.LEFT;
        float settle = Mth.clamp(ticks / 6.0F, 0, 1);

        event.setCanceled(true);
        pose.pushPose();
        if (event.getHand() == player.getUsedItemHand()) {
            float x = Mth.lerp(settle, side * 0.56F, side * 0.22F);
            float y = Mth.lerp(settle, -0.52F, -0.40F) + Mth.sin(ticks * 1.1F) * 0.005F;
            pose.translate(x, y, -0.72F);
            draw(items, player.getUseItem(), context, left, pose, event, BOWL);
        } else if (ticks < FEED_TICKS) {
            float p = ticks / FEED_TICKS;
            float eased = p * p * (3 - 2 * p);
            pose.translate(Mth.lerp(eased, side * 0.56F, -side * 0.20F), Mth.lerp(eased, -0.52F, -0.26F), -0.72F);
            pose.mulPose(Axis.ZP.rotationDegrees(side * 50 * eased));
            float scale = 1 - 0.85F * eased;
            pose.scale(scale, scale, scale);
            minecraft.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, event.getItemStack(),
                    context, left, pose, event.getMultiBufferSource(), event.getPackedLight());
        } else {
            float t = ticks - FEED_TICKS;
            float arrive = Mth.clamp(t / 5.0F, 0, 1);
            float swirl = t * 1.1F;
            float bowlX = -side * 0.22F, bowlY = -0.22F;
            float x = Mth.lerp(arrive, side * 0.56F, bowlX + Mth.cos(swirl) * 0.05F);
            float y = Mth.lerp(arrive, -0.52F, bowlY + Mth.sin(swirl) * 0.03F - Mth.abs(Mth.sin(swirl * 0.5F)) * 0.02F);
            pose.translate(x, y, -0.70F);
            pose.mulPose(Axis.ZP.rotationDegrees(side * (-28 + Mth.sin(swirl) * 6) * arrive));
            draw(items, player.getUseItem(), context, left, pose, event, PESTLE);
        }
        pose.popPose();
    }

    private static void draw(ItemRenderer items, ItemStack stack, ItemDisplayContext context,
                             boolean left, PoseStack pose, RenderHandEvent event, ModelResourceLocation model) {
        BakedModel baked = Minecraft.getInstance().getModelManager().getModel(model);
        items.render(stack, context, left, pose, event.getMultiBufferSource(), event.getPackedLight(),
                OverlayTexture.NO_OVERLAY, baked);
    }
}
