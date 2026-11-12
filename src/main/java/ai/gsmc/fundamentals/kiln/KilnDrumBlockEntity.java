package ai.gsmc.fundamentals.kiln;

import com.simibubi.create.content.kinetics.base.HorizontalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * A length of kiln drum. Lengths in a row along one axis are one kiln; the end with a blaze burner under it is the hot end, and
 * the turning drum walks its charge from the far end down to it, as a real kiln's tilt does. The burner heats the end it stands
 * under and the gas cools as it runs up the tube, so each length is {@link #DROP} degrees cooler than the one below it. A charge
 * roasts while it lies in lengths hot enough for its recipe, and the turn sets how long it lies there: a slow kiln roasts more.
 */
public class KilnDrumBlockEntity extends KineticBlockEntity {

    /** Items one length holds: a kiln runs a tenth full. */
    public static final int CAPACITY = 8;
    /** Degrees the gas loses for each length it climbs from the burner. */
    public static final int DROP = 150;
    /** Lengths in one kiln, at most. */
    public static final int LONGEST = 16;
    /** A charge passes one length in TRANSIT / rpm ticks: 80 at 32 RPM. */
    public static final float TRANSIT = 2560;

    private final List<Charge> charges = new ArrayList<>();

    private static final class Charge {
        ItemStack stack;
        float travel;
        int heat;

        Charge(ItemStack stack) {
            this.stack = stack;
        }
    }

    /** The kiln this length is in: the hot end, which way the charge walks toward it, and how far this length lies from it. */
    record Line(BlockPos hot, Direction flow, int distance, BlockPos feed, int celsius) {}

    public KilnDrumBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    Line line() {
        if (level == null) return null;
        Direction.Axis axis = getBlockState().getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS);
        Direction up = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
        BlockPos low = end(up.getOpposite(), axis), high = end(up, axis);
        int lowHeat = burner(low), highHeat = burner(high);
        if (lowHeat <= 0 && highHeat <= 0) return null;
        boolean towardLow = lowHeat >= highHeat;
        BlockPos hot = towardLow ? low : high;
        Direction flow = towardLow ? up.getOpposite() : up;
        int distance = Math.abs(axis.choose(hot.getX(), hot.getY(), hot.getZ()) - axis.choose(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()));
        return new Line(hot, flow, distance, towardLow ? high : low, Math.max(0, (towardLow ? lowHeat : highHeat) - DROP * distance));
    }

    private BlockPos end(Direction toward, Direction.Axis axis) {
        BlockPos pos = worldPosition;
        for (int i = 1; i < LONGEST; i++) {
            BlockPos next = pos.relative(toward);
            BlockState state = level.getBlockState(next);
            if (!(state.getBlock() instanceof KilnDrumBlock) || state.getValue(HorizontalAxisKineticBlock.HORIZONTAL_AXIS) != axis) break;
            pos = next;
        }
        return pos;
    }

    // shortcut: only Create's blaze burner fires a kiln; a TFMG firebox or a lava pit under it would want their own heat
    private int burner(BlockPos end) {
        BlockState below = level.getBlockState(end.below());
        if (!(below.getBlock() instanceof BlazeBurnerBlock)) return 0;
        return switch (BlazeBurnerBlock.getHeatLevelOf(below)) {
            case NONE -> 0;
            case SMOULDERING -> 250;
            case FADING -> 500;
            case KINDLED -> 1000;
            case SEETHING -> 1600;
        };
    }

    private int load() {
        return charges.stream().mapToInt(c -> c.stack.getCount()).sum();
    }

    /** Takes what fits at the feed end; anywhere else the drum is closed. */
    public ItemStack insert(ItemStack stack, boolean simulate) {
        Line line = line();
        if (stack.isEmpty() || line == null || !line.feed().equals(worldPosition)) return stack;
        int room = Math.min(CAPACITY - load(), stack.getCount());
        if (room <= 0) return stack;
        if (!simulate) {
            charges.add(new Charge(stack.copyWithCount(room)));
            sendData();
        }
        return stack.copyWithCount(stack.getCount() - room);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide || charges.isEmpty()) return;
        Line line = line();
        float rpm = isOverStressed() ? 0 : Math.abs(getSpeed());
        if (line == null || rpm == 0) return;
        boolean changed = false;
        for (int i = 0; i < charges.size(); i++) {
            Charge charge = charges.get(i);
            RecipeHolder<KilnRecipe> recipe = level.getRecipeManager()
                    .getRecipeFor(KilnRecipe.TYPE, new SingleRecipeInput(charge.stack), level).orElse(null);
            if (recipe != null && line.celsius() >= recipe.value().celsius() && ++charge.heat >= recipe.value().ticks()) {
                List<ItemStack> results = recipe.value().results();
                int count = charge.stack.getCount();
                charge.stack = results.getFirst().copyWithCount(results.getFirst().getCount() * count);
                charge.heat = 0;
                for (ItemStack more : results.subList(1, results.size())) {
                    Charge made = new Charge(more.copyWithCount(more.getCount() * count));
                    made.travel = charge.travel;
                    charges.add(made);
                }
                changed = true;
            }
            charge.travel = Math.min(1, charge.travel + rpm / TRANSIT);
        }
        for (var it = charges.iterator(); it.hasNext(); ) {
            Charge charge = it.next();
            if (charge.travel < 1) continue;
            if (line.distance() == 0) {
                discharge(charge.stack, line.flow());
                it.remove();
                changed = true;
            } else if (level.getBlockEntity(worldPosition.relative(line.flow())) instanceof KilnDrumBlockEntity next
                    && next.load() + charge.stack.getCount() <= CAPACITY) {
                charge.travel = 0;
                next.charges.add(charge);
                next.sendData();
                it.remove();
                changed = true;
            }
        }
        if (changed) sendData();
    }

    /** Out of the hot end: into whatever stands against it, or onto the ground. */
    private void discharge(ItemStack stack, Direction flow) {
        BlockPos out = worldPosition.relative(flow);
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, out, flow.getOpposite());
        ItemStack left = handler == null ? stack : ItemHandlerHelper.insertItem(handler, stack, false);
        if (left.isEmpty()) return;
        Vec3 at = Vec3.atCenterOf(worldPosition).add(Vec3.atLowerCornerOf(flow.getNormal()).scale(0.75));
        ItemEntity item = new ItemEntity(level, at.x, at.y, at.z, left);
        item.setDeltaMovement(Vec3.atLowerCornerOf(flow.getNormal()).scale(0.1));
        level.addFreshEntity(item);
    }

    /** Funnels, chutes and hoppers feed the kiln at its feed end; nothing is taken out but by the turn. */
    public static IItemHandler handler(KilnDrumBlockEntity drum, Direction side) {
        return new IItemHandler() {
            @Override public int getSlots() { return 1; }
            @Override public ItemStack getStackInSlot(int slot) { return ItemStack.EMPTY; }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return drum.insert(stack, simulate); }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
            @Override public int getSlotLimit(int slot) { return CAPACITY; }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return true; }
        };
    }

    public List<ItemStack> contents() {
        return charges.stream().map(c -> c.stack).toList();
    }

    @Override
    public void destroy() {
        super.destroy();
        for (Charge charge : charges) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), charge.stack);
        charges.clear();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        ListTag list = new ListTag();
        for (Charge charge : charges) {
            CompoundTag entry = new CompoundTag();
            entry.put("Item", charge.stack.save(registries));
            entry.putFloat("Travel", charge.travel);
            entry.putInt("Heat", charge.heat);
            list.add(entry);
        }
        tag.put("Charges", list);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        charges.clear();
        for (Tag entry : tag.getList("Charges", Tag.TAG_COMPOUND)) {
            CompoundTag charge = (CompoundTag) entry;
            ItemStack stack = ItemStack.parseOptional(registries, charge.getCompound("Item"));
            if (stack.isEmpty()) continue;
            Charge loaded = new Charge(stack);
            loaded.travel = charge.getFloat("Travel");
            loaded.heat = charge.getInt("Heat");
            charges.add(loaded);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        Line line = line();
        tooltip.add(Component.literal("    ").append(line == null
                ? Component.translatable("kiln.fundamentals.unfired").withStyle(ChatFormatting.GRAY)
                : Component.translatable("kiln.fundamentals.celsius", line.celsius()).withStyle(ChatFormatting.GOLD)));
        for (ItemStack stack : contents()) {
            tooltip.add(Component.literal("    ").append(Component.translatable("kiln.fundamentals.holds", stack.getCount(), stack.getHoverName()))
                    .withStyle(ChatFormatting.GRAY));
        }
        return true;
    }
}
