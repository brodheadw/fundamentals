package ai.gsmc.fundamentals.compute;

import ai.gsmc.fundamentals.Fundamentals;
import com.wildspell.fundamental.api.heat.Heat;
import dan200.computercraft.shared.computer.blocks.AbstractComputerBlockEntity;
import dan200.computercraft.shared.computer.core.ComputerFamily;
import dan200.computercraft.shared.computer.core.ServerComputer;
import dan200.computercraft.shared.turtle.blocks.TurtleBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/** What keeps a placed computer running: a live power supply beside it, and air cool enough for its chips. */
public final class Power {

    public static final int MIN_VOLTS = 50;
    /** Junction limit: past this the computer trips its thermal cutout. */
    public static final double CUTOUT_CELSIUS = 85;
    /** How much a running computer warms the air around it; an advanced one runs hotter. */
    public static final double WARMTH = 3, ADVANCED_WARMTH = 5;

    private Power() {}

    /** Why a computer here can't run, or null when it can. */
    public static String fault(Level level, BlockPos pos) {
        if (supply(level, pos) == null) return "no_power";
        if (Heat.at(level, pos) > CUTOUT_CELSIUS) return "overheated";
        return null;
    }

    public static PowerSupplyBlockEntity supply(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockEntity(pos.relative(direction)) instanceof PowerSupplyBlockEntity supply && supply.live()) return supply;
        }
        return null;
    }

    public static void tick(AbstractComputerBlockEntity computer) {
        Level level = computer.getLevel();
        ServerComputer running = computer.getServerComputer();
        if (level == null || level.isClientSide || running == null || !running.isOn() || computer instanceof TurtleBlockEntity
                || computer.getFamily() == ComputerFamily.COMMAND) return;
        String fault = fault(level, computer.getBlockPos());
        if (fault == null) {
            if (level.getGameTime() % 20 == 0) {
                Heat.boost(level, computer.getBlockPos(), computer.getFamily() == ComputerFamily.ADVANCED ? ADVANCED_WARMTH : WARMTH, 1, 40);
            }
            return;
        }
        running.shutdown();
        Component message = Component.translatable("message." + Fundamentals.MOD_ID + "." + fault).withStyle(ChatFormatting.RED);
        level.players().stream().filter(player -> player.blockPosition().closerThan(computer.getBlockPos(), 8))
                .forEach(player -> player.displayClientMessage(message, true));
    }
}
