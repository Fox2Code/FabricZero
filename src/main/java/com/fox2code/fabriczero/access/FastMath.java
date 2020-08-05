package com.fox2code.fabriczero.access;

import net.minecraft.util.math.MathHelper;

@SuppressWarnings("unused")
public class FastMath {
    public static double sin(double val) {
        return MathHelper.sin((float) val);
    }

    public static float sin(float val) {
        return MathHelper.sin(val);
    }

    public static double cos(double val) {
        return MathHelper.cos((float) val);
    }

    public static float cos(float val) {
        return MathHelper.cos(val);
    }
}
