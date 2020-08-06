package com.fox2code.fabriczero.reflectutils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.metadata.ModMetadataV1;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ReflectUtil {
    private static final JsonParser jsonParser = new JsonParser();
    private static Field enumConstantDirectory;
    private static final boolean ModMetadata_getCustomValues;

    static {
        try {
            enumConstantDirectory = Class.class.getDeclaredField("enumConstantDirectory");
            Java9Fix.setAccessible(enumConstantDirectory);
        } catch (Throwable ignored) {}
        boolean tmp;
        try {
            ModMetadata.class.getDeclaredMethod("getCustomValues");
            tmp = true;
        } catch (Exception e) {
            tmp = false;
        }
        ModMetadata_getCustomValues = tmp;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void copyEnumValue(Class<? extends Enum<?>> cl, String from, @Nullable String to) throws ReflectiveOperationException {
        Enum<?> unify = Enum.valueOf((Class) cl, from);
        for (Field f:cl.getDeclaredFields()) {
            if (f.isEnumConstant() && f.getName().equals(to)) {
                Java9Fix.setObject(null, f, unify);
            }
        }
        if (enumConstantDirectory == null) return;
        Map<String, Enum<?>> enumDirectory = (Map<String, Enum<?>>) enumConstantDirectory.get(cl);
        if (enumDirectory != null) {
            enumDirectory.put(to, unify);
        }
    }

    public static void setAccessible(AccessibleObject accessibleObject) throws ReflectiveOperationException {
        if (accessibleObject == null) {
            throw new NullPointerException("accessibleObject == null");
        }
        Java9Fix.setAccessible(accessibleObject);
    }

    public static Object forceGet(Object instance,Field field) throws ReflectiveOperationException {
        Java9Fix.setAccessible(field);
        return field.get(instance);
    }

    public static void forceSet(Object instance,Field field,Object value) throws ReflectiveOperationException {
        if (Modifier.isStatic(field.getModifiers()) != (instance == null)) {
            if (instance == null) {
                throw new IllegalAccessException("Can't set field on null instance!");
            } else {
                instance = null;
            }
        }
        // Invalid call to this method cause crash of the VM so we better do some checks before calling it
        Java9Fix.setObject(instance == null ? null :
                field.getDeclaringClass().cast(instance), field,
                value == null ? null : field.getType().cast(value));
    }

    public static void forceSet(Object instance,Field field,boolean value) throws ReflectiveOperationException {
        if (Modifier.isStatic(field.getModifiers()) != (instance == null)) {
            if (instance == null) {
                throw new IllegalAccessException("Can't set field on null instance!");
            } else {
                instance = null;
            }
        }
        // Invalid call to this method cause crash of the VM so we better do some checks before calling it
        Java9Fix.setBoolean(instance == null ? null :
                        field.getDeclaringClass().cast(instance), field, value);
    }

    public static void forceSet(Object instance,Field field,int value) throws ReflectiveOperationException {
        if (Modifier.isStatic(field.getModifiers()) != (instance == null)) {
            if (instance == null) {
                throw new IllegalAccessException("Can't set field on null instance!");
            } else {
                instance = null;
            }
        }
        // Invalid call to this method cause crash of the VM so we better do some checks before calling it
        Java9Fix.setInt(instance == null ? null :
                field.getDeclaringClass().cast(instance), field, value);
    }

    @SuppressWarnings("unchecked")
    public static <T> void forceReplace(Object instance, Field field, Function<T, T> value) throws ReflectiveOperationException {
        forceSet(instance, field, value.apply((T) forceGet(instance, field)));
    }

    @SuppressWarnings("unchecked")
    public static <K, V> Map<K, V> getModifiable(Map<K, V> map) {
        if (map.getClass().getName().endsWith("UnmodifiableMap")) {
            try {
                return (Map<K, V>) forceGet(map, getFirstOfType(map.getClass().getDeclaredFields(), Map.class));
            } catch (ReflectiveOperationException ignored) {}
        }
        return map;
    }

    private static Method CustomValueImpl_fromJsonElement = null;

    public static CustomValue cvFromJson(JsonElement jsonElement) {
        try {
            if (CustomValueImpl_fromJsonElement == null) {
                CustomValueImpl_fromJsonElement = Class.forName("net.fabricmc.loader.metadata.CustomValueImpl")
                        .getDeclaredMethod("fromJsonElement", JsonElement.class);
                Java9Fix.setAccessible(CustomValueImpl_fromJsonElement);
            }
            return (CustomValue) CustomValueImpl_fromJsonElement.invoke(null, jsonElement);
        } catch (Exception e) {
            return null;
        }
    }

    public static CustomValue cvFromJson(String json) {
        return cvFromJson(jsonParser.parse(json));
    }

    private static Field ModMetadataV1_custom = null;
    private static Field CustomValueContainer_customValues = null;

    @SuppressWarnings("unchecked")
    public static void injectCustomValue(ModMetadata modMetadata, String key, String value) {
        if (ModMetadata_getCustomValues) {
            Map<String, CustomValue> map = modMetadata.getCustomValues();
            if (map == Collections.EMPTY_MAP) {
                try {
                    if (ModMetadataV1_custom == null) {
                        ModMetadataV1_custom = ModMetadataV1.class.getDeclaredField("custom");
                        Java9Fix.setAccessible(ModMetadataV1_custom);
                    }
                    if (CustomValueContainer_customValues == null) {
                        CustomValueContainer_customValues =
                                ModMetadataV1.CustomValueContainer.class.getDeclaredField("customValues");
                        Java9Fix.setAccessible(CustomValueContainer_customValues);
                    }
                    forceSet(forceGet(modMetadata, ModMetadataV1_custom),
                            CustomValueContainer_customValues, Collections.unmodifiableMap(map = new HashMap<>()));
                } catch (ReflectiveOperationException ignored) {
                    return;
                }
            } else {
                map = ReflectUtil.getModifiable(map);
            }
            map.put(key, ReflectUtil.cvFromJson(value));
        } else try {
            if (ModMetadataV1_custom == null) {
                ModMetadataV1_custom = ModMetadataV1.class.getDeclaredField("custom");
                Java9Fix.setAccessible(ModMetadataV1_custom);
            }
            ((Map<String, JsonElement>)ModMetadataV1_custom.get(modMetadata))
                    .put(key, jsonParser.parse(value));
        } catch (ReflectiveOperationException ignored) {}
    }

    public static <T> T allocateInstance(Class<T> cls) throws ReflectiveOperationException {
        if (cls == null) {
            throw new NullPointerException("cls must not be null!");
        }
        if (cls.isInterface()) {
            throw new IllegalArgumentException("Can't allocate interface instance!");
        }
        return Java9Fix.allocateInstance(cls);
    }

    public static boolean isInternalUnsafe() {
        return Java9Fix.internalUnsafe;
    }

    public static Field getFirstOfType(Field[] fields,Class<?> type) {
        for (Field field:fields) {
            if (type.isAssignableFrom(field.getType())) {
                return field;
            }
        }
        return null;
    }

    /* private static Constructor<MethodHandles.Lookup> ctx;
    private static final MethodType lambda = MethodType.methodType(Runnable.class, String[].class);
    private static final MethodType main = MethodType.methodType(void.class, String[].class);
    private static final MethodType ret = MethodType.methodType(void.class);

    public static Thread asEntryPoint(Class<?> cls,String[] args) throws ReflectiveOperationException {
        if (cls == null) {
            throw new NullPointerException("Can't create an entry point on a null class!");
        }
        if (ctx == null) {
            ctx = MethodHandles.Lookup.class.
                    getDeclaredConstructor(Class.class,int.class);
            Java9Fix.setAccessible(ctx);
        }
        MethodHandles.Lookup lookup = ctx.newInstance(cls, -1);
        try {
            return new Thread((Runnable) LambdaMetafactory.metafactory(
                    lookup, "run", lambda, ret,
                    lookup.findStatic(cls, "main", main), ret)
                    .getTarget().invokeWithArguments((Object) args));
        }catch (ReflectiveOperationException e) {
            throw e;
        } catch (Throwable t) {
            t.printStackTrace();
            return null;
        }
    } */
}
