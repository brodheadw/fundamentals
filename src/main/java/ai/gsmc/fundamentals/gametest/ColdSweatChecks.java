package ai.gsmc.fundamentals.gametest;

import ai.gsmc.fundamentals.cold.Cold;
import ai.gsmc.fundamentals.cold.HeatTempModifier;
import ai.gsmc.fundamentals.heat.Thermometers;
import ai.gsmc.fundamentals.Fundamentals;
import com.momosoftworks.coldsweat.api.event.common.item.WaterskinEvent;
import com.momosoftworks.coldsweat.api.temperature.modifier.BiomeTempModifier;
import com.momosoftworks.coldsweat.api.temperature.modifier.BlockTempModifier;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.capability.handler.EntityTempManager;
import com.momosoftworks.coldsweat.common.capability.temperature.PlayerTempCap;
import com.momosoftworks.coldsweat.common.item.FilledWaterskinItem;
import com.momosoftworks.coldsweat.core.init.ModItemComponents;
import com.momosoftworks.coldsweat.core.init.ModItems;
import com.wildspell.fundamental.api.heat.Heat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidStack;

/** The Cold Sweat half of ColdTests, apart so nothing loads its classes when it is not installed. Returns a failure, or null. */
final class ColdSweatChecks {

    private ColdSweatChecks() {}

    static String check(Player player) {
        for (double celsius : new double[] {-40, 0, 15, 37, 1150}) {
            double theirs = Temperature.convert(celsius, Temperature.Units.C, Temperature.Units.MC, true);
            if (Math.abs(Cold.toColdSweat(celsius) - theirs) > 1e-9) {
                return celsius + " °C: ours " + Cold.toColdSweat(celsius) + ", Cold Sweat's " + theirs;
            }
        }
        var world = EntityTempManager.gatherTempModifiers(player).get(Temperature.Trait.WORLD);
        if (world.stream().noneMatch(mod -> mod instanceof HeatTempModifier)
                || world.stream().anyMatch(mod -> mod instanceof BiomeTempModifier || mod instanceof BlockTempModifier)) {
            return "the world temperature should be ours, not Cold Sweat's biomes and blocks: " + world;
        }
        double units = Temperature.apply(0, player, Temperature.Trait.WORLD, new HeatTempModifier());
        double celsius = Heat.at(player.level(), player.blockPosition());
        if (Math.abs(units * Cold.CELSIUS_PER_UNIT - celsius) > 1e-6) {
            return "Cold Sweat should read " + celsius + " °C, not " + units * Cold.CELSIUS_PER_UNIT;
        }
        PlayerTempCap cap = new PlayerTempCap();
        player.getInventory().items.set(0, new ItemStack(Thermometers.items().getFirst()));
        cap.calculateHudVisibility(player);
        return cap.showWorldTemp ? null : "one of our thermometers in the hotbar should show Cold Sweat's world gauge";
    }

    private static double temperature(ItemStack stack) {
        return stack.getOrDefault(ModItemComponents.WATER_TEMPERATURE.value(), 0.0);
    }

    static String flask(Player player) {
        Item empty = BuiltInRegistries.ITEM.get(Fundamentals.id("vacuum_flask"));
        Item filled = BuiltInRegistries.ITEM.get(Fundamentals.id("filled_vacuum_flask"));
        ItemStack skin = new ItemStack(ModItems.FILLED_WATERSKIN.get());
        skin.set(ModItemComponents.WATER_TEMPERATURE.value(), 40.0);
        var fill = new WaterskinEvent.Fill(new ItemStack(empty), skin.copy(), player, player.level(), player.blockPosition(), new FluidStack(Fluids.WATER, 250));
        NeoForge.EVENT_BUS.post(fill);
        ItemStack flask = fill.getResultWaterskin();
        if (!flask.is(filled) || temperature(flask) != 40) {
            return "Cold Sweat filling a vacuum flask should give a filled flask at the water's temperature, not " + flask + " at " + temperature(flask);
        }
        if (!flask.getCraftingRemainingItem().is(empty)) {
            return "a drunk flask should leave an empty flask, not " + flask.getCraftingRemainingItem();
        }
        player.getInventory().items.set(0, skin);
        player.getInventory().items.set(1, flask);
        for (int tick = 1; tick <= 1000; tick++) {
            player.tickCount = tick;
            skin.inventoryTick(player.level(), player, 0, false);
            flask.inventoryTick(player.level(), player, 1, false);
        }
        double skinLost = 40 - temperature(skin), flaskLost = 40 - temperature(flask);
        if (skinLost <= 0 || flaskLost <= 0 || Math.abs(skinLost / flaskLost - 10) > 0.5) {
            return "in the hotbar for 1,000 ticks a waterskin lost " + skinLost + " and a flask " + flaskLost + "; the flask should lose a tenth as much";
        }
        player.setItemInHand(InteractionHand.MAIN_HAND, flask);
        if (!FilledWaterskinItem.performPourAction(flask, player, InteractionHand.MAIN_HAND) || !player.getMainHandItem().is(empty)) {
            return "a filled flask should pour over its holder as a waterskin does and be left empty";
        }
        return null;
    }
}
