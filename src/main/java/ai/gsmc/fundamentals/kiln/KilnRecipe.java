package ai.gsmc.fundamentals.kiln;

import ai.gsmc.fundamentals.Fundamentals;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.List;

/** One item roasted in a rotary kiln: it must spend {@code ticks} in parts of the drum at least {@code celsius} hot. */
public record KilnRecipe(Ingredient ingredient, List<ItemStack> results, int celsius, int ticks) implements Recipe<SingleRecipeInput> {

    public static final RecipeType<KilnRecipe> TYPE = RecipeType.simple(Fundamentals.id("kiln"));
    public static final RecipeSerializer<KilnRecipe> SERIALIZER = new Serializer();

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return results.getFirst().copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return results.getFirst();
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    private static final class Serializer implements RecipeSerializer<KilnRecipe> {
        private static final MapCodec<KilnRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(KilnRecipe::ingredient),
                ItemStack.STRICT_CODEC.listOf(1, 4).fieldOf("results").forGetter(KilnRecipe::results),
                Codec.intRange(0, 3000).fieldOf("celsius").forGetter(KilnRecipe::celsius),
                Codec.intRange(1, 6000).fieldOf("ticks").forGetter(KilnRecipe::ticks)
        ).apply(i, KilnRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, KilnRecipe> STREAM_CODEC = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, KilnRecipe::ingredient,
                ItemStack.STREAM_CODEC.apply(ByteBufCodecs.list(4)), KilnRecipe::results,
                ByteBufCodecs.VAR_INT, KilnRecipe::celsius,
                ByteBufCodecs.VAR_INT, KilnRecipe::ticks,
                KilnRecipe::new);

        @Override
        public MapCodec<KilnRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, KilnRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
