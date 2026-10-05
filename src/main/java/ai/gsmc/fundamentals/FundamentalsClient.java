package ai.gsmc.fundamentals;

//? if fabric {
import ai.gsmc.fundamentals.registry.OreBlocks;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;
//?}

/**
 * Fabric client setup. Ores draw a transparent mineral texture over their host rock, which needs
 * the cutout render layer; NeoForge reads that from {@code render_type} in the model JSON.
 */
public class FundamentalsClient /*? if fabric {*/ implements ClientModInitializer /*?}*/ {

    //? if fabric {
    @Override
    public void onInitializeClient() {
        OreBlocks.ores().forEach(block -> BlockRenderLayerMap.INSTANCE.putBlock(block, RenderType.cutout()));
    }
    //?}
}
