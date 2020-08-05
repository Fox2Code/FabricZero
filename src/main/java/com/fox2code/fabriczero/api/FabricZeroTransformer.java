package com.fox2code.fabriczero.api;

import net.fabricmc.api.EnvType;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;

public interface FabricZeroTransformer extends Opcodes {
    boolean DEV = FabricZeroAPI.getInstance().isDev();
    EnvType ENV_TYPE = FabricZeroAPI.getInstance().getEnvType();

    void transform(ClassNode classNode, String name);

    /**
     * Use this method to indicate you modified the code of the class
     */
    default void markDirty(ClassNode classNode) {
        classNode.access |= FabricZeroAPI.FLAG_DIRTY;
    }

    /**
     * This method help to determine if a {@link InvokeDynamicInsnNode} is a lambda
     */
    default boolean isLambda(InvokeDynamicInsnNode dynamicInsnNode) {
        final Handle bsm = dynamicInsnNode.bsm;
        return bsm.getTag() == H_INVOKESTATIC && bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")
                && bsm.getName().equals("metafactory") && bsm.getDesc().equals("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;")
                && dynamicInsnNode.bsmArgs.length >= 2 && dynamicInsnNode.bsmArgs[1] instanceof Handle;
    }
}
