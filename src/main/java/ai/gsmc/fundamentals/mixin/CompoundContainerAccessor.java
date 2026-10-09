package ai.gsmc.fundamentals.mixin;

import net.minecraft.world.CompoundContainer;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** The two chests behind a double chest, so each half keeps its own oxidation clock. */
@Mixin(CompoundContainer.class)
public interface CompoundContainerAccessor {

    @Accessor("container1")
    Container fundamentals$first();

    @Accessor("container2")
    Container fundamentals$second();
}
