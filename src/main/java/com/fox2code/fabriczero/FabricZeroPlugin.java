package com.fox2code.fabriczero;

import com.fox2code.fabriczero.api.FabricZeroAPI;
import com.fox2code.fabriczero.api.FabricZeroTransformer;
import com.fox2code.fabriczero.impl.ModHideMap;
import com.fox2code.fabriczero.reflectutils.ReflectUtil;
import com.fox2code.fabriczero.reflectutils.ReflectedClass;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.FabricMixinTransformerProxy;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unchecked")
public final class FabricZeroPlugin implements IMixinConfigPlugin {
    public static final boolean MOD_UPDATER = FabricLoader.getInstance().isModLoaded("modupdater");
    // It's looks like Graou ate the service.

    public static final Logger LOGGER = LogManager.getLogger("FabricZero");
    private static final Object initLock = new Object();
    private static boolean init;

    static {
        try {
            Field modField = net.fabricmc.loader.FabricLoader.class.getDeclaredField("modMap");
            Map<String, ModContainer> mods = (Map<String, ModContainer>)
                    ReflectUtil.forceGet(FabricLoader.getInstance(), modField);
            mods = new ModHideMap(mods);
            ReflectUtil.forceSet(FabricLoader.getInstance(), modField, mods);
            FabricZeroRules.builtIn();
            FabricMixinTransformerProxy proxy = (FabricMixinTransformerProxy)
                    ReflectedClass.of(FabricLauncherBase.getLauncher().getTargetClassLoader())
                    .get("delegate").get0("mixinTransformer");
            proxy = FabricZeroTransformerHook.hookFrom(proxy);
            ReflectedClass.of(FabricLauncherBase.getLauncher().getTargetClassLoader())
                    .get("delegate").set0("mixinTransformer", proxy);
            LOGGER.info("FabricZero: Loaded!");
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

    public static void ensureInit() {
        // No op just to make "static {}" execute
    }

    @Override
    public void onLoad(String mixinPackage) {
        synchronized (initLock) {
            if (init) {
                return;
            }
            try {
                ReflectedClass processor = ReflectedClass.of(FabricLauncherBase.getLauncher().getTargetClassLoader())
                        .get("delegate").get("mixinTransformer").get("transformer").get("processor");
                // Help compatibility on mixins with incompatible access
                // Since FabricZero make every method public this does not really affect the final loaded bytecode
                processor.get("pendingConfigs").forEach$(mixinConfig ->
                        mixinConfig.get("overwriteOptions").set0("conformAccessModifiers", true));
                processor.get("configs").forEach$(mixinConfig ->
                        mixinConfig.get("overwriteOptions").set0("conformAccessModifiers", true));
                FabricLoader.getInstance().getAllMods().forEach(modContainer -> {
                    ModMetadata modMetadata = modContainer.getMetadata();
                    if (modMetadata.containsCustomValue("fabriczero:transformer")) {
                        String className = null;
                        Class<?> cls;
                        try {
                            className = modMetadata.getCustomValue("fabriczero:transformer").getAsString();
                            cls = Class.forName(className, false, FabricLauncherBase.getLauncher().getTargetClassLoader());
                            if (FabricZeroTransformer.class.isAssignableFrom(cls)) {
                                FabricZeroAPI.getInstance().addTransformer((FabricZeroTransformer) cls.newInstance());
                            } else {
                                LOGGER.error("Couldn't load " + className + " from " + modMetadata.getName() + " because the class doesn't extends FabricZeroTransformer!");
                            }
                        } catch (ClassNotFoundException c) {
                            LOGGER.error("Couldn't load " + className + " from " + modMetadata.getName() + " because the class doesn't exists!");
                        } catch (ClassCastException c) {
                            LOGGER.error(modMetadata.getName() + " declared an invalid transformer property");
                        } catch (IllegalAccessException e) {
                            LOGGER.error("Couldn't load " + className + " from " + modMetadata.getName() + " because the class doesn't have a public constructor!");
                        } catch (InstantiationException e) {
                            LOGGER.error("Couldn't load " + className + " from " + modMetadata.getName() + " because of an unexpected error:", e);
                        }
                    }
                });
            } catch (ReflectiveOperationException e) {
                e.printStackTrace();
            }
            init = true;
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return MOD_UPDATER || !mixinClassName.endsWith("_ModUpdater");
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
