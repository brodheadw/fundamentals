package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.separation.Hazards;
import ai.gsmc.fundamentals.separation.Separation;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.equipment.armor.BacktankUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class NickelTests {

    @GameTest(template = "battery", timeoutTicks = Hazards.CARBONYL_ONSET + 200)
    public void nickelCarbonylPoisonsTheUnmaskedNowAndLater(GameTestHelper helper) {
        helper.assertTrue(Hazards.chance(new FluidStack(Separation.fluid("nickel_carbonyl"), 1)) == 0, "nickel carbonyl should not eat its pipe");
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 2));
        float health = pig.getHealth();
        Hazards.leak(helper.getLevel(), helper.absolutePos(new BlockPos(4, 1, 2)));
        helper.runAfterDelay(30, () -> helper.assertTrue(pig.getHealth() < health && pig.hasEffect(MobEffects.CONFUSION), "an unmasked pig should be hurt at once"));
        helper.runAfterDelay(Hazards.CARBONYL_ONSET + 60, () -> {
            helper.assertTrue(pig.hasEffect(MobEffects.WITHER) || pig.isDeadOrDying(), "half a minute on, the pig's lungs should fill");
            helper.succeed();
        });
    }

    @GameTest(template = "battery", timeoutTicks = 200)
    public void theMaskKeepsNickelCarbonylOut(GameTestHelper helper) {
        Pig pig = helper.spawnWithNoFreeWill(EntityType.PIG, new BlockPos(4, 1, 2));
        ItemStack tank = AllItems.COPPER_BACKTANK.asStack();
        tank.set(AllDataComponents.BACKTANK_AIR, BacktankUtil.maxAirWithoutEnchants());
        pig.setItemSlot(EquipmentSlot.HEAD, AllItems.COPPER_DIVING_HELMET.asStack());
        pig.setItemSlot(EquipmentSlot.CHEST, tank);
        float health = pig.getHealth();
        Hazards.leak(helper.getLevel(), helper.absolutePos(new BlockPos(4, 1, 2)));
        helper.runAfterDelay(80, () -> {
            helper.assertTrue(pig.getHealth() == health && !pig.hasEffect(MobEffects.CONFUSION), "a masked pig should breathe its tank");
            helper.assertTrue(BacktankUtil.getAir(pig.getItemBySlot(EquipmentSlot.CHEST)) < BacktankUtil.maxAirWithoutEnchants(), "the mask should spend air");
            helper.succeed();
        });
    }
}
