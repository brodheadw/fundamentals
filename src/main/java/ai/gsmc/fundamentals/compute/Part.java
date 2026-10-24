package ai.gsmc.fundamentals.compute;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;

import java.util.List;

/** A fab part; its lang file may give it a line saying what it is, in the process it came from. */
public class Part extends Item {
    public Part(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = getDescriptionId() + ".tooltip";
        if (Language.getInstance().has(key)) tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
    }
}
