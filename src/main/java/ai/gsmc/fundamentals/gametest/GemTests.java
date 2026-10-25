package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.separation.Separation;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class GemTests {

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    private static ProcessingRecipe<?, ?> processing(GameTestHelper helper, String id) {
        return (ProcessingRecipe<?, ?>) helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id))
                .orElseThrow(() -> new AssertionError("no recipe " + id)).value();
    }

    private static boolean takes(ProcessingRecipe<?, ?> recipe, String item) {
        return recipe.getIngredients().stream().anyMatch(i -> i.test(stack(item)));
    }

    private static boolean takesFluid(ProcessingRecipe<?, ?> recipe, String id) {
        return recipe.getFluidIngredients().stream().anyMatch(i -> i.test(new FluidStack(BuiltInRegistries.FLUID.get(ResourceLocation.parse(id)), 1000)));
    }

    @GameTest(template = "empty")
    public void verneuilGrowsTheSapphireTheLedIsBuiltOn(GameTestHelper helper) {
        var verneuil = processing(helper, "fundamentals:uses/sapphire_boule");
        helper.assertTrue(takes(verneuil, "fundamentals:alumina") && takesFluid(verneuil, "tfmg:hydrogen") && takesFluid(verneuil, "fundamentals:oxygen")
                && verneuil.getResultItem(helper.getLevel().registryAccess()).is(stack("fundamentals:sapphire_boule").getItem()),
                "alumina in an oxyhydrogen flame should grow a sapphire boule");
        helper.assertTrue(processing(helper, "tfmg:vat_machine_recipe/neon").getFluidResults().stream().anyMatch(f -> f.is(Separation.fluid("oxygen"))),
                "air separation should give the oxygen");
        var led = processing(helper, "fundamentals:uses/led_chip");
        helper.assertTrue(takes(led, "fundamentals:sapphire_wafer") && !takes(led, "fundamentals:alumina"), "the LED should grow on a sapphire wafer, not alumina powder");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void aReticleIsLappedWithGarnetOrEmeryNotSand(GameTestHelper helper) {
        for (String abrasive : new String[] {"fundamentals:garnet_paper", "fundamentals:emery_cloth", "create:sand_paper"}) {
            RecipeWrapper input = new RecipeWrapper(new ItemStackHandler(NonNullList.of(ItemStack.EMPTY, stack("fundamentals:fused_quartz"), stack(abrasive))));
            var step = SequencedAssemblyRecipe.getRecipe(helper.getLevel(), input, AllRecipeTypes.DEPLOYING.getType(), DeployerApplicationRecipe.class);
            boolean laps = step.isPresent() && step.get().id().getPath().startsWith("compute/quartz_mask_blank");
            helper.assertTrue(laps == !abrasive.startsWith("create:"), abrasive + (laps ? " should not" : " should") + " lap a reticle's quartz");
        }
        helper.assertTrue(stack("fundamentals:garnet_paper").getMaxDamage() > stack("create:sand_paper").getMaxDamage(), "garnet paper should outlast sand paper");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 1000)
    public void bismuthLeftToCoolGrowsAHopperCrystal(GameTestHelper helper) {
        var melt = processing(helper, "fundamentals:uses/molten_bismuth");
        helper.assertTrue(takes(melt, "fundamentals:bismuth_ingot") && melt.getFluidResults().getFirst().is(Separation.fluid("molten_bismuth")),
                "a bismuth ingot should melt to molten bismuth");
        BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, BuiltInRegistries.BLOCK.get(ResourceLocation.parse("tfmg:casting_basin")));
        helper.runAfterDelay(5, () -> {
            IFluidHandler tank = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(pos), Direction.UP);
            helper.assertTrue(tank.fill(new FluidStack(Separation.fluid("molten_bismuth"), 144), IFluidHandler.FluidAction.EXECUTE) == 144,
                    "the casting basin should take an ingot's worth of molten bismuth");
            var items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(pos), Direction.DOWN);
            helper.succeedWhen(() -> helper.assertTrue(items.getStackInSlot(0).is(stack("fundamentals:bismuth_crystal").getItem()),
                    "the melt should freeze to a bismuth crystal, not " + items.getStackInSlot(0)));
        });
    }
}
