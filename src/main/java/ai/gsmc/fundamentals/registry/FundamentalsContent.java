package ai.gsmc.fundamentals.registry;

import ai.gsmc.fundamentals.Fundamentals;
import ai.gsmc.fundamentals.content.base_metals.BaseMetalMaterials;
import ai.gsmc.fundamentals.content.ferrous_ferroalloy.FerrousChains;
import ai.gsmc.fundamentals.content.ferrous_ferroalloy.FerrousMaterials;
import ai.gsmc.fundamentals.content.industrial_minerals.IndustrialChains;
import ai.gsmc.fundamentals.content.industrial_minerals.IndustrialMaterials;
import ai.gsmc.fundamentals.content.precious_pgm.PreciousChains;
import ai.gsmc.fundamentals.content.precious_pgm.PreciousMaterials;
import ai.gsmc.fundamentals.content.rare_earths.RareEarthMaterials;
import ai.gsmc.fundamentals.material.Material;
import ai.gsmc.fundamentals.material.MaterialRegistry;
import ai.gsmc.fundamentals.process.ProcessingChainRegistry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

public final class FundamentalsContent {

    private FundamentalsContent() {}

    public static void registerAll() {
        FerrousMaterials.register();
        FerrousChains.register();
        PreciousMaterials.register();
        PreciousChains.register();
        IndustrialMaterials.register();
        IndustrialChains.register();
        RareEarthMaterials.register();
        BaseMetalMaterials.register();

        Fundamentals.LOGGER.info("Registered materials {} and {} processing chains",
                MaterialRegistry.all().stream().collect(
                        Collectors.groupingBy(Material::group, LinkedHashMap::new, Collectors.counting())),
                ProcessingChainRegistry.size());

        List<String> problems = ProcessingChainRegistry.validateAll();
        if (!problems.isEmpty()) {
            throw new IllegalStateException("Invalid processing chains: " + String.join("; ", problems));
        }
    }
}
