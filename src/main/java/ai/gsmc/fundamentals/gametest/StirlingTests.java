package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class StirlingTests {

    @GameTest(template = "battery", timeoutTicks = 200, batch = "stirling")
    public void aStirlingEngineRunsOnLavaButNotOnStone(GameTestHelper helper) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("createpropulsion", "stirling_engine");
        if (!BuiltInRegistries.BLOCK.containsKey(id)) {
            helper.succeed(); // Create Propulsion is optional; run with -Ppropulsion to exercise this
            return;
        }
        Block engine = BuiltInRegistries.BLOCK.get(id);
        BlockPos lava = new BlockPos(2, 1, 2), onLava = lava.above(), onStone = new BlockPos(14, 2, 2);
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
            }
        }
        helper.setBlock(lava, Blocks.LAVA);
        helper.setBlock(onLava, engine);
        helper.setBlock(onStone.below(), Blocks.STONE);
        helper.setBlock(onStone, engine);
        helper.runAfterDelay(40, () -> {
            float hot = ((GeneratingKineticBlockEntity) helper.getBlockEntity(onLava)).getGeneratedSpeed();
            float cold = ((GeneratingKineticBlockEntity) helper.getBlockEntity(onStone)).getGeneratedSpeed();
            helper.assertTrue(hot != 0, "an engine on lava should turn, got " + hot);
            helper.assertTrue(cold == 0, "an engine on cold stone should not, got " + cold);
            helper.succeed();
        });
    }
}
