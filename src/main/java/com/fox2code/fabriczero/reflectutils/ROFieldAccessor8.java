package com.fox2code.fabriczero.reflectutils;

import sun.reflect.FieldAccessor;

/**
 * Exact copy of {@link ROFieldAccessor9} but for java8
 */
final class ROFieldAccessor8 implements FieldAccessor {
    private final FieldAccessor handle;

    public static Object from(Object fieldAccessor) {
        return new ROFieldAccessor8((FieldAccessor) fieldAccessor);
    }

    private ROFieldAccessor8(FieldAccessor handle) {
        this.handle = handle;
    }

    @Override
    public Object get(Object obj) throws IllegalArgumentException {
        return handle.get(obj);
    }

    @Override
    public boolean getBoolean(Object obj) throws IllegalArgumentException {
        return handle.getBoolean(obj);
    }

    @Override
    public byte getByte(Object obj) throws IllegalArgumentException {
        return handle.getByte(obj);
    }

    @Override
    public char getChar(Object obj) throws IllegalArgumentException {
        return handle.getChar(obj);
    }

    @Override
    public short getShort(Object obj) throws IllegalArgumentException {
        return handle.getShort(obj);
    }

    @Override
    public int getInt(Object obj) throws IllegalArgumentException {
        return handle.getInt(obj);
    }

    @Override
    public long getLong(Object obj) throws IllegalArgumentException {
        return handle.getLong(obj);
    }

    @Override
    public float getFloat(Object obj) throws IllegalArgumentException {
        return handle.getFloat(obj);
    }

    @Override
    public double getDouble(Object obj) throws IllegalArgumentException {
        return handle.getDouble(obj);
    }

    @Override
    public void set(Object obj, Object value) throws IllegalArgumentException, IllegalAccessException {
        // No op
    }

    @Override
    public void setBoolean(Object obj, boolean z) throws IllegalArgumentException, IllegalAccessException {
        // No op
    }

    @Override
    public void setByte(Object obj, byte b) throws IllegalArgumentException, IllegalAccessException {
        // No op
    }

    @Override
    public void setChar(Object obj, char c) throws IllegalArgumentException, IllegalAccessException {
        // No op
    }

    @Override
    public void setShort(Object obj, short s) throws IllegalArgumentException, IllegalAccessException {
        // No op
    }

    @Override
    public void setInt(Object obj, int i) throws IllegalArgumentException, IllegalAccessException {
        // No op
    }

    @Override
    public void setLong(Object obj, long l) throws IllegalArgumentException, IllegalAccessException {
        // No op
    }

    @Override
    public void setFloat(Object obj, float f) throws IllegalArgumentException, IllegalAccessException {
        // No op
    }

    @Override
    public void setDouble(Object obj, double d) throws IllegalArgumentException, IllegalAccessException {
        // No op
    }
}
