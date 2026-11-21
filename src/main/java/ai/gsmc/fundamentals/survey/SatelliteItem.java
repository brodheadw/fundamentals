package ai.gsmc.fundamentals.survey;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.fml.ModList;

import java.util.List;

/**
 * An Earth-observation satellite like the first Landsat: a camera behind a lens looking down, solar panels, nickel-cadmium cells
 * for the night side, a microcontroller and an antenna to send down what it sees. Carried up with Ad Astra and let go in Earth
 * orbit, it stays over the ground it was released above, to be tasked from a ground station.
 */
public class SatelliteItem extends Item {

    public SatelliteItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) return InteractionResultHolder.success(stack);
        if (!level.dimension().location().toString().equals(Satellites.ORBIT)) {
            player.displayClientMessage(Component.translatable("message.fundamentals.satellite.ground").withStyle(ChatFormatting.GRAY), true);
            return InteractionResultHolder.fail(stack);
        }
        deploy(server.getServer(), player, stack);
        level.playSound(null, player.blockPosition(), SoundEvents.PISTON_EXTEND, SoundSource.PLAYERS, 0.8F, 0.6F);
        return InteractionResultHolder.consume(stack);
    }

    /** Lets the satellite go over where the player is: it goes into orbit and out of the hand. */
    public static Satellites.Satellite deploy(MinecraftServer server, Player player, ItemStack stack) {
        String name = stack.has(DataComponents.CUSTOM_NAME) ? stack.getHoverName().getString() : "";
        Satellites.Satellite satellite = Satellites.get(server).launch(player.getUUID(), name, player.getBlockX(), player.getBlockZ());
        stack.consume(1, player);
        player.displayClientMessage(Component.translatable("message.fundamentals.satellite.released", satellite.label(), satellite.x(), satellite.z())
                .withStyle(ChatFormatting.AQUA), false);
        return satellite;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fundamentals.satellite").withStyle(ChatFormatting.GRAY));
        if (!ModList.get().isLoaded("ad_astra")) {
            tooltip.add(Component.translatable("tooltip.fundamentals.satellite.ad_astra").withStyle(ChatFormatting.RED));
        }
    }
}
