package ai.gsmc.fundamentals;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//? if fabric {
import net.fabricmc.api.ModInitializer;
//?}

//? if neoforge {
/*import net.neoforged.fml.common.Mod;
*///?}

/**
 * Common entrypoint for Fundamentals.
 *
 * Loader-specific wiring is selected at build time by Stonecutter comments
 * ({@code //? if fabric} / {@code //? if neoforge}). Shared logic lives in
 * {@link #init()} so every loader runs the same code path.
 */
//? if neoforge {
/*@Mod(Fundamentals.MOD_ID)
*///?}
public class Fundamentals /*? if fabric {*/ implements ModInitializer /*?}*/ {

    public static final String MOD_ID = "fundamentals";
    public static final Logger LOGGER = LoggerFactory.getLogger("Fundamentals");

    //? if fabric {
    @Override
    public void onInitialize() {
        init();
    }
    //?}

    //? if neoforge {
    /*public Fundamentals() {
        init();
    }
    *///?}

    /** Loader-agnostic setup. Register content, materials, and processing here. */
    public static void init() {
        LOGGER.info("Fundamentals initializing ({} loader)", loaderName());
        ai.gsmc.fundamentals.registry.FundamentalsContent.registerAll();
    }

    private static String loaderName() {
        //? if fabric {
        return "Fabric";
        //?}
        //? if neoforge {
        /*return "NeoForge";
        *///?}
    }
}
