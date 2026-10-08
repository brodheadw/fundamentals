package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.Map;

/** The recipes that spend the rare earths, and the other mods' recipes taken over so that they need them. */
@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class UsesTests {

    private static Recipe<?> recipe(GameTestHelper helper, String id) {
        var found = helper.getLevel().getRecipeManager().byKey(ResourceLocation.parse(id));
        helper.assertTrue(found.isPresent(), id + " did not load");
        return found.get().value();
    }

    private static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    @GameTest(template = "empty")
    public void theMagnetAlloysAreSinteredAndPolarizedIntoTheFactorysMagnet(GameTestHelper helper) {
        Map.of("fundamentals:uses/neodymium_iron_boron", "create:mixing", "fundamentals:uses/neodymium_iron_boron_from_didymium", "create:mixing",
                "fundamentals:uses/samarium_cobalt", "create:mixing", "fundamentals:uses/magnet_from_samarium_cobalt", "tfmg:polarizing")
                .forEach((id, type) -> helper.assertTrue(BuiltInRegistries.RECIPE_TYPE.getKey(recipe(helper, id).getType()).toString().equals(type),
                        id + " should be a " + type + " recipe"));
        Recipe<?> magnet = recipe(helper, "tfmg:polarizing/magnet");
        helper.assertTrue(magnet.getIngredients().get(0).test(stack("fundamentals:neodymium_iron_boron_ingot")),
                "the Factory's magnet should be polarized from NdFeB");
        helper.assertTrue(!magnet.getIngredients().get(0).test(stack("tfmg:magnetic_alloy_ingot")),
                "the Factory's magnetic alloy should no longer make a magnet on its own");
        helper.assertTrue(recipe(helper, "fundamentals:uses/neodymium_iron_boron").getIngredients().stream().anyMatch(i -> i.test(stack("fundamentals:raw_borax"))),
                "NdFeB wants boron, from borax");
        helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.parse("fundamentals:borax_ore")), "borax should be an ore");
        helper.succeed();
    }
}
