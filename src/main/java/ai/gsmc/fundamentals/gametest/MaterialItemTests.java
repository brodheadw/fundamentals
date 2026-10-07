package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.material.Material;
import ai.gsmc.fundamentals.registry.MaterialItems;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class MaterialItemTests {

    private static Item item(String id) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
    }

    @GameTest(template = "empty")
    public void everyMaterialItemHasItsModelTextureAndName(GameTestHelper helper) throws Exception {
        JsonObject lang;
        try (Reader in = new InputStreamReader(Fundamentals.class.getResourceAsStream("/assets/fundamentals/lang/en_us.json"), StandardCharsets.UTF_8)) {
            lang = new Gson().fromJson(in, JsonObject.class);
        }
        helper.assertTrue(!MaterialItems.items().isEmpty(), "no material items were registered");
        MaterialItems.items().forEach((id, item) -> {
            boolean block = item instanceof BlockItem;
            String texture = "/assets/fundamentals/textures/" + (block ? "block/" : "item/") + id.getPath() + ".png";
            helper.assertTrue(Fundamentals.class.getResource("/assets/fundamentals/models/item/" + id.getPath() + ".json") != null, id + " has no model");
            helper.assertTrue(Fundamentals.class.getResource(texture) != null, id + " has no texture");
            helper.assertTrue(lang.has((block ? "block." : "item.") + "fundamentals." + id.getPath()), id + " has no name");
        });
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void everyMaterialTagHoldsSomething(GameTestHelper helper) {
        for (Material material : MaterialItems.materials()) {
            for (String tag : material.tags()) {
                helper.assertTrue(BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, ResourceLocation.parse(tag))).filter(named -> named.size() > 0).isPresent(),
                        material.id() + " should be found under #" + tag);
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void rareEarthMineralsGrindAndWashOnCreatesMachines(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        for (String id : new String[] {"milling/bastnasite_dust", "crushing/xenotime_dust", "washing/raw_monazite", "washing/euxenite_dust"}) {
            var recipe = recipes.byKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, id));
            helper.assertTrue(recipe.isPresent(), id + " did not load");
            String machine = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.get().value().getType()).toString();
            helper.assertTrue(machine.equals("create:" + (id.startsWith("washing") ? "splashing" : id.substring(0, id.indexOf('/')))),
                    id + " is a " + machine + " recipe");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void theAdvancementsLoad(GameTestHelper helper) {
        for (String name : new String[] {"root", "bloom", "mortar", "collection", "concentrate", "rare_earths", "magnet"}) {
            helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, name)) != null,
                    "advancement " + name + " did not load");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public void nuggetsPackIntoIngotsAndIngotsIntoBlocks(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager();
        CraftingInput nuggets = CraftingInput.of(3, 3, Collections.nCopies(9, new ItemStack(item("fundamentals:neodymium_nugget"))));
        var ingot = recipes.getRecipeFor(RecipeType.CRAFTING, nuggets, helper.getLevel());
        helper.assertTrue(ingot.isPresent() && ingot.get().value().assemble(nuggets, helper.getLevel().registryAccess()).is(item("fundamentals:neodymium_ingot")),
                "nine neodymium nuggets should make an ingot");
        CraftingInput block = CraftingInput.of(1, 1, Collections.singletonList(new ItemStack(item("fundamentals:neodymium_block"))));
        var ingots = recipes.getRecipeFor(RecipeType.CRAFTING, block, helper.getLevel());
        ItemStack unpacked = ingots.map(recipe -> recipe.value().assemble(block, helper.getLevel().registryAccess())).orElse(ItemStack.EMPTY);
        helper.assertTrue(unpacked.is(item("fundamentals:neodymium_ingot")) && unpacked.getCount() == 9, "a neodymium block should unpack to nine ingots, got " + unpacked);
        helper.succeed();
    }
}
