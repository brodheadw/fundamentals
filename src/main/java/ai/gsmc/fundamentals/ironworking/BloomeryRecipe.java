package ai.gsmc.fundamentals.ironworking;

import ai.gsmc.fundamentals.Fundamentals;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

public record BloomeryRecipe(Ingredient ingredient, ItemStack result, ItemStack byproduct) implements Recipe<SingleRecipeInput> {

    public static final RecipeType<BloomeryRecipe> TYPE = RecipeType.simple(
            Fundamentals.id("bloomery"));
    public static final RecipeSerializer<BloomeryRecipe> SERIALIZER = new Serializer();

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    private static final class Serializer implements RecipeSerializer<BloomeryRecipe> {
        private static final MapCodec<BloomeryRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(BloomeryRecipe::ingredient),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(BloomeryRecipe::result),
                ItemStack.OPTIONAL_CODEC.optionalFieldOf("byproduct", ItemStack.EMPTY).forGetter(BloomeryRecipe::byproduct)
        ).apply(i, BloomeryRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, BloomeryRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, BloomeryRecipe::ingredient,
                ItemStack.STREAM_CODEC, BloomeryRecipe::result,
                ItemStack.OPTIONAL_STREAM_CODEC, BloomeryRecipe::byproduct,
                BloomeryRecipe::new);

        @Override
        public MapCodec<BloomeryRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BloomeryRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
