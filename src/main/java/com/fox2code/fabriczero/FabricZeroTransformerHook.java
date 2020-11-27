package com.fox2code.fabriczero;

import com.fox2code.fabriczero.api.FabricZeroAPI;
import com.fox2code.fabriczero.reflectutils.ReflectUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.spongepowered.asm.mixin.transformer.FabricMixinTransformerProxy;
import org.spongepowered.asm.util.asm.ASM;

import java.lang.reflect.Field;

final class FabricZeroTransformerHook extends FabricMixinTransformerProxy {
    private static final ClassVisitor classVisitor = new ClassVisitor(ASM.API_VERSION) {};

    private static final Field field;
    private FabricZeroAPI api;

    static {
        try {
            field = FabricMixinTransformerProxy.class.getDeclaredField("transformer");
        } catch (NoSuchFieldException e) {
            throw new Error(e);
        }
    }

    static FabricZeroTransformerHook hookFrom(FabricMixinTransformerProxy fabricMixinTransformerProxy) throws ReflectiveOperationException {
        FabricZeroTransformerHook transformerHook = ReflectUtil.allocateInstance(FabricZeroTransformerHook.class);
        ReflectUtil.forceSet(transformerHook, field, ReflectUtil.forceGet(fabricMixinTransformerProxy, field));
        return transformerHook.init();
    }

    private FabricZeroTransformerHook init() {
        this.api = FabricZeroAPI.getInstance();
        return this;
    }

    @Override
    public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass) {
        byte[] bytecode = basicClass;
        try {
            bytecode = super.transformClassBytes(name, transformedName, basicClass);
        } catch (Exception exception) {
            try {
                new ClassReader(basicClass).accept(classVisitor, 0);
                FabricZeroPlugin.LOGGER.error("Mixin failed to transform: "+ transformedName, exception);
            } catch (Exception e) {
                FabricZeroPlugin.LOGGER.error("Class: "+ transformedName + " has an invalid class format!", exception);
                return basicClass;
            }
        }
        return api.transformClass(bytecode, name);
    }
}
