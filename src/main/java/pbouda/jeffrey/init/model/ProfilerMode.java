package pbouda.jeffrey.init.model;

import java.util.Arrays;

public enum ProfilerMode {
    DIRECT, CUSTOM_PATH, CUSTOM_CONFIG;

    private static final ProfilerMode[] VALUES = values();

    private static final String ALL_VALID_VALUES = String.join(", ",
            Arrays.stream(VALUES)
                    .map(Enum::name)
                    .toList());

    public static ProfilerMode resolve(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Repository type cannot be null");
        }

        return Arrays.stream(VALUES)
                .filter(e -> e.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid repository type: " + value + ". Valid values: " + ALL_VALID_VALUES));
    }
}
