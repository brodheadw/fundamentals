package ai.gsmc.fundamentals.mixin.coldsweat;

import ai.gsmc.fundamentals.cold.Cold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.momosoftworks.coldsweat.common.blockentity.BoilerBlockEntity;
import com.momosoftworks.coldsweat.common.blockentity.IceboxBlockEntity;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// the boiler heats and the icebox chills only Cold Sweat's own filled waterskin; a filled vacuum flask too
@Mixin({BoilerBlockEntity.class, IceboxBlockEntity.class})
public abstract class FlaskHearthMixin {
    @WrapOperation(method = {"tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V", "checkForItems"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/core/Holder;)Z"))
    private boolean fundamentals$flaskWarms(ItemStack stack, Holder<Item> item, Operation<Boolean> original) {
        return original.call(stack, item) || Cold.isFilledFlask(stack);
    }
}
