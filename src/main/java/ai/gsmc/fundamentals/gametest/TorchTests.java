package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.Fundamentals;
import com.mojang.serialization.Dynamic;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(Fundamentals.MOD_ID)
@PrefixGameTestTemplate(false)
public class TorchTests {

    @GameTest(template = "empty")
    public void aBurntStubSavedInAWorldLoadsAsALitTorchFacingTheSameWay(GameTestHelper helper) {
        helper.assertTrue(!Blocks.TORCH.defaultBlockState().isRandomlyTicking(), "a torch should not take random ticks");
        // a chunk palette entry as 0.19.4 saved it
        CompoundTag saved = new CompoundTag();
        saved.putString("Name", "fundamentals:burnt_wall_torch");
        CompoundTag properties = new CompoundTag();
        properties.putString("facing", "north");
        saved.put("Properties", properties);
        BlockState state = BlockState.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, saved)).getOrThrow();
        helper.assertTrue(state.is(Blocks.WALL_TORCH) && state.getValue(WallTorchBlock.FACING) == Direction.NORTH,
                "a saved wall stub should load as a wall torch facing north, not " + state);
        helper.assertTrue(BuiltInRegistries.BLOCK.get(Fundamentals.id("burnt_torch")) == Blocks.TORCH, "a saved stub should load as a torch");
        helper.assertTrue(BuiltInRegistries.ITEM.get(Fundamentals.id("burnt_torch")) == Items.TORCH, "a stub item should load as a torch");
        helper.succeed();
    }
}
