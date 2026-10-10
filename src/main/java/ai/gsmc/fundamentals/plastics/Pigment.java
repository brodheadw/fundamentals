package ai.gsmc.fundamentals.plastics;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.DyeColor;

import javax.annotation.Nullable;

public enum Pigment implements StringRepresentable {
    NONE(null), WHITE(DyeColor.WHITE), ORANGE(DyeColor.ORANGE), MAGENTA(DyeColor.MAGENTA), LIGHT_BLUE(DyeColor.LIGHT_BLUE),
    YELLOW(DyeColor.YELLOW), LIME(DyeColor.LIME), PINK(DyeColor.PINK), GRAY(DyeColor.GRAY), LIGHT_GRAY(DyeColor.LIGHT_GRAY),
    CYAN(DyeColor.CYAN), PURPLE(DyeColor.PURPLE), BLUE(DyeColor.BLUE), BROWN(DyeColor.BROWN), GREEN(DyeColor.GREEN),
    RED(DyeColor.RED), BLACK(DyeColor.BLACK);

    @Nullable
    public final DyeColor dye;

    Pigment(@Nullable DyeColor dye) {
        this.dye = dye;
    }

    public static Pigment of(DyeColor dye) {
        return values()[dye.ordinal() + 1];
    }

    @Override
    public String getSerializedName() {
        return dye == null ? "none" : dye.getSerializedName();
    }
}
