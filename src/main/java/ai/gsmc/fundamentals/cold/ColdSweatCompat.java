package ai.gsmc.fundamentals.cold;

import ai.gsmc.fundamentals.Fundamentals;
import com.momosoftworks.coldsweat.api.event.common.insulation.InsulationTickEvent;
import com.momosoftworks.coldsweat.api.event.common.item.WaterskinEvent;
import com.momosoftworks.coldsweat.api.event.core.init.DefaultTempModifiersEvent;
import com.momosoftworks.coldsweat.api.event.core.registry.TempModifierRegisterEvent;
import com.momosoftworks.coldsweat.api.registry.TempModifierRegistry;
import com.momosoftworks.coldsweat.api.temperature.modifier.WaterTempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.api.util.placement.Matcher;
import com.momosoftworks.coldsweat.api.util.placement.Placement;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.data.codec.configuration.InsulatorData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;

import java.util.Set;
import java.util.function.BiConsumer;

/** Loaded only with Cold Sweat: its world temperature becomes ours, and down goes flat in the wet. */
final class ColdSweatCompat {

    static final ResourceLocation HEAT = Fundamentals.id("heat");
    // Cold Sweat's own guesses at the world, which our heat grid and climate replace; its hearths, water and items stay
    private static final Set<String> REPLACED = Set.of("cold_sweat:biomes", "cold_sweat:elevation", "cold_sweat:shade", "cold_sweat:cave_biomes",
            "cold_sweat:blocks", "cold_sweat:sublevel_blocks", "sereneseasons:season", "weather2:storm");
    // the wetness at which Cold Sweat draws a player dripping
    private static final double SOAKED = 0.2;
    // soaked down collapses and keeps about a third of its loft
    private static final double DOWN_LOST_WET = 2.0 / 3;

    private ColdSweatCompat() {}

    static void register() {
        NeoForge.EVENT_BUS.addListener((TempModifierRegisterEvent event) -> event.register(HEAT, HeatTempModifier::new));
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ColdSweatCompat::useOurHeat);
        NeoForge.EVENT_BUS.addListener(ColdSweatCompat::wetDown);
        NeoForge.EVENT_BUS.addListener(ColdSweatCompat::fillFlask);
    }

    static void registerFlasks(BiConsumer<ResourceLocation, Item> registry) {
        Cold.flask = new Flask();
        Cold.filledFlask = new FilledFlask();
        Cold.unfinishedFlask = new Item(new Item.Properties().stacksTo(1));
        registry.accept(Fundamentals.id("vacuum_flask"), Cold.flask);
        registry.accept(Fundamentals.id("filled_vacuum_flask"), Cold.filledFlask);
        registry.accept(Fundamentals.id("unfinished_vacuum_flask"), Cold.unfinishedFlask);
    }

    // Cold Sweat fills any waterskin as its own filled waterskin; a flask comes back a filled flask, at the same temperature
    private static void fillFlask(WaterskinEvent.Fill event) {
        if (event.getWaterskin().is(Cold.flask)) {
            event.setResultWaterskin(event.getResultWaterskin().transmuteCopy(Cold.filledFlask));
        }
    }

    private static void useOurHeat(DefaultTempModifiersEvent event) {
        boolean replaced = event.getModifiers(Temperature.Trait.WORLD).removeIf(mod -> {
            ResourceLocation id = TempModifierRegistry.getKey(mod);
            return id != null && REPLACED.contains(id.toString());
        });
        if (replaced) {
            event.addModifier(Temperature.Trait.WORLD, new HeatTempModifier().tickRate(10), Placement.FIRST.noDuplicates(Matcher.SAME_CLASS));
        }
    }

    private static void wetDown(InsulationTickEvent event) {
        Player player = event.getPlayer();
        double wet = Temperature.getModifier(player, Temperature.Trait.WORLD, WaterTempModifier.class)
                .map(mod -> Math.min(1, Math.abs(mod.getTemperature()) / SOAKED)).orElse(0.0);
        if (wet == 0) {
            return;
        }
        double down = 0;
        for (ItemStack armor : player.getArmorSlots()) {
            if (Parkas.tier(armor.getItem()) == Parka.DOWN) {
                for (InsulatorData insulator : ConfigSettings.INSULATING_ARMORS.get().get(armor.getItem())) {
                    down += insulator.getCold();
                }
            }
        }
        event.setProperty("cold_armor", event.getProperty("cold_armor") - down * DOWN_LOST_WET * wet);
    }
}
