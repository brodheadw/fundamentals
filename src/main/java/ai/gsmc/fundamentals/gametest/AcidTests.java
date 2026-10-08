package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.separation.Acids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class AcidTests {

    private static void pour(GameTestHelper helper, String acid, BlockPos at) {
        helper.setBlock(at, Acids.all().get(acid).block.defaultBlockState());
    }

    @GameTest(template = "battery", timeoutTicks = 300)
    public void hydrochloricAcidEatsCalciteButNotStoneAndIsSpent(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.CALCITE);
        helper.setBlock(new BlockPos(6, 1, 2), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 1, 1), Blocks.STONE);
        helper.setBlock(new BlockPos(2, 1, 3), Blocks.STONE);
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.STONE);
        pour(helper, "hydrochloric_acid", new BlockPos(3, 1, 2));
        pour(helper, "hydrochloric_acid", new BlockPos(5, 1, 2));
        helper.runAfterDelay(140, () -> {
            helper.assertBlockPresent(Blocks.AIR, new BlockPos(2, 1, 2));
            helper.assertBlockPresent(Blocks.STONE, new BlockPos(6, 1, 2));
            helper.assertTrue(!helper.getBlockState(new BlockPos(3, 1, 2)).is(Acids.all().get("hydrochloric_acid").block), "the acid that ate the calcite should be spent");
            helper.succeed();
        });
    }

    @GameTest(template = "battery", timeoutTicks = 300)
    public void hydrofluoricAcidEatsGlassAndHurts(GameTestHelper helper) {
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.GLASS);
        pour(helper, "hydrofluoric_acid", new BlockPos(2, 2, 2));
        var pig = helper.spawnWithNoFreeWill(net.minecraft.world.entity.EntityType.PIG, new BlockPos(5, 1, 2));
        pour(helper, "hydrofluoric_acid", new BlockPos(5, 1, 2));
        float health = pig.getHealth();
        helper.runAfterDelay(140, () -> {
            // the glass is gone; what is left of the acid runs into the hole, as it should
            helper.assertTrue(!helper.getBlockState(new BlockPos(2, 1, 2)).is(Blocks.GLASS), "hydrofluoric acid should eat glass");
            helper.assertTrue(pig.getHealth() < health, "a pig standing in hydrofluoric acid should be hurt");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public void everyAcidHasABucketAndABlock(GameTestHelper helper) {
        for (String acid : new String[] {"hydrochloric_acid", "hydrofluoric_acid", "nitric_acid", "phosphoric_acid"}) {
            helper.assertTrue(BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, acid + "_bucket")), acid + " has no bucket");
            helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, acid)), acid + " has no block");
            helper.assertTrue(BuiltInRegistries.FLUID.containsKey(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, acid + "_flowing")), acid + " has no flowing form");
        }
        helper.succeed();
    }
}
