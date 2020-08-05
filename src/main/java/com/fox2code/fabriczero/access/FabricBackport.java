package com.fox2code.fabriczero.access;

import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.nio.file.Path;

/**
 * BackPort some used Fabric APIs
 */
@SuppressWarnings("deprecation")
public class FabricBackport {
    public static Path getConfigDir(FabricLoader fabricLoader) {
        return fabricLoader.getConfigDirectory().toPath();
    }

    public static Path getGameDir(FabricLoader fabricLoader) {
        return fabricLoader.getGameDirectory().toPath();
    }

    public static void prepareModInit(net.fabricmc.loader.FabricLoader fabricLoader, File newRunDir,Object gameInstance) {
        fabricLoader.prepareModInit(newRunDir.toPath(), gameInstance);
    }
}
