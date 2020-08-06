package com.fox2code.fabriczero.reflectutils;

import com.fox2code.fabriczero.FabricZeroPlugin;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.game.GameProvider;
import net.fabricmc.loader.game.MinecraftGameProvider;
import net.fabricmc.loader.util.Arguments;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This class need to work without using ANY FabricZero classes
 */
final class AutoFixer {
    private static final boolean VERIFY_NONE =
            ManagementFactory.getRuntimeMXBean().getInputArguments().contains("-Xverify:none");

    static void plzFixme() {
        if (VERIFY_NONE) {
            System.out.println("Your JVM is not compatible with FabricZero!");
            System.exit(-3);
        }
        /*String entrypoint = FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT ?
                "net.fabricmc.loader.launch.knot.KnotClient" : "net.fabricmc.loader.launch.knot.KnotServer";*/
        FabricZeroPlugin.LOGGER.warn("The current environment doesn't allow FabricZero to launch correctly!");
        FabricZeroPlugin.LOGGER.warn("Restarting the game with a compatible environment!");
        FabricZeroPlugin.LOGGER.warn("To avoid this use an older JVM or add \"-Xverify:none\" as JVM args.");
        System.exit(-1);

        /*ArrayList<String> args = new ArrayList<>();
        try {
            args.addAll(Arrays.asList(AutoFixer9.getCommandLine().trim().split("\\s+")));
            int i = args.indexOf("-classpath");
            if (i == -1) {
                i = args.indexOf("-cp");
            }
            args.add(i, "-Xverify:none");
            System.out.println(args);
        } catch (Throwable t) {
            t.printStackTrace();
            args.clear();
            File java = new File(System.getProperty("java.home"));
            if (new File(java, "bin\\java.exe").exists()) {
                args.add(java.getAbsolutePath() + "\\bin\\java.exe");
            } else if (new File(java, "bin/java").exists()) {
                args.add(java.getAbsolutePath() + "/bin/java");
            } else {
                FabricZeroPlugin.LOGGER.error("Couldn't find java executable!");
                System.exit(-1);
            }
            args.addAll(ManagementFactory.getRuntimeMXBean().getInputArguments());
            args.addAll(Arrays.asList("-Xverify:none", "-classpath", System.getProperty("java.class.path"), entrypoint));
            GameProvider gameProvider = ((net.fabricmc.loader.FabricLoader) FabricLoader.getInstance()).getGameProvider();
            try {
                Field field = MinecraftGameProvider.class.getDeclaredField("arguments");
                field.setAccessible(false);
                args.addAll(Arrays.asList(((Arguments) field.get(gameProvider)).toArray()));
            } catch (Exception e) {
                if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
                    FabricZeroPlugin.LOGGER.error("Fail to get game arguments!");
                    FabricZeroPlugin.LOGGER.error("Your Java installation have disabled reflection API");
                    FabricZeroPlugin.LOGGER.error("But unfortunately FabricZero failed to enable it.");
                    FabricZeroPlugin.LOGGER.error("Please use a different Java installation.");
                    System.exit(-2);
                } else if (!gameProvider.canOpenErrorGui()) {
                    args.add("--nogui");
                }
                FabricZeroPlugin.LOGGER.warn("Guessed game argument from existing apis.");
                FabricZeroPlugin.LOGGER.warn("Some arguments might be missing.");
            }
        }
        Runtime.getRuntime().addShutdownHook(new Thread("Child Executor") {
            @Override
            public void run() {
                try {
                    System.out.println("Child exit code: "+
                            new ProcessBuilder(args).inheritIO().start().waitFor());
                } catch (Exception ioe) {
                    ioe.printStackTrace();
                }
            }
        });
        System.exit(0);*/
    }
}
