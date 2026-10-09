package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.separation.Separation;
import ai.gsmc.fundamentals.worldgen.DepositFeature;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.commands.FillBiomeCommand;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Seawater from the sea, and salt from seawater and rock salt, not from any water at all. */
@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class SaltTests {

    // Level.getBiome jitters among the neighbouring 4x4x4 biome cells, so a whole block of them is set.
    private static ItemStack bucketFrom(GameTestHelper helper, BlockPos at, ResourceKey<Biome> biome) {
        BlockPos pos = helper.absolutePos(at);
        FillBiomeCommand.fill(helper.getLevel(), pos.offset(-8, -8, -8), pos.offset(8, 8, 8),
                helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(biome));
        helper.setBlock(at, Blocks.WATER);
        return ((LiquidBlock) Blocks.WATER).pickupBlock(null, helper.getLevel(), pos, helper.getLevel().getBlockState(pos));
    }

    @GameTest(template = "empty")
    public void aBucketDrawsSeawaterFromTheSeaAndFreshWaterFromARiver(GameTestHelper helper) {
        BlockPos at = new BlockPos(1, 1, 1);
        helper.assertTrue(bucketFrom(helper, at, Biomes.DEEP_OCEAN).is(Separation.seawaterBucket()), "a bucket filled in the deep ocean should come up with seawater");
        helper.assertTrue(bucketFrom(helper, at, Biomes.BEACH).is(Separation.seawaterBucket()), "a bucket filled off a beach should come up with seawater");
        helper.assertTrue(bucketFrom(helper, at, Biomes.RIVER).is(Items.WATER_BUCKET), "a bucket filled in a river should come up with fresh water");
        ((BucketItem) Separation.seawaterBucket()).emptyContents(null, helper.getLevel(), helper.absolutePos(at), null, new ItemStack(Separation.seawaterBucket()));
        helper.assertBlockPresent(Blocks.WATER, at);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void saltComesFromSeawaterAndRockSaltNotFreshWater(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        ItemStack salt = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "salt")));
        boolean fromSeawater = false;
        for (var holder : recipes.getAllRecipesFor(AllRecipeTypes.MIXING.getType())) {
            ProcessingRecipe<?, ?> recipe = (ProcessingRecipe<?, ?>) holder.value();
            if (recipe.getRollableResults().stream().noneMatch(r -> r.getStack().is(salt.getItem()))) {
                continue;
            }
            helper.assertTrue(recipe.getFluidIngredients().stream().noneMatch(i -> i.ingredient().test(new FluidStack(Fluids.WATER, 1))),
                    holder.id() + " makes salt from fresh water");
            fromSeawater |= recipe.getFluidIngredients().stream().anyMatch(i -> i.ingredient().test(new FluidStack(Separation.fluid("seawater"), 1)))
                    && recipe.getFluidResults().stream().anyMatch(f -> f.is(Separation.fluid("bittern")));
        }
        helper.assertTrue(fromSeawater, "seawater should boil down to salt and bittern");
        var halite = recipes.byKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "uses/salt_from_halite_milling"));
        helper.assertTrue(halite.isPresent() && halite.get().value().getIngredients().get(0).test(new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "raw_halite")))), "a millstone should grind rock salt to salt");
        helper.assertTrue(recipes.byKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "uses/magnesium_chloride_from_bittern")).isPresent(),
                "bittern should boil down to magnesium chloride");
        var bed = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "evaporite_bed"));
        helper.assertTrue(bed != null && bed.config() instanceof DepositFeature.Config config && config.ores().stream()
                .anyMatch(ore -> BuiltInRegistries.BLOCK.getKey(ore.state().getBlock()).getPath().equals("halite_ore")), "the evaporite bed should carry halite");
        helper.succeed();
    }
}
