package pbouda.jeffrey.init;

import java.nio.file.Path;

public class DetectionFileResolver {

    private static final String PERF_DATA_OPTIONS = "-XX:+UsePerfData -XX:PerfDataSaveFile=" + Path.of(Replacements.CURRENT_SESSION, "perf-counters.hsperfdata");

    public String resolve(boolean perfCountersEnabled, Path currentSessionPath) {
        if (perfCountersEnabled) {
            return PERF_DATA_OPTIONS.replace(Replacements.CURRENT_SESSION, currentSessionPath.toString());
        } else {
            return "";
        }
    }
}
