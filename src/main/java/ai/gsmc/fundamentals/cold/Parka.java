package ai.gsmc.fundamentals.cold;

import net.minecraft.world.item.ArmorItem;

/** The four parkas past leather and wool, warmest last; their insulation is data, in cold_sweat/item/insulator. */
public enum Parka {
    FUR("fur", 8, "deer_hide"),
    DOWN("down", 7, "waxed_canvas"),
    SYNTHETIC("synthetic", 9, "nylon_ripstop"),
    EXPEDITION("expedition", 12, "eptfe_laminate");

    public final String id;
    final int durability;
    final String shell;

    Parka(String id, int durability, String shell) {
        this.id = id;
        this.durability = durability;
        this.shell = shell;
    }

    public String piece(ArmorItem.Type type) {
        return id + switch (type) {
            case HELMET -> "_hood";
            case CHESTPLATE -> "_parka";
            case LEGGINGS -> "_trousers";
            case BOOTS, BODY -> "_boots";
        };
    }
}
