package pbouda.jeffrey.init;

import pbouda.jeffrey.init.command.InitCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
        name = "",
        subcommands = {
                InitCommand.class,
        },
        mixinStandardHelpOptions = true,
        version = "Jeffrey CLI 0.1",
        description = "Jeffrey CLI Application to simplify the setup and maintenance"
)
public class CliApplication {

    public static void main(String... args) {
        int exitCode = new CommandLine(new CliApplication())
                .setUsageHelpWidth(160)
                .execute(args);
        System.exit(exitCode);
    }
}
