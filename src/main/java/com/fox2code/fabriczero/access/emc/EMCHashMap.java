package com.fox2code.fabriczero.access.emc;

import com.fox2code.fabriczero.reflectutils.ReflectedClass;

import java.util.*;

public class EMCHashMap<T> extends HashMap<String, T> {
    private static final Set<String> compatiblesMods =
            new HashSet<>(Arrays.asList("optifine", "lithium", "phosphor"));

    @Override
    public T put(String key, T value) {
        if (compatiblesMods.contains(key)) try {
            ReflectedClass.of(value).get("conflicts")
                    .removeIf(r -> compatiblesMods.contains(r.asString()));
        } catch (Throwable ignored) {}
        return super.put(key, value);
    }
}
