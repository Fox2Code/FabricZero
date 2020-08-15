package com.fox2code.fabriczero.api;

import com.fox2code.fabriczero.impl.ImplFabricZeroAPI;
import net.fabricmc.api.EnvType;

import java.io.File;

public interface FabricZeroAPI {
    int FLAG_DIRTY = 0x80000;

    static FabricZeroAPI getInstance() {
        return ImplFabricZeroAPI.INSTANCE;
    }

    boolean isHidden(String mod,String cls);

    /**
     * This method exist for optimisations purposes only
     */
    boolean hasHidingRules(String mod);

    void hideMod(String mod,String pkg);

    void addCurseProjectId(String mod,int projectId);

    void addCurseProjectId(String mod,int projectId,boolean strict);

    byte[] transformClass(byte[] bytecode, String name);

    void addTransformer(FabricZeroTransformer transformer);

    boolean isDev();

    EnvType getEnvType();

    File getFabricZeroConfigFile();

    boolean isClassDumpingEnabled();

    boolean isAccessModDisabled();
}
