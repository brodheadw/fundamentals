package ai.gsmc.fundamentals.client;

import ai.gsmc.fundamentals.registry.MortarItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * First-person view of grinding with the mortar: the bowl comes up toward the middle and is
 * worked in small circles, while the other hand brings its material in over the bowl and dips
 * it, so the two hands read as working together.
 */
public final class GrindingAnimation {

    private GrindingAnimation() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(RenderHandEvent.class, GrindingAnimation::feedTheMortar);
    }

    private static void feedTheMortar(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isUsingItem() || !(player.getUseItem().getItem() instanceof MortarItem)) {
            return;
        }
        float ticks = player.getTicksUsingItem() + event.getPartialTick();
        float arrive = Mth.clamp(ticks / 6.0F, 0, 1);  // both hands swing in over the first few ticks
        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float inward = arm == HumanoidArm.RIGHT ? -1 : 1;
        if (event.getHand() == player.getUsedItemHand()) {
            // The bowl: up toward the middle, then round and round.
            float swirl = ticks * 0.9F;
            event.getPoseStack().translate(
                    (inward * 0.20F + Mth.cos(swirl) * 0.02F) * arrive,
                    (0.14F + Mth.sin(swirl) * 0.02F) * arrive, 0);
        } else {
            // The material: in over the bowl, dipping as it is fed in.
            float dip = Mth.sin(ticks * 0.8F) * 0.04F;
            event.getPoseStack().translate(inward * 0.26F * arrive, (0.20F + dip) * arrive, -0.04F * arrive);
        }
    }
}
