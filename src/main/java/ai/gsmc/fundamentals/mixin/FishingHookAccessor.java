package ai.gsmc.fundamentals.mixin;

import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FishingHook.class)
public interface FishingHookAccessor {
    @Accessor("luck")
    int fundamentals$luck();

    @Mutable
    @Accessor("luck")
    void fundamentals$setLuck(int luck);
}
