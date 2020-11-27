package com.fox2code.fabriczero.reflectutils;

import net.fabricmc.loader.api.FabricLoader;
import net.gudenau.minecraft.asm.impl.ReflectionHelper;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

final class Java9Fix {
    private static final boolean java8 = System.getProperty("java.version").startsWith("1.");
    private static final boolean gudASM = FabricLoader.getInstance().isModLoaded("gud_asm");
    private static final Object unsafe;
    private static final Method fieldOffset, fieldOffset2;
    private static final Method staticFieldBase;
    private static final Method allocateInstance;
    private static final Method fieldPutBool;
    private static final Method fieldPutInt;
    private static Method fieldGetObject;
    private static final Method fieldPutObject;
    private static Method getModule;
    private static long moduleDescOffset;
    private static long moduleDescOpenOffset;
    static boolean internalUnsafe;
    static boolean fallBackMode;

    private static MethodHandle accessSetter;
    private static Field access;
    private static long accessOffset;

    static {
        if (gudASM) {
            try {
                accessSetter = ReflectionHelper.findSetter(AccessibleObject.class, "override", boolean.class);
            } catch (Exception ignored) {}
        }
        try { // Try this once to see if we can do it sooner
            Class<?> cl = Class.forName("jdk.internal.reflect.Reflection");
            Field field = getFieldBypass(cl, "fieldFilterMap");
            setAccessibleHelper(field);
            if (field != null) {
                ((Map<?, ?>) field.get(null)).clear();
            }
        } catch (Throwable ignored) {}
        Class<?> unsafeClass;
        try {
            try {
                unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
                internalUnsafe = true;
            } catch (ClassNotFoundException e) {
                unsafeClass = Class.forName("sun.misc.Unsafe");
            }
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            Object _unsafe;
            try {
                try {
                    setAccessibleHelper(field);
                } catch (Exception e) {
                    try {
                        access = AccessibleObject.class.getDeclaredField("override");
                    } catch (Exception e1) {
                        access = getFieldBypass(AccessibleObject.class, "override");
                    }
                    setAccessibleHelper(access);
                    access.setBoolean(field, true);
                }
            } catch (Exception e) {
                AutoFixer.plzFixme();
            }
            _unsafe = field.get(null);
            unsafe = _unsafe;
            fieldOffset = unsafeClass.getDeclaredMethod("objectFieldOffset", Field.class);
            fieldOffset2 = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);
            staticFieldBase = unsafeClass.getDeclaredMethod("staticFieldBase", Field.class);
            allocateInstance = unsafeClass.getDeclaredMethod("allocateInstance", Class.class);
            fieldPutBool = unsafeClass.getDeclaredMethod("putBoolean", Object.class, long.class, boolean.class);
            fieldPutInt = unsafeClass.getDeclaredMethod("putInt", Object.class, long.class, int.class);
            fieldPutObject = unsafeClass.getDeclaredMethod("putObject", Object.class, long.class, Object.class);
            try {
                setAccessibleHelper(fieldOffset);
                setAccessibleHelper(fieldOffset2);
                setAccessibleHelper(staticFieldBase);
                setAccessibleHelper(allocateInstance);
                setAccessibleHelper(fieldPutBool);
                setAccessibleHelper(fieldPutInt);
                setAccessibleHelper(fieldPutObject);
            } catch (ReflectiveOperationException reflectiveOperationException) {
                AutoFixer.plzFixme();
            }
        } catch (ReflectiveOperationException e) {
            throw new Error("Your JVM May be incompatible with FabricZero", e);
        }
        if (!java8) /* try */ {
            try {
                Class<?> cl = Class.forName("jdk.internal.reflect.Reflection");
                Field field = getFieldBypass(cl, "fieldFilterMap");
                setAccessibleHelper(field);
                if (field != null) {
                    ((Map<?, ?>) field.get(null)).clear();
                }
            } catch (Exception e) {
                System.out.println("[Java9Fix]: Unable to disable reflections blocker");
            }
            try {
                //Disable Java9+ Reflection Warnings
                Method putObjectVolatile = unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
                setAccessible(putObjectVolatile);
                Class<?> loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
                Field loggerField = loggerClass.getDeclaredField("logger");
                Long offset = (Long) fieldOffset2.invoke(unsafe, loggerField);
                putObjectVolatile.invoke(unsafe, loggerClass, offset, null);
            } catch (ReflectiveOperationException ignored) {
                System.out.println("[Java9Fix]: Unable to disable invalid access logging");
            }
            try {
                getModule = Class.class.getDeclaredMethod("getModule");
                Class<?> module = Class.forName("java.lang.Module");
                moduleDescOffset = (Long) fieldOffset.invoke(unsafe, module.getDeclaredField("descriptor"));
                Class<?> desc = Class.forName("java.lang.module.ModuleDescriptor");
                moduleDescOpenOffset = (Long) fieldOffset.invoke(unsafe, desc.getDeclaredField("open"));
                fieldGetObject = unsafeClass.getDeclaredMethod("getObject", Object.class, long.class);
                openModule(Class.class);
                openModule(unsafeClass);
            } catch (ReflectiveOperationException ignored) {
                System.out.println("[Java9Fix]: Unable to disable reflections checks");
            }
        }
        if (fallBackMode) try {
            fallBackMode = false;
            access = null;
            setAccessible(fieldOffset);
        } catch (Throwable ignored) {}
    }

    private static Field getFieldBypass(Class<?> cls,String fieldName) throws ReflectiveOperationException {
        Field[] fields;
        try {
            Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
            setAccessibleHelper(getDeclaredFields0);
            fields = (Field[]) getDeclaredFields0.invoke(cls, false);
        } catch (NoSuchMethodException e) {
            Method getDeclaredFieldsImpl = Class.class.getDeclaredMethod("getDeclaredFieldsImpl");
            setAccessibleHelper(getDeclaredFieldsImpl);
            fields = (Field[]) getDeclaredFieldsImpl.invoke(cls);
        }
        for (Field field:fields) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }

    private static void setAccessibleHelper(AccessibleObject field) throws ReflectiveOperationException {
        if (accessSetter != null) {
            try {
                accessSetter.invoke(field, true);
            } catch (Throwable ignored) {}
            if (field.isAccessible()) {
                return;
            }
        }
        try {
            field.setAccessible(true);
        } catch (Exception e) {
            Method.class.getMethod("invoke", Object.class, Object[].class).invoke(
                    Field.class.getMethod("setAccessible", boolean.class), field, new Object[]{true});
        }
    }

    public static void setAccessible(AccessibleObject field) throws ReflectiveOperationException {
        if (fallBackMode) {
            setAccessibleHelper(field);
            return;
        }
        if (access == null) try {
            access = AccessibleObject.class.getDeclaredField("override");
            accessOffset = (Long) fieldOffset.invoke(unsafe, access);
            fieldPutBool.invoke(unsafe, access, accessOffset, true);
        } catch (Exception e) {
            fallBackMode = true;
            setAccessibleHelper(field);
            return;
        }
        if (fieldPutBool != null) {
            fieldPutBool.invoke(unsafe, field, accessOffset, true);
        } else {
            field.setAccessible(true);
        }
    }

    public static void setBoolean(Object obj, Field field, boolean value) throws ReflectiveOperationException {
        fieldPutBool.invoke(unsafe, obj == null ? staticFieldBase.invoke(unsafe, field) : obj,
                (Long) (obj == null ? fieldOffset2 : fieldOffset).invoke(unsafe, field), value);
    }

    public static void setInt(Object obj, Field field, int value) throws ReflectiveOperationException {
        fieldPutInt.invoke(unsafe, obj == null ? staticFieldBase.invoke(unsafe, field) : obj,
                (Long) (obj == null ? fieldOffset2 : fieldOffset).invoke(unsafe, field), value);
    }

    public static void setObject(Object obj, Field field, Object value) throws ReflectiveOperationException {
        fieldPutObject.invoke(unsafe, obj == null ? staticFieldBase.invoke(unsafe, field) : obj,
                (Long) (obj == null ? fieldOffset2 : fieldOffset).invoke(unsafe, field), value);
    }

    @SuppressWarnings("unchecked")
    public static <T> T allocateInstance(Class<T> cls) throws ReflectiveOperationException {
        return (T) allocateInstance.invoke(unsafe, cls);
    }

    public static void openModule(Class<?> cl) throws ReflectiveOperationException {
        if (!java8 && fieldGetObject != null) {
            Object tmp = getModule.invoke(cl);
            tmp = fieldGetObject.invoke(unsafe, tmp, moduleDescOffset);
            fieldPutBool.invoke(unsafe, tmp, moduleDescOpenOffset, true);
        }
    }

    static boolean isJava8() {
        return java8;
    }

    static boolean isJava9() {
        return !java8;
    }
}
