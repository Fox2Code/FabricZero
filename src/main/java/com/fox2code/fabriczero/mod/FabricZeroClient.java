package com.fox2code.fabriczero.mod;

import com.fox2code.fabriczero.access.MCResources;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.launch.common.FabricLauncher;
import net.fabricmc.loader.launch.common.FabricLauncherBase;
import net.minecraft.client.MinecraftClient;

import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Environment(EnvType.CLIENT)
public class FabricZeroClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        Thread.currentThread().setPriority(8); // Focus on FPS if client
        MinecraftClient.getInstance().send(System::gc); // Optimize Memory usage
        Thread asyncLoader = new Thread("Async Class PreLoader") {
            @Override
            public void run() {
                FabricZero.LOGGER.info("Preloading classes...");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
                if (this.isInterrupted()) return;
                FabricLauncher fabricLauncher = FabricLauncherBase.getLauncher();
                ClassLoader classLoader = fabricLauncher.getTargetClassLoader();
                try (ZipFile zipFile = new ZipFile(FabricLauncherBase.minecraftJar.toFile())) {
                    Enumeration<? extends ZipEntry> enumeration = zipFile.entries();
                    int count = 0;
                    while (enumeration.hasMoreElements()) {
                        if (this.isInterrupted()) return;
                        String name = enumeration.nextElement().getName();
                        if (name.endsWith(".class")) {
                            name = name.substring(0, name.length()-6).replace('/', '.');
                            try {
                                Class.forName(name, false, classLoader);
                                count++;
                            } catch (ClassNotFoundException ignored) {}
                        }
                    }
                    FabricZero.LOGGER.info("Preloaded "+count+" classes!");
                } catch (IOException ioe) {
                    FabricZero.LOGGER.error("Error during class preload!", ioe);
                }
            }
        };
        asyncLoader.setDaemon(true);
        asyncLoader.start();
        if (!MCResources.successful) {
            FabricZero.LOGGER.warn("Failed to setup vanilla metadata protector!");
        }
    }
}
