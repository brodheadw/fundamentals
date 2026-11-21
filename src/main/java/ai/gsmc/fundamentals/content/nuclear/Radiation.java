package ai.gsmc.fundamentals.content.nuclear;

import ai.gsmc.fundamentals.Fundamentals;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.nuclearteam.createnuclear.api.radiation.RadiationRegistry;

// Create Nuclear's dose per item held, on its own scale: its raw uranium is 3, its yellowcake 4, its fuel rod 100.
public final class Radiation {

    private Radiation() {}

    public static void register() {
        Doses.DOSE.forEach((id, dose) -> RadiationRegistry.register().item(BuiltInRegistries.ITEM.get(Fundamentals.id(id))).value(dose).build());
    }

    public static double dose(ItemStack stack) {
        return RadiationRegistry.get(stack);
    }
}
