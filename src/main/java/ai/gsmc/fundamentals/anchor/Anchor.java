package ai.gsmc.fundamentals.anchor;

import ai.gsmc.fundamentals.Fundamentals;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * The road to a reality anchor, every step real but the last. A magnesium diboride superconductor, the wire MRI magnets are
 * wound from: boron won from boric acid with magnesium, packed with magnesium powder in a niobium barrier inside copper, drawn
 * and reacted under argon, wound and set in resin; cooled below its 39 K in a cryostat of liquid hydrogen. A quartz resonator
 * keeps its time, a microcontroller drives it, lead shields it and a lead-acid cell feeds it. What the coil then does to the
 * place around it is the anchor's own, and the one thing here no plant makes.
 */
public final class Anchor {
    private static final List<Item> ITEMS = new ArrayList<>();

    private Anchor() {}

    public static List<Item> items() { return List.copyOf(ITEMS); }

    private static Item item(BiConsumer<ResourceLocation, Item> registry, String name, Item item) {
        registry.accept(Fundamentals.id(name), item);
        ITEMS.add(item);
        return item;
    }

    public static void registerItems(BiConsumer<ResourceLocation, Item> registry) {
        for (String name : List.of("boron_oxide", "crude_boron", "amorphous_boron", "magnesium_diboride_precursor", "niobium_ingot", "niobium_sheet",
                "magnesium_diboride_billet", "drawn_magnesium_diboride_wire", "magnesium_diboride_wire", "coil_former", "superconducting_coil",
                "cryostat", "cryogenic_magnet", "quartz_resonator")) {
            item(registry, name, new Item(new Item.Properties()));
        }
        item(registry, "incomplete_superconducting_coil", new SequencedAssemblyItem(new Item.Properties()));
        item(registry, "incomplete_cryogenic_magnet", new SequencedAssemblyItem(new Item.Properties()));
        item(registry, "reality_anchor", new RealityAnchorItem(new Item.Properties().durability(RealityAnchorItem.CHARGE).rarity(Rarity.EPIC)));
    }
}
