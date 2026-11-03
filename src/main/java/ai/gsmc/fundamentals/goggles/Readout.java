package ai.gsmc.fundamentals.goggles;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

/**
 * Machine readouts through the goggles in the manner of Create's boiler: a status line, then one bar per thing the
 * machine depends on, green where it is enough and red where it is what holds the machine back.
 */
public final class Readout {
    public static final int SEGMENTS = 18;

    private Readout() {}

    public static Component indent(Component text) {
        return Component.literal("    ").append(text);
    }

    /** "Bloomery: burning", the machine's name and what it is doing. */
    public static void status(List<Component> tooltip, Component machine, String state, Object... args) {
        tooltip.add(indent(Component.empty().append(machine.copy().withStyle(ChatFormatting.GRAY)).append(": ")
                .append(Component.translatable("goggles.fundamentals.readout.status." + state, args).withStyle(ChatFormatting.WHITE))));
    }

    /** One factor: its name, a bar of how much of it there is out of {@code max}, and the reading itself. */
    public static void bar(List<Component> tooltip, String factor, double value, double max, boolean enough, Object reading) {
        int full = max <= 0 ? 0 : (int) Math.round(Mth.clamp(value / max, 0, 1) * SEGMENTS);
        tooltip.add(indent(Component.empty()
                .append(Component.translatable("goggles.fundamentals.readout." + factor).withStyle(ChatFormatting.GRAY)).append(" ")
                .append(Component.literal("|".repeat(full)).withStyle(enough ? ChatFormatting.GREEN : ChatFormatting.RED))
                .append(Component.literal("|".repeat(SEGMENTS - full)).withStyle(ChatFormatting.DARK_GRAY)).append(" ")
                .append(reading instanceof Component c ? c : Component.literal(String.valueOf(reading)))));
    }

    /** What stops the machine, when something does. */
    public static void needs(List<Component> tooltip, String need, Object... args) {
        tooltip.add(indent(Component.translatable("goggles.fundamentals.readout.needs." + need, args).withStyle(ChatFormatting.GOLD)));
    }
}
