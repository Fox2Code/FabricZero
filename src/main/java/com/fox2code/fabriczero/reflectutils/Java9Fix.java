package com.fox2code.fabriczero.reflectutils;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings("JavaReflectionMemberAccess")
final class Java9Fix {
    private static final boolean java8 = System.getProperty("java.version").startsWith("1.");
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

    static {
        Class<?> unsafeClass;
        try {
            try {
                unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
                internalUnsafe = true;
            } catch (ClassNotFoundException e) {
                unsafeClass = Class.forName("sun.misc.Unsafe");
            }
            Field field = unsafeClass.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = field.get(null);
            fieldOffset = unsafeClass.getDeclaredMethod("objectFieldOffset", Field.class);
            fieldOffset2 = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);
            staticFieldBase = unsafeClass.getDeclaredMethod("staticFieldBase", Field.class);
            allocateInstance = unsafeClass.getDeclaredMethod("allocateInstance", Class.class);
            fieldPutBool = unsafeClass.getDeclaredMethod("putBoolean", Object.class, long.class, boolean.class);
            fieldPutInt = unsafeClass.getDeclaredMethod("putInt", Object.class, long.class, int.class);
            fieldPutObject = unsafeClass.getDeclaredMethod("putObject", Object.class, long.class, Object.class);
        } catch (ReflectiveOperationException e) {
            throw new Error(e);
        }
        if (!java8) /* try */ {
            try {
                //Disable Java9+ Reflection Warnings
                Method putObjectVolatile = unsafeClass.getDeclaredMethod("putObjectVolatile", Object.class, long.class, Object.class);
                Method staticFieldOffset = unsafeClass.getDeclaredMethod("staticFieldOffset", Field.class);
                Class<?> loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
                Field loggerField = loggerClass.getDeclaredField("logger");
                Long offset = (Long) staticFieldOffset.invoke(unsafe, loggerField);
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
            } catch (ReflectiveOperationException ignored) {
                System.out.println("[Java9Fix]: Unable to disable reflections checks");
            }
        }
    }

    private static Field access;
    private static long accessOffset;

    public static void setAccessible(AccessibleObject field) throws ReflectiveOperationException {
        if (access == null) {
            access = AccessibleObject.class.getDeclaredField("override");
            accessOffset = (Long) fieldOffset.invoke(unsafe, access);
            fieldPutBool.invoke(unsafe, access, accessOffset, true);
        }
        fieldPutBool.invoke(unsafe, field, accessOffset, true);
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

    public static Object asROFieldAccessor(Object fieldAccessor)  {
        return java8 ? ROFieldAccessor8.from(fieldAccessor) : ROFieldAccessor9.from(fieldAccessor);
    }

    static boolean isJava8() {
        return java8;
    }

    static boolean isJava9() {
        return !java8;
    }
}
