package ai.gsmc.fundamentals.survey;

import ai.gsmc.fundamentals.heat.ThermometerBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

/**
 * The ground station's console, worked by what is in hand: paper takes a picture, a spyglass zooms, a filled map sets the target and
 * a compass sets it back overhead, a clock sets the recording. An empty hand reads it out, with the player's satellites; sneaking,
 * it tasks the next of them.
 */
public class GroundStationBlock extends SurveyBlock {

    public GroundStationBlock(Properties properties) {
        super(properties, Survey::groundStationEntity, GroundStationBlockEntity::new, false);
    }

    /** The console faces whoever sets it down. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(ThermometerBlock.FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        boolean paper = stack.is(Items.PAPER), spyglass = stack.is(Items.SPYGLASS), map = stack.is(Items.FILLED_MAP),
                compass = stack.is(Items.COMPASS), clock = stack.is(Items.CLOCK);
        if (!(paper || spyglass || map || compass || clock)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof GroundStationBlockEntity station)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        String key = "message.fundamentals.ground_station.";
        if (paper) {
            ItemStack frame = station.image(server);
            if (frame.isEmpty()) {
                player.displayClientMessage(Component.translatable(key + station.status(), Satellites.VIEW).withStyle(ChatFormatting.GRAY), true);
                return ItemInteractionResult.FAIL;
            }
            stack.consume(1, player);
            if (!player.getInventory().add(frame)) player.drop(frame, false);
            level.playSound(null, pos, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, SoundSource.BLOCKS, 1.0F, 0.8F);
        } else if (spyglass) {
            station.zoom();
            player.displayClientMessage(Component.translatable(key + "zoom", 128 << station.scale(), station.scale()), true);
        } else if (map) {
            var data = MapItem.getSavedData(stack, level);
            if (data == null) return ItemInteractionResult.FAIL;
            station.target(data.centerX, data.centerZ);
            player.displayClientMessage(Component.translatable(key + "target", station.targetX(), station.targetZ()), true);
        } else if (compass) {
            station.overhead();
            player.displayClientMessage(Component.translatable(key + "overhead"), true);
        } else {
            station.cycleRecording();
            player.displayClientMessage(station.interval() == 0 ? Component.translatable(key + "stopped")
                    : Component.translatable(key + "recording", station.interval()), true);
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level instanceof ServerLevel server) || !(level.getBlockEntity(pos) instanceof GroundStationBlockEntity station)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        String key = "message.fundamentals.ground_station.";
        List<Satellites.Satellite> owned = Satellites.get(server.getServer()).owned(player.getUUID());
        if (owned.isEmpty()) {
            player.sendSystemMessage(Component.translatable(key + "no_satellites").withStyle(ChatFormatting.GRAY));
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            station.cycle(server, player).ifPresent(s -> player.displayClientMessage(
                    Component.translatable(key + "tasked", s.label(), s.x(), s.z()).withStyle(ChatFormatting.AQUA), true));
            return InteractionResult.SUCCESS;
        }
        int tasked = station.satellite(server).map(Satellites.Satellite::id).orElse(-1);
        for (Satellites.Satellite s : owned) {
            player.sendSystemMessage(Component.translatable(key + "satellite", s.label(), s.x(), s.z(),
                    Math.round(Math.hypot(station.targetX() - s.x(), station.targetZ() - s.z())))
                    .withStyle(s.id() == tasked ? ChatFormatting.AQUA : ChatFormatting.GRAY));
        }
        return super.useWithoutItem(state, level, pos, player, hit);
    }
}
