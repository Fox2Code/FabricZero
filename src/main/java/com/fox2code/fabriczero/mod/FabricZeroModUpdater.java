package com.fox2code.fabriczero.mod;

import com.thebrokenrail.modupdater.api.entrypoint.ModUpdaterEntryPoint;

public class FabricZeroModUpdater implements ModUpdaterEntryPoint {
    @Override
    public boolean isVersionCompatible(String s) {
        return true; // We are Compatible with everything
    }
}
