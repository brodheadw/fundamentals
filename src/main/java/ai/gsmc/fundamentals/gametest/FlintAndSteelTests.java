package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class FlintAndSteelTests {

    @GameTest(template = "empty")
    public void flintAndSteelTakesAFewStrikes(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "striker"));
        BlockPos rack = helper.absolutePos(new BlockPos(1, 1, 1)), above = rack.above();
        level.setBlockAndUpdate(rack, Blocks.NETHERRACK.defaultBlockState());
        player.moveTo(Vec3.atCenterOf(above).add(0, 0, 1.5));
        int lit = 0;
        for (int i = 0; i < 60; i++) {
            level.setBlockAndUpdate(above, Blocks.AIR.defaultBlockState());
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
            player.gameMode.useItemOn(player, level, player.getMainHandItem(), InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(rack).add(0, 0.5, 0), Direction.UP, rack, false));
            if (level.getBlockState(above).is(Blocks.FIRE)) lit++;
        }
        level.setBlockAndUpdate(above, Blocks.AIR.defaultBlockState());
        helper.assertTrue(lit > 5 && lit < 40, "about one strike in three catches: " + lit + " of 60");
        helper.succeed();
    }
}
