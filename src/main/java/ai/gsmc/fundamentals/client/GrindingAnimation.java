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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * First-person view of grinding. The hand holding the mortar shows just the bowl, brought in
 * toward the middle. The other hand tips its material into the bowl over the first few ticks,
 * then comes back holding the pestle and works it round in the bowl. The bowl and the pestle
 * are models of their own, not items.
 */
public final class GrindingAnimation {

    private static final ModelResourceLocation BOWL = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "item/mortar_bowl"));
    private static final ModelResourceLocation PESTLE = ModelResourceLocation.standalone(
            ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "item/pestle"));

    /** Ticks for the material to go into the bowl before the pestle appears. */
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
        float settle = Mth.clamp(ticks / 6.0F, 0, 1);  // the bowl comes in over the first few ticks

        event.setCanceled(true);
        pose.pushPose();
        if (event.getHand() == player.getUsedItemHand()) {
            // The bowl, held in toward the middle, steadied against the grinding.
            float x = Mth.lerp(settle, side * 0.56F, side * 0.22F);
            float y = Mth.lerp(settle, -0.52F, -0.40F) + Mth.sin(ticks * 1.1F) * 0.005F;
            pose.translate(x, y, -0.72F);
            draw(items, player.getUseItem(), context, left, pose, event, BOWL);
        } else if (ticks < FEED_TICKS) {
            // The material: carried across to the bowl and tipped in, shrinking as it goes.
            float p = ticks / FEED_TICKS;
            float eased = p * p * (3 - 2 * p);
            pose.translate(Mth.lerp(eased, side * 0.56F, -side * 0.20F), Mth.lerp(eased, -0.52F, -0.26F), -0.72F);
            pose.mulPose(Axis.ZP.rotationDegrees(side * 50 * eased));  // tipped over the bowl
            float scale = 1 - 0.85F * eased;
            pose.scale(scale, scale, scale);
            minecraft.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(player, event.getItemStack(),
                    context, left, pose, event.getMultiBufferSource(), event.getPackedLight());
        } else {
            // The pestle: picked up, then worked round and round in the bowl.
            float t = ticks - FEED_TICKS;
            float arrive = Mth.clamp(t / 5.0F, 0, 1);
            float swirl = t * 1.1F;
            float bowlX = -side * 0.22F, bowlY = -0.22F;
            float x = Mth.lerp(arrive, side * 0.56F, bowlX + Mth.cos(swirl) * 0.05F);
            float y = Mth.lerp(arrive, -0.52F, bowlY + Mth.sin(swirl) * 0.03F - Mth.abs(Mth.sin(swirl * 0.5F)) * 0.02F);
            pose.translate(x, y, -0.70F);
            // A flat sprite has to stay facing the camera, so the pestle is tilted in the view
            // plane: head up, business end down in the bowl.
            pose.mulPose(Axis.ZP.rotationDegrees(side * (-28 + Mth.sin(swirl) * 6) * arrive));
            draw(items, player.getUseItem(), context, left, pose, event, PESTLE);
        }
        pose.popPose();
    }

    private static void draw(ItemRenderer items, net.minecraft.world.item.ItemStack stack, ItemDisplayContext context,
                             boolean left, PoseStack pose, RenderHandEvent event, ModelResourceLocation model) {
        BakedModel baked = Minecraft.getInstance().getModelManager().getModel(model);
        items.render(stack, context, left, pose, event.getMultiBufferSource(), event.getPackedLight(),
                OverlayTexture.NO_OVERLAY, baked);
    }
}
