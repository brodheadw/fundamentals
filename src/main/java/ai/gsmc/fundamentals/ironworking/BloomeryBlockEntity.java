package ai.gsmc.fundamentals.ironworking;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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

public class BloomeryBlockEntity extends BlockEntity {

    public static final int CAPACITY = 4;
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

    public static Optional<RecipeHolder<BloomeryRecipe>> recipeFor(Level level, ItemStack stack) {
        return level.getRecipeManager().getRecipeFor(BloomeryRecipe.TYPE, new SingleRecipeInput(stack), level);
    }

    boolean addOre(ItemStack stack) {
        if (lit() || hasProducts() || ore.size() >= CAPACITY || recipeFor(level, stack).isEmpty()) {
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

    boolean ignite() {
        if (lit() || hasProducts() || ore.isEmpty() || charcoal < ore.size()) {
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

    void takeProducts(BlockPos front) {
        products.forEach(stack -> Block.popResource(level, front, stack));
        products.clear();
        setChanged();
    }

    void dropContents() {
        ore.forEach(stack -> Block.popResource(level, worldPosition, stack));
        if (charcoal > 0) {
            Block.popResource(level, worldPosition, new ItemStack(Items.CHARCOAL, charcoal));
        }
        takeProducts(worldPosition);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("ore", save(ore, registries));
        tag.put("products", save(products, registries));
        tag.putInt("charcoal", charcoal);
        tag.putInt("burned", burned);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        load(ore, tag.getList("ore", Tag.TAG_COMPOUND), registries);
        load(products, tag.getList("products", Tag.TAG_COMPOUND), registries);
        charcoal = tag.getInt("charcoal");
        burned = tag.getInt("burned");
    }

    private static ListTag save(List<ItemStack> stacks, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        stacks.forEach(stack -> list.add(stack.save(registries)));
        return list;
    }

    private static void load(List<ItemStack> stacks, ListTag list, HolderLookup.Provider registries) {
        stacks.clear();
        list.forEach(entry -> ItemStack.parse(registries, entry).ifPresent(stacks::add));
    }
}
