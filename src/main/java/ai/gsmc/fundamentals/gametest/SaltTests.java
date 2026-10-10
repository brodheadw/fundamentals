package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.separation.Hazards;
import ai.gsmc.fundamentals.separation.Separation;
import com.simibubi.create.content.fluids.tank.BoilerData;
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
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class SaltTests {

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
    public void aBoilerRefusesSeawater(GameTestHelper helper) {
        IFluidHandler boiler = new BoilerData().createHandler();
        helper.assertTrue(boiler.fill(new FluidStack(Separation.fluid("seawater"), 1000), IFluidHandler.FluidAction.SIMULATE) == 0, "a boiler should refuse seawater");
        helper.assertTrue(boiler.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.SIMULATE) == 1000, "and take fresh water");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void theChlorAlkaliCellMakesChlorineCausticSodaAndHydrogen(GameTestHelper helper) {
        var holder = helper.getLevel().getRecipeManager().byKey(Fundamentals.id("uses/chlor_alkali"));
        helper.assertTrue(holder.isPresent(), "brine should electrolyse in a cell");
        ProcessingRecipe<?, ?> recipe = (ProcessingRecipe<?, ?>) holder.get().value();
        helper.assertTrue(recipe.getFluidIngredients().stream().anyMatch(i -> i.ingredient().test(new FluidStack(Separation.fluid("salt_brine"), 1)))
                && recipe.getIngredients().stream().anyMatch(i -> i.test(new ItemStack(BuiltInRegistries.ITEM.get(
                        Fundamentals.id("dimensionally_stable_anode"))))), "the cell takes brine over a dimensionally stable anode");
        for (var product : new net.minecraft.world.level.material.Fluid[] {Separation.fluid("chlorine"), Separation.fluid("caustic_soda"),
                BuiltInRegistries.FLUID.get(ResourceLocation.parse("tfmg:hydrogen"))}) {
            helper.assertTrue(recipe.getFluidResults().stream().anyMatch(f -> f.is(product)), "the cell should give " + BuiltInRegistries.FLUID.getKey(product));
        }
        var caustic = Separation.kind(Separation.fluid("caustic_soda"));
        helper.assertTrue(Hazards.eats(caustic, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfmg:aluminum_pipe")).defaultBlockState())
                && !Hazards.eats(caustic, com.simibubi.create.AllBlocks.FLUID_PIPE.getDefaultState()), "caustic soda should eat aluminium pipe and leave copper alone");
        helper.succeed();
    }
}
