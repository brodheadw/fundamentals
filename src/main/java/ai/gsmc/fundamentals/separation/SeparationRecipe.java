package ai.gsmc.fundamentals.separation;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.Optional;

/**
 * One cut of a mixer-settler battery: a liquor parts into its lighter and heavier rare earths over at least
 * {@code stages} stages, loaded with {@code organic} on top and stripped with {@code strip} at the far end.
 */
public record SeparationRecipe(Fluid liquor, Fluid organic, Fluid strip, int stages, Fluid light, Fluid heavy)
        implements Recipe<SeparationRecipe.Liquor> {

    public static final RecipeType<SeparationRecipe> TYPE = RecipeType.simple(
            ResourceLocation.fromNamespaceAndPath("fundamentals", "separation"));
    public static final RecipeSerializer<SeparationRecipe> SERIALIZER = new Serializer();

    public record Liquor(Fluid fluid) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return 0;
        }

        // The recipe manager skips inputs it thinks are empty, and it only knows about items.
        @Override
        public boolean isEmpty() {
            return fluid == Fluids.EMPTY;
        }
    }

    public static Optional<SeparationRecipe> forLiquor(Level level, Fluid liquor) {
        return level.getRecipeManager().getRecipeFor(TYPE, new Liquor(liquor), level).map(RecipeHolder::value);
    }

    @Override
    public boolean matches(Liquor input, Level level) {
        return input.fluid() == liquor;
    }

    @Override
    public ItemStack assemble(Liquor input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<?> getType() {
        return TYPE;
    }

    private static final class Serializer implements RecipeSerializer<SeparationRecipe> {
        private static final MapCodec<SeparationRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                BuiltInRegistries.FLUID.byNameCodec().fieldOf("liquor").forGetter(SeparationRecipe::liquor),
                BuiltInRegistries.FLUID.byNameCodec().fieldOf("organic").forGetter(SeparationRecipe::organic),
                BuiltInRegistries.FLUID.byNameCodec().fieldOf("strip").forGetter(SeparationRecipe::strip),
                Codec.intRange(1, 256).fieldOf("stages").forGetter(SeparationRecipe::stages),
                BuiltInRegistries.FLUID.byNameCodec().fieldOf("light").forGetter(SeparationRecipe::light),
                BuiltInRegistries.FLUID.byNameCodec().fieldOf("heavy").forGetter(SeparationRecipe::heavy)
        ).apply(i, SeparationRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, SeparationRecipe> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.registry(Registries.FLUID), SeparationRecipe::liquor,
                ByteBufCodecs.registry(Registries.FLUID), SeparationRecipe::organic,
                ByteBufCodecs.registry(Registries.FLUID), SeparationRecipe::strip,
                ByteBufCodecs.VAR_INT, SeparationRecipe::stages,
                ByteBufCodecs.registry(Registries.FLUID), SeparationRecipe::light,
                ByteBufCodecs.registry(Registries.FLUID), SeparationRecipe::heavy,
                SeparationRecipe::new);

        @Override
        public MapCodec<SeparationRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, SeparationRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
