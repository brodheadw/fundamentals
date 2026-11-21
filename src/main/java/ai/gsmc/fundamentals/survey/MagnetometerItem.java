package ai.gsmc.fundamentals.survey;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.registry.OreBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A fluxgate magnetometer, as prospectors and submarine hunters have carried since the 1940s: a permalloy core driven in and out
 * of saturation, whose second harmonic is the field along it. It reads the Earth's field and the anomaly of magnetic rock
 * nearby, which falls off as the cube of the distance, and points to where the anomaly comes from.
 */
public class MagnetometerItem extends Item {

    /** The Earth's field at middle latitudes. */
    public static final double EARTH = 50000;
    /** Smaller anomalies than this are lost in the day's drift. */
    static final double NOISE = 5;
    static final int RANGE = 12, EVERY = 20;
    /** A block's anomaly at one block, in nanotesla: magnetite's induced field, a lodestone's own, worked iron's. */
    static final double MAGNETITE = 20000, LODESTONE = 200000, IRON = 20000, HEMATITE = 50, ILMENITE = 500;
    private static Map<Block, Double> moments;

    public MagnetometerItem(Properties properties) {
        super(properties);
    }

    public record Reading(double anomaly, Vec3 toward, double nearest) {
        public double total() { return EARTH + anomaly; }
    }

    private static double moment(BlockState state) {
        if (moments == null) {
            moments = Map.of(Blocks.LODESTONE, LODESTONE, Blocks.IRON_BLOCK, IRON, Blocks.RAW_IRON_BLOCK, IRON,
                    BuiltInRegistries.BLOCK.get(Fundamentals.id("magnetite_ore")), MAGNETITE,
                    BuiltInRegistries.BLOCK.get(Fundamentals.id("hematite_ore")), HEMATITE,
                    BuiltInRegistries.BLOCK.get(Fundamentals.id("ilmenite_ore")), ILMENITE);
        }
        double moment = moments.getOrDefault(state.getBlock(), 0.0);
        if (moment == 0 || !state.hasProperty(OreBlock.GRADE)) return moment;
        return moment * switch (state.getValue(OreBlock.GRADE)) { case CORE -> 1.0; case EDGE -> 0.5; case TRACE -> 0.1; };
    }

    /** The anomaly at a place, which way its pull lies, and the distance to the strongest single source. */
    public static Reading read(ServerLevel level, Vec3 probe) {
        BlockPos at = BlockPos.containing(probe);
        double anomaly = 0, strongest = 0, nearest = 0;
        Vec3 toward = Vec3.ZERO;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-RANGE, -RANGE, -RANGE), at.offset(RANGE, RANGE, RANGE))) {
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            double moment = moment(state);
            if (moment == 0) continue;
            Vec3 to = Vec3.atCenterOf(pos).subtract(probe);
            double d = Math.max(1, to.length()), b = moment / (d * d * d);
            anomaly += b;
            toward = toward.add(to.normalize().scale(b));
            if (b > strongest) { strongest = b; nearest = d; }
        }
        return new Reading(anomaly, toward, nearest);
    }

    public static Component reading(Reading reading) {
        String key = "message.fundamentals.magnetometer.";
        String total = String.format(Locale.ROOT, "%,.0f", reading.total());
        if (reading.anomaly() < NOISE) return Component.translatable(key + "quiet", total, RANGE).withStyle(ChatFormatting.GRAY);
        Vec3 v = reading.toward();
        double dip = Math.toDegrees(Math.atan2(-v.y, Math.hypot(v.x, v.z)));
        return Component.translatable(key + "anomaly", total, String.format(Locale.ROOT, "%,.0f", reading.anomaly()),
                        Survey.bearing(v.x, v.z), String.format(Locale.ROOT, "%.0f", dip), String.format(Locale.ROOT, "%.0f", reading.nearest()))
                .withStyle(reading.anomaly() > 1000 ? ChatFormatting.GOLD : ChatFormatting.YELLOW);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player player) || !(selected || player.getOffhandItem() == stack)) return;
        if (player.tickCount % EVERY == 0) player.displayClientMessage(reading(read(server, player.position().add(0, 1, 0))), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fundamentals.magnetometer").withStyle(ChatFormatting.GRAY));
    }
}
