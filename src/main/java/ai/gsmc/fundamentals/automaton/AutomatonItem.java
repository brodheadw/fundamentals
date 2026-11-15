package ai.gsmc.fundamentals.automaton;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

import java.util.List;

/** A wound-down automaton you carry; set it on the ground and it's yours. It keeps whatever steam it had. */
public class AutomatonItem extends Item {

    public AutomatonItem(Properties properties) {
        super(properties);
    }

    static ItemStack of(Automaton automaton) {
        ItemStack stack = new ItemStack(Automata.item());
        CompoundTag tag = new CompoundTag();
        tag.putInt("Steam", automaton.steam());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        if (automaton.hasCustomName()) stack.set(DataComponents.CUSTOM_NAME, automaton.getCustomName());
        return stack;
    }

    static int steam(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return tag.contains("Steam") ? tag.getInt("Steam") : Automaton.MAX_STEAM;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        BlockPos at = context.getClickedPos().relative(context.getClickedFace());
        Automaton automaton = Automata.entity().create(level);
        if (automaton == null) return InteractionResult.FAIL;
        float yaw = context.getPlayer() == null ? 0 : context.getPlayer().getYRot() + 180;
        automaton.moveTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, yaw, 0);
        automaton.setYHeadRot(yaw);
        automaton.setSteam(steam(context.getItemInHand()));
        if (context.getPlayer() != null) automaton.setOwner(context.getPlayer().getUUID());
        if (context.getItemInHand().has(DataComponents.CUSTOM_NAME)) automaton.setCustomName(context.getItemInHand().get(DataComponents.CUSTOM_NAME));
        level.addFreshEntity(automaton);
        context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.fundamentals.automaton.steam", steam(stack) * 100 / Automaton.MAX_STEAM));
    }
}
