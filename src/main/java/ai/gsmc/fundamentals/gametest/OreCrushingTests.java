package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import com.simibubi.create.content.processing.recipe.ProcessingRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class OreCrushingTests {

    @GameTest(template = "empty")
    public void everyOreBlockCrushesWithExperience(GameTestHelper helper) {
        var nugget = BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:experience_nugget"));
        var recipes = helper.getLevel().getRecipeManager();
        int ores = 0;
        for (ResourceLocation id : BuiltInRegistries.ITEM.keySet()) {
            if (!id.getNamespace().equals(Fundamentals.MOD_ID) || !id.getPath().endsWith("_ore") || id.getPath().startsWith("placer_")
                    || id.getPath().equals("bauxite_ore")) continue;
            ItemStack ore = new ItemStack(BuiltInRegistries.ITEM.get(id));
            boolean crushed = recipes.getRecipes().stream().anyMatch(holder -> holder.value() instanceof ProcessingRecipe<?, ?> recipe
                    && holder.id().getPath().startsWith("crushing/") && recipe.getIngredients().stream().anyMatch(i -> i.test(ore))
                    && recipe.getRollableResults().stream().anyMatch(r -> r.getStack().is(nugget)));
            helper.assertTrue(crushed, id + " should crush with an experience nugget");
            ores++;
        }
        helper.assertTrue(ores > 60, "found the ore blocks: " + ores);
        helper.succeed();
    }
}
