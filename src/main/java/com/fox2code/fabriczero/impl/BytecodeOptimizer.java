package com.fox2code.fabriczero.impl;

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
        /* final boolean glStateManager = classNode.name.equals("net/minecraft/class_4493")
                || classNode.name.equals("com/mojang/blaze3d/platform/GlStateManager")
                || classNode.name.equals("com/mojang/blaze3d/systems/RenderSystem"); */
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
                        InsnList insnList = inline0((MethodInsnNode) insnNode, mathHelper);
                        if (insnList != null) {
                            methodNode.instructions.insertBefore(insnNode, insnList);
                            methodNode.instructions.remove(insnNode);
                            markDirty(classNode); // We touched it so we must call this method
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
                        if (mathHelper) {
                            LdcInsnNode ldcInsnNode = (LdcInsnNode) insnNode;
                            if (ldcInsnNode.cst instanceof Integer) {
                                switch ((Integer) ldcInsnNode.cst) {
                                    default:
                                        break;
                                    case 65536:
                                        ldcInsnNode.cst = 32768;
                                        break;
                                    case 65535:
                                        ldcInsnNode.cst = 32767;
                                }

                            } else if (ldcInsnNode.cst instanceof Double && (Double) ldcInsnNode.cst == 65536.0) {
                                ldcInsnNode.cst = 32768.0;
                            } else if (ldcInsnNode.cst instanceof Float) {
                                float d = (Float) ldcInsnNode.cst;
                                if (d == 65536F) {
                                    ldcInsnNode.cst = 32768F;
                                } else if (d == 10430.378F) {
                                    ldcInsnNode.cst = 5215.189F;
                                } else if (d == 16384.0F) {
                                    ldcInsnNode.cst = 8192.0F;
                                }
                            }
                        } else if (serverChunkManager) {
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
                    case IDIV: {
                        switch (previous.getOpcode()) {
                            case ICONST_2:
                                setOpcode(previous, ICONST_1);
                                setOpcode(insnNode, ISHR);
                                break;
                            case ICONST_4:
                                setOpcode(previous, ICONST_2);
                                setOpcode(insnNode, ISHR);
                                break;
                        }
                    }
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

    private static InsnList inline0(MethodInsnNode methodInsnNode, boolean mathHelper) {

        final String owner = methodInsnNode.owner;
        final String name = methodInsnNode.name;
        final String descriptor = methodInsnNode.desc;
        final boolean mathHelperCall = owner.equals("net/minecraft/class_3532")
                || owner.equals("net/minecraft/util/math/MathHelper");
        if (!owner.equals("java/lang/Math") && !owner.equals("java/lang/StrictMath") && !mathHelperCall) {
            return null;
        }
        if (owner.equals("java/lang/Math") && (name.equals("sqrt") || name.equals("sin")
                || name.equals("cos") || name.equals("asin") || name.equals("acos"))) {
            methodInsnNode.owner = (name.equals("sin") || name.equals("cos")) && !mathHelper ?
                    "com/fox2code/fabriczero/access/FastMath" : "java/lang/StrictMath";
            return null;
        }
        InsnList insns = new InsnList();
        if (descriptor.indexOf('I') != -1) {
            switch (name) {
                default:
                    return null;
                case "method_15382":
                    if (!mathHelperCall) {
                        return null;
                    }
                case "abs": {
                    LabelNode label = new LabelNode();
                    insns.add(new InsnNode(DUP));
                    insns.add(new JumpInsnNode(IFGE, label));
                    insns.add(new InsnNode(INEG));
                    insns.add(label);
                    break;
                }
                case "max": {
                    LabelNode label = new LabelNode();
                    insns.add(new InsnNode(DUP2));
                    insns.add(new JumpInsnNode(IF_ICMPGE, label));
                    insns.add(new InsnNode(SWAP));
                    insns.add(label);
                    insns.add(new InsnNode(POP));
                    break;
                }
                case "min": {
                    LabelNode label = new LabelNode();
                    insns.add(new InsnNode(DUP2));
                    insns.add(new JumpInsnNode(IF_ICMPLE, label));
                    insns.add(new InsnNode(SWAP));
                    insns.add(label);
                    insns.add(new InsnNode(POP));
                    break;
                }
            }
        } else if (descriptor.indexOf('D') != -1) {
            switch (name) {
                default:
                    return null;
                case "toRadians":
                    insns.add(new LdcInsnNode(180D));
                    insns.add(new InsnNode(DDIV));
                    insns.add(new LdcInsnNode(Math.PI));
                    insns.add(new InsnNode(DMUL));
                    break;
                case "toDegrees":
                    insns.add(new LdcInsnNode(180D));
                    insns.add(new InsnNode(DMUL));
                    insns.add(new LdcInsnNode(Math.PI));
                    insns.add(new InsnNode(DDIV));
                    break;
                case "abs": {
                    LabelNode label = new LabelNode();
                    insns.add(new InsnNode(DUP2));
                    insns.add(new InsnNode(DCONST_0));
                    insns.add(new InsnNode(DCMPG));
                    insns.add(new JumpInsnNode(IFGE, label));
                    insns.add(new InsnNode(DNEG));
                    insns.add(label);
                    break;
                }
            }
        } else if (descriptor.indexOf('F') != -1) {
            switch (name) {
                default:
                    return null;
                case "method_15379":
                    if (!mathHelperCall) {
                        return null;
                    }
                case "abs": {
                    LabelNode label = new LabelNode();
                    insns.add(new InsnNode(DUP));
                    insns.add(new InsnNode(FCONST_0));
                    insns.add(new InsnNode(FCMPG));
                    insns.add(new JumpInsnNode(IFGE, label));
                    insns.add(new InsnNode(FNEG));
                    insns.add(label);
                    break;
                }
                case "max": {
                    LabelNode label = new LabelNode();
                    insns.add(new InsnNode(DUP2));
                    insns.add(new InsnNode(FCMPL));
                    insns.add(new JumpInsnNode(IFGE, label));
                    insns.add(new InsnNode(SWAP));
                    insns.add(label);
                    insns.add(new InsnNode(POP));
                    break;
                }
                case "min": {
                    LabelNode label = new LabelNode();
                    insns.add(new InsnNode(DUP2));
                    insns.add(new InsnNode(FCMPL));
                    insns.add(new JumpInsnNode(IFLE, label));
                    insns.add(new InsnNode(SWAP));
                    insns.add(label);
                    insns.add(new InsnNode(POP));
                    break;
                }
                case "square":
                case "method_27285":
                    if (!mathHelperCall) {
                        return null;
                    }
                    insns.add(new InsnNode(DUP));
                    insns.add(new InsnNode(FMUL));
                    break;
            }
        } else {
            return null;
        }
        return insns;
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
