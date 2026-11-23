package ai.gsmc.fundamentals.power;

import ai.gsmc.fundamentals.plastics.Pigment;
import com.drmangotea.tfmg.content.electricity.base.IElectric;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.foundation.block.IBE;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * An electric lamp on The Factory's grid, fixed to a ceiling, wall or floor. Its light is the LIGHT property, which its block
 * entity drives as the lamp strikes, warms and runs. A fluorescent's light comes through a plastic diffuser, and the plastic's
 * colour (COLOR, dyed like any plastic) is the light's colour; a coloured diffuser passes less of it than a clear one.
 */
public class LampBlock extends FaceAttachedHorizontalDirectionalBlock implements IBE<LampBlockEntity> {
    public static final IntegerProperty LIGHT = BlockStateProperties.LEVEL;

    public static final EnumProperty<Pigment> COLOR = EnumProperty.create("color", Pigment.class);

    /** How a lamp comes on: a starter's flicker, an arc that warms from a first glow to full, or a filament that is simply lit. */
    public enum Start { STARTER, WARMS, FILAMENT }

    public enum Kind {
        /** One tube on a ballast behind a plastic diffuser: strikes after the starter's flicker. Ohms: about 36 W at 120 V. */
        BATTEN(400, box(6, 0, 0, 10, 3, 16), Start.STARTER, 15, 0, 0, 0, true),
        /** A recessed panel of two tubes behind a diffuser, for a ceiling of them. */
        TROFFER(200, box(0, 0, 0, 16, 3, 16), Start.STARTER, 15, 0, 0, 0, true),
        /** A tube of Wood's glass over a europium phosphor: near-ultraviolet, a dim violet to the eye that makes what fluoresces glow. */
        BLACKLIGHT(400, box(6, 0, 0, 10, 3, 16), Start.STARTER, 6, 0, 0, 0, false),
        /** High-pressure sodium: glows pink, then warms to orange over ten seconds; hot, it will not restrike till it cools. About 150 W. */
        SODIUM(96, box(3, 0, 2, 13, 5, 14), Start.WARMS, 15, 2, 200, 100, false),
        /** Low-pressure sodium: the neon in it glows red first, then the sodium's one pure yellow. The most light for the power, and the slowest to come up. */
        LOW_SODIUM(160, box(2, 0, 1, 14, 4, 15), Start.WARMS, 15, 1, 400, 0, false),
        /** Mercury vapour: a faint blue glow warming to a cold blue-white; a hot one waits longest of all to restrike. About 175 W. */
        MERCURY(82, box(3, 0, 2, 13, 5, 14), Start.WARMS, 14, 3, 120, 240, false),
        /** A tungsten filament in argon: lit the moment it is fed, and dimmer the lower the voltage. 60 W. */
        BULB(240, box(5, 0, 5, 11, 13, 11), Start.FILAMENT, 13, 0, 0, 0, false);

        public final float ohms;
        final VoxelShape floor;
        public final Start start;
        /** Full light, the first glow of an arc, the ticks it takes to warm, and the ticks a hot one must cool before it restrikes. */
        public final int full, glow, warm, cool;
        /** It has a plastic diffuser, so it can be dyed. */
        public final boolean tinted;

        Kind(float ohms, VoxelShape floor, Start start, int full, int glow, int warm, int cool, boolean tinted) {
            this.ohms = ohms;
            this.floor = floor;
            this.start = start;
            this.full = full;
            this.glow = glow;
            this.warm = warm;
            this.cool = cool;
            this.tinted = tinted;
        }
    }

    /** How much light a diffuser of this colour lets through, out of 15: dark and saturated plastics pass the least. */
    public static int passes(Pigment pigment) {
        return switch (pigment) {
            case NONE -> 15;
            case WHITE, YELLOW -> 14;
            case LIME, ORANGE, PINK -> 13;
            case LIGHT_GRAY, LIGHT_BLUE, CYAN -> 12;
            case MAGENTA, GREEN -> 11;
            case RED, PURPLE -> 10;
            case BLUE -> 9;
            case GRAY, BROWN -> 8;
            case BLACK -> 2;
        };
    }

    public final Kind kind;

    public LampBlock(Properties properties, Kind kind) {
        super(properties.lightLevel(state -> state.getValue(LIGHT)));
        this.kind = kind;
        registerDefaultState(stateDefinition.any().setValue(FACE, AttachFace.CEILING).setValue(FACING, Direction.NORTH).setValue(LIGHT, 0).setValue(COLOR, Pigment.NONE));
    }

    @Override
    protected MapCodec<? extends FaceAttachedHorizontalDirectionalBlock> codec() {
        return simpleCodec(properties -> new LampBlock(properties, kind));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING, LIGHT, COLOR);
    }

    /** It hangs from anything solid enough to screw into; the face-attached check would want a full face. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return true;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.oriented(kind.floor, state.getValue(FACE), state.getValue(FACING));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        if (!oldState.is(this)) withBlockEntityDo(level, pos, IElectric::onPlaced);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!newState.is(this)) IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public Class<LampBlockEntity> getBlockEntityClass() {
        return LampBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LampBlockEntity> getBlockEntityType() {
        return Electricity.lampEntity();
    }

    /** The floor shape turned to its face and facing, the way a lever's is. */
    static final class Shapes {
        static VoxelShape oriented(VoxelShape floor, AttachFace face, Direction facing) {
            var b = floor.bounds();
            double x0 = b.minX * 16, y0 = b.minY * 16, z0 = b.minZ * 16, x1 = b.maxX * 16, y1 = b.maxY * 16, z1 = b.maxZ * 16;
            boolean turned = facing.getAxis() == Direction.Axis.X;
            if (face == AttachFace.FLOOR) return turned ? box(z0, y0, x0, z1, y1, x1) : box(x0, y0, z0, x1, y1, z1);
            if (face == AttachFace.CEILING) return turned ? box(z0, 16 - y1, x0, z1, 16 - y0, x1) : box(x0, 16 - y1, z0, x1, 16 - y0, z1);
            // on a wall the long side runs along the wall, the depth out from it, and the width up it
            double along0 = z0, along1 = z1, up0 = 8 - (x1 - x0) / 2, up1 = 8 + (x1 - x0) / 2;
            return switch (facing) {
                case NORTH -> box(along0, up0, 16 - y1, along1, up1, 16 - y0);
                case SOUTH -> box(along0, up0, y0, along1, up1, y1);
                case WEST -> box(16 - y1, up0, along0, 16 - y0, up1, along1);
                default -> box(y0, up0, along0, y1, up1, along1);
            };
        }
    }
}
