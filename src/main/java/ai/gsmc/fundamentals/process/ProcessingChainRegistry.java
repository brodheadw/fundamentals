package ai.gsmc.fundamentals.process;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ProcessingChainRegistry {

    private static final Map<String, ProcessingChain> BY_ID = new LinkedHashMap<>();

    private ProcessingChainRegistry() {}

    public static ProcessingChain register(ProcessingChain chain) {
        if (BY_ID.putIfAbsent(chain.id(), chain) != null) {
            throw new IllegalStateException("Duplicate processing chain id '" + chain.id() + "'");
        }
        return chain;
    }

    public static int size() {
        return BY_ID.size();
    }

    public static List<String> validateAll() {
        List<String> errors = new ArrayList<>();
        for (ProcessingChain chain : BY_ID.values()) {
            errors.addAll(chain.validate());
        }
        return errors;
    }
}
