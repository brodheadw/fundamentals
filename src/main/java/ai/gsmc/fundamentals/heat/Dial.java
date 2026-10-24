package ai.gsmc.fundamentals.heat;

import net.minecraft.core.Direction;

/** A wall-mounted gauge whose needle sweeps a dial: thermometers and barometers. */
public interface Dial {
    Direction facing();

    /** Where the needle stands, 0 to 1 across the sweep. */
    float dial(float partialTick);
}
