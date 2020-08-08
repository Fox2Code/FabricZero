package com.fox2code.fabriczero.access.modupdater;

import com.fox2code.fabriczero.FabricZeroPlugin;
import com.fox2code.fabriczero.reflectutils.ReflectUtil;
import com.thebrokenrail.modupdater.ModUpdater;
import com.thebrokenrail.modupdater.data.ModUpdate;
import net.fabricmc.loader.ModContainer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.SplashScreen;
import net.minecraft.resource.ResourceReloadMonitor;
import net.minecraft.util.Unit;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

final class UpdateAll {
    public static final String REQUIRE_RESTART_SUFFIX = " §6(Require Restart)";
    private static Thread current;
    private static final Set<String> updatedMods = new HashSet<>();

    public static void updateAll() {
        if (current != null && current.getState() != Thread.State.TERMINATED) return;
        final ModUpdate[] modUpdates = getRealUpdates();
        if (modUpdates == null || modUpdates.length == 0) return;
        MinecraftClient client = MinecraftClient.getInstance();
        final UpdateAllMonitor updateAllMonitor = new UpdateAllMonitor();
        client.setOverlay(new SplashScreen(client, updateAllMonitor, throwable -> {}, false));
        (current = new Thread("UpdateAll Thread") {
            @Override
            public void run() {
                try {
                    int current = 1;
                    for (ModUpdate modUpdate:modUpdates) {
                        String modId = getModId(modUpdate.text);
                        File origFile = new File(
                                ((ModContainer)FabricLoader.getInstance().getModContainer(modId).orElseThrow(NullPointerException::new))
                                        .getOriginUrl().getFile());
                        String targetName = modUpdate.downloadURL.substring(modUpdate.downloadURL.lastIndexOf('/'));
                        File targetFile = new File(origFile.getParentFile(), targetName);
                        FabricZeroPlugin.LOGGER.info(
                                "Updating: "+modId+ " ( "+origFile.getName()+" -> "+targetName+" )");
                        try (FileOutputStream fileOutputStream = new FileOutputStream(targetFile)){
                            URL url = new URL(modUpdate.downloadURL);
                            URLConnection urlConnection = url.openConnection();
                            try {
                                urlConnection.setRequestProperty("Upgrade-Insecure-Requests", "1");
                                urlConnection.setRequestProperty("User-Agent", "FabricZero/1.0 ModUpdater/1.1");
                                IOUtils.copy(urlConnection.getInputStream(), fileOutputStream);
                            } finally {
                                IOUtils.closeQuietly(urlConnection.getInputStream());
                            }
                            origFile.deleteOnExit();
                        } catch (IOException ioe) {
                            FabricZeroPlugin.LOGGER.error("Failed to update: "+modId, ioe);
                            targetFile.delete();
                        }
                        if (!modUpdate.text.endsWith(REQUIRE_RESTART_SUFFIX)) {
                            setText(modUpdate, modUpdate.text + REQUIRE_RESTART_SUFFIX);
                        }
                        updatedMods.add(modId);
                        cacheOk = false;
                        updateAllMonitor.setProgress(current, modUpdates.length);
                    }
                } finally {
                    updateAllMonitor.setFinished();
                }
            }
        }).start();
    }

    private static ModUpdate[] cacheFrom;
    private static ModUpdate[] cacheTo;
    private static boolean cacheOk = false;

    @Nullable
    public static ModUpdate[] getRealUpdates() {
        final ModUpdate[] modUpdates = ModUpdater.getUpdates();
        if (modUpdates == cacheFrom && cacheOk) return cacheTo;
        if (modUpdates == null || modUpdates.length == 0) return null;
        ArrayList<ModUpdate> realModUpdates = new ArrayList<>(modUpdates.length);
        for (ModUpdate modUpdate : modUpdates) {
            if (modUpdate.text.endsWith(REQUIRE_RESTART_SUFFIX)) {
                continue;
            }
            String modId = getModId(modUpdate.text);
            if (updatedMods.contains(modId)) {
                setText(modUpdate, modUpdate.text + REQUIRE_RESTART_SUFFIX);
                continue;
            }
            realModUpdates.add(modUpdate);
        }
        ModUpdate[] realModUpdatesArray = realModUpdates.toArray(new ModUpdate[0]);
        cacheFrom = modUpdates;
        cacheTo = realModUpdatesArray;
        cacheOk = true;
        return realModUpdatesArray;
    }

    private static String getModId(String text) {
        return text.substring(text.indexOf('(')+1, text.indexOf(')'));
    }

    private static Field ModUpdate_text;

    private static void setText(ModUpdate modUpdate,String text) {
        try {
            if (ModUpdate_text == null) {
                ModUpdate_text = ModUpdate.class.getDeclaredField("text");
            }
            ReflectUtil.forceSet(modUpdate, ModUpdate_text, text);
        } catch (Exception ignored) {}
    }

    private static class UpdateAllMonitor implements ResourceReloadMonitor {
        private boolean finished;
        private float progress;

        @Override
        public CompletableFuture<Unit> whenComplete() {
            return null;
        }

        @Override
        public float getProgress() {
            return finished ? 1F : progress;
        }

        @Override
        public boolean isPrepareStageComplete() {
            return finished;
        }

        @Override
        public boolean isApplyStageComplete() {
            return finished;
        }

        @Override
        public void throwExceptions() {}

        public void setFinished() {
            this.finished = true;
        }

        public void setProgress(int  current, int max) {
            this.progress = fixUp(((float) current) / ((float) max));
        }

        private static float fixUp(float progress) {
            return MathHelper.clamp((0.1F + progress) / 1.1F, 0.1F, 1F);
        }
    }
}
