package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.elements.PeriodicTable;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.TreeNodePosition;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The server lays every tab out as a tree and sends the positions to clients; the periodic table is pinned to its grid right after. */
@Mixin(TreeNodePosition.class)
public abstract class TreeNodePositionMixin {

    @Inject(method = "run", at = @At("TAIL"))
    private static void fundamentals$periodicTable(AdvancementNode rootNode, CallbackInfo ci) {
        PeriodicTable.layout(rootNode);
    }
}
