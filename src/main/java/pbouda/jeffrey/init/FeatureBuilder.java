package pbouda.jeffrey.init;

import java.nio.file.Path;

public class FeatureBuilder {

    /* Performance data JVM options */
    private static final String PERF_DATA_OPTIONS = "-XX:+UsePerfData -XX:PerfDataSaveFile="
            + Path.of(Replacements.CURRENT_SESSION, "perf-counters.hsperfdata");

    /* Heap dump JVM options */
    private static final String HEAP_DUMP_OPTIONS = "-XX:+HeapDumpOnOutOfMemoryError "
            + "-XX:HeapDumpPath=" + Path.of(Replacements.CURRENT_SESSION, "heap-dump.hprof") + " "
            + "-XX:+CrashOnOutOfMemoryError "
            + "-XX:ErrorFile=" + Path.of(Replacements.CURRENT_SESSION, "hs-err.log");

    private boolean perfCountersEnabled;
    private boolean heapDumpEnabled;

    public FeatureBuilder setPerfCountersEnabled(boolean enabled) {
        perfCountersEnabled = enabled;
        return this;
    }

    public FeatureBuilder setHeapDumpEnabled(boolean enabled) {
        heapDumpEnabled = enabled;
        return this;
    }

    public String build(Path currentSessionPath) {
        StringBuilder options = new StringBuilder();

        if (perfCountersEnabled) {
            options.append(PERF_DATA_OPTIONS.replace(Replacements.CURRENT_SESSION, currentSessionPath.toString()));
            options.append(" ");
        }

        if (heapDumpEnabled) {
            options.append(HEAP_DUMP_OPTIONS.replace(Replacements.CURRENT_SESSION, currentSessionPath.toString()));
            options.append(" ");
        }

        return options.toString().trim();
    }
}
