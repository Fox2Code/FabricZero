package com.fox2code.fabriczero.mod;

import com.fox2code.fabriczero.FabricZeroPlugin;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class FabricZero implements ModInitializer {
    public static final Logger LOGGER = LogManager.getLogger("FabricZero");

    static {
        FabricZeroPlugin.ensureInit();
    }

    @Override
    public void onInitialize() {
        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            LOGGER.warn("Do not use this mod in your dev environment if you mod doesn't depends on it!");
        }
    }
}
