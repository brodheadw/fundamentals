package ai.gsmc.fundamentals.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * An ore block. Two properties describe where it came from:
 *
 * <ul>
 *   <li>{@link #GRADE} — how rich it is. An ore body is {@code core} at its heart, {@code edge}
 *       around that, and {@code trace} where the mineral peters out. Grade picks how much
 *       mineral the texture shows and what the block drops.</li>
 *   <li>{@link #HOST} — the rock it formed in. The mineral is drawn over that rock's texture, so
 *       one ore block sits naturally in stone, deepslate, granite, a Create stone or one of
 *       our own rocks.</li>
 * </ul>
 *
 * A block placed by hand is {@code edge} grade in {@code stone}.
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

    /**
     * Rocks an ore can sit in. Must match HOSTS in {@code tools/build_ore_data.py}, which writes
     * the models for each. The Create stones are only ever picked when Create is installed.
     */
    public enum Host implements StringRepresentable {
        STONE("minecraft:stone"), DEEPSLATE("minecraft:deepslate"), GRANITE("minecraft:granite"),
        DIORITE("minecraft:diorite"), ANDESITE("minecraft:andesite"), TUFF("minecraft:tuff"),
        CALCITE("minecraft:calcite"), DRIPSTONE("minecraft:dripstone_block"), SAND("minecraft:sand"),
        CARBONATITE("fundamentals:carbonatite"), GABBRO("fundamentals:gabbro"), SYENITE("fundamentals:syenite"),
        LATERITE("fundamentals:laterite"),
        ASURINE("create:asurine"), CRIMSITE("create:crimsite"), OCHRUM("create:ochrum"),
        VERIDIUM("create:veridium"), LIMESTONE("create:limestone"), SCORIA("create:scoria"),
        SCORCHIA("create:scorchia");

        private static Map<Block, Host> byBlock;

        private final String block;

        Host(String block) {
            this.block = block;
        }

        public String block() {
            return block;
        }

        /** The host matching a rock; anything unlisted counts as stone, or deepslate at depth. */
        public static Host of(BlockState rock) {
            if (byBlock == null) {
                byBlock = new HashMap<>();
                for (Host host : values()) {
                    BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(host.block))
                            .ifPresent(block -> byBlock.put(block, host));
                }
            }
            Host host = byBlock.get(rock.getBlock());
            if (host != null) {
                return host;
            }
            return rock.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES) ? DEEPSLATE : STONE;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public static final EnumProperty<Grade> GRADE = EnumProperty.create("grade", Grade.class);
    public static final EnumProperty<Host> HOST = EnumProperty.create("host", Host.class);

    public OreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(GRADE, Grade.EDGE).setValue(HOST, Host.STONE));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GRADE, HOST);
    }
}
