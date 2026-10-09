package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.oxidation.Oxidation;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** A hopper or pipe drawing from a shulker box ages what is in it first. */
@Mixin(value = SidedInvWrapper.class, remap = false)
public abstract class SidedInvWrapperAgeingMixin {

    @Shadow
    @Final
    protected WorldlyContainer inv;

    @Inject(method = "extractItem", at = @At("HEAD"))
    private void fundamentals$age(int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        Oxidation.taking(inv);
    }
}
