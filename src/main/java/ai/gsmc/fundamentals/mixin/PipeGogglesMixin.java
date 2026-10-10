package ai.gsmc.fundamentals.mixin;

import ai.gsmc.fundamentals.liquid.Liquids;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.content.fluids.FluidTransportBehaviour;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pipes.SmartFluidPipeBlockEntity;
import com.simibubi.create.content.fluids.pipes.StraightPipeBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;

import java.util.List;

@Mixin(value = {FluidPipeBlockEntity.class, StraightPipeBlockEntity.class, SmartFluidPipeBlockEntity.class}, remap = false)
public abstract class PipeGogglesMixin implements IHaveGoggleInformation {

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        BlockEntity self = (BlockEntity) (Object) this;
        return Liquids.describe(self.getLevel(), self.getBlockPos(), Liquids.carried(BlockEntityBehaviour.get(self, FluidTransportBehaviour.TYPE)), tooltip, true);
    }
}
