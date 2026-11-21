package ai.gsmc.fundamentals.survey;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.bombs.Fallout;
import ai.gsmc.fundamentals.content.nuclear.Doses;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * A Geiger-Müller tube: each particle that crosses it sets off an avalanche in the gas, heard as a click. Held, it clicks as fast
 * as the radiation around it, falling off as the square of the distance to each source, and says the rate in counts a minute.
 */
public class GeigerCounterItem extends Item {

    /** Cosmic rays and the rock's own traces: what a tube counts anywhere. */
    public static final double BACKGROUND = 20;
    /** Counts a minute per unit of Create Nuclear's dose one block from the tube. */
    static final double PER_DOSE = 600;
    /** Fresh fallout underfoot. */
    static final double FALLOUT = 30000;
    /** A small tube's counts a minute for a microsievert an hour. */
    static final double PER_MICROSIEVERT = 150;
    static final int RANGE = 8, EVERY = 10;
    // shortcut: rates kept by holder in memory; harmless since a rate is re-read every half second
    private static final Map<UUID, Double> RATE = new HashMap<>();
    private static final Map<Block, Double> BLOCK_DOSE = new HashMap<>();

    public GeigerCounterItem(Properties properties) {
        super(properties);
    }

    public static double dose(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        if (ModList.get().isLoaded("createnuclear")) return ai.gsmc.fundamentals.content.nuclear.Radiation.dose(stack);
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals(Fundamentals.MOD_ID) ? Doses.DOSE.getOrDefault(id.getPath(), 0.0) : 0;
    }

    private static double weight(Vec3 tube, Vec3 source) {
        return 1 / Math.max(1, tube.distanceToSqr(source));
    }

    /** The count rate at a place: ore in the rock, what lies in stores and on the ground or is carried, and fallout. */
    public static double countsPerMinute(ServerLevel level, Vec3 tube) {
        BlockPos at = BlockPos.containing(tube);
        double activity = 0;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-RANGE, -RANGE, -RANGE), at.offset(RANGE, RANGE, RANGE))) {
            var state = level.getBlockState(pos);
            if (state.isAir()) continue;
            double dose = BLOCK_DOSE.computeIfAbsent(state.getBlock(), block -> dose(new ItemStack(block)));
            if (dose > 0) activity += dose * weight(tube, Vec3.atCenterOf(pos));
        }
        for (int cx = (at.getX() - RANGE) >> 4; cx <= (at.getX() + RANGE) >> 4; cx++) {
            for (int cz = (at.getZ() - RANGE) >> 4; cz <= (at.getZ() + RANGE) >> 4; cz++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(cx, cz);
                if (chunk == null) continue;
                for (BlockEntity store : chunk.getBlockEntities().values()) {
                    if (!(store instanceof Container container) || !store.getBlockPos().closerThan(at, RANGE + 1)) continue;
                    double w = weight(tube, Vec3.atCenterOf(store.getBlockPos()));
                    for (int slot = 0; slot < container.getContainerSize(); slot++) {
                        ItemStack stack = container.getItem(slot);
                        activity += dose(stack) * stack.getCount() * w;
                    }
                }
            }
        }
        for (Entity entity : level.getEntities((Entity) null, new AABB(at).inflate(RANGE), entity -> entity instanceof ItemEntity || entity instanceof Player)) {
            double w = weight(tube, entity.position().add(0, entity.getBbHeight() / 2, 0));
            if (entity instanceof ItemEntity item) activity += dose(item.getItem()) * item.getItem().getCount() * w;
            else for (ItemStack stack : ((Player) entity).getInventory().items) activity += dose(stack) * stack.getCount() * w;
        }
        return BACKGROUND + PER_DOSE * activity + FALLOUT * Fallout.get(level).activity(at);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof Player player) || !(selected || player.getOffhandItem() == stack)) return;
        if (player.tickCount % EVERY == 0) {
            double cpm = countsPerMinute(server, player.position().add(0, 1, 0));
            RATE.put(player.getUUID(), cpm);
            if (player.tickCount % (EVERY * 2) == 0) player.displayClientMessage(reading(cpm), true);
        }
        double perTick = RATE.getOrDefault(player.getUUID(), BACKGROUND) / 1200;
        // past a few clicks a tick the tube is a continuous buzz; more sounds add nothing
        int clicks = Math.min(3, (int) perTick + (level.random.nextDouble() < perTick % 1 ? 1 : 0));
        for (int i = 0; i < clicks; i++) {
            level.playSound(null, player.getX(), player.getY() + 1, player.getZ(), SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.PLAYERS,
                    0.15F, 1.8F + level.random.nextFloat() * 0.4F);
        }
    }

    public static Component reading(double cpm) {
        double microsieverts = cpm / PER_MICROSIEVERT;
        String level = cpm < 100 ? "background" : microsieverts < 100 ? "elevated" : microsieverts < 10000 ? "dangerous" : "lethal";
        return Component.translatable("message.fundamentals.geiger_counter.reading", String.format(Locale.ROOT, "%,.0f", cpm),
                        String.format(Locale.ROOT, microsieverts < 10 ? "%.2f" : "%,.0f", microsieverts),
                        Component.translatable("message.fundamentals.geiger_counter." + level))
                .withStyle(level.equals("background") ? ChatFormatting.GREEN : level.equals("elevated") ? ChatFormatting.YELLOW : ChatFormatting.RED);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.fundamentals.geiger_counter").withStyle(ChatFormatting.GRAY));
    }
}
