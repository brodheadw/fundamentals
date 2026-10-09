package ai.gsmc.fundamentals.plastics;

import com.drmangotea.tfmg.content.decoration.pipes.TFMGPipeBlock;
import com.drmangotea.tfmg.content.decoration.pipes.TFMGPipes;
import com.simibubi.create.content.fluids.pipes.FluidPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/** The Factory Must Grow's plastic pipe with a dye in it: one block, its colour a property. It is made by dyeing a placed
 * plastic pipe and comes back off as a plain one. */
public class DyedPlasticPipeBlock extends TFMGPipeBlock {

    public static final EnumProperty<DyeColor> COLOR = EnumProperty.create("color", DyeColor.class);

    public DyedPlasticPipeBlock(Properties properties) {
        super(properties, TFMGPipes.PipeMaterial.PLASTIC);
        registerDefaultState(defaultBlockState().setValue(COLOR, DyeColor.WHITE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COLOR);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("tfmg:plastic_pipe")));
    }

    @Override
    public BlockEntityType<? extends FluidPipeBlockEntity> getBlockEntityType() {
        return Plastics.dyedPipeEntity();
    }
}
