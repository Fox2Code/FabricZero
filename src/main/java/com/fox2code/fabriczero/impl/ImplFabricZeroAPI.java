package com.fox2code.fabriczero.impl;

import com.fox2code.fabriczero.FabricZeroConfig;
import com.fox2code.fabriczero.FabricZeroPlugin;
import com.fox2code.fabriczero.api.FabricZeroAPI;
import com.fox2code.fabriczero.api.FabricZeroTransformer;
import com.fox2code.fabriczero.reflectutils.ReflectUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModDependency;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;
import org.spongepowered.asm.transformers.MixinClassWriter;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.*;

public final class ImplFabricZeroAPI implements FabricZeroAPI, Opcodes {
    static {
        FabricZeroPlugin.ensureInit();
    }
    private static final Object transformLock = new Object();

    public static final FabricZeroAPI INSTANCE = new ImplFabricZeroAPI();
    static final boolean DEV = FabricLauncherBase.getLauncher().isDevelopment();
    static final boolean REDIRECT_UNSAFE = ReflectUtil.isInternalUnsafe() && !FabricZeroConfig.disableUnsafeRedirect;
    static final Remapper REDIRECT_UNSAFE_REMAPPER = new Remapper() {
        @Override
        public String map(String internalName) {
            return internalName.equals("sun/misc/Unsafe")?
                    "jdk/internal/misc/Unsafe":internalName;
        }
    };
    static final EnvType ENV_TYPE = FabricLauncherBase.getLauncher().getEnvironmentType();
    static final boolean VERIFY_NONE =
            ManagementFactory.getRuntimeMXBean().getInputArguments().contains("-Xverify:none");
    private final Map<String, List<String>> hidingRules = new HashMap<>();
    private final List<FabricZeroTransformer> transformers = new LinkedList<>();
    private File configFile;
    private File dumpDir;
    private Boolean dumpClasses;
    private Boolean disableAccessMod;

    @Override
    public boolean isHidden(String mod, String cls) {
        List<String> rules = hidingRules.get(mod);
        if (rules != null) {
            for (String pkg:rules) {
                if (cls.startsWith(pkg)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasHidingRules(String mod) {
        return hidingRules.get(mod) != null;
    }

    @Override
    public void hideMod(String mod, String pkg) {
        hidingRules.computeIfAbsent(mod, k -> new LinkedList<>()).add(pkg);
    }

    @Override
    public void addCurseProjectId(String mod,final int projectId) {
        ModContainer container = FabricLoader.getInstance().getModContainer(mod).orElse(null);
        if (container == null) return;
        boolean strict = false;
        for (ModDependency modDependency : container.getMetadata().getDepends()) {
            if (modDependency.getModId().equals("minecraft")) {
                strict = true;
                break;
            }
        }
        addCurseProjectId(mod, projectId, strict);
    }

    @Override
    public void addCurseProjectId(String mod,final int projectId,final boolean strict) {
        FabricLoader.getInstance().getModContainer(mod).ifPresent(modContainer -> {
            ModMetadata modMetadata = modContainer.getMetadata();
            if (!modMetadata.containsCustomValue("modupdater")) {
                ReflectUtil.injectCustomValue(modMetadata, "modupdater",
                        "{\"strategy\": \"curseforge\"," +
                        "\"projectID\": "+ projectId + "," +
                        "\"strict\": "+strict+"}");
            }
        });
    }

    // removing ACC_DEPRECATED is just to reduce potential the game memory usage
    private static final int MASK = ~(ACC_PRIVATE|ACC_PROTECTED|ACC_DEPRECATED);

    private static int makePublic(int access) {
        return access&MASK|ACC_PUBLIC;
    }

    @Override
    public byte[] transformClass(byte[] bytecode, String name) {
        if (bytecode == null) return null;
        if (name.startsWith("com.fox2code.fabriczero.")) return bytecode;
        ClassReader classReader = new ClassReader(bytecode);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, ClassReader.EXPAND_FRAMES);
        boolean minecraft = name.startsWith("net.minecraft.");
        boolean dump = (minecraft || name.startsWith("com.mojang.")) && isClassDumpingEnabled();
        boolean dirty = this.transformClassNode(classNode);
        // Do not recalculate frames on unmodified classes to improve performances
        byte[] classBytes;
        ClassWriter classWriter;
        if (VERIFY_NONE) {
            // We always parse COMPUTE_MAXS when VERIFY_NONE is enabled to partially replace VM verification
            // In a no-verify env ClassWriter.COMPUTE_FRAMES are not necessary
            classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
        } else if (dirty) {
            classWriter = new MixinClassWriter(classReader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        } else {
            classWriter = new ClassWriter(classReader, 0);
        }
        if (REDIRECT_UNSAFE && !classNode.name.startsWith("net/gudenau/minecraft/asm/")) {
            classNode.accept(new ClassRemapper(classWriter, REDIRECT_UNSAFE_REMAPPER));
        } else {
            classNode.accept(classWriter);
        }

        classBytes = classWriter.toByteArray();
        if (dump) {
            if (dumpDir == null) {
                synchronized (transformLock) {
                    if (dumpDir == null) {
                        dumpDir = new File(
                                getFabricZeroConfigFile().getParentFile(),
                                "fabriczero_dump");
                        if (dumpDir.isFile()) {
                            dumpDir.delete();
                        }
                        dumpDir.mkdirs();
                    }
                }
            }
            File file = new File(dumpDir, name.replace(".","/")+".class");
            if (file.getParentFile().isDirectory() || file.getParentFile().mkdirs()) {
                try {
                    Files.write(file.toPath(), classBytes);
                } catch (IOException ignored) {}
                return classBytes;
            } else {
                FabricZeroPlugin.LOGGER.error("Failed to create: "+file.getParentFile().getPath());
            }
        }
        return classBytes;
    }

    private static final String DCT_DESC_OLD = "(Lnet/minecraft/class_4587;Lnet/minecraft/class_327;Lnet/minecraft/class_5348;III)V";
    private static final String DCT_DESC_NEW = "(Lnet/minecraft/class_4587;Lnet/minecraft/class_327;Lnet/minecraft/class_2561;III)V";

    @Override
    public boolean transformClassNode(ClassNode classNode) {
        String name = classNode.name.replace('/', '.');
        for (FabricZeroTransformer transformer:transformers) {
            if (!name.startsWith(transformer.getClass().getPackage().getName())) {
                transformer.transform(classNode, name);
            }
        }
        boolean minecraft = name.startsWith("net.minecraft.");
        boolean dump = (minecraft || name.startsWith("com.mojang.")) && isClassDumpingEnabled();
        boolean accMod = !isAccessModDisabled();
        if (accMod) classNode.access = makePublic(classNode.access);
        if (!dump) classNode.invisibleAnnotations = null;
        for (MethodNode methodNode: classNode.methods) {
            if (accMod) methodNode.access = makePublic(methodNode.access);
            if (!dump) methodNode.invisibleAnnotations = null;
        }
        for (FieldNode fieldNode: classNode.fields) {
            if (minecraft && accMod) fieldNode.access = makePublic(fieldNode.access); // Fix https://github.com/Fox2Code/FabricZero/issues/4
            if (!dump) fieldNode.invisibleAnnotations = null;
        }
        if (classNode.name.equals("net/minecraft/class_332")) {
            MethodNode drawCenteredText = null;
            for (MethodNode methodNode:classNode.methods) {
                if (methodNode.name.equals("method_27534")) {
                    drawCenteredText = methodNode;
                    break;
                }
            }
            if (drawCenteredText != null && (drawCenteredText.desc.equals(DCT_DESC_OLD)
                    || drawCenteredText.desc.equals(DCT_DESC_NEW))) {
                String[] methods = new String[]{"method_27534", "method_27535"};
                boolean modernize = drawCenteredText.desc.equals(DCT_DESC_OLD);
                for (String method : methods) {
                    MethodNode methodNode = new MethodNode(ACC_PUBLIC | (modernize ? ACC_STATIC : 0),
                            method, modernize ? DCT_DESC_NEW : DCT_DESC_OLD, null, null);
                    int i = modernize ? 0 : 1;
                    if (modernize) {
                        methodNode.instructions.add(new FieldInsnNode(GETSTATIC,
                                "com/fox2code/fabriczero/access/Helper",
                                "drawableHelper", "Lnet/minecraft/class_332;"));
                    }
                    methodNode.instructions.add(new VarInsnNode(ALOAD, i++));
                    methodNode.instructions.add(new VarInsnNode(ALOAD, i++));
                    methodNode.instructions.add(new VarInsnNode(ALOAD, i++));
                    methodNode.instructions.add(new TypeInsnNode(CHECKCAST,
                            modernize ? "net/minecraft/class_2561" : "net/minecraft/class_5348"));
                    methodNode.instructions.add(new VarInsnNode(ILOAD, i++));
                    methodNode.instructions.add(new VarInsnNode(ILOAD, i++));
                    methodNode.instructions.add(new VarInsnNode(ILOAD, i));
                    methodNode.instructions.add(new MethodInsnNode(modernize ? INVOKEVIRTUAL : INVOKESTATIC,
                            "net/minecraft/class_332", method, modernize ? DCT_DESC_OLD : DCT_DESC_NEW, false));
                    methodNode.instructions.add(new InsnNode(RETURN));
                    classNode.methods.add(methodNode);
                }
            }
            classNode.access |= FLAG_DIRTY;
        }
        BytecodeOptimizer.optimize(classNode);
        boolean dirty = (classNode.access & FLAG_DIRTY) != 0;
        if (dirty) classNode.access &=~ FLAG_DIRTY;
        return dirty;
    }

    @Override
    public void addTransformer(FabricZeroTransformer transformer) {
        transformers.add(transformer);
    }

    @Override
    public boolean isDev() {
        return DEV;
    }

    @Override
    public EnvType getEnvType() {
        return ENV_TYPE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public File getFabricZeroConfigFile() {
        if (configFile == null) {
            if (BytecodeOptimizer.redirectGetConfigDir) {
                configFile = new File(FabricLoader.getInstance()
                        .getConfigDirectory(), "fabriczero.cfg");
            } else {
                configFile = FabricLoader.getInstance().getConfigDir()
                        .resolve("fabriczero.cfg").toFile();
            }
        }
        return configFile;
    }

    @Override
    public boolean isClassDumpingEnabled() {
        if (dumpClasses == null) {
            dumpClasses = FabricZeroConfig.dumpClasses;
        }
        return dumpClasses;
    }

    @Override
    public boolean isAccessModDisabled() {
        if (disableAccessMod == null) {
            disableAccessMod = FabricZeroConfig.disableAccessMod;
        }
        return disableAccessMod;
    }
}
