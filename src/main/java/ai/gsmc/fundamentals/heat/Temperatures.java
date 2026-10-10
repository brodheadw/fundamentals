package ai.gsmc.fundamentals.heat;

import com.mojang.brigadier.Command;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Fundamentals' side of the temperature field, which Fundamentals: Principles keeps ({@link Heat}): the heat of Create's
 * blaze burner by its level, and {@code /heat}. The sources this mod adds are in its {@code fundamentalmagic:heat_source} data map.
 */
public final class Temperatures {

    private Temperatures() {}

    public static void setup(FMLCommonSetupEvent event) {
        // the burner's level is blockstate, which a data map cannot see; kindled is 1,000 °C, seething 1,600
        event.enqueueWork(() -> Heat.registerScaler(AllBlocks.BLAZE_BURNER.get(), state -> switch (BlazeBurnerBlock.getHeatLevelOf(state)) {
            case NONE -> 0.0; case SMOULDERING -> 0.25; case FADING -> 0.5; case KINDLED -> 1.0; case SEETHING -> 1.6; }));
    }

    public static Component readout(Level level, BlockPos pos) {
        double c = Heat.at(level, pos);
        return Component.translatable("heat.fundamentals.readout", String.format("%.0f", c), String.format("%.0f", Heat.fahrenheit(c)));
    }

    /** {@code /heat}: the temperature where you stand. */
    public static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("heat").executes(ctx -> {
            var source = ctx.getSource();
            source.sendSuccess(() -> readout(source.getLevel(), BlockPos.containing(source.getPosition())), false);
            return Command.SINGLE_SUCCESS;
        }));
    }
}
