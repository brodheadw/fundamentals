package ai.gsmc.fundamentals.worldgen;

//? if fabric {
import ai.gsmc.fundamentals.Fundamentals;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.biome.v1.ModificationPhase;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.GenerationStep;
//?}

/**
 * Puts the ore features into biomes on Fabric. NeoForge does this from the generated
 * {@code neoforge/biome_modifier} JSON instead, so this class is empty there.
 */
public final class OreSpawns {

    private OreSpawns() {}

    public static void apply() {
        //? if fabric {
        OreData data = OreData.get();
        for (OreData.Spawn spawn : data.add()) {
            var biomes = BiomeSelectors.tag(TagKey.create(Registries.BIOME, ResourceLocation.parse(spawn.biomes())));
            for (String feature : spawn.features()) {
                BiomeModifications.addFeature(biomes, GenerationStep.Decoration.UNDERGROUND_ORES,
                        ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.parse(feature)));
            }
        }
        BiomeModifications.create(ResourceLocation.fromNamespaceAndPath(Fundamentals.MOD_ID, "remove_vanilla_iron"))
                .add(ModificationPhase.REMOVALS, BiomeSelectors.foundInOverworld(), context -> {
                    for (String feature : data.remove()) {
                        context.getGenerationSettings().removeFeature(GenerationStep.Decoration.UNDERGROUND_ORES,
                                ResourceKey.create(Registries.PLACED_FEATURE, ResourceLocation.parse(feature)));
                    }
                });
        //?}
    }
}
