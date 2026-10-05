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
 * First-person half of grinding with the mortar. The mortar hand already moves (it uses the
 * brush animation); this brings the other hand's material in over the bowl and dips it, so the
 * two hands read as working together.
 */
public final class GrindingAnimation {

    private GrindingAnimation() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(RenderHandEvent.class, GrindingAnimation::feedTheMortar);
    }

    private static void feedTheMortar(RenderHandEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isUsingItem() || !(player.getUseItem().getItem() instanceof MortarItem)
                || event.getHand() == player.getUsedItemHand()) {
            return;
        }
        float ticks = player.getTicksUsingItem() + event.getPartialTick();
        float arrive = Mth.clamp(ticks / 6.0F, 0, 1);  // the hand swings in over the first few ticks
        HumanoidArm arm = event.getHand() == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float inward = arm == HumanoidArm.RIGHT ? -1 : 1;
        float dip = Mth.sin(ticks * 0.8F) * 0.04F;  // tipping material into the bowl
        event.getPoseStack().translate(inward * 0.32F * arrive, (-0.08F + dip) * arrive, -0.05F * arrive);
    }
}
