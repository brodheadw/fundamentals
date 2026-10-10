package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.elements.PeriodicTable;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.advancements.AdvancementWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AdvancementWidget.class)
public abstract class AdvancementWidgetMixin {

    @Shadow @Final private AdvancementNode advancementNode;

    @Inject(method = "drawConnectivity", at = @At("HEAD"), cancellable = true)
    private void fundamentals$noLines(GuiGraphics guiGraphics, int x, int y, boolean dropShadow, CallbackInfo ci) {
        if (PeriodicTable.isTable(advancementNode)) {
            ci.cancel();
        }
    }
}
