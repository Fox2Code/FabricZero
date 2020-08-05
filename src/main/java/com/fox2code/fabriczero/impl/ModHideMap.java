package com.fox2code.fabriczero.impl;

import com.fox2code.fabriczero.api.FabricZeroAPI;
import com.google.common.collect.ForwardingMap;
import net.fabricmc.loader.ModContainer;

import java.util.Map;

public class ModHideMap extends ForwardingMap<String, ModContainer> {
    private final Map<String, ModContainer> delegate;
    private final FabricZeroAPI fabricZeroAPI;

    public ModHideMap(Map<String, ModContainer> delegate) {
        this.delegate = delegate;
        this.fabricZeroAPI = FabricZeroAPI.getInstance();
    }

    @Override
    protected Map<String, ModContainer> delegate() {
        return delegate;
    }

    @Override
    public boolean containsKey(Object key) {
        boolean containKey = super.containsKey(key);
        if (containKey && fabricZeroAPI.hasHidingRules((String) key)) {
            StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
            String cls = stackTraceElements[3].getClassName();
            containKey = !fabricZeroAPI.isHidden((String) key, cls);
        }
        return containKey;
    }
}
