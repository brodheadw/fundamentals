package ai.gsmc.fundamentals.process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of all {@link ProcessingChain}s, mirroring {@link ai.gsmc.fundamentals.material.MaterialRegistry}.
 * Each group registers its own chains during init (PLAN §4).
 */
public final class ProcessingChainRegistry {

    private static final Map<String, ProcessingChain> BY_ID = new LinkedHashMap<>();

    private ProcessingChainRegistry() {}

    public static ProcessingChain register(ProcessingChain chain) {
        ProcessingChain prev = BY_ID.putIfAbsent(chain.id(), chain);
        if (prev != null) {
            throw new IllegalStateException("Duplicate processing chain id '" + chain.id() + "'");
        }
        return chain;
    }

    public static Collection<ProcessingChain> all() {
        return Collections.unmodifiableCollection(BY_ID.values());
    }

    public static int size() {
        return BY_ID.size();
    }

    /** Validates every registered chain; returns all problems found (empty = all valid). */
    public static List<String> validateAll() {
        List<String> errors = new ArrayList<>();
        for (ProcessingChain chain : BY_ID.values()) {
            errors.addAll(chain.validate());
        }
        return errors;
    }
}
