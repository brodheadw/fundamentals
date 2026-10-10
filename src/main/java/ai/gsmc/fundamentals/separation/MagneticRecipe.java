package ai.gsmc.fundamentals.separation;

import ai.gsmc.fundamentals.Fundamentals;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;

import java.util.Optional;

public record MagneticRecipe(Fluid liquor, Fluid light, Fluid heavy, float lightFraction, Fluid attracted, int passes, float curieShare)
        implements Recipe<SeparationRecipe.Liquor> {

    public static final RecipeType<MagneticRecipe> TYPE = RecipeType.simple(
            Fundamentals.id("magnetic"));
    public static final RecipeSerializer<MagneticRecipe> SERIALIZER = new Serializer();

    public int lightOf(int batch) {
        return SeparationRecipe.lightOf(batch, lightFraction);
    }

    public int heavyOf(int batch) {
        return batch - lightOf(batch);
    }

    public Fluid repelled() {
        return attracted == light ? heavy : light;
    }

    public int of(Fluid product, int batch) {
        return product == light ? lightOf(batch) : heavyOf(batch);
    }

    public double contrast(double celsius) {
        return Paramagnetism.relative(curieShare, celsius);
    }

    public static Optional<MagneticRecipe> forLiquor(Level level, Fluid liquor) {
        return level.getRecipeManager().getRecipeFor(TYPE, new SeparationRecipe.Liquor(liquor), level).map(RecipeHolder::value);
    }

    @Override
    public boolean matches(SeparationRecipe.Liquor input, Level level) {
        return input.fluid() == liquor;
    }

    @Override
    public ItemStack assemble(SeparationRecipe.Liquor input, HolderLookup.Provider registries) {
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

    private static final class Serializer implements RecipeSerializer<MagneticRecipe> {
        private static final MapCodec<MagneticRecipe> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
                BuiltInRegistries.FLUID.byNameCodec().fieldOf("liquor").forGetter(MagneticRecipe::liquor),
                BuiltInRegistries.FLUID.byNameCodec().fieldOf("light").forGetter(MagneticRecipe::light),
                BuiltInRegistries.FLUID.byNameCodec().fieldOf("heavy").forGetter(MagneticRecipe::heavy),
                Codec.floatRange(0, 1).fieldOf("light_fraction").forGetter(MagneticRecipe::lightFraction),
                BuiltInRegistries.FLUID.byNameCodec().fieldOf("attracted").forGetter(MagneticRecipe::attracted),
                Codec.intRange(1, 256).fieldOf("passes").forGetter(MagneticRecipe::passes),
                Codec.FLOAT.optionalFieldOf("curie_share", 1F).forGetter(MagneticRecipe::curieShare)
        ).apply(i, MagneticRecipe::new));
        private static final StreamCodec<RegistryFriendlyByteBuf, Fluid> FLUID = ByteBufCodecs.registry(Registries.FLUID);
        private static final StreamCodec<RegistryFriendlyByteBuf, MagneticRecipe> STREAM_CODEC = StreamCodec.of(
                (buf, cut) -> {
                    FLUID.encode(buf, cut.liquor());
                    FLUID.encode(buf, cut.light());
                    FLUID.encode(buf, cut.heavy());
                    ByteBufCodecs.FLOAT.encode(buf, cut.lightFraction());
                    FLUID.encode(buf, cut.attracted());
                    ByteBufCodecs.VAR_INT.encode(buf, cut.passes());
                    ByteBufCodecs.FLOAT.encode(buf, cut.curieShare());
                },
                buf -> new MagneticRecipe(FLUID.decode(buf), FLUID.decode(buf), FLUID.decode(buf), ByteBufCodecs.FLOAT.decode(buf),
                        FLUID.decode(buf), ByteBufCodecs.VAR_INT.decode(buf), ByteBufCodecs.FLOAT.decode(buf)));

        @Override
        public MapCodec<MagneticRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MagneticRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
