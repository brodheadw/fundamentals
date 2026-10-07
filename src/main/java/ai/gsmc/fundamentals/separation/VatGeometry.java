package ai.gsmc.fundamentals.separation;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * The vat's proportions, in sixteenths of a block. tools/build_separation_data.py owns the numbers and writes
 * them to fundamentals_vat.json beside the models it cuts from them; the renderer and the block read them
 * here, so a fluid is never drawn where the model has no wall and a player floats where the fluid is drawn.
 */
public record VatGeometry(int wall, int floor, int rimClearance, int weirBelowRim, int[] window) {

    public static final float PX = 1 / 16F;
    private static VatGeometry loaded;

    public static VatGeometry get() {
        if (loaded == null) {
            try (Reader in = new InputStreamReader(VatGeometry.class.getResourceAsStream("/fundamentals_vat.json"), StandardCharsets.UTF_8)) {
                loaded = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create().fromJson(in, VatGeometry.class);
            } catch (Exception e) {
                throw new IllegalStateException("Fundamentals: cannot read fundamentals_vat.json", e);
            }
        }
        return loaded;
    }

    /** The inside face of a wall, in blocks from the casing's edge. */
    public float wallIn() { return wall * PX; }

    /** The top of the floor, in blocks above the stage's bottom. */
    public float floorY() { return floor * PX; }

    /** How high the settled phases may stand in a stage {@code tall} blocks high. */
    public float brim(int tall) { return tall - rimClearance * PX; }

    /** The weir's lip between the trough and the bay; only the churn in the trough reaches it. */
    public float weir(int tall) { return tall - weirBelowRim * PX; }

    /** The depth the two phases share, half each when both are full. */
    public float depth(int tall) { return brim(tall) - floorY(); }

    /** The top of the aqueous layer, which lies on the floor. */
    public float aqueousTop(int tall, float aqueousFill) {
        return Math.min(brim(tall), floorY() + depth(tall) * 0.5F * aqueousFill);
    }

    /** The top of the organic layer floating on the aqueous: the surface of the vat. */
    public float surface(int tall, float aqueousFill, float organicFill) {
        return Math.min(brim(tall), aqueousTop(tall, aqueousFill) + depth(tall) * 0.5F * organicFill);
    }
}
