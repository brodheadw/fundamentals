package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.oxidation.Oxidation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = InvWrapper.class, remap = false)
public abstract class InvWrapperAgeingMixin {

    @Inject(method = "extractItem", at = @At("HEAD"))
    private void fundamentals$age(int slot, int amount, boolean simulate, CallbackInfoReturnable<ItemStack> cir) {
        Oxidation.taking(((InvWrapper) (Object) this).getInv());
    }
}
