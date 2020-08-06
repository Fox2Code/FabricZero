package com.fox2code.fabriczero.reflectutils;

import com.fox2code.udk.build.ASM;

import java.lang.reflect.AccessibleObject;

/**
 * This class require "-Xverify:none" to work on the latest JVMs
 */
final class SetAccessibleHax extends AccessibleObject {
    private static final SetAccessibleHax INSTANCE = new SetAccessibleHax();

    private SetAccessibleHax() {}

    static void test() {}

    static void setAccessible(AccessibleObject accessible) {
        try {
            accessible.setAccessible(true);
        } catch (Exception e) {
            INSTANCE.setAccessible0(accessible);
        }
    }

    private void setAccessible0(AccessibleObject accessible) throws SecurityException {
        ASM._ASTORE_0_(accessible); // This is a equivalent of "this = accessible;"
        super.setAccessible(true);
    }
}
