package com.fox2code.fabriczero.reflectutils;

import com.fox2code.fabriczero.FabricZeroPlugin;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class need to work without using ANY FabricZero classes
 * (Also note if FabricZero execute this class this probably mean
 *   that you can't do setAccessible(true) in this VM)
 */
final class AutoFixer {
    private static final boolean VERIFY_NONE =
            ManagementFactory.getRuntimeMXBean().getInputArguments().contains("-Xverify:none");

    static void plzFixme() {
        if (VERIFY_NONE) {
            System.out.println("Your JVM is not compatible with FabricZero!");
            System.exit(-3);
        }
        try {
            Class.forName("org.multimc.onesix.OneSixLauncher", false, AutoFixer.class.getClassLoader());
            // We are not compatible with MultiMC but lets help the user anyway
            FabricZeroPlugin.LOGGER.error("AutoFixer is not compatible with MultiMC at the moment!");
            FabricZeroPlugin.LOGGER.info("Here how to fix FabricZero manually on MultiMC");
            FabricZeroPlugin.LOGGER.info("Open your launcher -> Settings -> Java");
            FabricZeroPlugin.LOGGER.info("And add \"--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/jdk.internal.misc=ALL-UNNAMED --add-opens java.base/jdk.internal.vm.annotation=ALL-UNNAMED\"");
            FabricZeroPlugin.LOGGER.info("into the JVM arguments");
            System.exit(-1);
        } catch (Exception ignored) {}
        FabricZeroPlugin.LOGGER.warn("The current environment doesn't allow FabricZero to launch correctly!");
        FabricZeroPlugin.LOGGER.warn("Restarting the game with a compatible environment!");
        FabricZeroPlugin.LOGGER.warn("To avoid this use an older JVM or add \"-Xverify:none\" as JVM args.");

        ArrayList<String> args = new ArrayList<>();
        try {
            args.addAll(Arrays.asList(AutoFixer9.getCommandLine().trim().split("\\s+")));
            int i = args.indexOf("-classpath");
            if (i == -1) {
                i = args.indexOf("-cp");
            }
            args.addAll(i, Arrays.asList(
                    "--add-opens", "java.base/java.lang=ALL-UNNAMED",
                    "--add-opens", "java.base/jdk.internal.misc=ALL-UNNAMED",
                    "--add-opens", "java.base/jdk.internal.vm.annotation=ALL-UNNAMED"));
        } catch (Throwable t) {
            FabricZeroPlugin.LOGGER.error("AutoFixer couldn't do it's job!", t);
            System.exit(-2);
        }
        Runtime.getRuntime().addShutdownHook(new Thread("Child Executor") {
            @Override
            public void run() {
                try {
                    int exitCode;
                    System.out.println("Game exit code: "+
                            (exitCode = new ProcessBuilder(args).inheritIO().start().waitFor()));
                    if (exitCode != 0) { // In this VM we can't do System.halt() because no reflection 3:
                        System.exit(exitCode);
                    }
                } catch (Exception ioe) {
                    ioe.printStackTrace();
                }
            }
        });
        System.exit(0);
    }
}
