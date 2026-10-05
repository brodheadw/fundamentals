package ai.gsmc.fundamentals.registry;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.Locale;

/**
 * An ore block. Its {@link #GRADE} says how rich it is: an ore body is {@code core} at its heart,
 * {@code edge} around that, and {@code trace} where the mineral peters out. Grade picks the
 * texture and the drops; a block placed by hand is {@code edge}.
 */
public class OreBlock extends Block {

    public enum Grade implements StringRepresentable {
        CORE, EDGE, TRACE;

        /** The grade at a point of a deposit; {@code edge} runs 0 at the heart to 1 at the rim. */
        public static Grade at(double edge) {
            return edge < 0.4 ? CORE : edge < 0.8 ? EDGE : TRACE;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Grade> GRADE = EnumProperty.create("grade", Grade.class);

    public OreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(GRADE, Grade.EDGE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GRADE);
    }
}
