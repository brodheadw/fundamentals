package ai.gsmc.fundamentals.separation;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.GsonBuilder;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

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

    public float wallIn() { return wall * PX; }

    public float floorY() { return floor * PX; }

    public float brim(int tall) { return tall - rimClearance * PX; }

    public float weir(int tall) { return tall - weirBelowRim * PX; }

    public float depth(int tall) { return brim(tall) - floorY(); }

    public float aqueousTop(int tall, float aqueousFill) {
        return Math.min(brim(tall), floorY() + depth(tall) * 0.5F * aqueousFill);
    }

    public float surface(int tall, float aqueousFill, float organicFill) {
        return Math.min(brim(tall), aqueousTop(tall, aqueousFill) + depth(tall) * 0.5F * organicFill);
    }
}
