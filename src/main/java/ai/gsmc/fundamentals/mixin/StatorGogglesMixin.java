package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.magnet.MagnetBehaviour;
import com.drmangotea.tfmg.content.electricity.generators.large_generator.StatorBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(value = StatorBlockEntity.class, remap = false)
public abstract class StatorGogglesMixin {

    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        MagnetBehaviour magnet = BlockEntityBehaviour.get((BlockEntity) (Object) this, MagnetBehaviour.TYPE);
        if (magnet == null) {
            return false;
        }
        magnet.addToGoggleTooltip(tooltip);
        return true;
    }
}
