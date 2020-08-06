package com.fox2code.fabriczero.impl;

import com.fox2code.fabriczero.FabricZeroConfig;
import com.fox2code.fabriczero.FabricZeroPlugin;
import com.fox2code.fabriczero.api.FabricZeroAPI;
import com.fox2code.fabriczero.api.FabricZeroTransformer;
import com.fox2code.fabriczero.reflectutils.ReflectUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.transformers.MixinClassWriter;
import org.spongepowered.asm.transformers.TreeTransformer;

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
    static final boolean REDIRECT_UNSAFE = ReflectUtil.isInternalUnsafe();
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
        FabricLoader.getInstance().getModContainer(mod).ifPresent(modContainer -> {
            ModMetadata modMetadata = modContainer.getMetadata();
            if (!modMetadata.containsCustomValue("modupdater")) {
                ReflectUtil.injectCustomValue(modMetadata, "modupdater",
                        "{\"strategy\": \"curseforge\"," +
                        "\"projectID\": "+ projectId +"}");
            }
        });
    }

    // removing ACC_DEPRECATED is just to reduce the game memory usage
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
        for (FabricZeroTransformer transformer:transformers) {
            if (!name.startsWith(transformer.getClass().getPackage().getName())) {
                transformer.transform(classNode, name);
            }
        }
        boolean dump = (name.startsWith("net.minecraft.") || name.startsWith("com.mojang.")) && isClassDumpingEnabled();
        classNode.access = makePublic(classNode.access);
        if (!dump) classNode.invisibleAnnotations = null;
        for (MethodNode methodNode: classNode.methods) {
            methodNode.access = makePublic(methodNode.access);
            if (!dump) methodNode.invisibleAnnotations = null;
        }
        for (FieldNode fieldNode: classNode.fields) {
            fieldNode.access = makePublic(fieldNode.access);
            if (!dump) fieldNode.invisibleAnnotations = null;
        }
        BytecodeOptimizer.optimize(classNode);
        // Do not recalculate frames on unmodified classes to improve performances
        byte[] classBytes;
        ClassWriter classWriter;
        if (VERIFY_NONE) {
            classNode.access &=~ FLAG_DIRTY;
            // We always parse COMPUTE_MAXS when VERIFY_NONE is enabled to partially replace VM verification
            // In a no-verify env ClassWriter.COMPUTE_FRAMES are not necessary
            classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
        } else if ((classNode.access & FLAG_DIRTY) != 0) {
            classNode.access &=~ FLAG_DIRTY;
            classWriter = new MixinClassWriter(classReader, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
        } else {
            classWriter = new ClassWriter(classReader, 0);
        }
        if (REDIRECT_UNSAFE) {
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
}
