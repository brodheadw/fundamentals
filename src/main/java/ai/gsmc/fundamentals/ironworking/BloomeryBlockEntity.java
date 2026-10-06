package ai.gsmc.fundamentals.ironworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** What is inside a {@link BloomeryBlock}, how far the burn has got, and what it has made. */
public class BloomeryBlockEntity extends BlockEntity {

    /** Ore lumps one firing can take. */
    public static final int CAPACITY = 4;
    /** One firing, in ticks. A real one took hours; this is a minute. */
    public static final int BURN_TICKS = 1200;

    private final List<ItemStack> ore = new ArrayList<>();
    private final List<ItemStack> products = new ArrayList<>();
    private int charcoal;
    private int burned;

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
        return !products.isEmpty();
    }

    /** The bloomery recipe for a lump, if there is one. */
    public static Optional<RecipeHolder<BloomeryRecipe>> recipeFor(Level level, ItemStack stack) {
        return level.getRecipeManager().getRecipeFor(BloomeryRecipe.TYPE, new SingleRecipeInput(stack), level);
    }

    boolean addOre(ItemStack stack) {
        if (lit() || hasProducts() || ore.size() >= CAPACITY || level == null || recipeFor(level, stack).isEmpty()) {
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
        // Each lump becomes what its recipe says, uses up one lump of charcoal, and leaves its slag.
        for (ItemStack lump : bloomery.ore) {
            recipeFor(level, lump).ifPresent(recipe -> {
                bloomery.products.add(recipe.value().result().copy());
                if (!recipe.value().byproduct().isEmpty()) {
                    bloomery.products.add(recipe.value().byproduct().copy());
                }
            });
        }
        bloomery.charcoal -= bloomery.ore.size();
        bloomery.ore.clear();
        bloomery.burned = 0;
        level.setBlock(pos, state.setValue(BloomeryBlock.LIT, false), Block.UPDATE_ALL);
        bloomery.setChanged();
    }

    /** Rakes everything out of the front. */
    void takeProducts(BlockPos front) {
        if (level == null) {
            return;
        }
        products.forEach(stack -> Block.popResource(level, front, stack));
        products.clear();
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
        takeProducts(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        ore.forEach(stack -> list.add(stack.save(registries)));
        tag.put("ore", list);
        net.minecraft.nbt.ListTag made = new net.minecraft.nbt.ListTag();
        products.forEach(stack -> made.add(stack.save(registries)));
        tag.put("products", made);
        tag.putInt("charcoal", charcoal);
        tag.putInt("burned", burned);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ore.clear();
        for (net.minecraft.nbt.Tag entry : tag.getList("ore", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            ItemStack.parse(registries, entry).ifPresent(ore::add);
        }
        products.clear();
        for (net.minecraft.nbt.Tag entry : tag.getList("products", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            ItemStack.parse(registries, entry).ifPresent(products::add);
        }
        charcoal = tag.getInt("charcoal");
        burned = tag.getInt("burned");
    }
}
