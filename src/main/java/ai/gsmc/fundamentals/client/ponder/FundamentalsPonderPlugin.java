package ai.gsmc.fundamentals.client.ponder;

import ai.gsmc.fundamentals.Fundamentals;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.resources.ResourceLocation;

public final class FundamentalsPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return Fundamentals.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        helper.forComponents(Fundamentals.id("mixer_settler"))
                .addStoryBoard("mixer_settler", MixerSettlerScenes::battery);
    }
}
