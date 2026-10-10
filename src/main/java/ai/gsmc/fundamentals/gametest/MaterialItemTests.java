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
    public void everyItemHasItsModelAndName(GameTestHelper helper) throws Exception {
        JsonObject lang;
        try (Reader in = new InputStreamReader(Fundamentals.class.getResourceAsStream("/assets/fundamentals/lang/en_us.json"), StandardCharsets.UTF_8)) {
            lang = new Gson().fromJson(in, JsonObject.class);
        }
        helper.assertTrue(!MaterialItems.items().isEmpty(), "no material items were registered");
        for (Item item : BuiltInRegistries.ITEM) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
            if (!id.getNamespace().equals(Fundamentals.MOD_ID)) {
                continue;
            }
            helper.assertTrue(Fundamentals.class.getResource("/assets/fundamentals/models/item/" + id.getPath() + ".json") != null, id + " has no model");
            helper.assertTrue(lang.has(item.getDescriptionId()), id + " has no name");
        }
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
    public void theAdvancementsLoad(GameTestHelper helper) {
        for (String name : new String[] {"root", "bloom", "mortar", "collection", "concentrate", "rare_earths", "magnet"}) {
            helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(Fundamentals.id(name)) != null,
                    "advancement " + name + " did not load");
        }
        helper.succeed();
    }

}
