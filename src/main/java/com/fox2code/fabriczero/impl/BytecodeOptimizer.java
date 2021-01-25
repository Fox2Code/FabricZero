package com.fox2code.fabriczero.impl;

import com.fox2code.fabriczero.FabricZeroConfig;
import com.fox2code.fabriczero.api.FabricZeroAPI;
import com.fox2code.fabriczero.reflectutils.ReflectUtil;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.lang.reflect.Field;

final class BytecodeOptimizer implements Opcodes {
    static Field opcodeField;
    static boolean redirectGetConfigDir;
    static boolean redirectPrepareModInit;
    private static final boolean replaceStringRedirect = !FabricZeroConfig.disableStringRedirect;

    static {
        try {
            opcodeField = AbstractInsnNode.class.getDeclaredField("opcode");
        } catch (NoSuchFieldException ignored) {}
        try {
            FabricLoader.class.getDeclaredMethod("getConfigDir");
            redirectGetConfigDir = false;
        } catch (NoSuchMethodException e) {
            redirectGetConfigDir = true;
        }
        try {
            //noinspection JavaReflectionMemberAccess
            net.fabricmc.loader.FabricLoader.class.getDeclaredMethod("prepareModInit", File.class, Object.class);
            redirectPrepareModInit = false;
        } catch (NoSuchMethodException e) {
            redirectPrepareModInit = true;
        }
    }

    static void optimize(ClassNode classNode) {
        final boolean mathHelper = classNode.name.equals("net/minecraft/class_3532")
                || classNode.name.equals("net/minecraft/util/math/MathHelper");
        final boolean serverChunkManager = classNode.name.equals("net/minecraft/class_3215")
                || classNode.name.equals("net/minecraft/server/world/ServerChunkManager");
        final boolean emcLoader =
                classNode.name.startsWith("me/deftware/client/framework/main/bootstrap/");
        final boolean emcMarketAPI =
                classNode.name.equals("emc/marketplace/api/MarketplaceAPI");
        for (MethodNode methodNode:classNode.methods) {
            for (AbstractInsnNode insnNode : methodNode.instructions.toArray()) {
                AbstractInsnNode previous = insnNode.getPrevious();
                switch (insnNode.getOpcode()) {
                    case INVOKEVIRTUAL: {
                        MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                        String owner = methodInsnNode.owner;
                        String name = methodInsnNode.name;
                        String descriptor = methodInsnNode.desc;
                        if (redirectGetConfigDir && owner.equals("net/fabricmc/loader/api/FabricLoader")) {
                            switch (name) {
                                case "getGameDir":
                                case "getConfigDir":
                                    methodInsnNode.owner = "com/fox2code/fabriczero/access/FabricBackport";
                                    methodInsnNode.desc = methodInsnNode.desc.replace(
                                            "(", "(Lnet/fabricmc/loader/api/FabricLoader;");
                                    setOpcode(methodInsnNode, INVOKESTATIC);
                                default:
                            }
                        } else if (redirectPrepareModInit && owner.equals("net/fabricmc/loader/FabricLoader")) {
                            if (name.equals("prepareModInit") && descriptor.equals("(Ljava/io/File;Ljava/lang/Object;)V")) {
                                methodInsnNode.owner = "com/fox2code/fabriczero/access/FabricBackport";
                                methodInsnNode.desc = "(Lnet/fabricmc/loader/FabricLoader;Ljava/io/File;Ljava/lang/Object;)V";
                                setOpcode(methodInsnNode, INVOKESTATIC);
                            }
                        } else if (replaceStringRedirect && owner.equals("java/lang/String")
                                && !classNode.name.startsWith("org/apache/commons/") && !descriptor.equals("(CC)Ljava/lang/String;")
                                && (name.equals("replace") || name.equals("replaceFirst") || name.equals("replaceAll"))) {
                            setOpcode(methodInsnNode, INVOKESTATIC);
                            methodInsnNode.desc = "(Ljava/lang/String;" + descriptor.substring(1);
                            methodInsnNode.owner = name.equals("replace")
                                    ? "com/fox2code/fabriczero/access/FastString"
                                    : "org/apache/commons/lang3/StringUtils";
                        }
                    }
                        break;
                    case INVOKESTATIC:
                        if (emcLoader) {
                            MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                            if (methodInsnNode.owner.equals("java/net/URLClassLoader")
                                    && methodInsnNode.name.equals("newInstance")) {
                                methodInsnNode.owner = "com/fox2code/fabriczero/access/emc/EMCCompact";
                            }
                        }
                        break;
                    case INVOKEDYNAMIC:
                        InvokeDynamicInsnNode dynamicInsnNode = (InvokeDynamicInsnNode) insnNode;
                        if (isLambda(dynamicInsnNode)) {
                            Handle handle = (Handle) dynamicInsnNode.bsmArgs[1];
                            /* Note: com/mojang/blaze3d/systems/RenderSystem
                             * is both user and dev env class name */
                            if (handle.getOwner().equals("com/mojang/blaze3d/systems/RenderSystem")) {
                                switch (handle.getName()) {
                                    case "isOnRenderThread":
                                    case "isOnRenderThreadOrInit":
                                    case "isOnGameThread":
                                    case "isOnGameThreadOrInit":
                                    case "isInInitPhase":
                                        methodNode.instructions.insertBefore(insnNode,
                                                new FieldInsnNode(GETSTATIC, "com/fox2code/fabriczero/access/FastGL",
                                                        handle.getName(), "Ljava/util/function/Supplier;"));
                                        methodNode.instructions.remove(insnNode);
                                        markDirty(classNode);
                                }
                            }
                        }
                        break;
                    case Opcodes.LDC:
                        if (serverChunkManager) {
                            LdcInsnNode ldcInsnNode = (LdcInsnNode) insnNode;
                            if (ldcInsnNode.cst instanceof Integer) {
                                switch ((Integer) ldcInsnNode.cst) {
                                    default:
                                        break;
                                    case 4:
                                        if (methodNode.name.equals("<init>") || methodNode.desc.startsWith("(II")) {
                                            ldcInsnNode.cst = 32;
                                        }
                                        break;
                                    case 3:
                                        if (methodNode.name.equals("putInCache") || methodNode.name.equals("method_21738")) {
                                            ldcInsnNode.cst = 31;
                                        }
                                        break;
                                }
                            }
                        }
                        break;
                    case DDIV:
                        if (previous.getType() == AbstractInsnNode.LDC_INSN && (Double) ((LdcInsnNode) insnNode.getPrevious()).cst == 2.0D) {
                            setOpcode(insnNode, DMUL);
                            ((LdcInsnNode) insnNode.getPrevious()).cst = 0.5D;
                        }
                        break;
                    case FDIV:
                        if (previous.getType() == AbstractInsnNode.LDC_INSN && (
                                previous.getOpcode() == FCONST_2 || (Float) ((LdcInsnNode) previous).cst == 2.0F)) {
                            setOpcode(insnNode, FMUL);
                            if (previous.getOpcode() == FCONST_2) {
                                methodNode.instructions.insertBefore(previous, new LdcInsnNode(0.5F));
                                methodNode.instructions.remove(previous);
                            } else {
                                ((LdcInsnNode) previous).cst = 0.5F;
                            }
                        }
                        break;
                    case F2D:
                        if (previous.getOpcode() == D2F) {
                            methodNode.instructions.remove(previous);
                            methodNode.instructions.remove(insnNode);
                        }
                        break;
                    case D2F:
                        if (previous.getOpcode() == F2D) {
                            methodNode.instructions.remove(previous);
                            methodNode.instructions.remove(insnNode);
                        }
                        break;
                    case POP:
                        if (previous.getOpcode() == DUP) {
                            methodNode.instructions.remove(previous);
                            methodNode.instructions.remove(insnNode);
                        }
                        break;
                    case POP2:
                        if (previous.getOpcode() == DUP2) {
                            methodNode.instructions.remove(previous);
                            methodNode.instructions.remove(insnNode);
                        }
                        break;
                    case INVOKESPECIAL:
                        if (emcMarketAPI) {
                            MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                            if (methodInsnNode.owner.equals("java/util/HashMap")
                                    && methodInsnNode.name.equals("<init>")) {
                                methodInsnNode.owner = "com/fox2code/fabriczero/access/emc/EMCHashMap";
                            }
                        }
                        break;
                    case NEW:
                        if (emcMarketAPI) {
                            TypeInsnNode methodInsnNode = (TypeInsnNode) insnNode;
                            if (methodInsnNode.desc.equals("java/util/HashMap")) {
                                methodInsnNode.desc = "com/fox2code/fabriczero/access/emc/EMCHashMap";
                            }
                        }
                        break;
                }
            }
        }
        if (emcMarketAPI) markDirty(classNode);
    }

    private static void setOpcode(AbstractInsnNode insnNode,int opcode) {
        try {
            ReflectUtil.forceSet(insnNode, opcodeField, opcode);
        } catch (ReflectiveOperationException ignored) {}
    }

    private static void markDirty(ClassNode classNode) {
        classNode.access |= FabricZeroAPI.FLAG_DIRTY;
    }

    private static boolean isLambda(InvokeDynamicInsnNode dynamicInsnNode) {
        final Handle bsm = dynamicInsnNode.bsm;
        return bsm.getTag() == H_INVOKESTATIC && bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory")
                && bsm.getName().equals("metafactory") && bsm.getDesc().equals("(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;")
                && dynamicInsnNode.bsmArgs.length >= 2 && dynamicInsnNode.bsmArgs[1] instanceof Handle;
    }
}
