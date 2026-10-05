package ai.gsmc.fundamentals.ironworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/** What is inside a {@link BloomeryBlock} and how far the burn has got. */
public class BloomeryBlockEntity extends BlockEntity {

    /** Ore lumps one firing can take. */
    public static final int CAPACITY = 4;
    /** One firing, in ticks. A real one took hours; this is a minute. */
    public static final int BURN_TICKS = 1200;

    private final List<ItemStack> ore = new ArrayList<>();
    private int charcoal;
    private int burned;
    private int blooms;

    public BloomeryBlockEntity(BlockPos pos, BlockState state) {
        super(IronWorking.bloomeryEntity(), pos, state);
    }

    private boolean lit() {
        return getBlockState().getValue(BloomeryBlock.LIT);
    }

    public int oreCount() {
        return ore.size();
    }

    public int charcoalCount() {
        return charcoal;
    }

    public boolean hasProducts() {
        return blooms > 0;
    }

    boolean addOre(ItemStack stack) {
        if (lit() || hasProducts() || ore.size() >= CAPACITY) {
            return false;
        }
        ore.add(stack.copyWithCount(1));
        setChanged();
        return true;
    }

    boolean addCharcoal() {
        if (lit() || hasProducts() || charcoal >= CAPACITY) {
            return false;
        }
        charcoal++;
        setChanged();
        return true;
    }

    /** Needs ore, and a lump of charcoal for each lump of ore. */
    boolean ignite() {
        if (lit() || hasProducts() || ore.isEmpty() || charcoal < ore.size() || level == null) {
            return false;
        }
        burned = 0;
        level.setBlock(worldPosition, getBlockState().setValue(BloomeryBlock.LIT, true), Block.UPDATE_ALL);
        setChanged();
        return true;
    }

    static void tick(Level level, BlockPos pos, BlockState state, BloomeryBlockEntity bloomery) {
        if (++bloomery.burned < BURN_TICKS) {
            return;
        }
        // Each lump of ore becomes one bloom and uses up one lump of charcoal.
        bloomery.blooms = bloomery.ore.size();
        bloomery.charcoal -= bloomery.ore.size();
        bloomery.ore.clear();
        bloomery.burned = 0;
        level.setBlock(pos, state.setValue(BloomeryBlock.LIT, false), Block.UPDATE_ALL);
        bloomery.setChanged();
    }

    /** Rakes the blooms out of the front, each with the slag that ran off it. */
    void takeProducts(BlockPos front) {
        if (level == null) {
            return;
        }
        Block.popResource(level, front, new ItemStack(IronWorking.ironBloom(), blooms));
        Block.popResource(level, front, new ItemStack(IronWorking.slag(), blooms));
        blooms = 0;
        setChanged();
    }

    void dropContents() {
        if (level == null) {
            return;
        }
        ore.forEach(stack -> Block.popResource(level, worldPosition, stack));
        if (charcoal > 0) {
            Block.popResource(level, worldPosition, new ItemStack(Items.CHARCOAL, charcoal));
        }
        if (blooms > 0) {
            takeProducts(worldPosition);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        ore.forEach(stack -> list.add(stack.save(registries)));
        tag.put("ore", list);
        tag.putInt("charcoal", charcoal);
        tag.putInt("burned", burned);
        tag.putInt("blooms", blooms);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ore.clear();
        for (net.minecraft.nbt.Tag entry : tag.getList("ore", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            ItemStack.parse(registries, entry).ifPresent(ore::add);
        }
        charcoal = tag.getInt("charcoal");
        burned = tag.getInt("burned");
        blooms = tag.getInt("blooms");
    }
}
