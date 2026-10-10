package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.elements.PeriodicTable;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.critereon.InventoryChangeTrigger;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class PeriodicTableTests {

    @GameTest(template = "empty")
    public void everyElementSitsOnTheTableAndEveryFindableOneHasItsTag(GameTestHelper helper) {
        List<AdvancementHolder> elements = helper.getLevel().getServer().getAdvancements().getAllAdvancements().stream()
                .filter(h -> h.id().getNamespace().equals(Fundamentals.MOD_ID) && h.id().getPath().startsWith("elements/") && !h.id().equals(PeriodicTable.ROOT))
                .toList();
        helper.assertTrue(elements.size() == 118, "expected 118 element advancements, found " + elements.size());
        for (AdvancementHolder holder : elements) {
            DisplayInfo display = holder.value().display().orElseThrow();
            int z = display.getIcon().get(DataComponents.CUSTOM_MODEL_DATA).value();
            float[] cell = PeriodicTable.cell(z);
            helper.assertTrue(display.getX() == cell[0] && display.getY() == cell[1], holder.id() + " is at " + display.getX() + "," + display.getY() + ", not on the table");
            boolean findable = holder.value().criteria().values().stream().noneMatch(c -> c.trigger() == CriteriaTriggers.IMPOSSIBLE);
            if (findable) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, Fundamentals.id(holder.id().getPath()));
                helper.assertTrue(BuiltInRegistries.ITEM.getTag(tag).filter(named -> named.size() > 0).isPresent(), "#" + tag.location() + " is missing or empty");
            }
        }
        helper.succeed();
    }

}
