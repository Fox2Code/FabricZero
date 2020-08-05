package com.fox2code.fabriczero;

import com.fox2code.fabriczero.api.FabricZeroAPI;
import com.fox2code.fabriczero.reflectutils.ReflectUtil;
import org.spongepowered.asm.mixin.transformer.FabricMixinTransformerProxy;

import java.lang.reflect.Field;

final class FabricZeroTransformerHook extends FabricMixinTransformerProxy {
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
        return api.transformClass(super.transformClassBytes(name, transformedName, basicClass), name);
    }
}
