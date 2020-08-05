package com.fox2code.fabriczero.reflectutils;

import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Easier way to do reflection in java
 */
@SuppressWarnings("UnusedReturnValue")
public final class ReflectedClass implements Iterable<ReflectedClass>, Closeable {
    private static Field modifiersField;

    static {
        try {
            modifiersField = Field.class.getDeclaredField("modifiers");
            Java9Fix.setAccessible(modifiersField);
        } catch (ReflectiveOperationException ignored) {}
    }

    public static final ReflectedClass NULL = new ReflectedClass(null);

    public static ReflectedClass of(Object object) {
        if (object == null) {
            return NULL;
        } else if (object instanceof ReflectedClass) {
            return (ReflectedClass) object;
        } else {
            return new ReflectedClass(object);
        }
    }

    public static ReflectedClass forName(String name) throws ClassNotFoundException {
        return new ReflectedClass(Class.forName(name));
    }

    public static ReflectedClass forName(Class<?> ctx, String name) throws ClassNotFoundException {
        if (ctx == null) {
            return forName(name);
        }
        return new ReflectedClass(Class.forName(name.indexOf('.') == -1 ?
                ctx.getPackage().getName()+"."+name : name, false, ctx.getClassLoader()));
    }

    public static ReflectedClass $(Class<?> ctx, String exec) throws ReflectiveOperationException {
        int p = exec.indexOf('(');
        if (p == -1) {
            p = exec.lastIndexOf('#');
            ReflectedClass current;
            if (p == 0) {
                current = of(ctx);
            } else {
                current = forName(ctx, exec.substring(0, p));
            }
            return current.get(exec.substring(p+1));
        } else {
            String sub = exec.substring(0, p);
            p = sub.lastIndexOf('.');
            ReflectedClass current;
            if (p == 0) {
                current = of(ctx);
            } else {
                current = forName(ctx, sub.substring(0, p));
            }
            return current.run(sub.substring(p+1));
        }
    }

    private final Object object;

    private ReflectedClass(Object object) {
        this.object = object;
    }

    public Object getObject() {
        return this.isClass() ? null : this.object;
    }

    public Object getHandle() {
        return this.object;
    }

    @SuppressWarnings("unchecked")
    public <T> T $() {
        return (T) this.object;
    } // For shorter code

    public ReflectedClass get(String name) throws ReflectiveOperationException {
        return ReflectedClass.of(get0(name));
    }

    public ReflectedClass get(String name1, String name2) throws ReflectiveOperationException {
        return ReflectedClass.of(get0(name1, name2));
    }

    public Object get0(String name) throws ReflectiveOperationException {
        return findField(name).get(this.getObject());
    }

    public Object get0(String name1, String name2) throws ReflectiveOperationException {
        return findField2(name1, name2).get(this.getObject());
    }

    public void set(String name, Object value) throws ReflectiveOperationException {
        set0(name, value instanceof ReflectedClass ? ((ReflectedClass)value).getHandle() : value);
    }

    public void set(String name1, String name2, Object value) throws ReflectiveOperationException {
        set0(name1, name2, value instanceof ReflectedClass ? ((ReflectedClass)value).getHandle() : value);
    }

    public void set0(String name, Object value) throws ReflectiveOperationException {
        Field field = findField(name);
        if (field.isEnumConstant()) {
            ReflectUtil.forceSet(this.object, field, value);
            return;
            //throw new IllegalAccessException("Can't modify enum constants!");
        }
        if ((field.getModifiers() & Modifier.FINAL) != 0) {
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        }
        field.set(this.getObject(), value);
    }

    public void set0(String name1, String name2, Object value) throws ReflectiveOperationException {
        Field field = findField2(name1, name2);
        if (field.isEnumConstant()) {
            ReflectUtil.forceSet(this.object, field, value);
            return;
            //throw new IllegalAccessException("Can't modify enum constants!");
        }
        if ((field.getModifiers() & Modifier.FINAL) != 0) {
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        }
        field.set(this.getObject(), value);
    }

    public ReflectedClass run(String name, Object... args) throws ReflectiveOperationException {
        for (int i = 0;i < args.length;i++) if (args[i] instanceof ReflectedClass) args[i] = ((ReflectedClass) args[i]).getObject();
        return ReflectedClass.of(run0(name, args));
    }

    public Object run0(String name, Object... args) throws ReflectiveOperationException {
        return runAccessible(findMethod(name, args), args);
    }


    public Class<?> getObjClass() {
        return this.object == null ? null : this.object instanceof Class<?> ? (Class<?>) this.object : this.object.getClass();
    }

    @SuppressWarnings("ConstantConditions")
    public String getClassName() {
        return this.object == null ? "null" : this.getObjClass().getName();
    }

    public ReflectedClass newInstance(Object... args) throws ReflectiveOperationException {
        for (int i = 0;i < args.length;i++) if (args[i] instanceof ReflectedClass) args[i] = ((ReflectedClass) args[i]).getObject();
        return ReflectedClass.of(newInstance0(args));
    }

    public Object newInstance0(Object... args) throws ReflectiveOperationException {
        if (this.object instanceof Class<?>) {
            if (args.length == 0) {
                return runAccessible(((Class<?>) this.object).getDeclaredConstructor());
            } else {
                return runAccessible(((Class<?>) this.object).getDeclaredConstructors(), args);
            }
        } else throw new IllegalAccessException("Unable to create an instance on a non static context!");
    }

    private Object runAccessible(Executable[] executables,Object... args) throws ReflectiveOperationException {
        System.out.println(Arrays.toString(executables));
        System.out.println(Arrays.toString(args));
        global:
        for (Executable executable : executables) if (executable.getParameterCount() == args.length) {
            Parameter[] parameters = executable.getParameters();
            System.out.println(Arrays.toString(parameters));
            for (int i = 0;i < args.length;i++) if (args[i] != null) {
                if (!generify(parameters[i].getType()).isAssignableFrom(args[i].getClass())) {
                    System.out.println(parameters[i].getType() + " != " + args[i].getClass());
                    continue global;
                }
            }
            return runAccessible(executable, args);
        }
        throw new NoSuchMethodException("No compatible method found!");
    }

    private Field findField(String name) throws ReflectiveOperationException {
        Class<?> cl = this.getObjClass();
        while (cl != Object.class && cl != null) {
            for (Field field:cl.getDeclaredFields()) if (field.getName().equals(name) && Modifier.isStatic(field.getModifiers()) == this.isClass()) {
                if (!field.isAccessible()) Java9Fix.setAccessible(field);
                return field;
            }
            cl = cl.getSuperclass();
        }
        throw new NoSuchFieldException("No field found!");
    }

    private Field findField2(String name1,String name2) throws ReflectiveOperationException {
        Class<?> cl = this.getObjClass();
        while (cl != Object.class && cl != null) {
            for (Field field:cl.getDeclaredFields()) if ((field.getName().equals(name1) || field.getName().equals(name2)) && Modifier.isStatic(field.getModifiers()) == this.isClass()) {
                if (!field.isAccessible()) Java9Fix.setAccessible(field);
                return field;
            }
            cl = cl.getSuperclass();
        }
        throw new NoSuchFieldException("No field found!");
    }

    private Method findMethod(String name,Object... args) throws ReflectiveOperationException {
        Class<?> cl = this.getObjClass();
        while (cl != Object.class && cl != null) {
            global:
            for (Method method : cl.getDeclaredMethods()) if (method.getParameterCount() == args.length && method.getName().equals(name)) {
                Parameter[] parameters = method.getParameters();
                for (int i = 0; i < args.length; i++)
                    if (args[i] != null) {
                        if (!generify(parameters[i].getType()).isAssignableFrom(args[i].getClass()))
                            continue global;
                    }
                return method;
            }
            cl = cl.getSuperclass();
        }
        throw new NoSuchMethodException("No compatible method found!");
    }

    public Object runAccessible(Executable executable,Object... args) throws ReflectiveOperationException {
        if (!executable.isAccessible()) Java9Fix.setAccessible(executable);
        if (executable instanceof Constructor) {
            if (executable.getDeclaringClass().isEnum()) {
                return forceNewInstance((Constructor<?>) executable, args);
            }
            return ((Constructor<?>) executable).newInstance(args);
        }
        boolean isStatic;
        if ((isStatic = Modifier.isStatic(executable.getModifiers())) != this.isClass()) {
            if (isStatic) {
                throw new IllegalAccessException("Tried to run a static method on a non static context!");
            } else {
                throw new IllegalAccessException("Tried to run a non static method on a static context!");
            }
        }
        if (executable instanceof Method) {
            return ((Method) executable).invoke(this.getObject(),args);
        }
        throw new IllegalArgumentException("Invalid executable type! (Found: "+executable.getClass().getName()+")");
    }

    private Object forceNewInstance(Constructor<?> constructor,Object... args) throws ReflectiveOperationException {
        ReflectedClass reflectedConstruct = ReflectedClass.of(constructor);
        ReflectedClass ca = reflectedConstruct.get("constructorAccessor");
        if (ca.isNull()) {
            reflectedConstruct.set("constructorAccessor", ca = reflectedConstruct.run("acquireConstructorAccessor"));
        }
        return ca.run0("newInstance", (Object) args);
    }

    public String asString() {
        if (this.object instanceof String)
            return (String) this.object;
        throw new ClassCastException("Unable to cast " + this.getClassName() + " to String");
    }

    public int asInteger() {
        if (this.object instanceof Integer)
            return (Integer) this.object;
        throw new ClassCastException("Unable to cast " + this.getClassName() + " to Integer");
    }

    public boolean asBoolean() {
        if (this.object instanceof Boolean)
            return (Boolean) this.object;
        throw new ClassCastException("Unable to cast " + this.getClassName() + " to Boolean");
    }

    public Class<?> asClass() {
        if (this.object instanceof Class<?>)
            return (Class<?>) this.object;
        throw new ClassCastException("Unable to cast " + this.getClassName() + " to Class");
    }

    public boolean isClass() {
        return this.object instanceof Class;
    }

    public boolean isAbstract() {
        return this.object instanceof Class && (((Class<?>) this.object).getModifiers() & Modifier.ABSTRACT) != 0;
    }

    public boolean isEnum() {
        return this.object instanceof Class && ((Class<?>) this.object).isEnum();
    }

    public boolean isNull() {
        return this.object == null;
    }

    private static Class<?> generify(Class<?> cl) {
        if (!cl.isPrimitive()) return cl;
        if (cl == byte.class) return Byte.class;
        if (cl == short.class) return Short.class;
        if (cl == int.class) return Integer.class;
        if (cl == long.class) return Long.class;
        if (cl == float.class) return Float.class;
        if (cl == double.class) return Double.class;
        if (cl == boolean.class) return Boolean.class;
        if (cl == char.class) return Character.class;
        if (cl == void.class) return Void.class;
        throw new IllegalArgumentException("Invalid Primitive type!");
    }

    @Override
    public String toString() {
        return "ReflectedClass{"+ this.object + "}";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ReflectedClass &&
                Objects.equals(this.object, ((ReflectedClass) o).object);
    }

    @Override
    public int hashCode() {
        return object.hashCode()^~0;
    }

    @NotNull
    @Override
    public Iterator<ReflectedClass> iterator() {
        Object object = this.object;
        if (this.isClass()) {
            if (!this.isEnum()) {
                throw new ClassCastException("Can't iterate on static context");
            }
            try {
                object = this.run0("values");
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        if (object instanceof Object[]) {
            final Object[] array = (Object[]) object;
            return new Iterator<ReflectedClass>() {
                int index = 0;

                @Override
                public boolean hasNext() {
                    return !(index < array.length);
                }

                @Override
                public ReflectedClass next() {
                    return ReflectedClass.of(array[index++]);
                }
            };
        }
        if (!(object instanceof Iterable<?>)) {
            throw new ClassCastException(this.getClassName()+" is not Iterable!");
        }
        final Iterator<?> iterator = ((Iterable<?>) object).iterator();
        return new Iterator<ReflectedClass>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public ReflectedClass next() {
                return ReflectedClass.of(iterator.next());
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }

    public <T extends Throwable> void forEach$(ConsumerT<T> action) throws T {
        Objects.requireNonNull(action);
        for (ReflectedClass t : this) {
            action.accept(t);
        }
    }

    public boolean removeIf(Predicate<ReflectedClass> filter) {
        Objects.requireNonNull(filter);
        boolean removed = false;
        final Iterator<ReflectedClass> each = iterator();
        while (each.hasNext()) {
            if (filter.test(each.next())) {
                each.remove();
                removed = true;
            }
        }
        return removed;
    }

    public <T extends Throwable> boolean removeIf$(PredicateT<T> filter) throws T {
        Objects.requireNonNull(filter);
        boolean removed = false;
        final Iterator<ReflectedClass> each = iterator();
        while (each.hasNext()) {
            if (filter.test(each.next())) {
                each.remove();
                removed = true;
            }
        }
        return removed;
    }

    @Override
    public void close() throws IOException {
        if (this.object instanceof Closeable) {
            ((Closeable) this.object).close();
        }
    }

    public interface ConsumerT<T extends Throwable> {
        void accept(ReflectedClass t) throws T;
    }

    public interface PredicateT<T extends Throwable> {
        boolean test(ReflectedClass t) throws T;
    }
}
