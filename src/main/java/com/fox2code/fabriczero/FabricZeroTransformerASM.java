package com.fox2code.fabriczero;

import com.fox2code.fabriczero.api.FabricZeroAPI;
import net.gudenau.minecraft.asm.api.v1.AsmInitializer;
import net.gudenau.minecraft.asm.api.v1.AsmRegistry;
import net.gudenau.minecraft.asm.api.v1.Identifier;
import net.gudenau.minecraft.asm.api.v1.Transformer;
import org.objectweb.asm.tree.ClassNode;

public class FabricZeroTransformerASM implements AsmInitializer, Transformer {
    private static final Identifier identifier = new Identifier("fabriczero", "main");
    private FabricZeroAPI api;

    @Override
    public void onInitializeAsm() {
        this.api = FabricZeroAPI.getInstance();
        AsmRegistry.getInstance().registerTransformer(this);
    }

    @Override
    public Identifier getName() {
        return identifier;
    }

    @Override
    public boolean handlesClass(String s, String s1) {
        return true;
    }

    @Override
    public boolean transform(ClassNode classNode, Flags flags) {
        if (this.api.transformClassNode(classNode)) {
            flags.requestMaxes();
            flags.requestFrames();
        }
        return true;
    }
}
