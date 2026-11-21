package ai.gsmc.fundamentals.survey;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** A rubber balloon of hydrogen with a radiosonde on its line: let go, it climbs and radios back what it finds. */
public class WeatherBalloonItem extends Item {

    public WeatherBalloonItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            WeatherBalloon balloon = new WeatherBalloon(level, player.getX(), player.getY() + player.getBbHeight(), player.getZ(), player.getUUID());
            level.addFreshEntity(balloon);
            balloon.report();
            level.playSound(null, player.blockPosition(), SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 0.8F, 0.8F);
        }
        stack.consume(1, player);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fundamentals.weather_balloon").withStyle(ChatFormatting.GRAY));
    }
}
