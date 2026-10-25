package ai.gsmc.fundamentals.mixin.coldsweat;

import ai.gsmc.fundamentals.cold.Cold;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.momosoftworks.coldsweat.common.item.FilledWaterskinItem;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// Cold Sweat pours only its own filled waterskin over you; a filled vacuum flask pours the same
@Mixin(FilledWaterskinItem.class)
public abstract class FlaskPourMixin {
    @WrapOperation(method = "performPourAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/core/Holder;)Z"))
    private static boolean fundamentals$flaskPours(ItemStack stack, Holder<Item> item, Operation<Boolean> original) {
        return original.call(stack, item) || Cold.isFilledFlask(stack);
    }
}
