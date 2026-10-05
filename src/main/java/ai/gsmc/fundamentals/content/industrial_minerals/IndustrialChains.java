package ai.gsmc.fundamentals.content.industrial_minerals;

import ai.gsmc.fundamentals.material.MaterialForm;
import ai.gsmc.fundamentals.process.ProcessingChain;
import ai.gsmc.fundamentals.process.ProcessingChainRegistry;
import ai.gsmc.fundamentals.process.ProcessingStage;

/**
 * Solid-route processing chains for {@code industrial_minerals}. The fluid routes (phosphoric
 * acid, chlor-alkali, Solvay soda ash) wait on the shared {@code FLUID} form; these are the dry
 * ones that need nothing new.
 */
public final class IndustrialChains {

    private IndustrialChains() {}

    public static void register() {
        // Silicon (tech backbone): quartz + carbon -> carbothermic MG-Si. (Siemens polysilicon
        // upgrade is a later step once a purified-silicon tier exists.)
        ProcessingChainRegistry.register(
                ProcessingChain.builder("silicon", "silicon", IndustrialMaterials.GROUP)
                        .step(ProcessingStage.CARBOTHERMIC_REDUCTION, "quartz", MaterialForm.RAW, "silicon", MaterialForm.INGOT)
                        .build());

        // Soda ash from trona: calcine to Na2CO3 (the natural route; the Solvay route needs fluids).
        ProcessingChainRegistry.register(
                ProcessingChain.builder("soda_ash", "soda_ash", IndustrialMaterials.GROUP)
                        .step(ProcessingStage.CALCINATION, "trona", MaterialForm.RAW, "soda_ash", MaterialForm.DUST)
                        .build());

        // Potash: froth-float sylvite from its salt gangue to muriate of potash (KCl).
        ProcessingChainRegistry.register(
                ProcessingChain.builder("potash", "potash", IndustrialMaterials.GROUP)
                        .step(ProcessingStage.FROTH_FLOTATION, "sylvite", MaterialForm.RAW, "potash", MaterialForm.DUST)
                        .build());

        // Elemental phosphorus: carbothermic smelt of apatite in an electric furnace.
        ProcessingChainRegistry.register(
                ProcessingChain.builder("phosphorus", "phosphorus", IndustrialMaterials.GROUP)
                        .step(ProcessingStage.CARBOTHERMIC_REDUCTION, "apatite", MaterialForm.RAW, "phosphorus", MaterialForm.DUST)
                        .build());
    }
}
