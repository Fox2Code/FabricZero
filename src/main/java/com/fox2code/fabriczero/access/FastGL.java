package com.fox2code.fabriczero.access;

import com.mojang.blaze3d.systems.RenderSystem;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public class FastGL {
    public static final Supplier<Boolean> fallbackStub = () -> true;
    public static final Supplier<Boolean> isOnRenderThread;
    public static final Supplier<Boolean> isOnRenderThreadOrInit;
    public static final Supplier<Boolean> isOnGameThread;
    public static final Supplier<Boolean> isOnGameThreadOrInit;
    public static final Supplier<Boolean> isInInitPhase;

    static {
        Supplier<Boolean> tmp;
        try {
            tmp = RenderSystem::isOnRenderThread;
        } catch (Throwable t) {
            tmp = fallbackStub;
        }
        isOnRenderThread = tmp;
        try {
            tmp = RenderSystem::isOnRenderThreadOrInit;
        } catch (Throwable t) {
            tmp = fallbackStub;
        }
        isOnRenderThreadOrInit = tmp;
        try {
            tmp = RenderSystem::isOnGameThread;
        } catch (Throwable t) {
            tmp = fallbackStub;
        }
        isOnGameThread = tmp;
        try {
            tmp = RenderSystem::isOnGameThreadOrInit;
        } catch (Throwable t) {
            tmp = fallbackStub;
        }
        isOnGameThreadOrInit = tmp;
        try {
            tmp = RenderSystem::isInInitPhase;
        } catch (Throwable t) {
            tmp = fallbackStub;
        }
        isInInitPhase = tmp;
    }
}
