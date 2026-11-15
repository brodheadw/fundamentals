package ai.gsmc.fundamentals.automaton;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;

import java.util.List;

/**
 * A punched card, as Jacquard's loom read them: the holes are the program. A carry card is punched with where to take
 * from and where to bring to; a build card with two corners of what to fill and the store to fill it from.
 */
public class ProgramCard extends Item {
    public enum Kind { CARRY, BUILD }

    public static final int MAX_VOLUME = 4096;
    private final Kind kind;

    public ProgramCard(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    public static Kind kind(ItemStack stack) {
        return stack.getItem() instanceof ProgramCard card ? card.kind : null;
    }

    static CompoundTag holes(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    public static BlockPos at(ItemStack stack, String hole) {
        CompoundTag tag = holes(stack);
        return tag.contains(hole) ? BlockPos.of(tag.getLong(hole)) : null;
    }

    public static void punch(ItemStack stack, String hole, BlockPos pos) {
        CompoundTag tag = holes(stack);
        tag.putLong(hole, pos.asLong());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static boolean container(Level level, BlockPos pos) {
        return level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack card = context.getItemInHand();
        BlockPos clicked = context.getClickedPos();
        boolean sneaking = player != null && player.isSecondaryUseActive();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        String said;
        if (kind == Kind.CARRY || sneaking) {
            if (!container(level, clicked)) {
                tell(player, "not_container");
                return InteractionResult.FAIL;
            }
            String hole = kind == Kind.BUILD ? "supply" : sneaking ? "to" : "from";
            punch(card, hole, clicked);
            said = hole;
        } else {
            // the corner is the space in front of the face clicked: where a block would go
            BlockPos corner = clicked.relative(context.getClickedFace());
            CompoundTag tag = holes(card);
            String hole = tag.getBoolean("second") ? "b" : "a";
            punch(card, hole, corner);
            tag = holes(card);
            tag.putBoolean("second", hole.equals("a"));
            card.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            BlockPos a = at(card, "a"), b = at(card, "b");
            if (a != null && b != null && volume(a, b) > MAX_VOLUME) {
                tell(player, "too_big");
                return InteractionResult.FAIL;
            }
            said = hole;
        }
        if (player != null) player.displayClientMessage(Component.translatable("item.fundamentals.card.punched." + said,
                clicked.getX(), clicked.getY(), clicked.getZ()).withStyle(ChatFormatting.GOLD), true);
        return InteractionResult.CONSUME;
    }

    public static long volume(BlockPos a, BlockPos b) {
        return (long) (Math.abs(a.getX() - b.getX()) + 1) * (Math.abs(a.getY() - b.getY()) + 1) * (Math.abs(a.getZ() - b.getZ()) + 1);
    }

    private static void tell(Player player, String key) {
        if (player != null) player.displayClientMessage(Component.translatable("item.fundamentals.card." + key).withStyle(ChatFormatting.RED), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String name = kind.name().toLowerCase(java.util.Locale.ROOT);
        for (String hole : kind == Kind.CARRY ? List.of("from", "to") : List.of("a", "b", "supply")) {
            BlockPos pos = at(stack, hole);
            tooltip.add(pos == null ? Component.translatable("item.fundamentals.card.unpunched." + hole).withStyle(ChatFormatting.DARK_GRAY)
                    : Component.translatable("item.fundamentals.card.hole." + hole, pos.getX(), pos.getY(), pos.getZ()).withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("item.fundamentals." + name + "_card.how").withStyle(ChatFormatting.DARK_GRAY));
    }
}
